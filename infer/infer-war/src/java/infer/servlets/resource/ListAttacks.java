package infer.servlets.resource;

import infer.util.SolrUtil;
import infer.util.Util;
import infer.util.generated.IDS;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * 
 * @author Scott Streit
 * 
 *         This class contains the static methods necessary to access solr
 *         through intercepting calls and adding new calls.
 * 
 */
@Path("listAttacks")
@Produces(MediaType.APPLICATION_JSON)
public class ListAttacks {

	final static Logger log = Logger.getLogger(ListAttacks.class.getName());

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost(@QueryParam("from") String from,
			@QueryParam("to") String to, @QueryParam("id") String id, @QueryParam("ip") String ip,
			@DefaultValue("false") @QueryParam("reset") String reset)
			throws ServletException, IOException {
		return processListAttacks(from, to, id, reset, ip);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doGet(@QueryParam("from") String from,
			@QueryParam("to") String to, @QueryParam("id") String id, @QueryParam("ip") String ip,
			@DefaultValue("false") @QueryParam("reset") String reset)
			throws ServletException, IOException {
		String identifier = null;
		String property = null;

		if (id == null) {
			property = IDS.isAttackedByIP.toString();
			identifier = ip;
			
		} else {
			property = IDS.isAttackedByID.toString();
			identifier = id;
		}
		return processListAttacks(from, to, identifier, reset, property);
	}

	@Produces(MediaType.APPLICATION_JSON)
	private JsonObject processListAttacks(String fromTimeString,
			String toTimeString, String id, String resetString, String property) {

		boolean reset = false;

		if (resetString.equalsIgnoreCase("true")) {
			reset = true;
			log.info(" case of reset");
		}

		if (id != null) {
			JsonArrayBuilder statements = Json.createArrayBuilder();
			JsonObjectBuilder jsonStatement = Json.createObjectBuilder();

			jsonStatement = Json.createObjectBuilder();

			if (reset) {
				try {
					SolrUtil.deleteDocument(id);
				} catch (SolrServerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					throw new RuntimeException(e);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

					throw new RuntimeException(e);
				}

				jsonStatement.add("object_t", "true");
				jsonStatement.add("predicate_t", "whitelisted");
				jsonStatement.add("subject_t", id);
				statements.add(jsonStatement);
			} else {

				SolrDocumentList docs = SolrUtil.getDocsbySPO(null,
						property, id, 1);
				if (docs != null && docs.size() > 0) {
					SolrDocument s = docs.get(0);
					log.fine(" doc is " + s);
					log.fine(" doc fields are " + s.getFieldNames());

					jsonStatement.add("object_t", "true");
					jsonStatement.add("predicate_t", "blacklisted");
					jsonStatement.add("subject_t", id);
					statements.add(jsonStatement);
				} else {
					jsonStatement.add("object_t", "false");
					jsonStatement.add("predicate_t", "blacklisted");
					jsonStatement.add("subject_t", id);
					statements.add(jsonStatement);
				}

			}

			JsonObject result = Json.createObjectBuilder()
					.add("statements", statements).build();
			log.fine("JSON string is " + result.toString());

			return result;
		} else {

			JsonArrayBuilder statements = Json.createArrayBuilder();
			try {

				SolrDocumentList docs = SolrUtil.getDocsbySPO(null,
						property, null, SolrUtil.MAX_DOCS);

				Calendar fromTime = Util.toCalendar(fromTimeString);
				Calendar toTime = Util.toCalendar(toTimeString);

				for (int i = 0; i < docs.size(); i++) {
					SolrDocument doc = docs.get(i);
					String subject = (String) doc.getFieldValue("subject_t");
					String predicate = (String) doc
							.getFieldValue("predicate_t");
					String deviceID = (String) doc.getFieldValue("object_t");

					Date time = (Date) doc.getFieldValue("timestamp");
					TimeZone tz = TimeZone.getTimeZone("UTC");
					DateFormat df = new SimpleDateFormat(
							"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
					df.setTimeZone(tz);
					String timeAsISO = df.format(time);
					Calendar incidentTime = Util.toCalendar(timeAsISO);

					if (incidentTime.after(fromTime)
							&& incidentTime.before(toTime)) {

						JsonObject jsonStatement = Json.createObjectBuilder()
								.add("object_t", deviceID)
								.add("predicate_t", predicate)
								.add("subject_t", subject)
								.add("time", timeAsISO).build();
						statements.add(jsonStatement);
					}
				}

				JsonObject result = Json.createObjectBuilder()
						.add("statements", statements).build();
				log.fine("JSON string is " + result.toString());

				return result;

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

}
