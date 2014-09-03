package infer.servlets.resource;

import infer.util.LoadAboxWebService;
import infer.util.SolrUtil;
import infer.util.Util;
import infer.util.abox.LoadLocation;
import infer.util.abox.LoadLocationPerPerson;
import infer.util.generated.AutoIE;
import infer.util.generated.Vcard;
import infer.util.rules.AllValue;
import infer.util.rules.CalLocDest;
import infer.util.rules.CalTextDest;
import infer.util.rules.GEOPlace;
import infer.util.rules.GEOPoint;
import infer.util.rules.Like;
import infer.util.rules.Poi;
import infer.util.rules.PointToSegment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.DatatypeConverter;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.builtins.NoValue;

/**
 * 
 * @author Scott Streit
 * 
 *         This class contains the static methods necessary to access solr
 *         through intercepting calls and adding new calls.
 * 
 */
@Path("listTracksNextEvent")
@Produces(MediaType.APPLICATION_JSON)
public class ListTracksNextEvent{
	final static Logger log = Logger.getLogger(ListTracksNextEvent.class.getName());

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost(@QueryParam("name") String name, 
					@QueryParam("email") String email,
					@DefaultValue("39.338444") @QueryParam("lat") String lat,
					@DefaultValue("-77.08884") @QueryParam("lng") String lng,
					@DefaultValue("1") @QueryParam("poisPerPoint") int poiLimit,
					@DefaultValue("1") @QueryParam("iter") int iteration
					)
			throws ServletException, IOException {
		return processTrackPoints(name,email,lat,lng,poiLimit,iteration);

	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doGet(@QueryParam("name") String name, 
			@QueryParam("email") String email,
			@DefaultValue("39.338444") @QueryParam("lat") String lat,
			@DefaultValue("-77.08884") @QueryParam("lng") String lng,
			@DefaultValue("1") @QueryParam("poisPerPoint") int poiLimit,
			@DefaultValue("1") @QueryParam("iter") int iteration
			)
			throws ServletException, IOException {

		return processTrackPoints(name,email,lat,lng,poiLimit,iteration);

	}

	private JsonObject processTrackPoints(String name,String email,String lat, String lng,int poiLimit,int iteration) {

		
		
		BuiltinRegistry.theRegistry.register( new AllValue() );
		BuiltinRegistry.theRegistry.register( new Like() );
		BuiltinRegistry.theRegistry.register( new Poi() );
		BuiltinRegistry.theRegistry.register(new CalLocDest());
		BuiltinRegistry.theRegistry.register(new CalTextDest());
		BuiltinRegistry.theRegistry.register(new NoValue());
		BuiltinRegistry.theRegistry.register(new PointToSegment());
		
		try {

			Map<String, JsonArray> m;
			JsonObject result = null;
			JsonArrayBuilder pois = Json.createArrayBuilder();
			
			
			log.fine(" name = [" + name + "]");
			log.fine(" email = [" + email + "]");
			log.fine(" lat = [" + lat + "]");
			log.fine(" lng = [" + lng + "]");
			
			
			List rules = Rule.rulesFromURL("rules.txt");
			Reasoner reasoner = new GenericRuleReasoner(rules);

			Model ontModel = ModelFactory.createDefaultModel();
			InfModel model = ModelFactory.createInfModel(reasoner, ontModel);
			
			//Get the Person and Car
			String uri = Util.getPerson(name, email);
			Individual person = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
					+ uri);
			Individual auto;
			SolrDocumentList carDocs = SolrUtil.getDocsbySPO(person.toString(),AutoIE.operates.toString(), null,
					1);
			if(carDocs.size()>0){
				SolrDocument carDoc = carDocs.get(0);
				String autoString = (String) carDoc.getFieldValue("object_t");
				SolrDocumentList VINDocs = SolrUtil.getDocsbySPO(autoString,AutoIE.VIN.toString(), null,
						1);
				if(VINDocs.size()>0){
					SolrDocument VINDoc = VINDocs.get(0);
					String VIN = (String) VINDoc.getFieldValue("object_t");	
					auto = AutoIE.Automobile.createIndividual(AutoIE.Automobile
							.getURI() + VIN);
				}	
				else{
					auto = LoadLocation.createCar(model, name, email, "GM", "Saturn", "2005", "12345", 
						DatatypeConverter.printDateTime(new GregorianCalendar()),
						"white", "sedan", "IE", "driver", "FAKE CAR");
				}
			}
			else{
				auto = LoadLocation.createCar(model, name, email, "GM", "Saturn", "2005", "12345", 
						DatatypeConverter.printDateTime(new GregorianCalendar()),
						"white", "sedan", "IE", "driver", "FAKE CAR");
			}
			
			
			
			//Get the Event
			SolrDocumentList eventDocs = SolrUtil.getDocsbySPO(person.toString(),AutoIE.personCommitsToEvent.toString(), null,
						2);
		
			
			
			int index=1;
			if(eventDocs.size()>0){
				if(iteration>1){
					index=0;
					log.fine("SECOND EVENT");
					lat="39.2923803";
					lng="-76.6135388";
				}
				SolrDocument eventDoc = eventDocs.get(index);
				String event = (String) eventDoc.getFieldValue("object_t");
				SolrDocumentList destDocs = SolrUtil.getDocsbySPO(event,AutoIE.eventHasAsADestination.toString(), null,1);
				if(destDocs.size()>0){
					SolrDocument destDoc = destDocs.get(0);
					
					String dest  = (String) destDoc.getFieldValue("object_t");
					SolrDocumentList lngDocs = SolrUtil.getDocsbySPO(dest,Vcard.longitude.toString(), null,1);
					SolrDocumentList latDocs = SolrUtil.getDocsbySPO(dest,Vcard.latitude.toString(), null,1);
					log.fine("eventlng:"+lngDocs.get(0).toString());
					log.fine("eventlat:"+latDocs.get(0).toString());
					

					if(lngDocs.size()>0 && latDocs.size()>0){
						SolrDocument lngDoc = lngDocs.get(0);
						SolrDocument latDoc = latDocs.get(0);
						String eventLng  = (String) lngDoc.getFieldValue("object_t");
						String eventLat  = (String) latDoc.getFieldValue("object_t");
						log.fine("eventlng: " + eventLng);
						log.fine("eventLat: " + eventLat);
						byte[] ba = Util
								.query("https://maps.googleapis.com/maps/api/directions/json?origin="
										+ lat
										+ ","
										+ lng
										+ "&destination="+eventLat+","+eventLng+"&key=AIzaSyBq3o8JdB4Kp57915CrABXrNc8HfDSpj6c");

						ByteArrayInputStream bais = new ByteArrayInputStream(ba);
						Reader r = new InputStreamReader(bais);
						try {
							JsonReader reader = Json.createReader(r);
							JsonObject jsonString = reader.readObject();

							
							//Create Route and TrackPoints
							Individual route = AutoIE.Route.createIndividual(AutoIE.Route.getURI()
									+ System.currentTimeMillis());

							model.add(route, AutoIE.routeStatus, "INFERRED");
							model.add(auto, AutoIE.drives, route);
							model.add(route, AutoIE.isDrivenBy, auto);

							model.add(person, AutoIE.driverDrivesARoute, route);
							model.add(route, AutoIE.routeHasAsADriver, person);
							
							
							String plat="0";
							String plon="0";
					
							JsonArray routes = jsonString.getJsonArray("routes");
							JsonObject route1 = routes.getJsonObject(0);
							JsonArray legs = route1.getJsonArray("legs");
							JsonObject leg0 = legs.getJsonObject(0);
							
							JsonArrayBuilder points = Json.createArrayBuilder();
							points.add(leg0.getJsonObject("start_location"));
							log.fine("Origin before calendar event: "+leg0);
							JsonArray steps = leg0.getJsonArray("steps");
							
							
							for(int i=0;i<steps.size();i++){
								points.add(steps.get(i));
								log.fine("point to calendar event: "+steps.getJsonObject(i));
								JsonObject step = steps.getJsonObject(i);
								JsonObject end_loc = step.getJsonObject("end_location");
								JsonObject start_loc = step.getJsonObject("start_location");
								String curlat = end_loc.getJsonNumber("lat").toString();
								String curlng = end_loc.getJsonNumber("lng").toString();
								String prevlat = start_loc.getJsonNumber("lat").toString();
								String prevlng = start_loc.getJsonNumber("lng").toString();
								double curlat_double=Double.parseDouble(curlat);
								double curlng_double=Double.parseDouble(curlng);
								double plat_double=Double.parseDouble(prevlat);
								double plng_double=Double.parseDouble(prevlng);
								
								log.fine("distance lat_new: "+(curlat_double-plat_double));
								log.fine("distance lng_new: "+(curlng_double-plng_double));
								double dist_lat=curlat_double-plat_double;
								double dist_lng=curlng_double-plng_double;
								double slope=dist_lng/dist_lat;
								
								double newLat = curlat_double;
								double newLng = curlng_double;
								double dist_lat_count=0;
								double dist_lng_count=0;
								double divide = 5.0;
								if(Math.abs(dist_lat)>.075 || Math.abs(dist_lng)>.075){
									for(int k=0;k<5;k++){
										newLat=newLat-((dist_lat)/divide);
										newLng=newLng-((dist_lng)/divide);
										
										//Gathering and adding POIs (not currently being stored in SOLR).
										GEOPoint gpoint = new GEOPoint();
										gpoint.setLat(Double.toString(newLat));
										gpoint.setLng(Double.toString(newLng));
										List<GEOPlace> places = new ArrayList<GEOPlace>();
										log.fine("TEST: " +curlat+","+curlng+" and dist " + dist_lat +","+dist_lng+" and point is "+ gpoint.toString());
										places = SolrUtil.getPOIS(person.toString(), gpoint);
										if(places!=null){
											
											if(!places.isEmpty() && places.size()>poiLimit){
												places = places.subList(0, poiLimit);
											}
											
											for(int j=0;j<places.size();j++){	
												JsonObjectBuilder poi = Json.createObjectBuilder();
												GEOPlace gp = places.get(j);
												GEOPoint geo = gp.getPoint();
												if (geo != null) {
													poi.add("lat", geo.getLat());
													poi.add("lng", geo.getLng());
												}
												poi.add("streetAddress", gp.getStreetAddress());
												poi.add("name", gp.getEnterprise());
												poi.add("description", gp.getType());
												poi.add("actLat",curlat);
												poi.add("actLng",curlng);
												
												pois.add(poi.build());
											}
										}
										
									}
								}
								
								
								/*Individual trackPoint = LoadLocationPerPerson
										.createTrackPoint(auto, curlat, curlng, "point", Util.nowPlus(1000 * 60),
												model, plat, plon, SolrUtil.CLOSE_CHECK);
								
								if (trackPoint != null) {
									if (i == 0) {
										model.add(route, AutoIE.hasAsAPointOfOrigin,
												trackPoint);
										model.add(trackPoint, AutoIE.isOriginFor, route);
										
									} else {
										model.add(route, AutoIE.isTrackedBy, trackPoint);
									}
									plat = curlat;
									plon = curlng;
								}
								*/
								
								//Gathering and adding POIs (not currently being stored in SOLR).
								
								GEOPoint gpoint = new GEOPoint();
								gpoint.setLat(curlat);
								gpoint.setLng(curlng);
								List<GEOPlace> places = new ArrayList<GEOPlace>();
								
								places = SolrUtil.getPOIS(person.toString(), gpoint);
								if(places!=null){
									if(!places.isEmpty() && places.size()>poiLimit){
										places = places.subList(0, poiLimit);
									}
									for(int j=0;j<places.size();j++){	
										JsonObjectBuilder poi = Json.createObjectBuilder();
										GEOPlace gp = places.get(j);
										GEOPoint geo = gp.getPoint();
										if (geo != null) {
											poi.add("lat", geo.getLat());
											poi.add("lng", geo.getLng());
										}
										poi.add("streetAddress", gp.getStreetAddress());
										poi.add("name", gp.getEnterprise());
										poi.add("description", gp.getType());
										poi.add("actLat",curlat);
										poi.add("actLng",curlng);
										
										pois.add(poi.build());
										log.fine(" adding poi: " + poi.build());
									}
								}
								
								
							}
							
							result = Json.createObjectBuilder()
									.add("routes", routes)
									.add("infferedPOIs",pois.build()).build();
							
							ByteArrayOutputStream os = new ByteArrayOutputStream();
							log.info("completed processing, writing out N-TRIPLE");
							//model.write(os, "N-TRIPLE");
							ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
							//LoadAboxWebService.doStream(is, SolrUtil.getProperties());
							log.info("process completed successfully");
							
						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}
						
					}
					
		
					
					
					
				}
				
			}
			else{
				result = Json.createObjectBuilder().add("status","Unsuccesful.").build();
			}
			
			
			log.fine("json string is " + result);
			
			return result;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	

}
