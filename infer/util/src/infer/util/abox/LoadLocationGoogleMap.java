package infer.util.abox;

/*
 * @author Scott Streit
 * @version 0.1
 * Input from Excel xml files financial data and place into N3 and owl files.
 */
 
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import infer.util.Util;
import infer.util.generated.AutoIE;
import infer.util.generated.Vcard;
import infer.util.rules.GEOPoint;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 **
 */
public class LoadLocationGoogleMap {

	final static Logger logger = Logger.getLogger(LoadLocationGoogleMap.class
			.getName());
	public static double CLOSE_CHECK = 0.05;
	public static String  startDateTimeString = "NOW";
	public static long  millisecondsBetween = 1000;
	public static String prefix="1_";

	/**
	 * Pull File(s) from provided path and pass on for processing
	 * 
	 * @param arg
	 *            [0] = input file/directory Path
	 * @param arg
	 *            [1] = output directory Path
	 * @param arg
	 *            [2] = start time for data in iso8601string (2013-01-01T00:00:15.340Z) format or NOW for now.
	 * @param arg
	 *            [3] = distance between points in float (ex. 0.05 is 50 meters).         
	 * @param arg
	 *            [4] = time between points in milliseconds (long) (ex. 1000  is 1 second).
	 * @param arg
	 *            [5] = prefix for output file.

	 *            
	 *            
	 */
	public static void main(String args[]) throws Exception {
		File file = new File(args[0]);
		String destDir = file.getAbsolutePath().endsWith(File.separator) ? file
				.getAbsolutePath() : file.getAbsolutePath() + File.separator;
		logger.info(" file is " + file.getName());
		logger.info(" destDir = " + destDir);
		if (args.length >= 3) {
			startDateTimeString = args[2];
		}
		if (args.length >= 4) {
			CLOSE_CHECK = Double.parseDouble(args[3]);
		}
		if (args.length >= 5) {
			millisecondsBetween = Long.parseLong(args[4]);
		}
		if (args.length >= 6) {
			prefix = args[5];
		}
		
		if (startDateTimeString.equals("NOW")) {
			startDateTimeString = Util.getNowString();		
		}

		if (file.canRead()) {
			if (file.isDirectory()) {
				destDir = args[1] + File.separatorChar;
				String[] files = file.list();
				// an IO error could occur
				if (files != null) {
					for (int i = 0; i < files.length; i++) {

						try {

							doFile(destDir, file.getAbsolutePath()
									+ File.separatorChar + files[i]);
						} catch (Exception e) {
							e.printStackTrace();
							logger.severe("error processing file ");
						}
					}
				}
			}
		}
	}

	/**
	 * Process one file at a time. Create two output files per input file. One
	 * file in N3 format with an N3 appended to file name. The other in rdf/owl
	 * format with an appended .owl name.
	 * 
	 * Strips data from the xml file and passes on for processing into a jena
	 * model.
	 * 
	 * @param file
	 *            - the input file.
	 * @param destDir
	 *            - the destination directory to place the Abox results.
	 */
	private static void doFile(String destDir, String fileName)
			throws FileNotFoundException {

		Model model = ModelFactory.createDefaultModel();

		File file = new File(fileName);
		if (file.isHidden()) {
			return;
		}
		logger.info(" file is " + file);

		Individual susanSilver = createBusinessCard(model, "Susan Silver",
				"(765) 398-1842", "susansilver2012@gmail.com", "VIPCommunity",
				"02/28/1970");

		Individual car = LoadLocationGoogleMap.createCar(model, "Susan Silver",
				"susansilver2012@gmail.com", "Chrysler", " Town & Country ",
				"2013", "2C4RC1BG3DR614258",
				DatatypeConverter.printDateTime(new GregorianCalendar()),
				"white", "Minivan", "IE", "driver", "TEST DATA");

		Individual route = AutoIE.Route.createIndividual(AutoIE.Route.getURI()
				+ System.currentTimeMillis());

		model.add(route, AutoIE.routeStatus, "COMPLETED");
		model.add(car, AutoIE.drives, route);

		model.add(route, AutoIE.isDrivenBy, car);

		model.add(susanSilver, AutoIE.driverDrivesARoute, route);
		model.add(route, AutoIE.routeHasAsADriver, susanSilver);

		DomHandler dom = new DomHandler(file);

		NodeList rows = dom.getElementsByTagName("trkpt");

		String previousLat = "0";
		String previousLon = "0";

		for (int i = 0; i < rows.getLength(); i++) {
			Node row = rows.item(i);
			String lat = row.getAttributes().getNamedItem("lat").getNodeValue();
			String lon = row.getAttributes().getNamedItem("lon").getNodeValue();

			logger.info(" lat = " + lat + " lon = " + lon);
			NodeList rowChildren = row.getChildNodes();
			String tim = null;
			String placeStatus;
			for (int x = 0; x < rowChildren.getLength(); x++) {
				Node rowChild = rowChildren.item(x);
				String item = rowChild.getNodeName();
				logger.info("nodeName = " + rowChild.getNodeName()); // Tag
				if (item.equals("time")) {
					tim = dom.getFirstChildNodeValue(rowChild);
				}
				// Name
				logger.info("value = " + dom.getFirstChildNodeValue(rowChild)); // Contained
				// Value

				if (i == 0 || i == rows.getLength() - 1) {
					placeStatus = "places";
				} else {
					placeStatus = "point";
				}
				logger.fine(" before call to createTrackPoint " + millisecondsBetween + "date is " + startDateTimeString);				

				if (LoadLocationGoogleMap.createTrackPoint(route, lat, lon,
						placeStatus, startDateTimeString, model, previousLat,
						previousLon, CLOSE_CHECK, i) != null) {
					previousLat = lat;
					previousLon = lon;
					logger.fine(" before call to alter date millisecondsBetween " + millisecondsBetween + "date is " + startDateTimeString);				
					startDateTimeString = Util.datePlus(startDateTimeString, millisecondsBetween);
					logger.fine(" after call to alter date millisecondsBetween " + millisecondsBetween + "date is " + startDateTimeString);				
				}

			}
		}

		logger.fine("completed processing, outputting data");
		File outFile = new File(destDir + prefix + file.getName() + ".nt");
		// File outFile2 = new File(destDir + file.getName() + ".owl");
		PrintStream ps = new PrintStream(outFile);
		// PrintStream ps2 = new PrintStream(outFile2);
		logger.info("completed processing, writing out N-TRIPLE");
		model.write(ps, "N-TRIPLE");
		// logger.info("completed processing, writing out RDF/XML-ABBREV");
		// model.write(ps2, "RDF/XML-ABBREV");
		logger.info("process completed successfully");
	}

