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
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import infer.util.Util;
import infer.util.generated.AutoIE;
import infer.util.generated.Vcard;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;
import com.hp.hpl.jena.reasoner.rulesys.impl.BindingVector;

/**
 * Can be used in two arg form (X, P) or three arg form (X, P, V). 
 * In three arg form it succeeds if the triple  (X, P, V) is not
 * currently present, in two arg form it succeeds if there is not value
 * for (X, P, *).
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009-06-29 08:55:36 $
 */
public class Poi extends BaseBuiltin {
	final static Logger logger = Logger.getLogger(Poi.class
			.getName());

    /**
     * Return a name for this builtin, normally this will be the name of the 
     * functor that will be used to invoke it.
     */
    @Override
    public String getName() {
        return "Poi";
    }
    
    @Override
    public void headAction(Node[] args, int length, RuleContext context) {
 
        checkArgs(length, context);
        Node n0 = getArg(0, args, context);
        Node n1 = getArg(1, args, context);
        Node n2 = getArg(2, args, context);
        logger.info(" in head action " + n0 + n1 + n2);
        BindingVector bv = (BindingVector) context.getEnv();
        Node[] node = bv.getEnvironment();
        Node trackPoint = node[1];
        String latitude = node[2].toString(false);
        String longitude = node[3].toString(false);
        System.out.println(" trackpoint is " + trackPoint);
 
        System.out.println(" latitude is " + latitude);
        System.out.println(" longitude is " + longitude);
        getPOI(context, trackPoint, latitude, longitude);
        
        
// 
//        Node val = Node.createLiteral(synonyms.toString().toLowerCase());
//        context.add( new Triple( n0, n1, val ) );
        
    }

    /**
     * Flag as non-monotonic so the guard clause will get rerun after deferal
     * as part of a non-trivial conflict set.
     */
    @Override
    public boolean isMonotonic() {
        return false;
    }
    
    
	@SuppressWarnings("unchecked")
	public static Node getPOI(RuleContext context, Node trackPoint,
			String latitude, String longitude) {
		String streetAddress = null;
		String city = null;
		String state = null;
		String zip = null;
		String enterpriseShortName = null;
		String enterpriseLongName = null;
		Statement s = null;

		byte[] ba = Util
				.query("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
						+ latitude
						+ ","
						+ longitude
						+ "&rankby=distance&name=*&sensor=false&key=AIzaSyBq3o8JdB4Kp57915CrABXrNc8HfDSpj6c");

		JSONParser parser = new JSONParser();
		Map<String, JSONArray> m;
		ByteArrayInputStream bais = new ByteArrayInputStream(ba);
		Reader r = new InputStreamReader(bais);
		@SuppressWarnings("unchecked")
		Map<String, Object> highLevelKeys;
		try {
			m = (Map<String, JSONArray>) parser.parse(r);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		JSONArray results = m.get("results");
		logger.info(" results size is " + results.size());
		for (int i = 0; i < results.size(); i++) {
			JSONObject jo = (JSONObject) results.get(i);
			streetAddress = (String) jo.get("vicinity");
			enterpriseShortName = (String) jo.get("name");
			enterpriseLongName = enterpriseShortName;
			JSONArray types = (JSONArray) jo.get("types");
			String typeString = types.toString();
			logger.info(" type String = " + typeString);
			JSONObject location = (JSONObject) jo.get("geometry");
			location = (JSONObject) location.get("location");
			Double lat = (Double) location.get("lat");
			Double lng = (Double) location.get("lng");
			logger.info (" lat is " + lat + " lng is " + lng);
			logger.info(" streetAddress " + streetAddress);
			String address = streetAddress + lat + lng;
			String term = address.replaceAll(" ", "_");

			Individual vcardAddress = Vcard.Address
					.createIndividual(Vcard.Address.getURI() + term);

			
			context.add(new Triple(vcardAddress.asNode(), Vcard.latitude.asNode(), Node.createLiteral(lat.toString())));

			context.add(new Triple(vcardAddress.asNode(), AutoIE.typeOfPOI.asNode(), Node.createLiteral(typeString)));

			
			context.add(new Triple(vcardAddress.asNode(), Vcard.longitude.asNode(), Node.createLiteral(lng.toString())));

			
			context.add(new Triple(vcardAddress.asNode(), Vcard.street_address.asNode(),Node.createLiteral(address)));
			context.add(new Triple(vcardAddress.asNode(), AutoIE.shortNameOfPOI.asNode(), Node.createLiteral(enterpriseShortName)));
			context.add(new Triple(vcardAddress.asNode(), AutoIE.longNameOfPOI.asNode(),Node.createLiteral(enterpriseLongName)));
			context.add(new Triple(trackPoint, AutoIE.hasAsAPointOfInterest.asNode(), vcardAddress.asNode()));
			context.add(new Triple(vcardAddress.asNode(), AutoIE.hasAsALocation.asNode(), trackPoint));
			
		}
		return trackPoint;
	}

}
