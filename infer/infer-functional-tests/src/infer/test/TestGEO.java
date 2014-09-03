package infer.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import infer.util.LoadAboxWebService;
import infer.util.SolrUtil;
 
public class TestGEO {
	final static Logger logger = Logger.getLogger(TestGEO.class.getName());

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	public String getTrackPoints(String route, Properties properties, String since, boolean infer, String prop, String auto, boolean relevantHistory, boolean actual) {
		logger.info(" property to take from file is " + prop);
		String postUrl = properties.getProperty(prop);

		URL u = null;
		HttpURLConnection urlc = null;
		OutputStream out = null;
		
		

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
//			if (route != null) {
//				postUrl += "&route=" + URLEncoder.encode(route, "UTF-8");
//			}
			if (since != null) {
				postUrl += "&since=" + URLEncoder.encode(since, "UTF-8");
			}
			if (infer) {
				postUrl += "&infer=" + "true";
				
			}
			if (actual){
				postUrl += "&actual=" + "true";
				
			}
			if (relevantHistory){
				postUrl += "&history=" + "true";
				
			}
			if (auto != null) {
				postUrl += "&auto=" + URLEncoder.encode(auto, "UTF-8");
			}
			logger.info(" URI is [" + postUrl + "]");
			u = new URL(postUrl);


			urlc = (HttpURLConnection) u.openConnection();
			urlc.setRequestMethod("POST");

			urlc.setDoOutput(true);
			urlc.setDoInput(true);
			urlc.setAllowUserInteraction(true);
			urlc.setUseCaches(false);
			urlc.setRequestProperty("Content-Type", "application/octet-stream");
			urlc.setRequestProperty("Content-Language", "en-US");
			urlc.connect();
			out = urlc.getOutputStream();

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);

		} catch (IOException e) {
			throw new RuntimeException("IOException while posting data: " + e);

		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException x) { /* NOOP */
			}
		}
		/**
		 * Now Get The Response
		 */
		InputStream in = null;
		String resultString = null;
		try {

			in = urlc.getInputStream();
			SolrUtil.pipe(in, baos);
			resultString = baos.toString();
			logger.info("after pipe and getting String result is "
					+ resultString);
		} catch (IOException e) {
			throw new RuntimeException("IOException while reading response: "
					+ e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (urlc != null) {
					urlc.disconnect();
				}
			} catch (IOException x) { /* NOOP */
			}
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(
				resultString.getBytes());

		Reader r = new InputStreamReader(bais);

		JSONParser parser = new JSONParser();
		Map<String, Object> highLevelKeys;
		try {
			highLevelKeys = (Map<String, Object>) parser.parse(r);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		// logger.info(" highest level keys are " + highLevelKeys.keySet());
		Object o = highLevelKeys.get("tracks");

		return o.toString();

	}

	public String getAuto(String email, String userid, Properties properties) {
		String postUrl = properties.getProperty("AUTO_URI");
		String resultString = null;

		URL u = null;
		HttpURLConnection urlc = null;
		OutputStream out = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {

			postUrl += "&email=" + URLEncoder.encode(email, "UTF-8");
			postUrl += "&userid=" + URLEncoder.encode(userid, "UTF-8");
			logger.info(" URI is [" + postUrl + "]");
			u = new URL(postUrl);

			urlc = (HttpURLConnection) u.openConnection();
			urlc.setRequestMethod("POST");

			urlc.setDoOutput(true);
			urlc.setDoInput(true);
			urlc.setAllowUserInteraction(true);
			urlc.setUseCaches(false);
			urlc.setRequestProperty("Content-Type", "application/octet-stream");
			urlc.setRequestProperty("Content-Language", "en-US");
			urlc.connect();
			out = urlc.getOutputStream();

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);

		} catch (IOException e) {
			throw new RuntimeException("IOException while posting data: " + e);

		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException x) { /* NOOP */
			}
		}
		/**
		 * Now Get The Response
		 */
		InputStream in = null;
		try {

			in = urlc.getInputStream();
			SolrUtil.pipe(in, baos);
			resultString = baos.toString();
			logger.info("after pipe and getting String result is "
					+ resultString);
		} catch (IOException e) {
			throw new RuntimeException("IOException while reading response: "
					+ e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (urlc != null) {
					urlc.disconnect();
				}
			} catch (IOException x) { /* NOOP */
			}
		}

		logger.info(" result string is " + resultString);
		ByteArrayInputStream bais = new ByteArrayInputStream(
				resultString.getBytes());

		Reader r = new InputStreamReader(bais);

		JSONParser parser = new JSONParser();
		Map<String, JSONArray> highLevelKeys;
		try {
			highLevelKeys = (Map<String, JSONArray>) parser.parse(r);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		// logger.info(" highest level keys are " + highLevelKeys.keySet());
		JSONArray o = highLevelKeys.get("results");
		JSONObject jo = (JSONObject) o.get(0);
		String auto = (String) jo.get("auto");
		logger.info("auto is " + auto);

		return auto;

	}

	@Test
	public void testAuto() {
		InputStream is = null;
		is = TestGEO.class.getResourceAsStream("/0.nt");
		Properties properties = SolrUtil.getProperties();
		LoadAboxWebService.doStream(is, properties);
		SolrUtil.optimize();
		long startTime = System.currentTimeMillis();
		String auto = getAuto("susansilver2012@gmail.com", "Susan Silver", properties);
		SolrUtil.writeTimingEvent("getAuto", startTime, System.currentTimeMillis());
		assert (auto != null);
	}

	@Test
	public void testRouteCurrent() {
		InputStream is = null;
		Properties properties = SolrUtil.getProperties();
		is = TestGEO.class.getResourceAsStream("/0.nt");

		LoadAboxWebService.doStream(is, properties);
		SolrUtil.optimize();
		long startTime = System.currentTimeMillis();

		String auto = getAuto("susansilver2012@gmail.com", "Susan Silver", properties);
		SolrUtil.writeTimingEvent("getAuto in testRouteCurrent", startTime, System.currentTimeMillis());
		String route = getRoute(auto, "INPROGRESS", properties);
		SolrUtil.writeTimingEvent("getRoute InProgress in testRouteCurrent", startTime, System.currentTimeMillis());
		assert (route != null);
	}

	@Test
	public void testRouteHistorical() {
		InputStream is = null;
		is = TestGEO.class.getResourceAsStream("/1_FairfieldInnKokomoToIndianapolisInternationalAirport.kml.gpx.nt");
		Properties properties = SolrUtil.getProperties();

		LoadAboxWebService.doStream(is, properties);
		SolrUtil.optimize();

		String auto = getAuto("susansilver2012@gmail.com", "Susan Silver", properties);
		logger.info(" auto is " + auto);
		String route = getRoute(auto, "COMPLETED", properties);
		logger.info(" route is " + route);
		assert (route != null);
	}

	
	@Test
	public void testTrackPoints() {
		InputStream is = null;
		Properties properties = SolrUtil.getProperties();
		is = TestGEO.class.getResourceAsStream("/0.nt");

		LoadAboxWebService.doStream(is, properties);
		SolrUtil.optimize();
		long startTime = System.currentTimeMillis();

		String auto = getAuto("susansilver2012@gmail.com", "Susan Silver", properties);
		SolrUtil.writeTimingEvent("getAuto in testTrackPoints", startTime, System.currentTimeMillis());
		String route = getRoute(auto, "INPROGRESS", properties);
		SolrUtil.writeTimingEvent("getRoute InProgress in testTrackPoints", startTime, System.currentTimeMillis());
		String trackPoints = getTrackPoints(route, properties, "2010-05-01T20:30:37.704Z", false, "TRACKPOINTS_URI", "12345",false, true);
		SolrUtil.writeTimingEvent("getTrackPoints InProgress in testTrackPoints", startTime, System.currentTimeMillis());
		assert (trackPoints != null);
	}

	@Test
	public void testCurrentPartialTrackPoints() {
		InputStream is = null;
		Properties properties = SolrUtil.getProperties();
		is = TestGEO.class.getResourceAsStream("/0.nt");

		LoadAboxWebService.doStream(is, properties);
		SolrUtil.optimize();
		long startTime = System.currentTimeMillis();

		String auto = getAuto("susansilver2012@gmail.com", "Susan Silver", properties);
		SolrUtil.writeTimingEvent("getAuto in testTrackPoints", startTime, System.currentTimeMillis());
		String route = getRoute(auto, "INPROGRESS", properties);
		SolrUtil.writeTimingEvent("getRoute InProgress in testTrackPoints", startTime, System.currentTimeMillis());
		String trackPoints = getTrackPoints(route, properties, "2013-05-01T20:30:37.704Z", false,"TRACKPOINTS_URI", "12345",false, true);
		SolrUtil.writeTimingEvent("getTrackPoints InProgress in testTrackPoints", startTime, System.currentTimeMillis());
		assert (trackPoints != null);
	}
	
	@Test
	public void testCurrentPartialTrackPointsInfer() {
		InputStream is = null;
		Properties properties = SolrUtil.getProperties();

		is = TestGEO.class.getResourceAsStream("/1_FairfieldInnKokomoToIndianapolisInternationalAirport.kml.gpx.nt");

		LoadAboxWebService.doStream(is, properties);
		
		is = TestGEO.class.getResourceAsStream("/0.nt");
		LoadAboxWebService.doStream(is, properties);

		
		SolrUtil.optimize();
		
		LoadAboxWebService.doStream(is, properties);
		SolrUtil.optimize();
		long startTime = System.currentTimeMillis();

		String auto = getAuto("susansilver2012@gmail.com", "Susan Silver", properties);
		
		String route = getRoute(auto, "INPROGRESS", properties);
		String trackPoints = getTrackPoints(route, properties, "2013-05-01T20:30:37.704Z", true, "TRACKPOINTS_URI", "2C4RC1BG3DR614258",false, true);
		logger.info("infer result is[" + trackPoints + "]");
		SolrUtil.writeTimingEvent("getTrackPoints InProgress in testTrackPoints", startTime, System.currentTimeMillis());
		boolean hasInferred = trackPoints.contains("inferred");
		Assert.assertTrue("Must have at least one inferred point", hasInferred);
		boolean hasHistory = trackPoints.contains("history");
		Assert.assertFalse("Must have not one inferred point", hasHistory);
	}

	@Test
	public void testCurrentPartialTrackPointsInferWithRelevantHistory() {
		InputStream is = null;
		Properties properties = SolrUtil.getProperties();
		
		is = TestGEO.class.getResourceAsStream("/1_FairfieldInnKokomoToIndianapolisInternationalAirport.kml.gpx.nt");

		LoadAboxWebService.doStream(is, properties);
		
	
		
		is = TestGEO.class.getResourceAsStream("/2_FairfieldInnKokomoToIndianapolisInternationalAirport.kml.gpx.nt");

		LoadAboxWebService.doStream(is, properties);

		
		is = TestGEO.class.getResourceAsStream("/2_FairfieldInnKokomotoSpringMillStateParkIndiana.kml.gpx.nt");

		LoadAboxWebService.doStream(is, properties);

		is = TestGEO.class.getResourceAsStream("/3_FairfieldInnKokomotoSpringMillStateParkIndiana.kml.gpx.nt");

		LoadAboxWebService.doStream(is, properties);

		is = TestGEO.class.getResourceAsStream("/3_FairfieldInnKokomoToIndianapolisInternationalAirport.kml.gpx.nt");

		LoadAboxWebService.doStream(is, properties);


		is = TestGEO.class.getResourceAsStream("/3_FairfieldInnKokomotoEvansvilleRegionalAirport.kml.gpx.nt");

		LoadAboxWebService.doStream(is, properties);
		
		SolrUtil.optimize();
		long startTime = System.currentTimeMillis();

			
		is = TestGEO.class.getResourceAsStream("/0.nt");

		LoadAboxWebService.doStream(is, properties);
		
		is = TestGEO.class.getResourceAsStream("/1.nt");

		LoadAboxWebService.doStream(is, properties);
		
		SolrUtil.optimize();
		
		String auto = getAuto("susansilver2012@gmail.com", "Susan Silver", properties);
		logger.info(" auto is " + auto);
		
		String route = getRoute(auto, "INPROGRESS", properties);
		
		

		logger.info(" before bettting results for 0,1 route is + " + route);
		String trackPoints = getTrackPoints(route, properties, "2013-01-01T20:30:37.704Z", true, "TRACKPOINTS_URI", "2C4RC1BG3DR614258", true, true);
		logger.info(" trackPoints = "  + trackPoints);
		boolean hasInferred = trackPoints.contains("inferred");
		
		Assert.assertTrue("Must have at least one inferred point", hasInferred);
		
		boolean hasHistory = trackPoints.contains("history");
		
		Assert.assertTrue("Must have at least one history point", hasHistory);
		
		
//		logger.info(" result is after 0, 1 is " +  trackPoints + " inferred route should be Indianapolis airport");

		is = TestGEO.class.getResourceAsStream("/2.nt");

		LoadAboxWebService.doStream(is, properties);
		
		is = TestGEO.class.getResourceAsStream("/3.nt");

		LoadAboxWebService.doStream(is, properties);
		
		SolrUtil.optimize();
		logger.info (" before getting result for 2,3");
		trackPoints = getTrackPoints(route, properties, "2013-05-17T16:44:01.565Z", true, "TRACKPOINTS_URI", "2C4RC1BG3DR614258", true, true);
//			logger.info(" result is after 2, 3 is " +  trackPoints + " inferred route should be The Park");	

	}

	
	public String getRoute(String auto, String status, Properties properties) {

		String postUrl = properties.getProperty("ROUTE_URI");

		URL u = null;
		HttpURLConnection urlc = null;
		OutputStream out = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {

			postUrl += "&auto=" + URLEncoder.encode(auto, "UTF-8");
			postUrl += "&status=" + URLEncoder.encode(status, "UTF-8");
			logger.info(" URI is [" + postUrl + "]");
			u = new URL(postUrl);

			urlc = (HttpURLConnection) u.openConnection();
			urlc.setRequestMethod("POST");

			urlc.setDoOutput(true);
			urlc.setDoInput(true);
			urlc.setAllowUserInteraction(true);
			urlc.setUseCaches(false);
			urlc.setRequestProperty("Content-Type", "application/octet-stream");
			urlc.setRequestProperty("Content-Language", "en-US");
			urlc.connect();
			out = urlc.getOutputStream();

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);

		} catch (IOException e) {
			throw new RuntimeException("IOException while posting data: " + e);

		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException x) { /* NOOP */
			}
		}
		/**
		 * Now Get The Response
		 */
		InputStream in = null;
		String resultString = null;
		try {

			in = urlc.getInputStream();
			SolrUtil.pipe(in, baos);
			resultString = baos.toString();
			logger.info("after pipe and getting String result is "
					+ resultString);
		} catch (IOException e) {
			throw new RuntimeException("IOException while reading response: "
					+ e);
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (urlc != null) {
					urlc.disconnect();
				}
			} catch (IOException x) { /* NOOP */
			}
		}

		ByteArrayInputStream bais = new ByteArrayInputStream(
				resultString.getBytes());

		Reader r = new InputStreamReader(bais);

		JSONParser parser = new JSONParser();
		Map<String, JSONArray> highLevelKeys;
		try {
			highLevelKeys = (Map<String, JSONArray>) parser.parse(r);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		logger.info(" highest level keys are " + highLevelKeys.keySet());
		JSONArray o = highLevelKeys.get("results");
		JSONObject jo = (JSONObject) o.get(0);
		String route = (String) jo.get("route");
		logger.info("route is " + route);
		return route;

	}

}
