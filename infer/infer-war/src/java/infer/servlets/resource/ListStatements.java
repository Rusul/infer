package infer.servlets.resource;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

/**
 * 
 * @author Scott Streit
 * 
 *         This class contains the static methods necessary to access solr
 *         through intercepting calls and adding new calls.
 * 
 */
@Path("listStatements")
@Produces(MediaType.APPLICATION_JSON)
public class ListStatements{
	final static Logger log = Logger.getLogger(ListStatements.class
			.getName());
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost(@QueryParam("subject") String r,
			@QueryParam("predicate") String p,
			@QueryParam("object") String o)
			throws ServletException, IOException {
		return processListStatements(r,p,o);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doGet(@QueryParam("subject") String r,
			@QueryParam("predicate") String p,
			@QueryParam("object") String o)
			throws ServletException, IOException {
		
		return processListStatements(r,p,o);	
	}
	
	
	private JsonObject processListStatements(String r, String p, String o) {
		
		
		JsonArrayBuilder statements = Json.createArrayBuilder();

		try {
			
			
			SolrDocumentList docs = SolrUtil.getDocsbySPO(r, p, o,
					SolrUtil.MAX_DOCS);
			
			for (int i = 0; i < docs.size(); i++) {
				SolrDocument doc = docs.get(i);
				String subject = (String) doc.getFieldValue("subject_t");
				String predicate = (String) doc.getFieldValue("predicate_t");
				String object = (String) doc.getFieldValue("object_t");
				String reifiedId = (String) doc.getFieldValue("reifiedId_t");
				
				
				JsonObjectBuilder jsonStatement = Json.createObjectBuilder()
				.add("object_t", object)
				.add("predicate_t", predicate)
				.add("subject_t", subject);
				
				
				if(reifiedId != null){
					jsonStatement.add("reifiedId_t", reifiedId);
				}
				
				statements.add(jsonStatement.build());
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

