
package infer.servlets.resource;

import infer.util.SolrUtil;
import infer.util.Util;
import infer.util.generated.AutoIE;
import infer.util.generated.Vcard;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.servlet.ServletException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.collections.list.SetUniqueList;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * 
 * @author Scott Streit
 * 
 *         This class contains the static methods necessary to access solr
 *         through intercepting calls and adding new calls.
 * 
 */

@Path("rerank")
@Produces(MediaType.APPLICATION_JSON)
@Consumes("text/plain")
public class ReRank{
	final static Logger log = Logger.getLogger(ReRank.class
			.getName());
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public JsonObject doPost(String message, @QueryParam("userid") String userId, 
			@QueryParam("email") String email,
			@QueryParam("query") String query)
			throws ServletException, IOException {
		return rerankWithNoSearch(message,userId,email,query);	
	}


	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	private JsonObject rerankWithNoSearch(String json, String userId, String email, String query) {
		Map<String, Map<String, Object>> m = null;
		Map<String, Object> m2;
		JSONArray j;
		try {

			
			log.fine("in consider user received search is " + json);
			log.info("userid receive id " + userId);
			log.info("email is " + email);
			log.info("query is " + query);
			log.fine("received document is " + json);

			JsonArrayBuilder jsonReturned = Json.createArrayBuilder();
			if (userId != null) {
				long startTime = System.currentTimeMillis();
				jsonReturned = considerUser(userId, email, query, json);
				SolrUtil.writeTimingEvent(" TIME IN  considerUser ", startTime,
						System.currentTimeMillis());

			}
			else{
				jsonReturned.add("none");
			}
			JsonObject jsonObject = Json.createObjectBuilder().add("results", jsonReturned.build()).build();
			return jsonObject;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private JsonArrayBuilder considerUser(String user, String email, String searchTerm,
			String json) throws IOException, ServletException {
		long startTime = System.currentTimeMillis();
		long endTime = startTime;
		
		InputStream bis = new ByteArrayInputStream(json.getBytes());
	    JsonReader jsonReader = Json.createReader(bis);
	    JsonObject jsonObject = jsonReader.readObject();
	    JsonObject jsonObject2 = jsonObject.getJsonObject("d");
	    JsonArray result = jsonObject2.getJsonArray("results");
	        

		String person = Util.getPerson(user, email);
		person = Vcard.VCard.getURI()+person;

		startTime = System.currentTimeMillis();
		List<String> notInterests = SetUniqueList
				.decorate(new ArrayList<String>());

		List<String> interests = SetUniqueList
				.decorate(new ArrayList<String>());
		SolrUtil.getInterests(person, interests, notInterests);
		log.fine(" INTERESTS are " + interests);
		log.fine(" NOT INTERESTS are " + notInterests);
		startTime = SolrUtil.writeTimingEvent("getting Interests ", startTime,
				System.currentTimeMillis());

		StringBuffer reason = new StringBuffer();
		String[] interestArray = interests.toArray(new String[0]);
		String[] notInterestArray = notInterests.toArray(new String[0]);
		Map<Float, List<JsonObject>> map = new TreeMap<Float, List<JsonObject>>();
		// Consider feedback loop. Number of times selected is relevant and must
		// be weighted.
		// Index with search term requested.
		Map<String, Float> previous = SolrUtil.getPrevious(user, email);
		log.fine(" previous map is " + previous
				+ " Current time in milliseconds " + System.currentTimeMillis());
		log.fine(" NUMBER OF STARTING RESULTS IS " + result
				+ " Current time in milliseconds " + System.currentTimeMillis());
		List<JsonObject> l = null;
		for (int i = 0; i < result.size(); i++) {
			JsonObject jo = result.getJsonObject(i);
			float userRating = new Float(jo.getString("UserRating"));
			float distance = new Float(jo.getString("Distance"));
			String text = jo.getString("Description");
			text = text.toLowerCase();
			log.fine("ITERATING JSON OBJECT is " + jo.toString()
					+ " Current time in milliseconds "
					+ System.currentTimeMillis());

			log.fine("ITERATING UserRating = " + userRating + " distance "
					+ distance + "JSON OBJECT is " + text
					+ " Current time in milliseconds "
					+ System.currentTimeMillis());
			float valToAdd = userRating - distance;
			float val = 0;
			Float previousValue = previous.get(text);

			if (previousValue != null) {
				log.fine(" GOT A PREVIOUS for [" + text + "]"
						+ " with value of " + previousValue
						+ " Current time in milliseconds "
						+ System.currentTimeMillis());
				val = previousValue + valToAdd;
				l = map.get(val);

				if (l == null) {
					l = new ArrayList<JsonObject>();
				}
				l.add(jo);
				map.put(val, l);
				reason.append(" text selected before [" + text + "]");
			} else {
				if (StringUtils.startsWithAny(text, interestArray)) {
					reason.append(" interest match at start ");
					val = val + 10;
				} else {
					if (StringUtils.endsWithAny(text, interestArray)) {
						reason.append(" interest match at end ");
						val = val + 10;
					}
				}
				if (StringUtils.startsWithAny(text, notInterestArray)) {
					reason.append(" NOT interest match at start ");
					val = val - 5;
				} else {
					if (StringUtils.endsWithAny(text, notInterestArray)) {
						reason.append(" NOT interest match at end ");
						val = val - 5;
					}
				}
				val = val + valToAdd;
				log.fine(" before putting in map " + " i is [" + i
						+ "] val is " + val + " jo is " + jo
						+ " Current time in milliseconds "
						+ System.currentTimeMillis());

				l = map.get(val);

				if (l == null) {
					l = new ArrayList<JsonObject>();
				}
				l.add(jo);
				map.put(val, l);
			}
		}
		startTime = SolrUtil.writeTimingEvent("looping objects ", startTime,
				System.currentTimeMillis());

		log.fine(" map size is " + map.size());
		JsonArrayBuilder resultToReturn = Json.createArrayBuilder();
		Float[] keys = new Float[map.size()];

		map.keySet().toArray(keys);

		for (int i = keys.length - 1; i > -1; i--) { // loop DESCENDINGLY
			l = map.get(keys[i]);
			for (int j = 0; j < l.size(); j++) {
				log.fine(" putting rows at " + " key is " + keys[i] + l.get(j));
				resultToReturn.add(l.get(j));
			}
		}

		startTime = SolrUtil.writeTimingEvent("ordering objects ", startTime,
				System.currentTimeMillis());

		// return a resorted list
		log.fine("  NUMBER OF ENDING RESULTS " + resultToReturn.build().size()
				+ " Current time in milliseconds " + System.currentTimeMillis());
		
		SolrUtil.writeTimingEvent("result JSON string is " + resultToReturn.toString(), startTime,
				System.currentTimeMillis());

		startTime = SolrUtil.writeTimingEvent("writing search to solr ",
				startTime, System.currentTimeMillis());

		return resultToReturn;

	}

	static List<String> getFriends(String person) {
		List<String> list = new ArrayList<String>();
		SolrDocumentList docs = SolrUtil.getDocsbySPO(person,
				AutoIE.isFriendOf.toString(), null, 100);
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String object = (String) doc.getFieldValue("object_t");
			list.add(object);
		}
		return list;
	}
	
	private String JSONToString(String indent, JSONObject jsonObject)
			throws IOException {
		String s;
		if (indent == null || indent.equalsIgnoreCase("OFF")) {
			s = jsonObject.toJSONString();

		} else {
			Writer writer = new JSonWriter();
			jsonObject.writeJSONString(writer);
			s = writer.toString();
		}
		return s;

	}
}


