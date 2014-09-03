package infer.servlets.resource;

import infer.util.JenaUtil;
import infer.util.Util;
import infer.util.abox.LoadIncidents;
import infer.util.generated.Provenance;
import infer.util.rules.AllValue;
import infer.util.rules.Blacklist;
import infer.util.rules.CalLocDest;
import infer.util.rules.CalTextDest;
import infer.util.rules.Like;
import infer.util.rules.Poi;
import infer.util.rules.PointToSegment;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.ServletException;
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
import javax.xml.bind.DatatypeConverter;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.builtins.NoValue;

@Path("checkSecurity")
public class CheckSecurity {

	final static Logger log = Logger.getLogger(CheckSecurity.class.getName());

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost(@QueryParam("ip") String ip, 
			@QueryParam("mac") String mac,
			@QueryParam("devId") String devId,
			@QueryParam("dom") String dom,
			@QueryParam("fField") String fField,
			@QueryParam("aVal") String aVal,
			@QueryParam("dVal") String dVal) throws ServletException, IOException {
		return checkSecurity(ip,mac,devId,dom,fField,aVal,dVal);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doGet(@QueryParam("ip") String ip, 
			@QueryParam("mac") String mac,
			@QueryParam("devId") String devId,
			@QueryParam("dom") String dom,
			@QueryParam("fField") String fField,
			@QueryParam("aVal") String aVal,
			@QueryParam("dVal") String dVal)
			throws ServletException, IOException {
		return checkSecurity(ip,mac,devId,dom,fField,aVal,dVal);
	}
	
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject checkSecurity(String ipAddress, String macAddress, String deviceId, 
			String domain, String failedField, String actualValue, String desiredValue) 
					throws IOException{
		
			JenaUtil.loadRules();
			Model model = ModelFactory.createDefaultModel();
			
			Individual provenance = LoadIncidents.createProvenance(model, "IDS", "websec");
		
			Calendar cal = Util.getNow();
			String time = Util.calendarToISO8601String(cal);

			LoadIncidents.createIncident(model, ipAddress, domain, macAddress,
					deviceId, "none", failedField, actualValue, desiredValue,
					time, provenance);
			log.fine(" before loadIntoSolr the statements are \n"
					+ model.listStatements().toList().toString() + "\n");

			JenaUtil.loadIntoSolr(model);
			JsonObject result = Json.createObjectBuilder()
					.add("status", "Incident Added.").build();
			
			return result;

	}
}