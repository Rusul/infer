/**
 * CountServlet serves any web page to store the page count
 * CountServlet doPost() method - read explicit data sent by the client (form data).
 * CountServlet doGet() method - read implicit data sent by the client (request headers).
 * 
 */

package infer.servlets.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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

import org.apache.commons.io.IOUtils;

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

@Path("loadData")
public class LoadData{

	final static Logger log = Logger.getLogger(LoadData.class
			.getName());

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost()
			throws ServletException, IOException {
		
		JenaUtil.loadRules();
		String abox = null;
		
		/*
		 * Have to process one file at a time.  Jena models do not order and the routes have to
		 * be ordered.
		 */
		for (int i=0; i < 44; i++) {
			String fileName = "/" + i + ".nt";
			log.info(" loading filename = " + fileName);
			SolrUtil.optimize();
		}

		JsonObject result = Json.createObjectBuilder()
				.add("status", "success").build();
		return result;
	}
	
}
