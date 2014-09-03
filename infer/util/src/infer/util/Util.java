package infer.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import infer.util.abox.LoadEvent;
import infer.util.generated.Vcard;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;

public class Util {
	final static Logger logger = Logger.getLogger(LoadEvent.class.getName());
	public static void main(String args[]) throws Exception {
		
		Calendar c1 = Util.toCalendar("2013-08-06T11:12:24.3920-04:00");
		Calendar c2 = Util.toCalendar("2013-08-06T11:13:02.3920-04:00");
		
		
		
		
		
		logger.info(" date time1 is " + Util.calendarToISO8601String(c1));
		logger.info(" date time2 is " + Util.calendarToISO8601String(c2));

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
	@SuppressWarnings("unchecked")
	public static byte[] query(String postUrl) {
		URL u = null;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			u = new URL(postUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		HttpURLConnection urlc = null;
		try {
			try {
				urlc = (HttpURLConnection) u.openConnection();
				try {
					urlc.setRequestMethod("POST");
				} catch (ProtocolException e) {
					throw new RuntimeException(e);

				}
				urlc.setDoOutput(true);
				urlc.setDoInput(true);
				urlc.setUseCaches(false);
				urlc.setAllowUserInteraction(false);
			} catch (IOException e) {
				throw new RuntimeException(
						"Connection error (is Solr running at " + postUrl
								+ " ?): " + e);
			}

			OutputStream out = null;
			try {
				out = urlc.getOutputStream();
				String str = "";
				byte[] bytes = str.getBytes("UTF-8");
				InputStream input = new ByteArrayInputStream(bytes);

				pipe(input, out);
			} catch (IOException e) {
				throw new RuntimeException("IOException while posting data: "
						+ e);
			} finally {
				try {
					if (out != null)
						out.close();
				} catch (IOException x) { /* NOOP */
				}
			}

			InputStream in = null;
			try {
				in = urlc.getInputStream();
				pipe(in, baos);
			} catch (IOException e) {
				throw new RuntimeException(
						"IOException while reading response: " + e);
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException x) { /* NOOP */
				}
			}

		} finally {
			if (urlc != null)
				urlc.disconnect();
		}

		return baos.toByteArray();
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

	public static String getPerson(String name, String email) {
		String uri = name + "-" + email;
		uri = uri.replaceAll(" ", "_");
		return uri;
	}
	
	public static String nowPlus(long valToAdd) {
		String calString = getNowString();
		return datePlus(calString, valToAdd);
	}

	public static Calendar getNow() {		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date()); 
		return cal;
	}
	
	
	public static Calendar getNowPlus(long ms) {		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		long mills = cal.getTimeInMillis();
		mills += ms;
		cal.setTimeInMillis(mills);
		return cal;
	}
	

	
	public static String calendarToISO8601String(Calendar cal) {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		String my8601formattedDate = df.format(cal.getTime());
		return my8601formattedDate;	
	}
	
	
	public static String getNowString() {
		Calendar cal = getNow();
		return calendarToISO8601String(cal);
	}

	
	public static String datePlus(String iso8601string, long ms) {	
		Calendar cal = toCalendar(iso8601string);
		
		long mills = cal.getTimeInMillis();
		mills += ms;
		cal.setTimeInMillis(mills);
		return calendarToISO8601String(cal);
	}

	public static String todayAt(int hour, int minute) {

		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date()); 
		logger.fine(" in todayAt now is " +  calendarToISO8601String(cal));
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		return calendarToISO8601String(cal);
		

	}
	
    public static Calendar toCalendar(final String iso8601string) {
        if (iso8601string == null || iso8601string.trim().length() < 10) {
        	return null;
        }
    	if (!iso8601string.contains("Z")) {
    		return toCalendarTZ(iso8601string);
    	}
        Calendar calendar = GregorianCalendar.getInstance();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
       
        Date date;
		try {
			date = df.parse(iso8601string);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
        calendar.setTime(date);
        return calendar;
    } 
    public static Calendar toCalendarTZ(final String val) {
        Calendar calendar = GregorianCalendar.getInstance();
        int dot = val.indexOf(".");
        int mult = 1;
        int sign = val.indexOf("+");
        if (sign > -1) {
        	mult = -1;
        }
        String most = val.substring(dot-1);
        String hourString = val.substring(dot+6, dot+8);
        int hour = Integer.parseInt(hourString);
        logger.fine("hour is" + hour);
        logger.fine("mult is " + mult);
        
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
    
        Date date;
		try {
			date = df.parse(val);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
        calendar.setTime(date);
        calendar.add(Calendar.HOUR,mult*hour);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        return calendar;
    }   
 

    public static int getTimeOfDayInMinutes(Calendar cal) {
    	logger.finer(" in getTimeOfDayInMinutes with cal = " + Util.calendarToISO8601String(cal));
    	
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		cal.setTimeZone(TimeZone.getTimeZone("UTC"));


		
		int minute = cal.get(Calendar.MINUTE);

		int ret = (hour * 60) + minute;
	  	logger.finer(" with cal = " + Util.calendarToISO8601String(cal) + " with hour = " + hour + " minute = " + minute + "ret = " + ret + " time zone = " + cal.getTimeZone()  );
		
		return ret;
    }

    public static List<String> decodePoly(String encoded) {
    	 
        List<String> poly = new ArrayList<String>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
 
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            logger.fine("LAT: " + (lat *1e-5)+"-->"+index+" out of "+len);
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            logger.fine("and LNG: " + (lng *1e-5)+"-->"+index+" out of "+len);
            String p = ((((double) lat / 1E5)) + " " + 
                        (((double) lng / 1E5)));
            poly.add(p);
        }
 
        return poly;
    }
    
  
    public static java.util.List<java.lang.Integer> decode(String encoded_polylines, int initial_capacity) {
        java.util.List<java.lang.Integer> trucks = new java.util.ArrayList<java.lang.Integer>(initial_capacity);
        int truck = 0;
        int carriage_q = 0;
        for (int x = 0, xx = encoded_polylines.length(); x < xx; ++x) {
            int i = encoded_polylines.charAt(x);
            i -= 63;
            int _5_bits = i << (32 - 5) >>> (32 - 5);
            truck |= _5_bits << carriage_q;
            carriage_q += 5;
            boolean is_last = (i & (1 << 5)) == 0;
            if (is_last) {
                boolean is_negative = (truck & 1) == 1;
                truck >>>= 1;
                if (is_negative) {
                    truck = ~truck;
                }
                trucks.add(truck);
                carriage_q = 0;
                truck = 0;
            }
        }
        return trucks;
    }

}
