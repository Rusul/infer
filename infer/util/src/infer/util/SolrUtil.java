package infer.util;

import infer.util.generated.AutoIE;
import infer.util.generated.IDS;
import infer.util.generated.Vcard;
import infer.util.rules.GEOPlace;
import infer.util.rules.GEOPoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.collections.list.SetUniqueList;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * 
 * @author Scott Streit
 * 
 *         This class contains the static methods necessary to Solr. The calls
 *         to Solr are through web service calls (post) and do not require any
 *         jar files from Solr. The assumptions are that the aSor schema.xml
 *         file has dynamic fields defined as an _type suffix. For example a
 *         text field named banana in the relational database becomes banana_t
 *         in solr. There is a dependency on a file named load.properties off
 *         the classpath.
 * 
 */
public class SolrUtil {
	private static Properties properties = null;
	final static Logger logger = Logger.getLogger(SolrUtil.class.getName());

	private static Map<String, RouteHistorySummary> sessionRouteHistorySummary = new HashMap<String, RouteHistorySummary>();
	public static double CLOSE_CHECK = 0.05;
	private static double INFER_CLOSE_CHECK = 0.05;
	private static double INFER_TEST_CHECK = 0.05;

	private static double REMOVE_ROUTE_CLOSE_CHECK = 0.15;
	public static int MAX_LIKES = 100;
	public static int MAX_DOCS = 10000;
	public static int NUM_FAIL_POINTS = 5;

	private static SolrServer solrServer = null;
	private static String solrURI = null;
	private static String zooURI = null;
	private static String collection = null;
	public static String identifierField = "";
	public static String hierarchyField = null;
	public static String hierarchySeparator = null;
	public static String ruleURL = null;
	

	public static boolean commitEveryTriple = false;

	private static Calendar HISTORY_SINCE = Util
			.toCalendar("2013-01-01T00:00:15.340Z");

	public static Properties getProperties() {
		if (properties == null) {
			InputStream is = null;
			is = SolrUtil.class.getResourceAsStream("/load.properties");

			properties = new Properties();
			try {
				properties.load(is);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				logger.severe(e.toString());
				throw new RuntimeException(
						" could not load properties file of /load.properties"
								+ e);
			}
			String val = properties.getProperty("CLOSE_CHECK");
			if (val != null) {
				CLOSE_CHECK = Double.parseDouble(val);
			}
			val = properties.getProperty("INFER_CLOSE_CHECK");
			if (val != null) {
				INFER_CLOSE_CHECK = Double.parseDouble(val);
			}
			val = properties.getProperty("REMOVE_ROUTE_CLOSE_CHECK");
			if (val != null) {
				REMOVE_ROUTE_CLOSE_CHECK = Double.parseDouble(val);
			}
			val = properties.getProperty("HISTORY_SINCE");
			if (val != null) {
				HISTORY_SINCE = Util.toCalendar(val);
			}
			val = properties.getProperty("NUM_FAIL_POINTS");
			if (val != null) {
				NUM_FAIL_POINTS = Integer.parseInt(val);
			}
			val = properties.getProperty("SOLR_URI");
			if (val != null) {
				solrURI = val;
			}

			val = properties.getProperty("RULE_URL");
			logger.fine(" RULE_URL from property file is " + val);
			if (val != null) {
				ruleURL = val;
			}

			val = System.getProperty("zookeeper");
			logger.fine(" ZOO_URI from System property is " + val);
			if (val != null) {
				zooURI = val;
			}

			val = System.getProperty("collection");
			logger.fine(" collection from System property is " + val);
			if (val != null) {
				collection = val;
			}
			
			val = properties.getProperty("COMMIT_EVERY_TRIPLE");
			if ("true".equals(val)) {
				commitEveryTriple = true;
			}

		}
		return properties;
	}

