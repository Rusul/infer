/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infer.util.rules;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import infer.util.SolrUtil;
import infer.util.Util;
import infer.util.generated.AutoIE;
import infer.util.generated.Vcard;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;
import com.hp.hpl.jena.reasoner.rulesys.impl.BindingVector;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

/**
 * Can be used in two arg form (X, P) or three arg form (X, P, V). In three arg
 * form it succeeds if the triple (X, P, V) is not currently present, in two arg
 * form it succeeds if there is not value for (X, P, *).
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009-06-29 08:55:36 $
 */
public class PointToSegment extends BaseBuiltin {
	final static Logger logger = Logger.getLogger(PointToSegment.class
			.getName());
	
	private static double LINE_SEGMENT_TEST_CHECK = 0.1;

	/**
	 * Return a name for this builtin, normally this will be the name of the
	 * functor that will be used to invoke it.
	 */
	@Override
	public String getName() {
		return "PointToSegment";
	}

	@Override
	public void headAction(Node[] args, int length, RuleContext context) {

		checkArgs(length, context);
		Node n0 = getArg(0, args, context);
		Node n1 = getArg(1, args, context);
		Node n2 = getArg(2, args, context);

		logger.fine(" in head action " + n0 + n1 + n2);
		BindingVector bv = (BindingVector) context.getEnv();
		Node[] node = bv.getEnvironment();
		Node trackPoint = node[1];
		String latitude = node[2].toString(false);
		String longitude = node[3].toString(false);
		String when = node[4].toString(false);

		GEOPoint point = new GEOPoint();
		point.setLat(latitude);
		point.setLng(longitude);
		point.setWhen(when);

		logger.fine(" trackpoint is " + trackPoint);
		logger.fine(" latitude is " + latitude);
		logger.fine(" longitude is " + longitude);
		logger.fine(" when is " + when);
		getLineSegment(context, n0, trackPoint, point);

		//
		// Node val = Node.createLiteral(synonyms.toString().toLowerCase());
		// context.add( new Triple( n0, n1, val ) );

	}

	/**
	 * Flag as non-monotonic so the guard clause will get rerun after deferal as
	 * part of a non-trivial conflict set.
	 */
	@Override
	public boolean isMonotonic() {
		return false;
	}

