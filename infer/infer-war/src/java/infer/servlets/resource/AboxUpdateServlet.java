/**
 * CountServlet serves any web page to store the page count
 * CountServlet doPost() method - read explicit data sent by the client (form data).
 * CountServlet doGet() method - read implicit data sent by the client (request headers).
 * 
 */

package infer.servlets.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.builtins.NoValue;

import infer.util.JenaUtil;
import infer.util.SolrUtil;
import infer.util.rules.AllValue;
import infer.util.rules.Blacklist;
import infer.util.rules.CalLocDest;
import infer.util.rules.CalTextDest;
import infer.util.rules.Like;
import infer.util.rules.Poi;
import infer.util.rules.PointToSegment;

@Path("aboxUpdate")
public class AboxUpdateServlet{

	final static Logger log = Logger.getLogger(AboxUpdateServlet.class
			.getName());

	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost(String message,@DefaultValue("false") @QueryParam("optimize") String optimize)
			throws ServletException, IOException {
		return updateAbox(message,optimize);
	}
	
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject updateAbox(String abox,String optimize) throws IOException{
		
		JenaUtil.loadRules();
		
		log.info((" content length is " + abox.length()));
		
		log.fine(" before loadIntoSolr the abox is \n" + abox + "\n");
		JenaUtil.loadIntoSolr(abox);
		if (optimize.equals("true")) {
			SolrUtil.optimize();
		}
		JsonObject result = Json.createObjectBuilder()
				.add("status", "results").build();
		return result;
	}
}
