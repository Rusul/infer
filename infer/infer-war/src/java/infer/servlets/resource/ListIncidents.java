package infer.servlets.resource;

import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
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

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import infer.util.SolrUtil;
import infer.util.Util;
import infer.util.generated.IDS;

/**
 * 
 * @author Scott Streit
 * 
 *         This class contains the static methods necessary to access solr
 *         through intercepting calls and adding new calls.
 * 
 */
@Path("listIncidents")
@Produces(MediaType.APPLICATION_JSON)
public class ListIncidents extends HttpServlet  {
	final static Logger log = Logger.getLogger(ListIncidents.class
			.getName());
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost(@QueryParam("from") String from,
			@QueryParam("to") String to,
			@DefaultValue("3HOURS") @QueryParam("gap") String gap)
			throws ServletException, IOException {
		return processListIncidents(from,to,gap);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doGet(@QueryParam("from") String from,
			@QueryParam("to") String to,
			@DefaultValue("3HOURS") @QueryParam("gap") String gap)
			throws ServletException, IOException {
		return processListIncidents(from,to,gap);
	}

	
	private JsonObject processListIncidents(String fromTimeString, String toTimeString, String gap) {
			
		JsonArrayBuilder statements = Json.createArrayBuilder();
		try{	
			
		SolrDocumentList docs = SolrUtil.getDocsbySPO(null, 
				IDS.hasSourceIP.toString(), null, SolrUtil.MAX_DOCS);
		
		Calendar fromTime = Util.toCalendar(fromTimeString);
		Calendar toTime = Util.toCalendar(toTimeString);

			for (int i = 0; i < docs.size(); i++) {
				SolrDocument doc = docs.get(i);
				String subject = (String) doc.getFieldValue("subject_t");
				String predicate = (String) doc.getFieldValue("predicate_t");
				String sourceIP = (String) doc.getFieldValue("object_t");
				
				Date time = (Date) doc.getFieldValue("timestamp");
				TimeZone tz = TimeZone.getTimeZone("UTC");
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				df.setTimeZone(tz);
				String timeAsISO = df.format(time);
				Calendar incidentTime = Util.toCalendar(timeAsISO);
				
				
				if(incidentTime.after(fromTime) && incidentTime.before(toTime)){
				
					JsonObject jsonStatement = Json.createObjectBuilder()
							.add("object_t", sourceIP)
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

