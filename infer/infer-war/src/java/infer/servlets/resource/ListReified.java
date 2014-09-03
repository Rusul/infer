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
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
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
@Path("listReified")
@Produces(MediaType.APPLICATION_JSON)
public class ListReified extends HttpServlet  {
	final static Logger log = Logger.getLogger(ListReified.class
			.getName());
	
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost(@QueryParam("subject") String r,
					@QueryParam("predicate") String p,
					@QueryParam("object") String o,
					@QueryParam("reifId") String reifId)
			throws ServletException, IOException {
		return processReification(r,p,o,reifId);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doGet(@QueryParam("subject") String r,
				@QueryParam("predicate") String p,
				@QueryParam("object") String o,
				@QueryParam("reifId") String reifId)
			throws ServletException, IOException {
		return processReification(r,p,o,reifId);
	}
	
	
	private JsonObject processReification(String r, String p, String o, String reifId) {

		JsonArrayBuilder statements = Json.createArrayBuilder();
		

		try {

			log.fine("REIF ID: " + reifId);

			SolrDocumentList docs = SolrUtil.getReificationDocs(reifId);
			for (int i = 0; i < docs.size(); i++) {
				SolrDocument doc = docs.get(i);
				String subject = (String) doc.getFieldValue("r_subject_t");
				String predicate = (String) doc.getFieldValue("r_predicate_t");
				String object = (String) doc.getFieldValue("r_object_t");
				log.fine("DOCS: " + doc);
				JsonObject jsonStatement = Json.createObjectBuilder()
						.add("r_object_t", object)
						.add("r_predicate_t", predicate)
						.add("r_subject_t", subject).build();
				statements.add(jsonStatement);

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