	private static void printStatements(StmtIterator si) {
		while (si.hasNext()) {
			Statement s = si.next();

			logger.info("Statement is " + s);
		}

	}

	/**
	 * Create a basic business card for a person using vcard
	 * 
	 * @param model
	 * @param name
	 * @param phone
	 * @param email
	 * @return VCard Individual
	 */
	public static Individual createBusinessCard(Model model, String name,
			String phone, String email, String orgName, String birthDate) {
		if (name == null && phone == null && email == null)
			return null;
		String uri = Util.getPerson(name, email);
		Individual person = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri);
		Statement s = null;
		s = model.createStatement(person, Vcard.fn, name);
		model.add(s);
		return person;
	}

	public static Individual createCar(Model model, String name, String email,
			String carMake, String carModel, String carYear, String VIN,
			String buldDate, String color, String bodyStyle, String who,
			String what, String source) {
		String uri = Util.getPerson(name, email);
		Individual person = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri);
		Individual auto = AutoIE.Automobile.createIndividual(AutoIE.Automobile
				.getURI() + VIN);

		Statement s = model.createStatement(person, AutoIE.operates, auto);

		model.add(s);

		s = model.createStatement(auto, AutoIE.isOperatedBy, person);
		model.add(s);

		s = model.createStatement(auto, AutoIE.carMake, carMake);
		model.add(s);
		s = model.createStatement(auto, AutoIE.carModel, carModel);
		model.add(s);
		s = model.createStatement(auto, AutoIE.carYear, carYear);
		model.add(s);

		return auto;
	}

	public static Individual createTrackPoint(Individual route,
			String latitude, String longitude, String pointStatus, String when,
			Model model, String pl, String plon, double closeVal, int index) {
		logger.info(" in createTrackPoint with when = " + when);

		if (GEOPoint.isClose(latitude, longitude, pl, plon, closeVal)) {
			return null;
		}
		Individual trackPoint = AutoIE.TrackPoint
				.createIndividual(AutoIE.TrackPoint.getURI() + "_"
						+ when + "_" +  System.currentTimeMillis());
		Statement s = null;
		
		if (index == 0) {
			s = model.createStatement(route, AutoIE.hasAsAPointOfOrigin, trackPoint);
			model.add(s);
			s = model.createStatement(trackPoint, AutoIE.isOriginFor, route);
			model.add(s);			
		}

		s = model.createStatement(trackPoint, Vcard.latitude, latitude);
		model.add(s);

		s = model.createStatement(trackPoint, Vcard.longitude, longitude);
		model.add(s);
		s = model.createStatement(route, AutoIE.isTrackedBy, trackPoint);

		model.add(s);
		s = model.createStatement(trackPoint, AutoIE.tracks, route);
		model.add(s);

		s = model.createStatement(trackPoint, AutoIE.trackPointWhen, when);
		model.add(s);

		s = model.createStatement(trackPoint, AutoIE.pointStatus, pointStatus);
		model.add(s);

		return trackPoint;

	}

}
