package infer.servlets.resource;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
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
import infer.util.abox.LoadLikes;

/**
 * 
 * @author Scott Streit
 * 
 *         This class contains the static methods necessary to access solr
 *         through intercepting calls and adding new calls.
 * 
 */
@Path("writeFacebookLikes")
@Produces(MediaType.APPLICATION_JSON)
public class writeFacebookLikes extends HttpServlet  {
	final static Logger log = Logger.getLogger(writeFacebookLikes.class
			.getName());
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost(@QueryParam("name") String name, 
			@QueryParam("email") String email,
			@QueryParam("categories") List<String> categories,
			@QueryParam("likes") List<String> likes,
			@QueryParam("birthdays") String birthday)
			throws ServletException, IOException {	
		return processListStatements(name, email, categories, likes, birthday);	
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doGet(@QueryParam("name") String name, 
			@QueryParam("email") String email,
			@QueryParam("categories") List<String> categories,
			@QueryParam("likes") List<String> likes,
			@QueryParam("birthdays") String birthday)
			throws ServletException, IOException {	
		return processListStatements(name, email, categories, likes, birthday);	
	}
	

	
	private JsonObject processListStatements(String name, String email, List<String> categories, 
			List<String> likes, String birthday) {
		
		JsonArrayBuilder statements = Json.createArrayBuilder();


		try {
			
			birthday="01/01/1970";
			log.fine("Name="+name);
			log.fine("Email="+email);
			log.fine("Birthday="+birthday);
			log.fine("Likes="+likes);
			log.fine("Categories="+categories);
		     LoadLikes.doFromServlet(name, email, birthday,likes, categories);
			
			
			

			
			JsonObject jsonStatement = Json.createObjectBuilder()
				.add("name", name)
				.add("email", email)
				.add("categories", categories.toString())
				.add("likes", likes.toString()).build();
			statements.add(jsonStatement);
			
			JsonObject result = Json.createObjectBuilder()
				.add("statements", statements.build()).build();
			
			return result;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}


}

