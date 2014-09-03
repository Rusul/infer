package infer.test;

import infer.util.SolrUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;



public class TestIntrude {
	final static Logger logger = Logger.getLogger(TestSelect.class
			.getName());

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	
	}

	@Test	
	public void testCheckSecurity() {
		

		String postUrl = "http://localhost:8080/infer/rest/checkSecurity";
		
		URL u = null;
		HttpURLConnection urlc = null;
		OutputStream out = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			
			postUrl += "?ip="+ URLEncoder.encode("192.444.33.1","UTF-8");
			postUrl += "&mac="+ URLEncoder.encode("3522","UTF-8");
			postUrl += "&devId="+ URLEncoder.encode("1","UTF-8");
			postUrl += "&dom="+ URLEncoder.encode("domain","UTF-8");
			postUrl += "&fField="+ URLEncoder.encode("val1","UTF-8");

			postUrl += "&aVal="+ URLEncoder.encode("123456","UTF-8");
			postUrl += "&dVal="+ URLEncoder.encode("654321","UTF-8");			
			
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

}
