package infer.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import infer.util.rules.GEOPoint;

public class RouteHistorySummary {
	final static Logger log = Logger.getLogger(RouteHistorySummary.class
			.getName());

	private int maxEntry = -1;
	private int maxEntryValue;

	private GEOPoint origin;

	private List<Route> listOfRoutes = new ArrayList<Route>();
	private List<String> routesRemoved = new ArrayList<String>();
	private List<CalendarEvent> calendarEvents = new ArrayList<CalendarEvent>();

	public GEOPoint getOrigin() {
		return origin;
	}

	public void setOrigin(GEOPoint origin) {
		this.origin = origin;
	}

	public List<Route> getListOfRoutes() {
		return listOfRoutes;
	}

	public void setListOfRoutes(List<Route> listOfRoutes) {
		this.listOfRoutes = listOfRoutes;
	}

	public List<CalendarEvent> getCalendarEvents() {
		return calendarEvents;
	}

	public void setCalendarEvents(List<CalendarEvent> calendarEvents) {
		this.calendarEvents = calendarEvents;
	}

	public List<String> getRoutesRemoved() {
		return routesRemoved;
	}

	public void setRoutesRemoved(List<String> routesRemoved) {
		this.routesRemoved = routesRemoved;
	}

	public int getMaxEntry() {
		return maxEntry;
	}

	public void setMaxEntry(int maxEntry) {
		this.maxEntry = maxEntry;
	}

	public int getMaxEntryValue() {
		return maxEntryValue;
	}

	public void setMaxEntryValue(int maxEntryValue) {
		this.maxEntryValue = maxEntryValue;
	}

	public void addRoute(Route route) {
		listOfRoutes.add(route);
	}

	public void recalculateMaxEntry() {
		if (maxEntry != -1) {
			return;
		}
		maxEntryValue = 0;
		for (int i = 0; i < listOfRoutes.size(); i++) {
			Route re = listOfRoutes.get(i);
			if (re.getCount() > maxEntryValue) {
				maxEntryValue = re.getCount();
				maxEntry = i;
			}
		}
	}

	public void recalculateMaxEntryConsiderTime() {
		Calendar now = Util.getNow();
		recalculateMaxEntryConsiderTime(now);
	}

	public void recalculateMaxEntryConsiderTime(Calendar actualRouteTime) {
		int nowMin = Util.getTimeOfDayInMinutes(actualRouteTime);
		log.fine(" actualRouteTime in minutes is " + nowMin
				+ " with timezone  = " + actualRouteTime.getTimeZone());
		maxEntryValue = 0;
		maxEntry = -1;
		List<Route> routes = getListOfRoutes();
		if (routes.size() > 0) {
			maxEntry = 0;
		}
		for (int i = 0; i < routes.size(); i++) {
			Route route = routes.get(i);
			for (int k = 0; k < route.getStartTimes().size(); k++) {
				Calendar c = route.getStartTimes().get(k);

				int then = Util.getTimeOfDayInMinutes(c);
				int timePart = Math.abs(1440 - Math.abs(nowMin - then));
				int countPart = route.getCount() * 100;
				int tempVal = timePart + countPart;
				if (tempVal > maxEntryValue) {
					log.fine("WINNER  route is " + route.getRouteName()
							+ " tempVal = " + tempVal + "maxEntryValue = "
							+ maxEntryValue + " index = " + i
							+ " number of line segments are "
							+ route.getLineSegments().size());
					maxEntry = i;
					maxEntryValue = tempVal;
				} else {
					log.fine("LOSER  route is " + route.getRouteName()
							+ " tempVal = " + tempVal + "maxEntryValue = "
							+ maxEntryValue + " index = " + i);
				}
			}
		}
		log.fine(" leaving recalculateMaxEntryConsiderTime with maxEntry = " + maxEntry);
	}

	/**
	 * Return a String representation of the Object
	 * 
	 * @return the representing String.
	 */
	@Override
	public String toString() {
		return " maxEntry " + maxEntry + " maxEntryValue: " + maxEntryValue
				+ " routes removed: " + routesRemoved + "origin is " + origin
				+ " routes= " + listOfRoutes;

	}

}
