package infer.util;

import java.util.Calendar;
import java.util.logging.Logger;

import infer.util.rules.GEOPoint;

public class CalendarEvent implements Comparable<CalendarEvent> {
	final static Logger log = Logger.getLogger(CalendarEvent.class.getName());
	public static final int DESCENDING = -1;
	public static final int ASCENDING = 1;

	private String description;
	private GEOPoint point;
	private String location;
	private Calendar start;
	private Calendar end;
	private int duration;
	private String eventURI = null;
	private int timeToEvent;
	
//  Possible Values are     "Late for Event"  "Upcoming Event"
	private String type = "Upcoming Event";

	private long delayInMinutes;

	private boolean isInferredDestination;

	public String getEventURI() {
		return eventURI;
	}

	public void setEventURI(String eventURI) {
		this.eventURI = eventURI;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getDelayInMinutes() {
		return delayInMinutes;
	}

	public void setDelayInMinutes(long delayInMinutes) {
		this.delayInMinutes = delayInMinutes;
	}

	public int getTimeToEvent() {
		return timeToEvent;
	}

	public void setTimeToEvent(int timeToEvent) {
		this.timeToEvent = timeToEvent;
	}


	
	public boolean isNotNull() {
		if (point == null) {
			return false;
		}
		if (point.getLat() == null) {
			return false;
		}
		if (point.getLng() == null) {
			return false;
		}
		if (start == null) {
			return false;
		}
		if (end == null) {
			return false;
		}
		return true;
	}

	public Calendar getStart() {
		return start;
	}

	public void setStart(Calendar start) {
		this.start = start;
	}
	public Calendar getEnd() {
		return end;
	}

	public void setEnd(Calendar end) {
		this.end = end;
	}

	
	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public GEOPoint getPoint() {
		return point;
	}

	public void setPoint(GEOPoint point) {
		this.point = point;
	}

	public boolean isInferredDestination() {
		return isInferredDestination;
	}

	public void setInferredDestination(boolean isInferredDestination) {
		this.isInferredDestination = isInferredDestination;
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
	public int compareTo(CalendarEvent o) {
		
		if (start != null) {
			return (start.compareTo(o.getStart()));
		}
		return -1;
	}

	/**
	 * Determine whether fields are equal. Solely considers description and time..
	 */
	@Override
	public boolean equals(Object obj) {
		log.fine(" in equals with obj = " + obj + "this  =" + this);

		CalendarEvent c = (CalendarEvent) obj;

		if (c == null) {
			return false;
		}
		if (this.description == null) {
			return false;
		}
		if (!this.description.equals(c.description)) {
			return false;
		}
		if (this.location == null) {
			return false;
		}
		if (!this.location.equals(c.location)) {
			return false;
		}
		if (this.start == null) {
			return false;
		}
		if (!this.start.equals(c.start)) {
			return false;
		}
		if (this.end == null) {
			return false;
		}
		if (!this.end.equals(c.end)) {
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
		return "eventURI = " + eventURI + " inferredDestination = " + isInferredDestination + " timeToEvent = " + timeToEvent + " description: " + description +  "startTime = " + Util.calendarToISO8601String(start) + " endTime =  " + Util.calendarToISO8601String(end) + " duration = " + duration + " type = " + type + " delay is " + delayInMinutes + " point: " + point;
	}

}