	public static boolean alreadyExists(RuleContext context, Node route,
			GEOPoint point, Double isCloseDist) {
		

		SolrDocumentList segDocs = SolrUtil.getDocsbySPO(route.toString(),
				AutoIE.hasLineSegment.toString(), null, 1);
		if(segDocs.size()>0){
					

			SolrDocumentList pointsDocs = SolrUtil.getDocsbySPO(
					(String) segDocs.get(0).getFieldValue("object_t"),
					AutoIE.hasSegmentPoint.toString(), null, SolrUtil.MAX_DOCS);
			
			for (int m = 0; m < pointsDocs.size(); m++) {
				SolrDocumentList latDoc = SolrUtil.getDocsbySPO(
						(String) pointsDocs.get(m).getFieldValue("object_t"),
						Vcard.latitude.toString(), null, 1);
				SolrDocumentList lngDoc = SolrUtil.getDocsbySPO(
						(String) pointsDocs.get(m).getFieldValue("object_t"),
						Vcard.longitude.toString(), null, 1);

				GEOPoint p1 = new GEOPoint();
				p1.setLat((String) latDoc.get(0).getFieldValue("object_t"));
				p1.setLng((String) lngDoc.get(0).getFieldValue("object_t"));

				if (p1.isClose(point, isCloseDist)) {
					logger.fine("existing line segment found: "
							+ (String) segDocs.get(0).getFieldValue("object_t"));
					return true;
				};

			}
			
			SolrDocumentList pointsDocs2 = SolrUtil.getDocsbySPO(
					(String) segDocs.get(0).getFieldValue("object_t"),
					AutoIE.hasCurrentPoint.toString(), null, SolrUtil.MAX_DOCS);
			
			for (int m = 0; m < pointsDocs2.size(); m++) {
				SolrDocumentList latDoc = SolrUtil.getDocsbySPO(
						(String) pointsDocs2.get(m).getFieldValue("object_t"),
						Vcard.latitude.toString(), null, 1);
				SolrDocumentList lngDoc = SolrUtil.getDocsbySPO(
						(String) pointsDocs2.get(m).getFieldValue("object_t"),
						Vcard.longitude.toString(), null, 1);

				GEOPoint p1 = new GEOPoint();
				p1.setLat((String) latDoc.get(0).getFieldValue("object_t"));
				p1.setLng((String) lngDoc.get(0).getFieldValue("object_t"));

				if (p1.isClose(point, isCloseDist)) {
					logger.fine("existing line segment found: "
							+ (String) segDocs.get(0).getFieldValue("object_t"));
					return true;
				};

			}
			
			

		}
		logger.fine("the point "+ point + " is not near any existing segments.");
		return false;
	}

	
	@SuppressWarnings("unchecked")
	public static Node getLineSegment(RuleContext context, Node route,
			Node trackPoint, GEOPoint point) {
		/*
		 * String postalCode = null; String adminCode1 = null; String adminCode2
		 * = null; String fraddl = null; String fraddr = null; String toaddl =
		 * null; String toaddr = null; String countryCode = null; String
		 * placeName = null; String mtfcc = null; String adminName1 = null;
		 * String adminName2 = null; String distance = null;
		 */
		String segName = null;
		String[] segEnd1LngLat;
		String[] segEnd2LngLat;
		String segPointsString = null;
		String segUri = null;
		String latitude = point.getLat();
		String longitude = point.getLng();
		String when = point.getWhen();
		String[] segPointsArray = null;
		JSONObject jsonLatLng = new JSONObject();
		JSONArray jsonLatLngArray = new JSONArray();
		JSONArray contextLines = new JSONArray();
		JSONArray solrLines = new JSONArray();
		Statement s = null;

		logger.fine("CURRENT POINT IS " + point);
		
		
		
			byte[] ba = Util
					.query("http://api.geonames.org/findNearbyStreetsJSON?lat="
							+ latitude + "&lng=" + longitude
							+ "&username=stephen.suffian");
			JSONParser parser = new JSONParser();
			Map<String, Object> m;
			ByteArrayInputStream bais = new ByteArrayInputStream(ba);
			Reader r = new InputStreamReader(bais);
			@SuppressWarnings("unchecked")
			Map<String, Object> highLevelKeys;

			try {
				m = (Map<String, Object>) parser.parse(r);
				logger.fine("street segment: " + m.get("streetSegment"));
			} catch (Exception e) {
				logger.fine("FAILED TO CONNECT TO EXTERNAL SERVICE OR NO LINE SEGMENTS FOUND");
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			JSONObject jsonSeg = null;
					
				if(m.get("streetSegment") instanceof JSONArray){
					 JSONArray results = (JSONArray) m.get("streetSegment");
					 jsonSeg = (JSONObject) results.get(0);
					logger.fine("ARRAY");
				}
				else if((m.get("streetSegment") instanceof JSONObject)){
					jsonSeg = (JSONObject) m.get("streetSegment");
					logger.fine("OBJECT");
				}

				segName = (String) jsonSeg.get("name");
				segPointsString = (String) jsonSeg.get("line");

				segPointsArray = segPointsString.split(",");
				segEnd1LngLat = segPointsArray[0].split(" ");
				segEnd2LngLat = segPointsArray[segPointsArray.length - 1]
						.split(" ");
				segUri = segPointsArray[0].replaceAll(" ", "_")
						+ "_"
						+ segPointsArray[segPointsArray.length - 1].replaceAll(
								" ", "_");
				logger.fine("line is : " + segPointsString + " with name: "
						+ segName);
				
				Double pToSegEnd1;
				Double pToSegEnd2;
				Double prevToSegEnd1;
				Double prevToSegEnd2;
				String timeSegEnd1 ="";
				String timeSegEnd2= "";
				
				GEOPoint segEnd1 = new GEOPoint();
				segEnd1.setLat(segEnd1LngLat[1]);
				segEnd1.setLng(segEnd1LngLat[0]);
				
				GEOPoint segEnd2 = new GEOPoint();
				segEnd2.setLat(segEnd2LngLat[1]);
				segEnd2.setLng(segEnd2LngLat[0]);
				
			
			
					Individual lineSegment;
					SolrDocumentList segDocs = SolrUtil.getDocsbySPO(
							route.toString(),
							AutoIE.hasLineSegment.toString(), null, 1);
					//Creates Line Segments. Applies timestamps to the points comprising the segments.
					//-1 ms for the point that is earlier in the route than the actual point generated it
					//+1 ms for hte point that is later in the route than the actual point that generated it
					if(segDocs.size()>0){

						
						SolrDocumentList pointDocs = SolrUtil.getDocsbySPO(
								(String) segDocs.get(0).getFieldValue("object_t"),
								AutoIE.hasSegmentPoint.toString(), null, 2);
						
							SolrDocumentList latDocs1 = SolrUtil.getDocsbySPO(
									(String) pointDocs.get(0).getFieldValue("object_t"),
									Vcard.latitude.toString(), null, 1);
							
							SolrDocumentList lngDocs1 = SolrUtil.getDocsbySPO(
									(String) pointDocs.get(0).getFieldValue("object_t"),
									Vcard.longitude.toString(), null, 1);
							
							SolrDocumentList latDocs2 = SolrUtil.getDocsbySPO(
									(String) pointDocs.get(1).getFieldValue("object_t"),
									Vcard.latitude.toString(), null, 1);
							
							SolrDocumentList lngDocs2 = SolrUtil.getDocsbySPO(
									(String) pointDocs.get(1).getFieldValue("object_t"),
									Vcard.longitude.toString(), null, 1);	
							String prevLat1 = (String) latDocs1.get(0).getFieldValue("object_t");
							String prevLng1 = (String) lngDocs1.get(0).getFieldValue("object_t");
							String prevLat2 = (String) latDocs2.get(0).getFieldValue("object_t");
							String prevLng2 = (String) lngDocs2.get(0).getFieldValue("object_t");
							
							if(prevLat1.equals(segEnd1LngLat[1])&&prevLng1.equals(segEnd1LngLat[0])){
								if(prevLat2.equals(segEnd2LngLat[1])&&prevLng2.equals(segEnd2LngLat[0])){
									logger.fine("segment is same as previous.");
									return trackPoint;
								}
							}
							else if(prevLat2.equals(segEnd1LngLat[1])&&prevLng2.equals(segEnd1LngLat[0])){
								if(prevLat1.equals(segEnd2LngLat[1])&&prevLng1.equals(segEnd2LngLat[0])){
									logger.fine("segment is same as previous.");
									return trackPoint;
								}
							}
							else{
								logger.fine("new segment. previous segment was " + prevLat1 + " " + 
							prevLng1 + ", " + prevLat2 + " " + prevLng2 + " and this segment is " + segEnd1LngLat[1] + " " 
							+ segEnd1LngLat[0] + ", " + segEnd2LngLat[1] + " " + segEnd2LngLat[0]);
							}
							
							
							//Same Name check
							SolrDocumentList nameDocs = SolrUtil.getDocsbySPO(
									(String) segDocs.get(0).getFieldValue("object_t"),
									Vcard.Name.toString(), segName, 1);
							
						if(nameDocs.size()>0){
							lineSegment = AutoIE.LineSegment
									.createIndividual((String) nameDocs.get(0)
											.getFieldValue("object_t"));
							logger.fine("SAME NAME of " + segName);
							
						}
						else{
							lineSegment = AutoIE.LineSegment
									.createIndividual(AutoIE.LineSegment.getURI()
											+ System.currentTimeMillis());
							context.add(new Triple(route, AutoIE.hasLineSegment
									.asNode(), lineSegment.asNode()));
							
						}
						//End Same Name Check
						
						//Directionality Check
						SolrDocumentList pointsDocs = SolrUtil.getDocsbySPO(
								(String) segDocs.get(0).getFieldValue("object_t"),
								AutoIE.hasSegmentPoint.toString(), null, 1);
						SolrDocumentList latDoc = SolrUtil.getDocsbySPO(
								(String) pointsDocs.get(0).getFieldValue("object_t"),
								Vcard.latitude.toString(), null, 1);
						SolrDocumentList lngDoc = SolrUtil.getDocsbySPO(
								(String) pointsDocs.get(0).getFieldValue("object_t"),
								Vcard.longitude.toString(), null, 1);

						logger.fine("LATEST SEGMENT POINT FOUND AND IT IS : " + (String) latDoc.get(0).getFieldValue("object_t")
								+", "+(String) lngDoc.get(0).getFieldValue("object_t"));
						prevToSegEnd1= GEOPoint
								.haversine(Double.valueOf(segEnd1LngLat[1]),
										Double.valueOf(segEnd1LngLat[0]),
										Double.valueOf((String) latDoc.get(0).getFieldValue("object_t")),
										Double.valueOf((String) lngDoc.get(0).getFieldValue("object_t")));
						prevToSegEnd2 = GEOPoint
								.haversine(Double.valueOf(segEnd2LngLat[1]),
										Double.valueOf(segEnd2LngLat[0]),
										Double.valueOf((String) latDoc.get(0).getFieldValue("object_t")),
										Double.valueOf((String) lngDoc.get(0).getFieldValue("object_t")));
						if(prevToSegEnd1<prevToSegEnd2){
							timeSegEnd1 = Util.datePlus(when, -1);
							timeSegEnd2 = Util.datePlus(when,1);
						}
						else{
							timeSegEnd1 = Util.datePlus(when, 1);
							timeSegEnd2 = Util.datePlus(when,-1);
						}
						
						
					}
					else{
						logger.fine("FIRST LINE SEGMENT TO BE MADE.");
						lineSegment = AutoIE.LineSegment
								.createIndividual(AutoIE.LineSegment.getURI()
										+ System.currentTimeMillis());
						context.add(new Triple(route, AutoIE.hasLineSegment
								.asNode(), lineSegment.asNode()));
						
						SolrDocumentList originDocs = SolrUtil.getDocsbySPO(
								route.toString(),
								AutoIE.hasAsAPointOfOrigin.toString(), null, 1);
						if(originDocs.size()>0){
							SolrDocumentList latDoc = SolrUtil.getDocsbySPO(
									(String) originDocs.get(0).getFieldValue("object_t"),
									Vcard.latitude.toString(), null, 1);
							SolrDocumentList lngDoc = SolrUtil.getDocsbySPO(
									(String) originDocs.get(0).getFieldValue("object_t"),
									Vcard.longitude.toString(), null, 1);

							
							logger.fine("ORIGIN POINT FOUND AND IT IS : " + (String) latDoc.get(0).getFieldValue("object_t") +
									" " + (String) lngDoc.get(0).getFieldValue("object_t"));
							pToSegEnd1= GEOPoint
									.haversine(Double.valueOf(segEnd1LngLat[1]),
											Double.valueOf(segEnd1LngLat[0]),
											Double.valueOf(latitude),
											Double.valueOf(longitude));
							pToSegEnd2 = GEOPoint
									.haversine(Double.valueOf(segEnd2LngLat[1]),
											Double.valueOf(segEnd2LngLat[0]),
											Double.valueOf(latitude),
											Double.valueOf(longitude));
							if (pToSegEnd1 < pToSegEnd2) {
								//P is closer to SegEnd1
								Double oToSegEnd2= GEOPoint
										.haversine(Double.valueOf(segEnd2LngLat[1]),
												Double.valueOf(segEnd2LngLat[0]),
												Double.valueOf((String) latDoc.get(0).getFieldValue("object_t")),
												Double.valueOf((String) lngDoc.get(0).getFieldValue("object_t")));
								if(oToSegEnd2<pToSegEnd2){
									timeSegEnd1 = Util.datePlus(when, 1);
									timeSegEnd2 = Util.datePlus(when,-1);
								}
								else{
									timeSegEnd1 = Util.datePlus(when, -1);
									timeSegEnd2 = Util.datePlus(when,1);
								}
							}
							else{
								//P is closer to SegEnd2
								Double oToSegEnd1= GEOPoint
										.haversine(Double.valueOf(segEnd1LngLat[1]),
												Double.valueOf(segEnd1LngLat[0]),
												Double.valueOf((String) latDoc.get(0).getFieldValue("object_t")),
												Double.valueOf((String) lngDoc.get(0).getFieldValue("object_t")));
								if(oToSegEnd1<pToSegEnd1){
									timeSegEnd1 = Util.datePlus(when,-1);
									timeSegEnd2 = Util.datePlus(when,1);
									logger.fine("Point"+segEnd1LngLat[1]+" " + segEnd1LngLat[0] + " is earlier.");
								}
								else{
									timeSegEnd1 = Util.datePlus(when, 1);
									timeSegEnd2 = Util.datePlus(when,-1);
									logger.fine("Point"+segEnd1LngLat[1]+" " + segEnd1LngLat[0] + " is later.");
								}
							}
									
						}
						else{
							logger.fine("No origin Found.");
						}
						
					}
					
					context.add(new Triple(lineSegment.asNode(),
							AutoIE.hasCurrentPoint.asNode(), trackPoint));
					context.add(new Triple(trackPoint, AutoIE.isCurrentPointOf
							.asNode(), lineSegment.asNode()));

					context.add(new Triple(lineSegment.asNode(),
							AutoIE.lineSegmentWhen.asNode(), NodeFactory
									.createLiteral(when)));
					context.add(new Triple(lineSegment.asNode(), Vcard.Name
							.asNode(), NodeFactory.createLiteral(segName)));

					Individual lsTrackPoint1 = AutoIE.TrackPoint
							.createIndividual(AutoIE.TrackPoint.getURI()
									+ System.currentTimeMillis() + 0);

					context.add(new Triple(lineSegment.asNode(),
							AutoIE.hasSegmentPoint.asNode(), lsTrackPoint1
									.asNode()));
					context.add(new Triple(lsTrackPoint1.asNode(),
							AutoIE.isSegmentPointOf.asNode(), lineSegment
									.asNode()));

					context.add(new Triple(lsTrackPoint1.asNode(),
							Vcard.latitude.asNode(), NodeFactory
									.createLiteral(segEnd1LngLat[1])));

					context.add(new Triple(lsTrackPoint1.asNode(),
							Vcard.longitude.asNode(), NodeFactory
									.createLiteral(segEnd1LngLat[0])));

					Individual lsTrackPoint2 = AutoIE.TrackPoint
							.createIndividual(AutoIE.TrackPoint.getURI()
									+ System.currentTimeMillis() + 1);

					context.add(new Triple(lineSegment.asNode(),
							AutoIE.hasSegmentPoint.asNode(), lsTrackPoint2
									.asNode()));
					context.add(new Triple(lsTrackPoint2.asNode(),
							AutoIE.isSegmentPointOf.asNode(), lineSegment
									.asNode()));

					context.add(new Triple(lsTrackPoint2.asNode(),
							Vcard.latitude.asNode(), NodeFactory
									.createLiteral(segEnd2LngLat[1])));
					context.add(new Triple(lsTrackPoint2.asNode(),
							Vcard.longitude.asNode(), NodeFactory
									.createLiteral(segEnd2LngLat[0])));
					
					context.add(new Triple(lsTrackPoint1.asNode(),
								AutoIE.trackPointWhen.asNode(), NodeFactory
										.createLiteral(timeSegEnd1)));
						
					context.add(new Triple(lsTrackPoint2.asNode(),
								AutoIE.trackPointWhen.asNode(), NodeFactory
										.createLiteral(timeSegEnd2)));
						

			
		
		return trackPoint;
	}

}
