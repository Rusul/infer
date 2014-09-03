package infer.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import infer.util.LoadAboxWebService;
import infer.util.SolrUtil;



public class TestSelect {
	final static Logger logger = Logger.getLogger(TestSelect.class
			.getName());

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	
	}

	@Test	
	public void testQueryNoSearch() {
		Properties properties = SolrUtil.getProperties();


		String postUrl = properties.getProperty("SEARCH_URI");
		
		URL u = null;
		HttpURLConnection urlc = null;
		OutputStream out = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			
//			postUrl += "&userid="+ URLEncoder.encode("Klaus Busse","UTF-8");
//			postUrl += "&email="+ URLEncoder.encode("http://www.facebook.com/klausbusse","UTF-8");
			postUrl += "&userid="+ URLEncoder.encode("Jeff Platnum","UTF-8");
			postUrl += "&email="+ URLEncoder.encode("jeffplatinum2013@gmail.com","UTF-8");
//			postUrl += "&userid="+ URLEncoder.encode("Steve Silver","UTF-8");
//			postUrl += "&email="+ URLEncoder.encode("stevesilver2012@gmail.com","UTF-8");
//			postUrl += "&userid="+ URLEncoder.encode("Susan Silver","UTF-8");
//			postUrl += "&email="+ URLEncoder.encode("susansilver2012@gmail.com","UTF-8");
//			postUrl += "&query="+ URLEncoder.encode("restaurants","UTF-8");
//			postUrl += "&userid="+ URLEncoder.encode("John Frank","UTF-8");
//			postUrl += "&email="+ URLEncoder.encode("John_Frank_infer1@hotmail.com","UTF-8");
			
			
			
//			http://54.225.193.188:8080/infer/rerank?search=false&infer=true&query=burger&userid=susan%20silver&email=susansilver2012%40gmail.com
//			postUrl += "&userid="+ URLEncoder.encode("Mary Frank","UTF-8");
//			postUrl += "&email="+ URLEncoder.encode("Mary_Frank_infer@hotmail.com","UTF-8");

//			postUrl += "&useFriendsFacebookLikes=false";
//			postUrl += "&useMyFacebookLikes=true";

			postUrl += "&query="+ URLEncoder.encode("burger","UTF-8");
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
			InputStream input = TestSelect.class.getResourceAsStream("/content.txt");


			SolrUtil.pipe(input, out);
			

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
			logger.info("result is " + resultString);
		} catch (IOException e) {
			throw new RuntimeException(
					"IOException while reading response: " + e);
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

	
		assert(resultString.length() > 0);

		
		
	}

	//@Test	
	public void testListStatements() {
		Properties properties = SolrUtil.getProperties();
		
		String postUrl = properties.getProperty("LIST_URI");
		
		URL u = null;
		HttpURLConnection urlc = null;
		OutputStream out = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			
			postUrl += URLEncoder.encode("http://www.w3.org/2006/vcard/ns#VCardMary_Frank-Mary_Frank_infer@hotmail.com", "UTF-8");
//			postUrl += ","+ URLEncoder.encode("http://www.compscii.com/ontologies/0.1/AutoIE.owl#hasIndividualInterestOf","UTF-8");
			postUrl += ","+ "null";
			postUrl += ","+ "null";

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
			logger.info("result is " + resultString);
		} catch (IOException e) {
			throw new RuntimeException(
					"IOException while reading response: " + e);
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

	
		assert(resultString.length() > 0);

		
		
	}

}