	/**
	 * Pipes everything from the source to the dest. If dest is null, then
	 * everything is read from source and thrown away.
	 * 
	 * @param source
	 *            a stream of data to send.
	 * @param dest
	 *            a destination for the input.
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static void pipe(InputStream source, OutputStream dest)
			throws IOException {
		byte[] buf = new byte[1024];
		int read = 0;
		while ((read = source.read(buf)) >= 0) {
			if (null != dest)
				dest.write(buf, 0, read);
		}
		if (null != dest)
			dest.flush();
	}

	public static float match(String text, List<String> likes,
			List<String> interests, StringBuffer reason) {
		logger.fine(" MATCH text =" + text + " likes = " + likes
				+ " interests = " + interests);
		text = text.toLowerCase();
		// First are any interests in the text.
		for (int i = 0; i < interests.size(); i++) {
			logger.fine("*** in interest loop text is [" + text + "]");
			logger.fine("*** interest is [" + interests.get(i) + "]");
			if (text.contains(interests.get(i))) {
				String msg = " adding 100 have an interest in text match, interest is "
						+ interests.get(i) + "\n";
				reason.append(msg);
				logger.fine(msg);
				return 100;
			}
		}

		// First are any likes in the text.
		for (int i = 0; i < likes.size(); i++) {
			if (text.contains(likes.get(i))) {
				String msg = " adding 5 have an likes match, like is "
						+ likes.get(i) + "\n";
				reason.append(msg);
				logger.fine(msg);
				return 5;
			}
		}

		String[] tokens = text.split(" ");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].length() < 4) {
				continue;
			}

			if (interests.contains(tokens[i])) {
				String msg = "adding 100 interest has token with is "
						+ tokens[i] + "\n";
				reason.append(msg);
				return 100;
			}

			if (likes.contains(tokens[i])) {
				String msg = " addiing 5 like has token with is " + tokens[i]
						+ "\n";
				reason.append(msg);
				logger.fine(msg);
				return 5;
			}
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public static List<String> getLikes(String person) {
		List<String> categories = SetUniqueList
				.decorate(new ArrayList<String>());
		SolrDocumentList docs = getDocsbySPO(person.toString(),
				AutoIE.describesLikesAs.toString(), null, MAX_LIKES);

		for (int i = 0; i < docs.size(); i++) {

			SolrDocument doc = docs.get(i);
			String object = (String) doc.getFieldValue("object_t");
			SolrDocumentList docs2 = getDocsbySPO(object,
					AutoIE.opinionCategory.toString(), null, 100);
			for (int j = 0; j < docs2.size(); j++) {
				SolrDocument doc2 = docs2.get(j);
				String object2 = (String) doc2.getFieldValue("object_t");
				logger.fine("LIKE opinionCategory is " + object2);
				addPieces(categories, object2);
			}
			logger.fine(" AT END OF CATEGORY LIKES likes are " + categories);

			SolrDocumentList docs3 = getDocsbySPO(object,
					AutoIE.opinionText.toString(), null, MAX_LIKES);

			for (int k = 0; k < docs3.size(); k++) {
				SolrDocument doc3 = docs3.get(k);
				String object3 = (String) doc3.getFieldValue("object_t");
				logger.fine("LIKE OPINIONTEXT is " + object3);
				addPieces(categories, object3);
			}

			SolrDocumentList docs4 = getDocsbySPO(object,
					AutoIE.opinionSynonyms.toString(), null, MAX_LIKES);

			for (int l = 0; l < docs4.size(); l++) {
				SolrDocument doc4 = docs4.get(l);
				String object4 = (String) doc4.getFieldValue("object_t");
				logger.fine("LIKE OPINIONSYNONYM is " + object4);
				String[] syns = object4.split(",");
				for (int m = 0; m < syns.length; m++) {
					categories.add(syns[m].trim());
				}
			}
		}
		logger.fine(" AT END OF GET LIKES likes are " + categories);
		return categories;
	}

	public static void addPieces(List<String> categories, String term) {
		logger.fine("add pieces " + term);
		String[] parts = term.split("&| |;|\\/");
		int indexOf = term.indexOf("&");
		if (indexOf > 0) {
			String pieceOne = term.substring(0, indexOf);
			String pieceTwo = term.substring(indexOf + 1);
			String[] one = pieceOne.split("&| |;|\\/");
			String[] two = pieceTwo.trim().split("&| |;|\\/");

			categories.add(one[one.length - 1].toLowerCase());
			categories.add(two[0].toLowerCase());
			return;
		} else if (term.contains(";")) {
			int where = term.indexOf(";");
			String temp = term.substring(0, where);
			String[] x = temp.split("&| |;|\\/");
			if (x.length == 1) {
				categories.add(x[0].toLowerCase());
			}
			return;
		}
		if (parts.length > 1) {
			return;
		}
		term = term.replace("general|community", "");
		term = term.trim();
		if (term.length() > 0) {
			categories.add(term.toLowerCase());
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Float> getPrevious(String userid, String email) {
		Map<String, Float> map = new HashMap<String, Float>();
		String personString = Util.getPerson(userid, email);
		SolrDocumentList docs = getDocsbySPO(personString,
				AutoIE.storesAsHistogram.toString(), null, MAX_DOCS);

		for (int i = 0; i < docs.size(); i++) {

			SolrDocument doc = docs.get(i);
			String subject = (String) doc.getFieldValue("subject_t");
			String predicate = (String) doc.getFieldValue("predicate_t");
			String object = (String) doc.getFieldValue("object_t");
			SolrDocumentList docs2 = getDocsbySPO(object,
					AutoIE.selectedTerms.toString(), null, MAX_DOCS);
			for (int j = 0; j < docs2.size(); j++) {
				SolrDocument doc2 = docs2.get(j);
				String object2 = (String) doc2.getFieldValue("object_t");
				String term = object2.toLowerCase();
				Float f = map.get(term);
				if (f != null) {

					map.put(term, f + 30);
				} else {
					map.put(term, new Float(30));
				}
			}
		}
		return map;
	}

	public static long writeTimingEvent(String term, long startTime,
			long endTime) {
		logger.info(term + " time since in milliseconds "
				+ (endTime - startTime));
		return endTime;
	}

	public static boolean considerRoute(String route, String status) {

		SolrDocumentList docs = getDocsbySPO(route,
				AutoIE.routeStatus.toString(), null, 1);
		logger.fine(" in considerRoute with route = " + route + " docs are "
				+ docs);
		String routeStatus = null;
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String object = (String) doc.getFieldValue("object_t");
			routeStatus = object;
			logger.fine(" routeStatus =" + routeStatus);
		}
		if (status == null) {
			if (routeStatus == null) {
				return false;
			}
		} else {
			if (status.equals(routeStatus)) {
				logger.fine("RETURNING for route " + route + " status is "
						+ status + " routeStatus is " + routeStatus);

				return true;
			}

		}
		return false;
	}

	public static String getLastDriverBasedOnAuto(String auto) {
		logger.fine(" auto is in getDriverBasedOnAuto " + auto);
		SolrDocumentList docs = getDocsbySPO(auto,
				AutoIE.isOperatedBy.toString(), null, 1);
		String person = null;
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String subject = (String) doc.getFieldValue("subject_t");
			String predicate = (String) doc.getFieldValue("predicate_t");
			String object = (String) doc.getFieldValue("object_t");
			person = object;
			logger.fine(" person is in getDriverBasedOnAuto " + person);
		}
		return person;
	}

	public static String getDriver(String route) {
		SolrDocumentList docs = getDocsbySPO(route,
				AutoIE.isDrivenBy.toString(), null, 1);
		String auto = null;
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String subject = (String) doc.getFieldValue("subject_t");

			String predicate = (String) doc.getFieldValue("predicate_t");
			String object = (String) doc.getFieldValue("object_t");
			auto = object;

		}
		docs = getDocsbySPO(auto, AutoIE.isOperatedBy.toString(), null, 1);
		String person = null;
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String subject = (String) doc.getFieldValue("subject_t");

			String predicate = (String) doc.getFieldValue("predicate_t");
			String object = (String) doc.getFieldValue("object_t");
			person = object;

		}
		return person;
	}

	public static List<GEOPoint> getPoints(String person, int noRoutes,
			String status, Calendar since) {

		long startTime = System.currentTimeMillis();

		List<String> autos = getListOfObjects(person,
				AutoIE.operates.toString(), 5);
		List<String> routes = getRoutesFor(autos.get(0), status,
				AutoIE.drives.toString());

		List<GEOPoint> retList = getPoints(routes, noRoutes, since, null,
				false, false);

		startTime = SolrUtil.writeTimingEvent(" TIME IN  getPoints ",
				startTime, System.currentTimeMillis());

		Collections.sort(retList);

		return retList;
	}

	public static Route getActualRoute(List<String> l, Calendar since,
			int maxActualPoints) {
		// First get actual points

		List<Route> actualRouteList = getLineSegments(l, since, 1, "actual",
				maxActualPoints);

		Route actualRoute = null;
		if (actualRouteList != null && actualRouteList.size() != 0) {
			actualRoute = actualRouteList.get(0);
			GEOPoint origin = getOriginPoint(actualRoute.getRouteName(),"actual");
			actualRoute.setOrigin(origin);
			logger.fine(" Actual route is " + actualRoute + " and should be " + maxActualPoints +" line segments long");
		}
		return actualRoute;
	}
	
	public static GEOPoint getOriginPoint(String routeName, String status){
		logger.fine("setting origin point");
		SolrDocumentList docsPoint = getDocsbySPO(routeName,
				AutoIE.hasAsAPointOfOrigin.toString(), null,MAX_DOCS);
		if(docsPoint.size()>0){
			SolrDocument docPoint = docsPoint.get(0);
			String originPoint = (String) docPoint.getFieldValue("object_t");
			
			SolrDocumentList docsLat = getDocsbySPO(originPoint,
					Vcard.latitude.toString(), null,MAX_DOCS);
			SolrDocument docLat = docsLat.get(0);
			String latitude = (String) docLat.getFieldValue("object_t");
			
			SolrDocumentList docsLng = getDocsbySPO(originPoint,
					Vcard.longitude.toString(), null,MAX_DOCS);
			SolrDocument docLng = docsLng.get(0);
			String longitude = (String) docLng.getFieldValue("object_t");
			
			SolrDocumentList docsWhen = getDocsbySPO(originPoint,
					AutoIE.trackPointWhen.toString(), null,MAX_DOCS);
			SolrDocument docWhen = docsWhen.get(0);
			String when = (String) docWhen.getFieldValue("object_t");
			
			GEOPoint origin = new GEOPoint();
			origin.setLat(latitude);
			origin.setLng(longitude);
			origin.setWhen(when);
			origin.setStatus(status);
			
			return origin;
		}
		else{
			return null;
		}
	}

	public static boolean isSatisfied(CalendarEvent ce, Route actualRoute) {
		if (ce == null || ce.getPoint() == null) {
			return false;
		}
		if (actualRoute == null) {
			return false;
		}
		if (ce.getPoint().isClose(actualRoute.getOrigin(), INFER_CLOSE_CHECK)) {
			return true;
		}
		return false;
	}

	public static void markSatisfied(CalendarEvent ce) {
		Model m = ModelFactory.createDefaultModel();
		Resource r = m.createResource(ce.getEventURI());
		Literal l = m.createLiteral("SATISFIED");
		m.add(r, AutoIE.eventStatus, l);
		JenaUtil.indexRDF(m, JenaUtil.name);
	}

	public static Route fillInRoute(Route r) {
		SolrDocumentList docs = SolrUtil.getDocsbySPO(r.getRouteName(),
				AutoIE.isTrackedBy.toString(), null, SolrUtil.MAX_DOCS, true);
		GEOPoint gp = null;
		for (int j = 0; j < docs.size(); j++) {
			SolrDocument doc = docs.get(j);
			String trackPoint = (String) doc.getFieldValue("object_t");

			gp = SolrUtil.fillInGEOPoint(trackPoint,
					Util.toCalendar("0000-00-00T00:00:00.000Z"), "INPROGRESS");
		}
		return r;
	}

	public static Map<String, List<Route>> getRouteAndStatus(String id,
			String person, Route actualRoute, Calendar since, String who,
			boolean infer, boolean history, boolean events, boolean routeTime,
			boolean places, int maxActualPoints, boolean inferredCalDest,
			LineSegment lastSegment) {
		GEOPoint lastPoint = lastSegment.getCurrentPoint();
		logger.fine(" INFER_CLOSE_CHECK is " + INFER_CLOSE_CHECK);
		List<CalendarEvent> calendarEvents = null;
		Map<String, List<Route>> statusRoutes = new HashMap<String, List<Route>>();
		if (events) {
			Calendar rightNow = Util.getNow();
			calendarEvents = getCalendarEvents(person, rightNow, lastPoint);
			logger.fine("right now is "
					+ Util.calendarToISO8601String(rightNow)
					+ " after getCalendarEvents events IN THE FUTURE = "
					+ calendarEvents);
			for (int i = 0; i < calendarEvents.size(); i++) {
				CalendarEvent ce = calendarEvents.get(0);
				if (isSatisfied(ce, actualRoute)) {
					markSatisfied(ce);
					continue;
				}
				String eventNowString = ce.getPoint().getWhen();
				logger.fine(" eventNowString = " + eventNowString);

				Calendar eventNow = Util.toCalendar(eventNowString);
				long nowInMinutes = Util.getTimeOfDayInMinutes(rightNow);
				logger.fine(" now in minutes = " + nowInMinutes);

				long eventInMinutes = Util.getTimeOfDayInMinutes(eventNow);
				logger.fine(" event in minutes = " + eventInMinutes);
				if (eventInMinutes - nowInMinutes < 70) {
					Route r = new Route();
					r.setRouteName(actualRoute.getRouteName());
					r.getLastLineSegment().getPoints().add(ce.getPoint());
					List<Route> routes = new ArrayList<Route>();
					routes.add(r);
					statusRoutes.put("inferred", routes);
					logger.fine(" inferredCalDest = " + inferredCalDest);
					if (!inferredCalDest) {
						return statusRoutes;
					}
				}
			}

			logger.info(" calendar events are " + calendarEvents);

		}
		if (actualRoute == null || actualRoute.getLineSegments() == null) {
			return statusRoutes;
		}
		logger.fine(" actualRoute is " + actualRoute);
		if (actualRoute.getLineSegments().size() > 0) {
			logger.fine("case of actual points > 0 history = " + history
					+ " infer =" + infer);
			if (history || infer) {

				maintainHistory(id, person, actualRoute, since, who, routeTime);

				statusRoutes = getHistoryAndInferredPoints(
						actualRoute.getRouteName(),
						sessionRouteHistorySummary.get(id), lastSegment, infer,
						history);

			}
			List<Route> a = new ArrayList<Route>();
			a.add(actualRoute);
			statusRoutes.put("actual", a);
			// nothing current. so let's go back 20 minutes and get an event.
			// Calendar previous = Util.getNowPlus(-1 * (1000 * 60 * 20));
			// List<CalendarEvent> ce = getCalendarEvents(person, previous,
			// lastPoint);
			// logger.fine(" before calling addPOIS with place = " + places
			// + " and  ce = " + ce);
			addPOIS(calendarEvents, events, actualRoute,
					actualRoute.getRouteName(), statusRoutes, person, places);

		}
		return statusRoutes;

	}

	public static void addPOIS(List<CalendarEvent> ce, boolean events,
			Route actualRoute, String route,
			Map<String, List<Route>> statusRoutes, String person, boolean places) {
		CalendarEvent oldEvent = null;
		List<LineSegment> actualLineSegs = actualRoute.getLineSegments();
		GEOPoint lastPoint = actualLineSegs.get(actualLineSegs.size() - 1)
				.getCurrentPoint();
		logger.fine(" in addPOIS with the last actual point = " + lastPoint);

		if (places) {

			List<String> interests = new ArrayList<String>();

			getInterests(person, interests, null);
			StringBuffer buff = new StringBuffer();
			for (int i = 0; i < interests.size(); i++) {
				if (i > 0) {
					buff.append("|");
				}
				buff.append(interests.get(i));
			}
			logger.fine(" interests are " + interests);
			List<GEOPlace> gps = getPOI(lastPoint, buff.toString());
			if (gps != null && gps.size() > 0) {
				logger.fine(" got a gps ");
				Route r = new Route();
				r.setAroundDestination(gps);
				r.setRouteName(route);
				List<Route> routes = new ArrayList<Route>();
				routes.add(r);
				logger.fine(" Adding POI Radar before putting inferredPOIS in statusRoutes");
				statusRoutes.put("inferredPOIs", routes);
			}
		}
	}
	
	public static List<GEOPlace> getPOIS(String person, GEOPoint point){
		logger.fine(" in addPOIS with the point = " + point);
			
		List<String> interests = new ArrayList<String>();

		getInterests(person, interests, null);
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < interests.size(); i++) {
			if (i > 0) {
				buff.append("|");
			}
			buff.append(interests.get(i));
		}
		
		logger.fine(" interests are " + interests);
		List<GEOPlace> gps = getPOI(point, buff.toString());
			
		return gps;
	}
	
	public static List<Route> getLineSegments(String route, Calendar since,
			String status) {
		List<String> l = new ArrayList<String>();
		l.add(route);
		return getLineSegments(l, since, Integer.MAX_VALUE, status);
	}

	public static List<Route> getLineSegments(List<String> routes,
			Calendar since, int maxRoutes, String status) {
		return getLineSegments(routes, since, maxRoutes, status,
				Integer.MAX_VALUE);
	}

	public static List<Route> getLineSegments(List<String> routes,
			Calendar since, int maxRoutes, String status, int maxPoints) {
		// if the status is null, do not consider it.
		logger.fine(" in getLineSegments wtih routes = " + routes + " since = "
				+ since + " maxRoutes = " + maxRoutes + " status =" + status
				+ " maxPoints = " + maxPoints);
		List<Route> routesToReturn = new ArrayList<Route>();

		for (int i = 0; i < routes.size() && i < maxRoutes; i++) {
			Route r = new Route();
			r.setRouteName(routes.get(i));
			logger.fine(" in getLineSegments loop with i =" + i
					+ " and route is " + r);
			SolrDocumentList docs = getDocsbySPO(routes.get(i).toString(),
					AutoIE.hasLineSegment.toString(), null,MAX_DOCS);
			logger.fine(" in getLineSegments with doc of " + docs);
			for (int j = 0; j < docs.size(); j++) {
				SolrDocument doc = docs.get(j);
				String lineSegment = (String) doc.getFieldValue("object_t");
				logger.fine("before fillInLineSegment with lineSegment " + lineSegment);
				LineSegment ls = fillInLineSegment(lineSegment, since, status);
				
				
				if (ls != null) {
					logger.fine(" line segment is " + ls.getPoints());
					ls.setStatus(status);
					r.getLineSegments().add(ls);
				}
			}
			logger.fine("before sort is " + r.getLineSegments());
			Collections.sort(r.getLineSegments());
			r.setStartTime();
			logger.fine("after sort is " + r.getLineSegments());
			for(int l = r.getLineSegments().size()-1;l>maxPoints-1;l--){
				r.getLineSegments().remove(l);
			}
			logger.fine("after limit from maxPoints: " + r.getLineSegments());
			
			boolean found = false;
			for (int z = 0; z < routesToReturn.size(); z++) {
				Route previousRoute = routesToReturn.get(z);
				if (previousRoute.isSimliarTo(r)) {
					found = true;
					previousRoute.getStartTimes().add(r.getWhenStarted());
					break;
				}
			}
			if (!found) {
				routesToReturn.add(r);
			}
		}
		return routesToReturn;
	}

	public static List<CalendarEvent> getCalendarEvents(String person,
			Calendar since, GEOPoint lastPoint) {
		logger.fine(" in getCalendarEvents with person = " + person
				+ " since is " + Util.calendarToISO8601String(since));

		// if the status is null, do not consider it.
		List<CalendarEvent> retList = new ArrayList<CalendarEvent>();

		SolrDocumentList docs = getDocsbySPO(person,
				AutoIE.personCommitsToEvent.toString(), null, 100, false);

		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String event = (String) doc.getFieldValue("object_t");
			logger.fine(" prior to calling fillInCalendarEvent with statement = "
					+ doc);
			CalendarEvent ce = fillInCalendarEvent(event, since, lastPoint);
			if (ce != null && ce.isNotNull()) {
				retList.add(ce);
			}
		}

		Collections.sort(retList);
		List<CalendarEvent> c = new ArrayList<CalendarEvent>();
		logger.fine(" CALENDAR EVENTS before reducing to 1 are " + retList);
		if (retList != null && retList.size() > 0) {
			c.add(retList.get(0));
		}

		return c;
	}

	public static List<GEOPoint> getPoints(String lineSegment, Calendar since,
			String auto, boolean infer, boolean history) {
		List<String> l = new ArrayList<String>();
		l.add(lineSegment);
		return getPoints(l, 1, since, auto, infer, history);
	}

	public static List<GEOPoint> getPoints(List<String> routes, int maxRoutes,
			Calendar since, String auto, boolean infer, boolean history) {
		logger.fine(" in getPoints with routes = " + routes + " auto = " + auto
				+ "infer = " + infer);
		// if the status is null, do not consider it.
		List<GEOPoint> retList = new ArrayList<GEOPoint>();
		for (int i = 0; i < routes.size() && i < maxRoutes; i++) {

			SolrDocumentList docs = getDocsbySPO(routes.get(i),
					AutoIE.isTrackedBy.toString(), null, Integer.MAX_VALUE);
			for (int j = 0; j < docs.size(); j++) {
				SolrDocument doc = docs.get(j);
				String trackPoint = (String) doc.getFieldValue("object_t");
				GEOPoint gp = fillInGEOPoint(trackPoint, since, "history");
				if (gp != null) {
					retList.add(gp);
				}
			}
		}
		Collections.sort(retList);
		return retList;
	}

	public static void maintainHistory(String id, String person, Route route,
			Calendar since, String who, boolean routeTime) {
		logger.fine(" in maintainHistory for " + person + " route is " + route
				+ " actual route so far is " + route);
		storeHistory(id, person, route, who);
		removeUnrelatedHistory(id, route, who, routeTime);

	}
	public static void resetHistory(String sessionId){
		sessionRouteHistorySummary.remove(sessionId);
	}
	public static void storeHistory(String sessionId, String person,
			Route routeSoFar, String auto) {
		logger.fine(" in storeHistory after return test with auto = " + auto
				+ " and routeSoFar = " + routeSoFar + "sessionId =" + sessionId);

		RouteHistorySummary routeHistorySummary = sessionRouteHistorySummary
				.get(sessionId);
		if (routeHistorySummary != null) {
			List<Route> routeHistoryEntryList = routeHistorySummary
					.getListOfRoutes();
			if (routeHistoryEntryList != null
					&& routeHistoryEntryList.size() > 0) {
				Route route = routeHistoryEntryList.get(0);
				GEOPoint geoPoint = route.getOrigin();
				if (!geoPoint
						.isClose(routeSoFar.getOrigin(), INFER_CLOSE_CHECK)) {
					logger.fine(" in having a session history but blanking it out because we have a new starting point of "
							+ routeSoFar.getOrigin());
					routeHistorySummary = null;
					sessionRouteHistorySummary.remove(sessionId);
				}
			}
		}

		if (routeHistorySummary == null) {
			logger.fine(" creating a routeHistorySummary ");
			routeHistorySummary = new RouteHistorySummary();
			sessionRouteHistorySummary.put(sessionId, routeHistorySummary);
			List<String> routes = getRoutesFor(person,
					AutoIE.driverDrivesARoute.toString(), "COMPLETED");
			logger.fine("number of completed routes are " + routes.size()
					+ " list of completed routes before getPoints are "
					+ routes);
			List<Route> historyRoutes = getLineSegments(routes, HISTORY_SINCE,
					100, "history", 10000);
			
			logger.fine(" HISTORY ROUTES are" + historyRoutes);

			for (int i = 0; i < historyRoutes.size(); i++) {
				Route historyRoute = historyRoutes.get(i);
				GEOPoint origin = getOriginPoint(historyRoute.getRouteName(),"history");
				historyRoute.setOrigin(origin);
				logger.fine(" considering route CLOSENESS in for loop "
						+ historyRoute.getRouteName()
						+ " actual point is "
						+ routeSoFar.getOrigin()
						+ " history origin is "
						+ historyRoute.getOrigin()
						+ " history origin ls is "
						+ historyRoute.getFirstLineSegment()
						+ " havsign value is "
						+ routeSoFar.getOrigin().haversine(
								historyRoute.getOrigin()));

				if (routeSoFar.getOrigin().isClose(historyRoute.getOrigin(),
						INFER_TEST_CHECK)) {
					logger.fine(" got a close route at START.  So origin is close");
					routeHistorySummary.addRoute(historyRoute);
					if (routeHistorySummary.getOrigin() == null) {
						routeHistorySummary.setOrigin(routeSoFar.getOrigin());
					}

				}

			}
		}
	}

	public static int segmentIsOnRoute(Route r, LineSegment lineSegment,
			double closeVal) {
		// Scott TODO
		List<LineSegment> routeSegments = r.getLineSegments();

		logger.fine("last Line Segment: "+lineSegment);
		for (int i=0;i<routeSegments.size();i++){
			logger.fine("this line segment: " +routeSegments.get(i));
			if(routeSegments.get(i).equals(lineSegment)){
				return 0;
			}
		}
		return -1;
	}

	public static void removeUnrelatedHistory(String sessionId,
			Route routeSoFar, String auto, boolean routeTime) {
		logger.fine(" in removeUnrelatedHistory with routeTime = " + routeTime
				+ " routesSoFar = " + routeSoFar);
		GEOPoint lastPoint = routeSoFar.getLastLineSegment().getCurrentPoint();
		logger.fine("removeUnrelatedHistory lastPoint is " + lastPoint);
		RouteHistorySummary routeHistorySummary = sessionRouteHistorySummary
				.get(sessionId);

		logger.fine(" at Start of removeUnrelatedHistory routeHistorySummary = "
				+ routeHistorySummary);
		LineSegment lastSegment = routeSoFar.getLastLineSegment();
		
		if (routeHistorySummary != null
				&& routeHistorySummary.getListOfRoutes() != null
				&& routeHistorySummary.getListOfRoutes().size() > 0) {
			for (int k = routeHistorySummary.getListOfRoutes().size() - 1; k >= 0; k--) {
				Route r = routeHistorySummary.getListOfRoutes().get(k);
				logger.fine(" REMOVE_ROUTE_CLOSE_CHECK= "
						+ REMOVE_ROUTE_CLOSE_CHECK);
				int closePoint = segmentIsOnRoute(r, lastSegment,
						REMOVE_ROUTE_CLOSE_CHECK);
				if (closePoint == -1) {
					logger.fine(" nothing close for " + lastPoint
							+ " for route " + r.getRouteName());
					routeHistorySummary.getRoutesRemoved()
							.add(r.getRouteName());
					routeHistorySummary.getListOfRoutes().remove(k);
					routeHistorySummary.setMaxEntry(-1);
				}
			}
		}

		if (routeHistorySummary != null) {
			logger.fine(" before recalculateMaxEntry routeHistorySummary max entry is  = "
					+ routeHistorySummary.getMaxEntry());
			if (routeTime) {
				logger.fine(" case of routeTime considered");
				routeHistorySummary.recalculateMaxEntryConsiderTime(Util
						.toCalendar(lastPoint.getWhen()));
			} else {
				logger.fine(" case of routeTime NOT considered");
				routeHistorySummary.recalculateMaxEntry();
			}
			logger.fine(" after recalculateMaxEntry routeHistorySummary.getMaxEntry = "
					+ routeHistorySummary.getMaxEntry());
		}
	}

	public static Map<String, List<Route>> getHistoryAndInferredPoints(
			String routeString, RouteHistorySummary routeHistorySummary,
			LineSegment lastSegment, boolean infer, boolean history) {
		Map<String, List<Route>> mapToAdd = new HashMap<String, List<Route>>();
		if (!infer && !history) {
			return mapToAdd;
		}
		
		GEOPoint lastPoint = lastSegment.getCurrentPoint();
		List<Route> historyRoutes = new ArrayList<Route>();
		List<Route> inferredRoute = new ArrayList<Route>();
		List<Route> inferredHistory = new ArrayList<Route>();

		logger.fine(" in getHisttoryandInferredPoints with lastPoint = "
				+ lastPoint + "routeHistorySummary.getMaxEntry() "
				+ routeHistorySummary.getMaxEntry());

		LineSegment lineSegment = null;
		for (int k = 0; k < routeHistorySummary.getListOfRoutes().size(); k++) {
			logger.fine(" in loop with k = " + k
					+ "routeHistorySummary.getMaxEntry()"
					+ routeHistorySummary.getMaxEntry());

			Route route = routeHistorySummary.getListOfRoutes().get(k);
			if (infer && routeHistorySummary.getMaxEntry() == k) {
				// case of inferred route.
				logger.fine(" case of inferred " + route);
				inferredRoute.add(route);
				Route iHistory = new Route();
				iHistory.setRouteName(route.getRouteName());
				for (int j = 0; j < route.getLineSegments().size(); j++) {
					iHistory.getLineSegments().add(
							(route.getLineSegments().get(j)));
				}
				route.setRouteName(routeString);
				inferredHistory.add(iHistory);
				List<LineSegment> ls = route.getLineSegments();
				int segmentNumber = route.segmentWithSegment(lastSegment);
				if (segmentNumber != -1) {
					for (int z = 0; z < ls.size(); z++) {
						lineSegment = ls.get(z);
						if (z <= segmentNumber) {
							lineSegment.setStatus("actual");
						} else {
							Calendar newTime = Util.toCalendar(lastPoint
									.getWhen());
							newTime.add(Calendar.SECOND, z);
							lineSegment.setWhen(Util
									.calendarToISO8601String(newTime));
							lineSegment.setStatus("inferred");
						}
					}

				}
			} else {
				historyRoutes.add(route);

			}
			if (history) {
				mapToAdd.put("history", historyRoutes);
			}
			if (infer) {
				mapToAdd.put("inferred", inferredRoute);
				mapToAdd.put("inferredHistory", inferredHistory);

			}
		}

		return mapToAdd;
	}

	public static List<String> getListOfObjects(String resource,
			String property, int maxValues) {
		SolrDocumentList docs = getDocsbySPO(resource, property, null,
				maxValues);
		List<String> retList = new ArrayList<String>();
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String object = (String) doc.getFieldValue("object_t");
			retList.add(object);

		}
		return retList;

	}

	public static List<String> getRoutesFor(String person, String property,
			String status) {
		logger.fine(" in getRoutesFor with person = " + person + " property = "
				+ property + " status = " + status);
		List<String> ret = new ArrayList<String>();
		SolrDocumentList docs = getDocsbySPO(person.toString(),
				property.toString(), null, 100);
		logger.fine(" in getRoutesFor with docs =" + docs);
		String route = null;
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String subject = (String) doc.getFieldValue("subject_t");

			String predicate = (String) doc.getFieldValue("predicate_t");
			String object = (String) doc.getFieldValue("object_t");
			route = object;
			if (considerRoute(route, status)) {
				ret.add(route);
			}
		}
		return ret;
	}

	public static String getFirstRoute(String auto, String status) {
		logger.fine(" in getFirstRoute with auto = " + auto + " status = "
				+ status + " property is " + AutoIE.drives.toString());
		// if the status is null, do not consider it.
		Properties p = SolrUtil.getProperties();

		SolrDocumentList docs = getDocsbySPO(auto, AutoIE.drives.toString(),
				null, 1);
		String route = null;
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String subject = (String) doc.getFieldValue("subject_t");

			String predicate = (String) doc.getFieldValue("predicate_t");
			String object = (String) doc.getFieldValue("object_t");
			route = object;
			logger.fine(" route is " + route);
			if (considerRoute(route, status)) {
				logger.fine(" found route of " + route + " with status of "
						+ status);
				return route;
			}
		}
		return null;
	}

	public static CalendarEvent addTimeToEvent(CalendarEvent ce,
			GEOPoint lastPoint) {
		if (lastPoint == null || ce.getPoint() == null
				|| lastPoint.getLat() == null || lastPoint.getLat() == null
				|| ce.getPoint().getLat() == null
				|| ce.getPoint().getLng() == null) {
			return ce;
		}
		String url = "http://maps.googleapis.com/maps/api/distancematrix/json?origins="
				+ lastPoint.getLat()
				+ ","
				+ lastPoint.getLng()
				+ "&destinations="
				+ ce.getPoint().getLat()
				+ ","
				+ ce.getPoint().getLng() + "&key=AIzaSyBq3o8JdB4Kp57915CrABXrNc8HfDSpj6c&sensor=false";
		byte[] ba = Util.query(url);

		JSONParser parser = new JSONParser();
		Calendar now = Util.getNow();
		Calendar start = ce.getStart();

		long nowInMil = now.getTimeInMillis();
		long startInMil = start.getTimeInMillis();
		long betweenMil = startInMil - nowInMil;
		long betweenSec = betweenMil / 1000;
		int minToEvent = (int) betweenSec / 60;
		ce.setTimeToEvent(minToEvent);
		Map<String, JSONArray> m;
		ByteArrayInputStream bais = new ByteArrayInputStream(ba);
		Reader r = new InputStreamReader(bais);
		@SuppressWarnings("unchecked")
		Map<String, Object> highLevelKeys;
		try {
			m = (Map<String, JSONArray>) parser.parse(r);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		logger.fine(" keys returned are " + m.keySet());
		JSONArray rows = m.get("rows");
		if (rows == null || rows.size() == 0) {
			return ce;
		}
		logger.fine(" rows are " + rows);
		for (int i = 0; i < rows.size(); i++) {
			logger.fine(" rows[" + i + "]=" + rows.get(i));
		}
		JSONObject holder = (JSONObject) rows.get(0);
		JSONArray ja = (JSONArray) holder.get("elements");
		JSONObject element = (JSONObject) ja.get(0);
		logger.fine(" element is " + element);
		logger.fine(" element keys are " + element.keySet());

		JSONObject duration = (JSONObject) element.get("duration");
		logger.fine(" duration is " + duration);
		if (duration != null) {

			long delay = (Long) duration.get("value");
			ce.setDelayInMinutes(delay / 60);
		}

		return ce;
	}

	public static CalendarEvent fillInCalendarEvent(String event,
			Calendar since, GEOPoint lastPoint) {
		logger.finer(" in fillInCalendarEvent with event =" + event);
		CalendarEvent ce = new CalendarEvent();
		GEOPoint gp = null;
		ce.setEventURI(event);
		SolrDocumentList docs = getDocsbySPO(event,
				AutoIE.eventStatus.toString(), null, 1);

		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String object = (String) doc.getFieldValue("object_t");

			if (object.equals("SATISFIED")) {
				return null;
			}
		}

		docs = getDocsbySPO(event, AutoIE.eventDescription.toString(), null, 1);
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String object = (String) doc.getFieldValue("object_t");
			ce.setDescription(object);
		}

		docs = getDocsbySPO(event, AutoIE.eventDestinationType.toString(),
				null, 1);

		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String object = (String) doc.getFieldValue("object_t");
			if (object.startsWith("infer")) {
				ce.setInferredDestination(true);
			}
		}

		docs = getDocsbySPO(event, AutoIE.eventHasAsALocation.toString(), null,
				1);

		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String object = (String) doc.getFieldValue("object_t");
			ce.setLocation(object);
		}
		docs = getDocsbySPO(event, AutoIE.eventStartTime.toString(), null, 1);
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String object = (String) doc.getFieldValue("object_t");
			Calendar c = Util.toCalendar(object);
			if (c.before(since)) {
				logger.fine("REMOVE CALENDAR EVENT calendar event is before since with calendar event = "
						+ object
						+ " and since = "
						+ Util.calendarToISO8601String(since));
				return null;
			}
			ce.setStart(Util.toCalendar(object));
		}
		docs = getDocsbySPO(event, AutoIE.eventEndTime.toString(), null, 1);
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String object = (String) doc.getFieldValue("object_t");
			ce.setEnd(Util.toCalendar(object));
		}

		SolrDocumentList docs2 = getDocsbySPO(event,
				AutoIE.eventHasAsADestination.toString(), null, 1);
		for (int j = 0; j < docs2.size(); j++) {
			SolrDocument doc2 = docs2.get(j);
			String trackPoint2 = (String) doc2.getFieldValue("object_t");
			logger.fine("got an event trackpoint of " + trackPoint2);
			gp = fillInGEOPoint(trackPoint2, since, "inferred");
			logger.fine(" after fillInGEOPoint gp = " + gp);
			if (gp == null) {
				return null;
			}
			ce.setPoint(gp);

		}
		if (lastPoint != null) {
			addTimeToEvent(ce, lastPoint);
		}
		logger.fine(" calendar event created is " + ce);
		return ce;

	}

	public static LineSegment fillInLineSegment(String segment, Calendar since,
			String status) {
		logger.fine(" in fillInLineSegment with segment =" + segment);
		GEOPoint gp = null;
		LineSegment lineSegment = new LineSegment();
		SolrDocumentList docs = getDocsbySPO(segment,
				AutoIE.lineSegmentWhen.toString(), null, MAX_DOCS);

		logger.fine(" in fillInLineSegment with docs = " + docs);

		if (docs != null && docs.size() > 0) {
			SolrDocument doc = docs.get(0);
			String object = (String) doc.getFieldValue("object_t");
			logger.fine(" when is .. in its original form " + object);

			Calendar when = Util.toCalendar(object);
			if (when == null) {
				return null;
			}
			logger.fine(" when is " + Util.calendarToISO8601String(when)
					+ " time zone = " + when.getTimeZone());
			if (since != null) {
				if (when.before(since)) {
					logger.fine(" case of event is long ago returning null");
					return null;
				}
			}
			lineSegment.setWhen(object);

			SolrDocumentList trackPoints = getDocsbySPO(segment,
					AutoIE.hasSegmentPoint.toString(), null, MAX_DOCS);

			logger.fine(" track points are " + trackPoints);
			for (int i = 0; i < trackPoints.size(); i++) {
				SolrDocument doc2 = trackPoints.get(i);
				String trackPoint = (String) doc2.getFieldValue("object_t");

				logger.fine(" fillInLineSegment before call to fillInGEOPoint with trackPoint = "
						+ trackPoint);

				gp = fillInGEOPoint(trackPoint, since, status);

				logger.fine(" fillInLineSegment after call to fillInGEOPoint with point = "
						+ gp);
				if(gp!=null){
					
	
					lineSegment.getPoints().add(gp);
				}
			}
			
			SolrDocumentList currentPoint = getDocsbySPO(segment,
					AutoIE.hasCurrentPoint.toString(), null, 1);

			logger.fine(" current point is " + currentPoint);

			for (int i = 0; i < currentPoint.size(); i++) {
				SolrDocument doc2 = currentPoint.get(0);
				String currentPointString = (String) doc2.getFieldValue("object_t");

				logger.fine(" fillInLineSegment before call to fillInGEOPoint with trackPoint = "
						+ currentPointString);

				gp = fillInGEOPoint(currentPointString, when, status);

				logger.fine(" fillInLineSegment after call to fillInGEOPoint with point = "
						+ gp);

			
				lineSegment.setCurrentPoint(gp);
				
			}

		}
		Collections.sort(lineSegment.getPoints());
		lineSegment.setStartPoint(lineSegment.getPoints().get(0));
		lineSegment.setEndPoint(lineSegment.getPoints().get(lineSegment.getPoints().size()-1));
		logger.fine(" LINESEGMENT is " + lineSegment);
		return lineSegment;

	}

	public static GEOPoint fillInGEOPoint(String trackPoint, Calendar since,
			String status) {
		logger.fine(" in fillInGEOPoint with trackPoint =" + trackPoint);
		GEOPoint gp = null;
		SolrDocumentList docs = getDocsbySPO(trackPoint,
				AutoIE.trackPointWhen.toString(), null, MAX_DOCS);
		logger.fine(" in fillInGEOPoint with trackPoint docs =" + docs);
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String object = (String) doc.getFieldValue("object_t");
			logger.fine(" when is .. in its original form " + object);

			Calendar when = Util.toCalendar(object);
			if (when == null) {
				return null;
			}
			logger.fine(" when is " + Util.calendarToISO8601String(when)
					+ " time zone = " + when.getTimeZone());
			if (since != null) {
				if (when.before(since)) {
					logger.fine(" case of event is long ago returning null");
					return null;
				}
			}
			gp = new GEOPoint();
			gp.setWhen(object);
			gp.setStatus(status);
		}
		SolrDocumentList docs2 = getDocsbySPO(trackPoint,
				Vcard.latitude.toString(), null, MAX_DOCS);

		logger.fine(" in fillInGEOPoint with latitude docs =" + trackPoint);

		if (docs2 == null || docs2.size() == 0) {
			return null;
		}
		for (int i = 0; i < docs2.size(); i++) {
			SolrDocument doc2 = docs2.get(i);
			String object2 = (String) doc2.getFieldValue("object_t");
			try {
			} catch (NumberFormatException e) {
				return null;
			}
			gp.setLat(object2);
		}
		SolrDocumentList docs3 = getDocsbySPO(trackPoint,
				Vcard.longitude.toString(), null, MAX_DOCS);

		logger.fine(" in fillInGEOPoint with longitude docs =" + trackPoint);

		if (docs3 == null || docs3.size() == 0) {
			return null;
		}
		for (int i = 0; i < docs3.size(); i++) {
			SolrDocument doc3 = docs3.get(i);
			String object3 = (String) doc3.getFieldValue("object_t");
			try {
			} catch (NumberFormatException e) {
				return null;
			}
			gp.setLng(object3);
		}
		logger.fine(" returning gp of " + gp);
		return gp;

	}

	@SuppressWarnings("unchecked")
	public static void getInterests(String person, List<String> interests,
			List<String> notInterests) {
		long startTime = System.currentTimeMillis();
		logger.fine(" in getInterests with person =" + person);

		SolrDocumentList docs = getDocsbySPO(person,
				AutoIE.hasIndividualInterestOf.toString(), null, MAX_DOCS);
		startTime = writeTimingEvent(
				"getting first statement before iterator ", startTime,
				System.currentTimeMillis());

		for (int i = 0; i < docs.size(); i++) {

			SolrDocument doc = docs.get(i);
			startTime = writeTimingEvent(
					"got first statement inside iterator ", startTime,
					System.currentTimeMillis());

			String subject = (String) doc.getFieldValue("subject_t");
			String predicate = (String) doc.getFieldValue("predicate_t");
			String object = (String) doc.getFieldValue("object_t");
			logger.fine(" subject is " + subject);
			logger.fine(" predicate is " + predicate);
			logger.fine(" object is " + object);
			SolrDocumentList docs2 = getDocsbySPO(object,
					AutoIE.interestTerms.toString(), null, MAX_DOCS);
			for (int j = 0; j < docs2.size(); j++) {
				startTime = writeTimingEvent(
						"got second statement inside iterator ", startTime,
						System.currentTimeMillis());
				SolrDocument doc2 = docs2.get(j);
				String object2 = (String) doc2.getFieldValue("object_t");
				String[] terms = object2.split(",");
				logger.fine(" INTEREST object2 is " + object2);
				for (int k = 0; k < terms.length; k++) {
					interests.add(terms[k].trim().toLowerCase());
				}
			}
			if (notInterests != null) {
				SolrDocumentList docs3 = getDocsbySPO(object,
						AutoIE.notInterestTerms.toString(), null, MAX_DOCS);
				for (int k = 0; k < docs3.size(); k++) {
					startTime = writeTimingEvent(
							"got third statement inside iterator ", startTime,
							System.currentTimeMillis());

					SolrDocument doc3 = docs3.get(k);
					String object3 = (String) doc3.getFieldValue("object_t");
					String[] terms = object3.split(",");
					logger.fine(" NOT INTEREST object3 is " + object3);
					for (int m = 0; m < terms.length; m++) {
						notInterests.add(terms[m].trim().toLowerCase());
					}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void getInterests(String[] groups, List<String> interests,
			List<String> notInterests) {

		for (int i = 0; i < groups.length; i++) {
			String r = AutoIE.Group.getURI() + groups[i];
			SolrDocumentList docs2 = getDocsbySPO(r,
					AutoIE.interestName.toString(), null, MAX_DOCS);

			for (int j = 0; j < docs2.size(); j++) {
				SolrDocument doc2 = docs2.get(j);
				String object2 = (String) doc2.getFieldValue("object_t");
				logger.fine(" INTEREST object2 is " + object2);
				String[] terms = object2.split(",");
				for (int k = 0; k < terms.length; k++) {
					interests.add(terms[k].toLowerCase());
				}
			}
			SolrDocumentList docs3 = getDocsbySPO(r,
					AutoIE.notInterestTerms.toString(), null, MAX_DOCS);
			for (int l = 0; l < docs3.size(); l++) {
				SolrDocument doc3 = docs3.get(l);
				String object3 = (String) doc3.getFieldValue("object_t");
				String[] terms = object3.split(",");
				logger.fine(" NOT INTEREST object3 is " + object3);
				for (int m = 0; m < terms.length; m++) {
					notInterests.add(terms[m].trim().toLowerCase());
				}
			}

		}
	}

	public static SolrServer getSolrServer() throws MalformedURLException {
		if (solrServer == null) {
			solrServer = new CloudSolrServer(getZooURI());
			((CloudSolrServer)solrServer).setDefaultCollection(collection);
		}
		return solrServer;
	}

	public static String getSolrURI() {
		if (solrURI == null) {
			getProperties();
		}
		return solrURI;
	}
	
	public static String getZooURI() {
		if (zooURI == null) {
			getProperties();
		}
		return zooURI;
	}



	public static RouteHistorySummary getRouteHistorySummaryBasedOnId(String id) {
		return sessionRouteHistorySummary.get(id);
	}

	public static void resetRouteHistorySummaryBasedOnId(String id) {
		sessionRouteHistorySummary.remove(id);
	}

	public static SolrDocumentList getDocsbySPO(String subject,
			String predicate, String object, int maxRows) {
		SolrServer server = null;
		try {
			server = getSolrServer();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		SolrQuery query = new SolrQuery();
		SolrDocumentList sdl = null;

		try {
			sdl = getDocsbySPO(server, query, subject, predicate, object, "\"",
					maxRows, true);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return sdl;
	}

	public static SolrDocumentList getDocsbySPO(String subject,
			String predicate, String object, int maxRows, boolean descending) {
		SolrServer server = null;
		try {
			server = getSolrServer();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		SolrQuery query = new SolrQuery();
		SolrDocumentList sdl = null;

		try {
			sdl = getDocsbySPO(server, query, subject, predicate, object, "\"",
					maxRows, descending);
		} catch (SolrServerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return sdl;
	}

	public static SolrDocumentList getDocsbySPO(SolrServer server,
			SolrQuery query, String subject, String predicate, String object,
			String prefix, int maxRows) throws SolrServerException {
		return getDocsbySPO(server, query, subject, predicate, object, prefix,
				maxRows, true);
	}

	public static SolrDocumentList getDocsbySPO(SolrServer server,
			SolrQuery query, String subject, String predicate, String object,
			String prefix, int maxRows, boolean descending)
			throws SolrServerException {

		String stringQuery = null;
		if (subject!=null) {
			stringQuery = " subject_t:" + prefix
					+ ClientUtils.escapeQueryChars(subject) + prefix;

			if (predicate != null && !(predicate.equals("ANY"))) {
				query.addFilterQuery("predicate_t:"
						+ ClientUtils.escapeQueryChars(predicate));
				query.set("q.op", "AND");
			}

			if (object != null && !(object.equals("ANY"))) {
				query.addFilterQuery("object_t:"
						+ ClientUtils.escapeQueryChars(object));
				query.set("q.op", "AND");
			}
			query.setQuery(stringQuery);
		}
		else if (predicate != null) {
			stringQuery="predicate_t:" + prefix
					+ ClientUtils.escapeQueryChars(predicate) + prefix;
			if (object != null && !(object.equals("ANY"))) {
				query.addFilterQuery("object_t:"
						+ ClientUtils.escapeQueryChars(object));
				query.set("q.op", "AND");
			}
			query.setQuery(stringQuery);
		} 
		
		
		query.addField("id");
		query.addField("subject_t");
		query.addField("predicate_t");
		query.addField("object_t");
		query.addField("reifiedId_t");
		query.addField("timestamp");
		if (descending) {
			query.addSortField("timestamp", ORDER.desc);
		} else {
			query.addSortField("timestamp", ORDER.asc);
		}
		query.setRows(maxRows);
		logger.finer("query is  " + query);
		QueryResponse rsp;

		rsp = server.query(query);
		logger.finer(" result from query is " + rsp);
		SolrDocumentList docs = rsp.getResults();
		return docs;
	}

	/**
	 * This method is the only method that requires the Model to be cast to a
	 * SolrStore. The optmize does a solr optimize on the indes.
	 */
	public static void optimize() {
		try {
			SolrServer server = getSolrServer();
			server.optimize();
		} catch (IOException ex) {
			logger.finer("logging" + ex.getMessage());
			throw new RuntimeException(ex);

		} catch (SolrServerException ex) {
			logger.finer("logging" + ex.getMessage());
			throw new RuntimeException(ex);
		}
	}

