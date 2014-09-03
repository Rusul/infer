package infer.servlets.resource;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.ListIterator;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
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

import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import infer.util.SolrUtil;
import infer.util.generated.IDS;
import infer.util.Util;

/**
 * 
 * @author Scott Streit
 * 
 *         This class contains the static methods necessary to access solr
 *         through intercepting calls and adding new calls.
 * 
 */
@Path("listFacets")
@Produces(MediaType.APPLICATION_JSON)
public class ListFacets {
	
	final static Logger log = Logger.getLogger(ListFacets.class
			.getName());
	
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doPost(@QueryParam("from") String from,
			@QueryParam("to") String to,
			@DefaultValue("3HOURS") @QueryParam("gap") String gap)
			throws ServletException, IOException {
		return processListFacets(from,to,gap);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject doGet(@QueryParam("from") String from,
			@QueryParam("to") String to,
			@DefaultValue("3HOURS") @QueryParam("gap") String gap)
			throws ServletException, IOException {
		return processListFacets(from,to,gap);
	}
	
	private JsonObject processListFacets(String fromTime, String toTime, String gap) {
	


		try {
			
			
			List facetsInc = SolrUtil.getFacetsByObjectTime(null, IDS.incidentTime.toString(), null,
					fromTime,toTime,"+"+gap,true);
			
			RangeFacet.Date date = (RangeFacet.Date) facetsInc.get(0);
			JsonArrayBuilder incidents = Json.createArrayBuilder();

			for(int i = 0;i<date.getCounts().size();i++){
				JsonObject singleInc = Json.createObjectBuilder()
						.add("time",date.getCounts().get(i).getValue())
						.add("count", date.getCounts().get(i).getCount()).build();
				incidents.add(singleInc);
			}
			
			List facetsAttack = SolrUtil.getFacetsByObjectTime(null, IDS.attackStartTime.toString(), null,
					fromTime,toTime,"+"+gap,true);
			
			RangeFacet.Date date2 = (RangeFacet.Date) facetsAttack.get(0);
			JsonArrayBuilder attacks = Json.createArrayBuilder();
			for(int i = 0;i<date2.getCounts().size();i++){
				JsonObject singleAttack = Json.createObjectBuilder()
						.add("time",date2.getCounts().get(i).getValue())
						.add("count", date2.getCounts().get(i).getCount()).build();
				attacks.add(singleAttack);	
			}
			JsonObject result = Json.createObjectBuilder()
					.add("incidents", incidents)
					.add("attacks", attacks.build()).build();
			
			log.fine("JSON string is " + result.toString());
			return result;

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}


}

