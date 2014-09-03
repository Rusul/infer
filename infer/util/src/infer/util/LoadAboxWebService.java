package infer.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 
 * @author Scott Streit
 * 
 *         This class contains the static methods necessary to Solr. The calls
 *         to Solr are through web service calls (post) and do not require any
 *         jar files from Solr. There is a dependency on a file named
 *         load.properties off the classpath.
 * 
 */
public class LoadAboxWebService {

	final static Logger logger = Logger.getLogger(LoadAboxWebService.class
			.getName());
	private static Properties properties = null;

	public static void main(String args[]) throws Exception {
		properties = SolrUtil.getProperties();
		File file = null;
		if (args.length > 0) {
			file = new File(args[0]);

		} else {

			file = new File((String) properties.get("INPUT_DIR"));
		}
		if (file.canRead()) {
			if (file.isDirectory()) {

				String[] files = file.list();
				Arrays.sort(files);
				// an IO error could occur
				if (files != null) {
					for (int i = 0; i < files.length; i++) {

						try {
							logger.info(" processing file # " + i
									+ " name of file is " + file
									+ File.pathSeparatorChar + files[i]);

							doFile(new File(file.getAbsolutePath()
									+ File.separatorChar + files[i]),
									properties);
							logger.info(" successfully processed file # " + i
									+ " name of file is " + file.getName());
						} catch (Exception e) {
							logger.severe("error processing file # " + i
									+ " name is " + file.getName()
									+ " absolute name "
									+ file.getAbsolutePath());
							e.printStackTrace();
						}

					}
				}
			} else {
				try {
					doFile(file, properties);
				} catch (Exception e) {
					logger.severe("error processing file " + file.getName()
							+ "absolute name " + file.getAbsolutePath());
					e.printStackTrace();
				}

			}
		}
	}

	public static void doStream(InputStream input, Properties properties) {
		String postUrl = properties.getProperty("LOAD_ABOX_URI");
		logger.info(" in doStream with postUrl = " + postUrl);
		URL u = null;
		HttpURLConnection urlc = null;
		OutputStream out = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			u = new URL(postUrl);

			urlc = (HttpURLConnection) u.openConnection();
			urlc.setRequestMethod("POST");

			urlc.setDoOutput(true);
			urlc.setDoInput(true);
			urlc.setUseCaches(false);
			urlc.setAllowUserInteraction(false);
			urlc.setRequestProperty("Content-Type", "application/octet-stream");
			urlc.setRequestProperty("Content-Language", "en-US");

			out = urlc.getOutputStream();
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

		try {

			in = urlc.getInputStream();
			SolrUtil.pipe(in, baos);
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

	}

	/**
	 * Load the StringBuffer of Solr input xml into Solr.
	 * 
	 * @param sb
	 *            a StringBuffer of solr input xml.
	 * @param properties
	 *            containing the url of the solr server for posting.
	 * @param msgs
	 *            a stream buffer containing resulting messages from the solr
	 *            server.
	 */
	public static void doFile(File file, Properties properties) {
		if (file.isHidden()) {
			return;
		}
		FileInputStream input;
		try {
			input = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		doStream(input, properties);

	}

}