	public static SolrDocumentList getReificationDocs(String reifiedId)
			throws SolrServerException, MalformedURLException {
		SolrDocumentList docs = null;
		if (reifiedId != null) {
			QueryResponse qr;
			SolrQuery q = new SolrQuery();
			reifiedId = reifiedId.replaceAll(":", "\\:");
			q.addField("r_subject_t");
			q.addField("r_predicate_t");
			q.addField("r_object_t");
			q.addField("r_reifiedId_t");
			q.setQuery("r_subject_t:" + reifiedId);
			SolrServer server = getSolrServer();
			qr = server.query(q);
			docs = qr.getResults();

			for (int i = 0; i < docs.size(); i++) {
				SolrDocument doc = docs.get(i);
				String rPredicate = (String) doc.getFieldValue("r_predicate_t");
				String rObject = (String) doc.getFieldValue("r_object_t");
				String refId = (String) doc.getFieldValue("r_reifiedId_t");
				if (refId != null) {
					docs.addAll(getReificationDocs(refId));
				}
			}
		}
		return docs;
	}

	@SuppressWarnings("unchecked")
	public static List<GEOPlace> getPOI(GEOPoint geoPoint, String typesOfPlaces) {
		logger.fine(" in getPOI with typesofPlaces = " + typesOfPlaces
				+ " geoPoint = " + geoPoint);
		
		if(typesOfPlaces.equals("")){
			logger.fine("no interest terms available for this person.");
			return null;
		}
		
		String streetAddress = null;
		String enterpriseShortName = null;
		List<GEOPlace> ret = new ArrayList<GEOPlace>();
		byte[] ba = Util
				.query("https://maps.googleapis.com/maps/api/place/nearbysearch/json?location="
						+ geoPoint.getLat()
						+ ","
						+ geoPoint.getLng()
						+ "&rankby=distance&name=*&sensor=false&key=AIzaSyBq3o8JdB4Kp57915CrABXrNc8HfDSpj6c&types="
						+ typesOfPlaces);

		JSONParser parser = new JSONParser();
		Map<String, JSONArray> m;
		ByteArrayInputStream bais = new ByteArrayInputStream(ba);
		Reader r = new InputStreamReader(bais);
		@SuppressWarnings("unchecked")
		Map<String, Object> highLevelKeys;
		try {
			m = (Map<String, JSONArray>) parser.parse(r);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		logger.fine(" keys returned are " + m.keySet());
		logger.fine("error message: " + m.get("error_message"));
		JSONArray results = m.get("results");
		logger.fine(" results size is " + results.size());
		for (int i = 0; i < results.size(); i++) {
			GEOPlace geoPlace = new GEOPlace();
			JSONObject jo = (JSONObject) results.get(i);
			streetAddress = (String) jo.get("vicinity");
			enterpriseShortName = (String) jo.get("name");

			geoPlace.setStreetAddress(streetAddress);

			geoPlace.setEnterprise(enterpriseShortName);

			JSONArray types = (JSONArray) jo.get("types");
			String typeString = types.toString();
			String newTypeString = convertType(typeString);
			geoPlace.setType(newTypeString);
			logger.fine(" type String = " + typeString);
			JSONObject location = (JSONObject) jo.get("geometry");
			location = (JSONObject) location.get("location");
			Double lat = (Double) location.get("lat");
			Double lng = (Double) location.get("lng");
			GEOPoint gp = new GEOPoint();
			gp.setLat(lat.toString());
			gp.setLng(lng.toString());
			gp.setStatus("inferred");
			gp.setWhen(Util.getNowString());
			geoPlace.setPoint(gp);
			ret.add(geoPlace);
		}
		return ret;
	}

	private static String convertType(String types) {
		String returnString = "";
		if (types.contains("accounting"))
			return "Services";
		if (types.contains("airport"))
			return "Services";
		if (types.contains("amusement_park"))
			return "Kids";
		if (types.contains("aquarium"))
			return "Kids";
		if (types.contains("art_gallery"))
			return "Leisure";
		if (types.contains("atm	Services"))
			return "Services";
		if (types.contains("bakery"))
			return "Food";
		if (types.contains("bank"))
			return "Services";
		if (types.contains("bar"))
			return "Food";
		if (types.contains("beauty_salon"))
			return "Services";
		if (types.contains("bicycle_store"))
			return "Shopping";
		if (types.contains("book_store"))
			return "Shopping";
		if (types.contains("bowling_alley"))
			return "Leisure";
		if (types.contains("bus_station"))
			return "Vehicle";
		if (types.contains("cafe"))
			return "Food";
		if (types.contains("campground"))
			return "Leisure";
		if (types.contains("car_dealer"))
			return "Vehicle";
		if (types.contains("car_rental"))
			return "Services";
		if (types.contains("car_repair"))
			return "Vehicle";
		if (types.contains("car_wash"))
			return "Vehicle";
		if (types.contains("casino"))
			return "Leisure";
		if (types.contains("cemetery"))
			return "Other";
		if (types.contains("church"))
			return "Other";
		if (types.contains("city_hall"))
			return "Other";
		if (types.contains("clothing_store"))
			return "Shopping";
		if (types.contains("convenience_store"))
			return "Other";
		if (types.contains("courthouse"))
			return "Other";
		if (types.contains("dentist"))
			return "Health";
		if (types.contains("department_store"))
			return "Shopping";
		if (types.contains("doctor"))
			return "Health";
		if (types.contains("electrician"))
			return "Services";
		if (types.contains("electronics_store"))
			return "Shopping";
		if (types.contains("embassy"))
			return "Other";
		if (types.contains("establishment"))
			return "Other";
		if (types.contains("finance"))
			return "Other";
		if (types.contains("fire_station"))
			return "Other";
		if (types.contains("florist"))
			return "Shopping";
		if (types.contains("food"))
			return "Food";
		if (types.contains("funeral_home"))
			return "Other";
		if (types.contains("furniture_store"))
			return "Shopping";
		if (types.contains("gas_station"))
			return "Vehicle";
		if (types.contains("general_contractor"))
			return "Services";
		if (types.contains("grocery_or_supermarket"))
			return "Food";
		if (types.contains("gym"))
			return "Health";
		if (types.contains("hair_care"))
			return "Health";
		if (types.contains("hardware_store"))
			return "Shopping";
		if (types.contains("health"))
			return "Health";
		if (types.contains("hindu_temple"))
			return "Other";
		if (types.contains("home_goods_store"))
			return "Shopping";
		if (types.contains("hospital"))
			return "Health";
		if (types.contains("insurance_agency"))
			return "Services";
		if (types.contains("jewelry_store"))
			return "Shopping";
		if (types.contains("laundry"))
			return "Services";
		if (types.contains("lawyer"))
			return "Services";
		if (types.contains("library"))
			return "Leisure";
		if (types.contains("liquor_store"))
			return "Food";
		if (types.contains("local_government_office"))
			return "Other";
		if (types.contains("locksmith"))
			return "Services";
		if (types.contains("lodging"))
			return "Other";
		if (types.contains("meal_delivery"))
			return "Food";
		if (types.contains("meal_takeaway"))
			return "Food";
		if (types.contains("mosque"))
			return "Other";
		if (types.contains("movie_rental"))
			return "Leisure";
		if (types.contains("movie_theater"))
			return "Leisure";
		if (types.contains("moving_company"))
			return "Services";
		if (types.contains("museum"))
			return "Leisure";
		if (types.contains("night_club"))
			return "Leisure";
		if (types.contains("painter"))
			return "Services";
		if (types.contains("park"))
			return "Kids";
		if (types.contains("parking"))
			return "Other";
		if (types.contains("pet_store"))
			return "Pet";
		if (types.contains("pharmacy"))
			return "Health";
		if (types.contains("physiotherapist"))
			return "Health";
		if (types.contains("place_of_worship"))
			return "Other";
		if (types.contains("plumber"))
			return "Services";
		if (types.contains("police"))
			return "Services";
		if (types.contains("post_office"))
			return "Services";
		if (types.contains("real_estate_agency"))
			return "Services";
		if (types.contains("restaurant"))
			return "Food";
		if (types.contains("roofing_contractor"))
			return "Services";
		if (types.contains("rv_park"))
			return "Leisure";
		if (types.contains("school"))
			return "Kids";
		if (types.contains("shoe_store"))
			return "Shopping";
		if (types.contains("shopping_mall"))
			return "Shopping";
		if (types.contains("spa	"))
			return "Health";
		if (types.contains("stadium"))
			return "Leisure";
		if (types.contains("storage"))
			return "Services";
		if (types.contains("store"))
			return "Shopping";
		if (types.contains("subway_station"))
			return "Vehicle";
		if (types.contains("synagogue"))
			return "Other";
		if (types.contains("taxi_stand"))
			return "Vehicle";
		if (types.contains("train_station"))
			return "Vehicle";
		if (types.contains("travel_agency"))
			return "Services";
		if (types.contains("university"))
			return "Other";
		if (types.contains("veterinary_care"))
			return "Pet";
		if (types.contains("zoo"))
			return "Kids";
		return returnString;
	}
	

	public static SolrDocumentList getDocsByObjectTime(String subject, String predicate, String object,
			String fromTime, String toTime, boolean descending)
			throws SolrServerException {
		SolrServer server = null;
		try {
			server = getSolrServer();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		SolrQuery query = new SolrQuery();
		String prefix = "\"";
		
		if (predicate != null && subject == null
				&& (object == null || object.equals("ANY"))) {
			query.setQuery("predicate_t:" + prefix
					+ ClientUtils.escapeQueryChars(predicate) + prefix);
		} else {
			String stringQuery = null;
			if (subject == null) {
				stringQuery = " subject_t:*";
			}
			else{
			stringQuery = " subject_t:" + prefix
					+ ClientUtils.escapeQueryChars(subject) + prefix;
			}
			if (predicate != null && !(predicate.equals("ANY"))) {
				query.addFilterQuery("predicate_t:"
						+ ClientUtils.escapeQueryChars(predicate));
				query.set("q.op", "AND");
			}

			if (object != null && !(object.equals("ANY"))) {
				query.addFilterQuery("object_t:"
						+ ClientUtils.escapeQueryChars(object));
				query.set("q.op", "AND");
			}
			query.setQuery(stringQuery);
		}
		if (fromTime !=null && toTime !=null){
			query.addFilterQuery("object_dt:["+ClientUtils.escapeQueryChars(fromTime)
					+" TO "+ClientUtils.escapeQueryChars(toTime)+"]");
			query.set("q.op", "AND");
		}
		else if(fromTime!=null){
			query.addFilterQuery("object_dt:["+ClientUtils.escapeQueryChars(fromTime)
					+" TO NOW]");
			query.set("q.op", "AND");
		}
		else if(toTime!=null){
			query.addFilterQuery("object_dt:[* TO "+ClientUtils.escapeQueryChars(toTime)+"]");
			query.set("q.op", "AND");
		}
		query.addField("id");
		query.addField("subject_t");
		query.addField("predicate_t");
		query.addField("object_t");
		query.addField("reifiedId_t");
		query.addField("timestamp");
		if (descending) {
			query.addSortField("timestamp", ORDER.desc);
		} else {
			query.addSortField("timestamp", ORDER.asc);
		}
		logger.fine("query is  " + query);
		QueryResponse rsp;

		rsp = server.query(query);
		logger.fine(" result from query is " + rsp);
		SolrDocumentList docs = rsp.getResults();
		return docs;
	}

	public static void deleteDocument(String id) throws SolrServerException, IOException {
		SolrServer server = null;
		SolrQuery query = new SolrQuery();
		String stringQuery = "subject_t:*";
		query.addFilterQuery("predicate_t:"
				+ ClientUtils.escapeQueryChars(IDS.isAttackedByID.toString()));
		query.addFilterQuery("object_t:"
				+ ClientUtils.escapeQueryChars(id));
		query.set("q.op", "AND");

		String prefix = "\"";

		try {
			server = getSolrServer();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		query.setQuery(stringQuery);
		server.deleteByQuery(query.getQuery());
		server.commit();
	}

	public static List<RangeFacet> getFacetsByObjectTime(String subject, String predicate, String object,
			String fromTime, String toTime, String gap, boolean descending)
			throws SolrServerException {
		SolrServer server = null;
		try {
			server = getSolrServer();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		SolrQuery query = new SolrQuery();
		String prefix = "\"";
		if (predicate != null && subject == null
				&& (object == null || object.equals("ANY"))) {
			query.setQuery("predicate_t:" + prefix
					+ ClientUtils.escapeQueryChars(predicate) + prefix);
		} else {
			String stringQuery = null;
			if (subject == null) {
				stringQuery = " subject_t:*";
			}
			else{
			stringQuery = " subject_t:" + prefix
					+ ClientUtils.escapeQueryChars(subject) + prefix;
			}
			if (predicate != null && !(predicate.equals("ANY"))) {
				query.addFilterQuery("predicate_t:"
						+ ClientUtils.escapeQueryChars(predicate));
				query.set("q.op", "AND");
			}

			if (object != null && !(object.equals("ANY"))) {
				query.addFilterQuery("object_t:"
						+ ClientUtils.escapeQueryChars(object));
				query.set("q.op", "AND");
			}
			

			query.setQuery(stringQuery);
		}
		
		query.setFacet(true);
		String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
		Date fromDate = null;
		try {
			fromDate = new SimpleDateFormat(pattern).parse(fromTime);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Date now = new Date();
		query.addDateRangeFacet("timestamp", fromDate, now, gap);
		query.addFacetQuery("timestamp:["+ClientUtils.escapeQueryChars(fromTime)+" TO NOW]");
		if (descending) {
			query.addSortField("timestamp", ORDER.desc);
		} else {
			query.addSortField("timestamp", ORDER.asc);
		}
		query.setRows(0);
		logger.finer("query is  " + query);
		QueryResponse rsp;

		rsp = server.query(query);
		logger.finer(" result from query is " + rsp);	
		List facets = rsp.getFacetRanges();
		return facets;
	}


}
