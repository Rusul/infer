package infer.util.rules;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.logging.Logger;

import infer.util.Util;
import infer.util.abox.LoadLocationGoogleMap;

public class GEOPoint implements Comparable<GEOPoint> {
	final static Logger log = Logger.getLogger(GEOPoint.class.getName());
	public static final int DESCENDING = -1;
	public static final int ASCENDING = 1;
	private String lat;
	private String lng;
	private String when;
	private String status;

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLng() {
		return lng;
	}

	public void setLng(String lng) {
		this.lng = lng;
	}

	public String getWhen() {
		return when;
	}

	public void setWhen(String when) {
		this.when = when;
	}

	public static Logger getLog() {
		return log;
	}

	// default sort order is descending.

	int sortOrder = ASCENDING;

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Implement the Comparable interface. If the fields match return 0.
	 * Otherwise return based on count.
	 * 
	 * @param o
	 *            category to compare
	 * @return the int mapping to the comparable interface.
	 */
	@Override
	public int compareTo(GEOPoint o) {
		
		Calendar tc = null;
		Calendar c = null;
		if (this.getWhen() == null) {
			return 1 * sortOrder;
		}
		tc = Util.toCalendar(this.getWhen());
		if (o.getWhen() == null) {
			return -1 * sortOrder;
		}
		c = Util.toCalendar(o.getWhen());
		int res = tc.compareTo(c);
		
		return res;

	}

	/**
	 * Determine whether fields are equal. Solely considers field.
	 */
	@Override
	public boolean equals(Object obj) {
		log.fine(" in equals with obj = " + obj + "this  =" + this);

		GEOPoint c = (GEOPoint) obj;

		if (c == null) {
			return false;
		}
		if (this.lat == null) {
			return false;
		}
		if (this.lng == null) {
			return false;
		}
		if (this.when == null) {
			return false;
		}
		if (!this.lat.equals(c.lat)) {
			return false;
		}
		if (!this.lng.equals(c.lng)) {
			return false;
		}
		if (!this.when.equals(c.when)) {
			return false;
		}
		if (this.status != null && c.status != null) {
			if (!this.status.equals(c.getStatus())) {
				return false;
			}
		}

		return true;
	}

	public static boolean isClose(String lat, String lon, String plat,
			String plon, Double closeDef) {
		
		if (lat == null || lat.trim().length() == 0) {
			return false;			
		}
		if (lon == null || lon.trim().length() == 0) {
			return false;			
		}

		Double dlat = Double.valueOf(lat);
		Double dlon = Double.valueOf(lon);
		Double dplat = Double.valueOf(plat);
		Double dplon = Double.valueOf(plon);

		double distance = haversine(dlat, dlon, dplat, dplon);
		log.fine("closeDef is " + closeDef + " distance is " + distance + " first point it " + dlat + ":" + dlon + " second is " + dplat + ":" + dplon);
		
		if (distance < closeDef) {
			return true;
		}
		return false;
	} 

	public boolean isClose(GEOPoint other, Double closeDef) {
		log.fine(" in isClose  with closeDef = " + closeDef);
		if (other == null) {
			return false;
		}
		if (this.getLat() == null || this.getLat().trim().length() == 0) {
			return false;			
		}
		if (this.getLng() == null || this.getLng().trim().length() == 0) {
			return false;			
		}

		double dlat = Double.valueOf(this.getLat());
		double dlon = Double.valueOf(this.getLng());
		double dplat = Double.valueOf(other.getLat());
		double dplon = Double.valueOf(other.getLng());

		double distance = haversine(dlat, dlon, dplat, dplon);
		
		if (distance <= closeDef) {
			log.fine("returning true with closeDef is " + closeDef + " distance is " + distance + " this point  is "+ this + " other point is " + other);
			return true;
		}
		log.fine("returning false with closeDef is " + closeDef + " distance is " + distance + " this point  is "+ this + " other point is " + other);

		return false;
	}
	
    public static final double R = 6372.8; // In kilometers
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }

    public double haversine(GEOPoint other) {
		double dlat = Double.valueOf(this.getLat());
		double dlon = Double.valueOf(this.getLng());
		double dplat = Double.valueOf(other.getLat());
		double dplon = Double.valueOf(other.getLng());

		double distance = haversine(dlat, dlon, dplat, dplon);
		return distance;
    	
    }


    
	/**
	 * Return a String representation of the Object
	 * 
	 * @return the representing String.
	 */
	@Override
	public String toString() {
		return " lat: " + lat + " lng: " + lng + " when: " + when + " status: "
				+ status;
	}

}
