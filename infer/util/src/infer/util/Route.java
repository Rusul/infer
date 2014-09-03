package infer.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import infer.util.rules.GEOPlace;
import infer.util.rules.GEOPoint;

public class Route {
	final static Logger log = Logger.getLogger(Route.class.getName());

	private List<GEOPlace> aroundDestination;

	private List<LineSegment> lineSegs = new ArrayList<LineSegment>();

	private String routeName;

	private int count;

	private String status;
	
	private GEOPoint origin;

	private List<Calendar> startTimes = new ArrayList<Calendar>();

	public List<LineSegment> getLineSegments() {
		return lineSegs;
	}

	public void setLineSegements(List<LineSegment> lineSegs) {
		this.lineSegs = lineSegs;
	}

	public LineSegment getLastLineSegment() {
		int i = lineSegs.size();
		if (i > 0) {
			return lineSegs.get(i - 1);
		}
		return null;
	}

	public LineSegment getFirstLineSegment() {
		int i = lineSegs.size();
		if (i > 0) {
			return lineSegs.get(0);
		}
		return null;
	}

	public List<Calendar> getStartTimes() {
		return startTimes;
	}

	public void setStartTime() {
		if (startTimes == null && startTimes.size() == 0) {
			if (getFirstLineSegment() != null) {
				startTimes.add(Util.toCalendar(getFirstLineSegment()
						.getStartPoint().getWhen()));
			}
		}
	}

	public void setStartTimes(List<Calendar> startTimes) {
		this.startTimes = startTimes;
	}

	public int segmentWithSegment(LineSegment otherLineSegment) {
		for (int i = 0; i < lineSegs.size(); i++) {
			LineSegment thisLineSegment = lineSegs.get(i);
			if (thisLineSegment.isSegmentOnSegment(otherLineSegment)) {
				return i;
			}
		}
		return -1;
	}

	public String getRouteName() {
		return routeName;
	}

	public void setRouteName(String routeName) {
		this.routeName = routeName;
	}

	public GEOPoint getOrigin() {
		return origin;
	}
	
	public void setOrigin(GEOPoint origin) {
		this.origin = origin;
	}

	public GEOPoint getDestination() {
		if (lineSegs == null || lineSegs.size() == 0) {
			return null;
		}
		return getLastLineSegment().getCurrentPoint();
	}

	public Calendar getWhenStarted(Calendar c1) {
		int oldTimeInMins = Util.getTimeOfDayInMinutes(c1);
		int timeInMins = 1440;
		int ctr = 0;
		Calendar newCal = null;
		if (getOrigin() != null) {
			Calendar c = this.getWhenStarted();
			int newTimeInMins = Util.getTimeOfDayInMinutes(c);
			int j = Math.abs(oldTimeInMins - newTimeInMins);
			if (j < timeInMins) {
				newCal = c;
				timeInMins = j;
			}

		}
		return newCal;
	}

	public Calendar getWhenStarted() {
		LineSegment ls = getFirstLineSegment();
		if (ls != null) {
			return Util.toCalendar(ls.getStartPoint().getWhen());
		}
		return null;

	}

	public Calendar getWhenEnded() {
		LineSegment ls = getLastLineSegment();
		if (ls != null) {
			return Util.toCalendar(ls.getStartPoint().getWhen());
		}
		return null;

	}

	public List<GEOPlace> getAroundDestination() {
		return aroundDestination;
	}

	public void setAroundDestination(List<GEOPlace> aroundDestination) {
		this.aroundDestination = aroundDestination;
	}

	public int getCount() {
		return startTimes.size();
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isSimliarTo(Route other) {
		boolean result = false;
		if (this.getLineSegments() == null
				|| this.getLineSegments().size() == 0
				|| other.getLineSegments() == null
				|| other.getLineSegments().size() == 0) {
			return result;
		}
		if (this.getFirstLineSegment().equals(other.getFirstLineSegment())
				&& this.getLastLineSegment().equals(other.getLastLineSegment())) {
			if (Math.abs(this.getLineSegments().size()
					- other.getLineSegments().size()) < 2) {
				return true;
			}
		}
		return result;
	}

	/**
	 * Return a String representation of the Object
	 * 
	 * @return the representing String.
	 */
	@Override
	public String toString() {

		StringBuffer p = new StringBuffer();

		if (getWhenStarted() != null) {
			p.append("whenStarted = "
					+ Util.calendarToISO8601String(getWhenStarted()));
		}
		if (getWhenEnded() != null) {
			p.append("whenEnded ="
					+ Util.calendarToISO8601String(getWhenEnded()));
		}

		for (int i = 0; i < getLineSegments().size(); i++) {
			p.append(" <LineSegment: [" + i + "]>"
					+ getLineSegments().get(i).toString() + "</LineSegment:["
					+ i + "]> \n");
		}
		if (aroundDestination != null) {
			p.append("aroundDestination = " + aroundDestination);
		}

		return "routeName is " + routeName + " origin = " + getOrigin()
				+ "destination = " + getDestination() + " count is " + count
				+ " status = " + status + " " + p.toString();

	}

}
