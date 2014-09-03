package infer.servlets.resource;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import infer.util.CalendarEvent;
import infer.util.LineSegment;
import infer.util.Route;
import infer.util.RouteHistorySummary;
import infer.util.SolrUtil;
import infer.util.Util;
import infer.util.generated.AutoIE;
import infer.util.rules.GEOPlace;
import infer.util.rules.GEOPoint;

/**
 * 
 * @author Scott Streit
 * 
 *         This class contains the static methods necessary to access solr
 *         through intercepting calls and adding new calls.
 * 
 */
@Path("listTrackPoints")
@Produces(MediaType.APPLICATION_JSON)
public class ListTrackPoints extends HttpServlet {
	final static Logger log = Logger.getLogger(ListTrackPoints.class.getName());

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost(@QueryParam("auto") String auto, 
					@QueryParam("inference") String inference,
					@QueryParam("route") String routeString,
					@DefaultValue("2013-01-01T02:16:48.3830-05:00") @QueryParam("since") String since,
					@QueryParam("history") String historyString,
					@QueryParam("events") String eventsString,
					@QueryParam("routeTime") String routeTimeString,
					@QueryParam("id") String id, 
					@QueryParam("listNearbyPOI") String listPOI,
					@QueryParam("actual") String actualString, 
					@QueryParam("maxActualSegments") String maxActualSegmentsString,
					@QueryParam("reset") String resetString)
			throws ServletException, IOException {
		return processTrackPoints(auto,inference,routeString,since,historyString,eventsString,routeTimeString,id,
				listPOI,actualString,maxActualSegmentsString,resetString);

	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doGet(@QueryParam("auto") String auto, 
					@QueryParam("inference") String inference,
					@QueryParam("route") String routeString,
					@DefaultValue("2013-01-01T02:16:48.3830-05:00") @QueryParam("since") String since,
					@QueryParam("history") String historyString,
					@QueryParam("events") String eventsString,
					@QueryParam("routeTime") String routeTimeString,
					@QueryParam("id") String id, 
					@QueryParam("listNearbyPOI") String listPOI,
					@QueryParam("actual") String actualString, 
					@QueryParam("maxActualSegments") String maxActualSegmentsString,
					@QueryParam("reset") String resetString)
			throws ServletException, IOException {

		return processTrackPoints(auto,inference,routeString,since,historyString,eventsString,routeTimeString,id,
				listPOI,actualString,maxActualSegmentsString,resetString);

	}

	private JsonObject processTrackPoints(String auto,String inference,String routeString,String since,
				String historyString,String eventsString,String routeTimeString,String id, 
				String listPOI, String actualString,String maxActualSegmentsString,
				String resetString) {
		Map<String, Map<String, Object>> m = null;
		Map<String, Object> m2;
		JSONArray j;
		try {
			boolean infer = true;
			boolean history = true;
			boolean places = true;
			boolean actual = true;
			boolean events = true;
			boolean routeTime = true;
			boolean reset=false;
			int maxActualSegments = SolrUtil.MAX_DOCS;


			
			log.fine(" historyString = [" + historyString + "]");
			log.fine(" since = [" + since + "]");
			log.fine(" auto = [" + auto + "]");
			log.fine(" routeTimeString = [" + routeTimeString + "]");
			log.fine(" listNearbyPOI = [" + listPOI + "]");

			if (maxActualSegmentsString != null) {
				maxActualSegments = Integer.parseInt(maxActualSegmentsString);
			}
			if ("false".equals(inference)) {
				infer = false;
			}
			if ("false".equals(historyString)) {
				log.fine(" case of history as false");
				history = false;
			}
			if ("false".equals(listPOI)) {
				places = false;
			}
			if ("false".equals(actualString)) {
				actual = false;
			}
			if ("true".equals(resetString)) {
				reset = true;
			}
			if ("false".equals(eventsString)) {
				events = false;
			}
			if ("false".equals(routeTimeString)) {
				routeTime = false;
			}
			
			
			if(reset){
				SolrUtil.resetHistory(auto);
			}
			if (id == null) {
				id = auto;
			}
			String driver = null;
			String fullAuto = AutoIE.Automobile.getURI() + auto;
			if (routeString == null) {
				log.fine(" fullAuto is " + fullAuto);
				routeString = SolrUtil.getFirstRoute(fullAuto, "INPROGRESS");
				log.fine(" current route is " + routeString);
				// If there is no in progress route, then get the driver based
				// on the last route of the car.
				if (routeString == null) {
					driver = SolrUtil.getLastDriverBasedOnAuto(fullAuto);
				} else {
					log.fine(" route based on auto is " + routeString);
					driver = SolrUtil.getDriver(routeString);
					log.fine(" driver is " + driver);
					// Case of no route in progress.
					log.fine("for auto " + fullAuto
							+ "  route based on auto is" + routeString);
				}
			}
			else{
				driver = SolrUtil.getDriver(routeString);
			}
				JsonObject jsonReturned = routeTrackPoint(id, driver, routeString, since, fullAuto,
						actual, infer, history, places, events, routeTime,
						maxActualSegments);
				
				return jsonReturned;
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private JsonObject routeTrackPoint(String id, String person, String route,
			String since, String auto, boolean actual, boolean infer,
			boolean history, boolean places, boolean events, boolean routeTime,
			int maxActualPoints) throws IOException, ServletException {

		log.fine("in getting route" + route + " actual = " + actual
				+ " infer = " + infer + " history =" + history + " events ="
				+ events + " routeTime = " + routeTime + " places = " + places);
		GEOPoint lastPoint = null;
		Calendar calendarSince = Util.toCalendar(since);
		Route actualRoute = null;
		LineSegment lastSegment = null;
		
		JsonArrayBuilder ja = Json.createArrayBuilder();
		boolean isJaEmpty=true;
		
		boolean inferredCalDest = false;
		List<CalendarEvent> ces = null;
		
		if (route != null) {
			List<String> l = new ArrayList<String>();
			l.add(route);
			actualRoute = SolrUtil.getActualRoute(l, calendarSince, maxActualPoints);
			log.fine("HERE-pre if statement");
			if (actualRoute != null &&actualRoute.getLastLineSegment()!=null) {
				log.fine("HERE-inside if statement");
				lastSegment = actualRoute.getLastLineSegment();
				lastPoint = lastSegment.getCurrentPoint();
				log.fine("first seg: " + actualRoute.getFirstLineSegment());
				log.fine("end seg: " + lastSegment);
				
				Calendar eventSince = Util.getNowPlus(-1 * (3600 * 1000));
				ces = SolrUtil.getCalendarEvents(person, eventSince, lastPoint);
				log.fine(" CALENDAR events are " + ces);
				
				for (int i = 0; i < ces.size(); i++) {
					CalendarEvent ce = ces.get(i);
					log.fine(" CALENDAR FOR JSON [" + i + "]=" + ce);
					if (ce.isInferredDestination()) {
						inferredCalDest = true;
					}
					if (ce.isNotNull()) {
						JsonObject jo = Json.createObjectBuilder()
							.add("EventName", ce.getDescription())
							.add("TimetoEvent", ce.getTimeToEvent())
							.add("Delay", ce.getDelayInMinutes())
							.add("AlertType", ce.getType())
							.add("lat", ce.getPoint().getLat())
							.add("lng", ce.getPoint().getLng())
							.add("Location", ce.getLocation())
							.add("StartTime", Util.calendarToISO8601String(ce.getStart()))
							.add("EndTime", Util.calendarToISO8601String(ce.getEnd()))
							.add("Author", person).build();
						ja.add(jo);
						isJaEmpty=false;
					}
				}
			}
			else{
				route=null;
			}
		}

		JsonObjectBuilder res = Json.createObjectBuilder();
		if (ja == null || isJaEmpty) {
			res.add("inferredAlert", "EMPTY");
		} else {
			res.add("inferredAlert", ja);
		}

		JsonObjectBuilder tracks = Json.createObjectBuilder();

		JSONParser parser = new JSONParser();
		log.fine("in route track point with route = " + route
				+ " with person =  " + person + " auto = " + auto
				+ " history = " + history + " maxAcutalPoints = "
				+ maxActualPoints + " routeTime = " + routeTime);

		

		Map<String, List<Route>> statusRoutes = null;
		log.fine(" before checking routes route is " + route);
		if (route != null) {
			statusRoutes = SolrUtil.getRouteAndStatus(id, person, actualRoute,
					calendarSince, auto, infer, history, events, routeTime,
					places, maxActualPoints, inferredCalDest, lastSegment);
			log.fine(" number of routes returned are " + statusRoutes.size());
			log.fine(" all routes returned are with points " + statusRoutes);

			/**
			 * Example of output format.
			 * 
			 * 
			 * {
			 * 
			 * inferredAlert: [
			 * 
			 * {
			 * 
			 * "Alert Type": "Late for Event",
			 * 
			 * "Author": "Susan Silver",
			 * 
			 * "EventName": "Lunch with Bob from Company X",
			 * 
			 * "Location": "Panera Bread",
			 * 
			 * "StartTime": "YYYY-MM-DDTHH:MM:SS.FFZ",
			 * 
			 * "EndTime": "YYYY-MM-DDTHH:MM:SS.FFZ",
			 * 
			 * "TimetoEvent": 20,
			 * 
			 * "Delay": 600,
			 * 
			 * "lat": 40.455269,
			 * 
			 * "long": -86.04031,
			 * 
			 * }
			 * 
			 * ]
			 * 
			 * “tracks”:[
			 * 
			 * “current”:[
			 * 
			 * “track”:
			 * "http:\/\/www.compscii.com\/ontologies\/0.1\/AutoIE.owl#RouteCURR
			 * E N T _ N U M B E R " ,
			 * 
			 * “trackpoints”:[
			 * 
			 * {
			 * 
			 * "when":"2013-05-17T16:44:00.720Z",
			 * 
			 * "lng":"-86.109756000",
			 * 
			 * "lat":"40.455269000"
			 * 
			 * },
			 * 
			 * {
			 * 
			 * "when":"2013-05-17T16:44:00.720Z",
			 * 
			 * "lng":"-86.109756000",
			 * 
			 * "lat":"40.455269000"
			 * 
			 * }
			 * 
			 * ]
			 * 
			 * ],
			 * 
			 * “history”:[
			 * 
			 * “track”: "http:\/\/www.compscii.com\/ontologies\/0.1\/AutoIE.owl#
			 * RouteHISTORICAL_ROUTE_NUMBER",
			 * 
			 * “trackpoints”:[
			 * 
			 * {
			 * 
			 * "when":"2013-05-17T16:44:00.720Z",
			 * 
			 * "lng":"-86.109756000",
			 * 
			 * "lat":"40.455269000"
			 * 
			 * },
			 * 
			 * {
			 * 
			 * "when":"2013-05-17T16:44:00.720Z",
			 * 
			 * "lng":"-86.109756000",
			 * 
			 * "lat":"40.455269000"
			 * 
			 * }
			 * 
			 * ],
			 * 
			 * “destination”:
			 * 
			 * {
			 * 
			 * “streetAddress”:”1800 E Lincoln Rd”,
			 * 
			 * "city":"Kokomo",
			 * 
			 * "state":"IN",
			 * 
			 * "postalCode":"46902",
			 * 
			 * "name":" Safety Technical Center ",
			 * 
			 * "lng":"-86.127182000",
			 * 
			 * "lat":"40.274212000"
			 * 
			 * }
			 * 
			 * ],
			 * 
			 * “inferred”:[
			 * 
			 * “track”:
			 * "http:\/\/www.compscii.com\/ontologies\/0.1\/AutoIE.owl#RouteINFERR
			 * E D _ R O U T E _ N U M B E R " ,
			 * 
			 * “priority”:1,
			 * 
			 * “trackpoints”:[
			 * 
			 * {
			 * 
			 * "when":"2013-05-17T16:44:00.720Z",
			 * 
			 * "lng":"-86.109756000",
			 * 
			 * "lat":"40.455269000"
			 * 
			 * },
			 * 
			 * {
			 * 
			 * "when":"2013-05-17T16:44:00.720Z",
			 * 
			 * "lng":"-86.109756000",
			 * 
			 * "lat":"40.455269000"
			 * 
			 * }
			 * 
			 * ],
			 * 
			 * “destination”:
			 * 
			 * {
			 * 
			 * “streetAddress”:”1800 E Lincoln Rd”,
			 * 
			 * "city":"Kokomo",
			 * 
			 * "state":"IN",
			 * 
			 * "postalCode":"46902",
			 * 
			 * "name":" Safety Technical Center ",
			 * 
			 * "lng":"-86.127182000",
			 * 
			 * "lat":"40.274212000"
			 * 
			 * }
			 * 
			 * ]
			 * 
			 * “inferred_destinations”:[
			 * 
			 * {
			 * 
			 * "status":"nearby_poi",
			 * 
			 * “streetAddress”:”1800 E Lincoln Rd”,
			 * 
			 * "city":"Kokomo",
			 * 
			 * "state":"IN",
			 * 
			 * "postalCode":"46902",
			 * 
			 * "name":" Safety Technical Center ",
			 * 
			 * "lng":"-86.127182000",
			 * 
			 * "lat":"40.274212000"
			 * 
			 * }
			 * 
			 * ]
			 * 
			 * }
			 */
			
			addPlacesToResult(statusRoutes, tracks, "inferredPOIs");

			if (actual) {
				addToResult(statusRoutes, tracks, "actual", since, false, true);
			}

			if (infer) {
				addToResult(statusRoutes, tracks, "inferred", since, true, true);
				addToResult(statusRoutes, tracks, "inferredHistory", since,  false,
						false);
			}
			if (history) {
				addToResult(statusRoutes, tracks, "history", since, false, true);
				JsonArrayBuilder routesRemoved = Json.createArrayBuilder();
				RouteHistorySummary routeHistorySummary = SolrUtil
						.getRouteHistorySummaryBasedOnId(id);
				if (routeHistorySummary != null) {
					for (int i = 0; i < routeHistorySummary.getRoutesRemoved()
							.size(); i++) {
						JsonObject o = Json.createObjectBuilder()
							.add(routeHistorySummary.getRoutesRemoved().get(i),
								routeHistorySummary.getRoutesRemoved().get(i)).build();
						routesRemoved.add(o);
					}
					tracks.add("routesRemoved", routesRemoved.build());
				}
			}
		}
		if (tracks == null) {
			res.add("tracks", "EMPTY");

		} else {
			res.add("tracks", tracks.build());
		}
	
		return res.build();
	}

	public static void addToResult(Map<String, List<Route>> statusRoutes,
			JsonObjectBuilder tracks, String term, String since, boolean destination, boolean route) {
		
		JsonArrayBuilder trackArray = Json.createArrayBuilder();
		List<Route> termRoutes = statusRoutes.get(term);
		log.fine("with term " + term + " in addToResult with term = " + term
				+ " termRoutes = " + termRoutes);
		if (termRoutes == null) {
			return;
		}
		for (int z = 0; z < termRoutes.size(); z++) {
			JsonArrayBuilder pointAr = Json.createArrayBuilder();

			Route termRoute = termRoutes.get(z);

			log.fine(" in addToResult with routeName = "
					+ termRoute.getRouteName());
			
			if (route) {
				List<LineSegment> ls = termRoute.getLineSegments();
				
				GEOPoint originPoint = termRoute.getOrigin();
				GEOPoint firstPoint = originPoint;
				Calendar originWhen = Util.toCalendar(originPoint.getWhen());
				Calendar sinceWhen = Util.toCalendar(since);
				if(originWhen.before(sinceWhen)){
					firstPoint = ls.get(0).getCurrentPoint();
				}

				for (int zz = 0; zz < ls.size(); zz++) {
					LineSegment lineSegment = ls.get(zz);
					GEOPoint startPoint = lineSegment.getStartPoint();
					GEOPoint endPoint = lineSegment.getEndPoint();
					GEOPoint currentPoint = lineSegment.getCurrentPoint();
					
					if(zz==0){
						JsonObject jos = Json.createObjectBuilder()
							.add("lat", firstPoint.getLat())
							.add("lng", firstPoint.getLng())
							.add("when", firstPoint.getWhen())
							.add("status", firstPoint.getStatus()).build();
						pointAr.add(jos);
					}
					else{
						JsonObject jo = Json.createObjectBuilder()
							.add("lat", startPoint.getLat())
							.add("lng", startPoint.getLng())
							.add("when", startPoint.getWhen())
							.add("status", startPoint.getStatus()).build();
						pointAr.add(jo);
					}
						if(zz==ls.size()-1){
							JsonObject joe = Json.createObjectBuilder()
								.add("lat", currentPoint.getLat())
								.add("lng", currentPoint.getLng())
								.add("when", currentPoint.getWhen())
								.add("status",currentPoint.getStatus()).build();
							pointAr.add(joe);
						}
						else{
							JsonObject jo2 = Json.createObjectBuilder()
								.add("lat", endPoint.getLat())
								.add("lng", endPoint.getLng())
								.add("when", endPoint.getWhen())
								.add("status",endPoint.getStatus()).build();
							pointAr.add(jo2);
						}
				}
			}
			JsonObjectBuilder jor = Json.createObjectBuilder()
				.add("track", termRoute.getRouteName())
				.add("trackPoints", pointAr);

			GEOPoint geo = termRoute.getDestination();
			if (destination && geo != null) {
				JsonObject dest = Json.createObjectBuilder()
					.add("lat", geo.getLat())
					.add("lng", geo.getLng()).build();
				jor.add("destination", dest);
			}
			trackArray.add(jor);
		}
		tracks.add(term, trackArray);

	}

	public static boolean addPlacesToResult(
			Map<String, List<Route>> statusRoutes, JsonObjectBuilder tracks,
			String term) {
		JsonArrayBuilder trackArray = Json.createArrayBuilder();
		List<Route> termRoutes = statusRoutes.get(term);
		log.fine("ADD PLACES with term " + term
				+ " in addPlacesToResult with term = " + term
				+ " termRoutes = " + termRoutes);
		if (termRoutes == null) {
			return true;
		}
		for (int z = 0; z < termRoutes.size(); z++) {

			Route termRoute = termRoutes.get(z);
			List<GEOPlace> places = termRoute.getAroundDestination();

			if (places == null) {
				return true;
			}
			for (int i = 0; i < places.size(); i++) {
				JsonObjectBuilder dest = Json.createObjectBuilder();

				GEOPlace gp = places.get(i);

				String name = gp.getEnterprise().toUpperCase();

				GEOPoint geo = gp.getPoint();
				if (geo != null) {
					dest.add("lat", geo.getLat());
					dest.add("lng", geo.getLng());
				}
				dest.add("streetAddress", gp.getStreetAddress());
				dest.add("name", gp.getEnterprise());
				dest.add("description", gp.getType());
				log.fine(" adding dest to trackArray " + dest);
				trackArray.add(dest.build());
			}

		}
		tracks.add(term, trackArray.build());
		return false;

	}

	

}
