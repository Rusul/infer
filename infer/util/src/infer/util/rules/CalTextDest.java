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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections.list.SetUniqueList;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import infer.util.generated.AutoIE;
import infer.util.generated.Vcard;
import infer.util.Util;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.reasoner.rulesys.BindingEnvironment;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinException;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;
import com.hp.hpl.jena.reasoner.rulesys.impl.BindingVector;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

import infer.util.Route;
import infer.util.SolrUtil;

/**
 * Can be used in two arg form (X, P) or three arg form (X, P, V). 
 * In three arg form it succeeds if the triple  (X, P, V) is not
 * currently present, in two arg form it succeeds if there is not value
 * for (X, P, *).
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009-06-29 08:55:36 $
 */
public class CalTextDest extends BaseBuiltin {
	final static Logger logger = Logger.getLogger(CalTextDest.class
			.getName());

    /**
     * Return a name for this builtin, normally this will be the name of the 
     * functor that will be used to invoke it.
     */
    @Override
    public String getName() {
        return "CalTextDest";
    }
    
    @Override
    public void headAction(Node[] args, int length, RuleContext context) {

    	checkArgs(length, context);
        Node n0 = getArg(0, args, context);
        Node n1 = getArg(1, args, context);
        logger.info("GENERATE TEXT DESTINATION in head action " + n0 + " " + n1);
        BindingVector bv = (BindingVector) context.getEnv();
        Node[] node = bv.getEnvironment();
        logger.info(" node length is "+ node.length);
        String person = node[1].toString(false);
        String eventDes = node[3].toString(false);
        SolrDocumentList docsTemp = SolrUtil.getDocsbySPO(person,AutoIE.driverDrivesARoute.toString(), null,
        		1,true);
        if (docsTemp == null || docsTemp.size() == 0) {
        	return;
        }
        SolrDocument docTemp = docsTemp.get(0);
        String route = (String) docTemp.getFieldValue("object_t");
	
	    Route r = new Route();
	    r.setRouteName(route);
	    SolrUtil.fillInRoute(r);
	        
	        GEOPoint lastPoint=r.getDestination();
	        String query = null;
			try {
				query = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
						+ lastPoint.getLat()
						+ ","
						+ lastPoint.getLng()
						+ "&rankby=distance&name="+URLEncoder.encode(eventDes,"UTF-8")
						+ "&sensor=false&key=AIzaSyBq3o8JdB4Kp57915CrABXrNc8HfDSpj6";
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        byte[] ba = Util.query(query);
	        
	        JSONParser parser = new JSONParser();
			Map<String, JSONArray> m;
			ByteArrayInputStream bais = new ByteArrayInputStream(ba);
			Reader re = new InputStreamReader(bais);
			@SuppressWarnings("unchecked")
			Map<String, Object> highLevelKeys;
			try {
				m = (Map<String, JSONArray>) parser.parse(re);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
	
	
			logger.info("location query to google: "+query);
			logger.info("results are: "+m.get("results"));
			JSONArray jsonString = m.get("results");
			JSONObject results = (JSONObject) jsonString.get(0);
			JSONObject geometry = (JSONObject) results.get("geometry");
			JSONObject location = (JSONObject) geometry.get("location");
			Double lat = (Double) location.get("lat");
			Double lng = (Double) location.get("lng");
			
			Individual trackPoint = AutoIE.TrackPoint
					.createIndividual(AutoIE.TrackPoint.getURI()
							+ System.currentTimeMillis());
			
			context.add(new Triple(n0, AutoIE.eventHasAsADestination.asNode(), trackPoint.asNode()));
			context.add(new Triple(n0, AutoIE.eventDestinationType.asNode(),Node.createLiteral("inferredByDescription")));

			context.add(new Triple(trackPoint.asNode(), AutoIE.DestinationHasAsAnEvent.asNode(), n0));
			context.add(new Triple(trackPoint.asNode(), Vcard.latitude.asNode(), Node.createLiteral(lat.toString())));
			context.add(new Triple(trackPoint.asNode(), Vcard.longitude.asNode(), Node.createLiteral(lng.toString())));
			context.add(new Triple(trackPoint.asNode(), AutoIE.trackPointWhen.asNode(), Node.createLiteral(Util.nowPlus(1000*60*10).toString())));
			context.add(new Triple(trackPoint.asNode(), AutoIE.pointStatus.asNode(), Node.createLiteral("point")));     
    }

     /**
     * Flag as non-monotonic so the guard clause will get rerun after deferal
     * as part of a non-trivial conflict set.
     */
    @Override
    public boolean isMonotonic() {
        return false;
    }
    
}
