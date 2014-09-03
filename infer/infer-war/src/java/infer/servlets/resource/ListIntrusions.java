package infer.servlets.resource;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
@Path("listIntrusions")
@Produces(MediaType.APPLICATION_JSON)
public class ListIntrusions {
	final static Logger log = Logger.getLogger(ListIntrusions.class
			.getName());
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost(@QueryParam("devId") String deviceId,
			@QueryParam("ip") String ipAddress)
			throws ServletException, IOException {
		return processListIncidents(deviceId,ipAddress);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doGet(@QueryParam("devId") String deviceId,
			@QueryParam("ip") String ipAddress)
			throws ServletException, IOException {
		return processListIncidents(deviceId,ipAddress);
	}

	
	private JsonObject processListIncidents(String deviceId, 
			String ipAddress) {
			
		JsonArrayBuilder incident = Json.createArrayBuilder();
		JsonArrayBuilder incidents = Json.createArrayBuilder();
		try{	
			
		SolrDocumentList docs;
		if(ipAddress != null){
		docs = SolrUtil.getDocsbySPO(null, 
				IDS.hasSourceIP.toString(), ipAddress, SolrUtil.MAX_DOCS);
		}
		else{
		docs = SolrUtil.getDocsbySPO(null, 
				IDS.hasSourceDeviceId.toString(), deviceId, SolrUtil.MAX_DOCS);
		}
		
			for (int i = 0; i < docs.size(); i++) {
				SolrDocument doc = docs.get(i);
				String subject = (String) doc.getFieldValue("subject_t");
				SolrDocumentList docs2 = SolrUtil.getDocsbySPO(subject,
						null,null,SolrUtil.MAX_DOCS);
				
				for(int j = 0; j<docs2.size();j++){
					SolrDocument doc2 = docs2.get(j);
					String predicate = (String) doc2.getFieldValue("predicate_t");
					String object = (String) doc2.getFieldValue("object_t");
					
					JsonObject jsonStatement = Json.createObjectBuilder()
							.add("subject_t", subject)
							.add("predicate_t", predicate)
							.add("object_t", object).build();
					incident.add(jsonStatement);
				}
				incidents.add(incident);
				
			}

			JsonObject result = Json.createObjectBuilder()
					.add("statements", incidents).build();
			log.fine("JSON string is " + result.toString());
			
			return result;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}


}

