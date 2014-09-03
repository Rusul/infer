package infer.util.rules;

import java.util.logging.Logger;

public class GEOPlace implements Comparable<GEOPlace> {
	final static Logger log = Logger.getLogger(GEOPlace.class.getName());
	public static final int DESCENDING = -1;
	public static final int ASCENDING = 1;

	private String streetAddress;
	private String enterprise;
	private GEOPoint point;
	private String type;
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}


	public GEOPoint getPoint() {
		return point;
	}

	public void setPoint(GEOPoint point) {
		this.point = point;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public String getEnterprise() {
		return enterprise;
	}

	public void setEnterprise(String enterprise) {
		this.enterprise = enterprise;
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
	public int compareTo(GEOPlace o) {
		
		if (point != null) {
			return (point.compareTo(o.getPoint()));
		}
		return -1;
	}

	/**
	 * Determine whether fields are equal. Solely considers description and time..
	 */
	@Override
	public boolean equals(Object obj) {
		log.fine(" in equals with obj = " + obj + "this  =" + this);

		GEOPlace c = (GEOPlace) obj;

		if (c == null) {
			return false;
		}
		if (this.streetAddress == null) {
			return false;
		}
		if (!this.streetAddress.equals(c.streetAddress)) {
			return false;
		}
		if (this.enterprise == null) {
			return false;
		}
		if (!this.enterprise.equals(c.enterprise)) {
			return false;
		}
		if (this.type == null) {
			return false;
		}
		if (!this.type.equals(c.type)) {
			return false;
		}
		if (this.point != null && c.getPoint() != null) {
			return point.equals(c.getPoint());
		} else {
			return false;
		}

	}


	/**
	 * Return a String representation of the Object
	 * 
	 * @return the representing String.
	 */
	@Override
	public String toString() {
		return "type: " + type + " enterprise: " + enterprise + " Street Address " + streetAddress + " point: " + point;
	}

}
