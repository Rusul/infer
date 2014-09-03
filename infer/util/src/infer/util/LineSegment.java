package infer.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import infer.util.rules.GEOPlace;
import infer.util.rules.GEOPoint;

public class LineSegment implements Comparable<LineSegment> {
	final static Logger log = Logger.getLogger(LineSegment.class.getName());

	public static final int DESCENDING = -1;
	public static final int ASCENDING = 1;
	public static final double EQUALS_CLOSE_CHECK = 0.01;

	private GEOPoint start;
	private GEOPoint current;
	private GEOPoint end;
	private String when;

	private String status;

	private List<GEOPoint> points = new ArrayList<GEOPoint>();

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<GEOPoint> getPoints() {
		return points;
	}

	public void setPoints(List<GEOPoint> points) {
		this.points = points;
	}

	public GEOPoint getStartPoint() {
		
		return start;
	}

	public void setStartPoint(GEOPoint start) {
		this.start = start;
	}

	public GEOPoint getCurrentPoint() {
		if (current == null) {
			return end;
		}
		return current;
	}

	public void setCurrentPoint(GEOPoint current) {
		this.current = current;
	}

	int sortOrder = ASCENDING;

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public String getWhen() {
		return when;
	}

	public void setWhen(String when) {
		this.when = when;
	}

	public GEOPoint getEndPoint() {
		return end;
	}
	
	public void setEndPoint(GEOPoint end) {
		this.end = end;
	}

	/**
	 * Return a String representation of the Object
	 * 
	 * @return the representing String.
	 */
	@Override
	public String toString() {

		StringBuffer p = new StringBuffer();

		for (int i = 0; i < getPoints().size(); i++) {
			GEOPoint pt = getPoints().get(i);
			p.append(" Point: [" + i + "] " + pt + "\n");
		}

		return " start = " + start + " end = " + end + " status = " + status
				+  "<POINTS> " + p.toString() + " </POINTS> ";
		

	}

	@Override
	public int compareTo(LineSegment l) {

		Calendar tc = null;
		Calendar c = null;
		if (this.getWhen() == null) {
			return 1 * sortOrder;
		}
		tc = Util.toCalendar(this.getWhen());
		if (l.getWhen() == null) {
			return -1 * sortOrder;
		}
		c = Util.toCalendar(l.getWhen());
		int res = tc.compareTo(c);

		return res;

	}
	
	@Override
	public boolean equals(Object other) {
		LineSegment o = (LineSegment) other;
		if (this.getStartPoint() == null || o.getStartPoint() == null) {
			return false;
		}
		if (this.getStartPoint().isClose(o.getStartPoint(), EQUALS_CLOSE_CHECK )) {
			if (this.getEndPoint().isClose(o.getEndPoint(), EQUALS_CLOSE_CHECK )) {
				return true;
			}
		}
		return false;
	}


	public boolean isSegmentOnSegment(LineSegment otherLS) {
		return this.getStartPoint().getLat()
				.equals(otherLS.getStartPoint().getLat())
				&& this.getStartPoint().getLng()
						.equals(otherLS.getStartPoint().getLng());

	}

}
