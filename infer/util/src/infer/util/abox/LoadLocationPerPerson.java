package infer.util.abox;

/*
 * @author Scott Streit
 * @version 0.1
 * Input from Excel xml files financial data and place into N3 and owl files.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import infer.util.Util;
import infer.util.generated.AutoIE;
import infer.util.generated.Vcard;
import infer.util.rules.GEOPoint;

/**
 * LoadAwardsData
 * 
 * Based on NITRD provided NSF data in awards.xml
 */
public class LoadLocationPerPerson {

	final static Logger logger = Logger.getLogger(LoadLocationPerPerson.class
			.getName());

	/**
	 * Pull File(s) from provided path and pass on for processing
	 * 
	 * @param arg
	 *            [0] = input file/directory Path
	 * @param arg
	 *            [1] = output directory Path
	 */
	public static void main(String args[]) throws Exception {
		File file = new File(args[0]);
		String dest = args[1];
		String prefix = args[2];
		String personURI = args[3];
		String vin = args[4];
		String close = args[5];
		String when = Util.getNowString();
		if (args.length > 6) {
			when = args[6];
		}
		double CLOSE_CHECK = Double.parseDouble(args[5]);
		logger.info(" close check is" + CLOSE_CHECK);

		String destDir = file.getAbsolutePath().endsWith(File.separator) ? file
				.getAbsolutePath() : file.getAbsolutePath() + File.separator;
		logger.info(" file is " + file.getName());
		logger.info(" destDir = " + destDir);

		if (file.canRead()) {
			if (file.isDirectory()) {
				destDir = args[1] + File.separatorChar;
				String[] files = file.list();
				// an IO error could occur
				if (files != null) {
					for (int i = 0; i < files.length; i++) {

						try {

							doFile(destDir, file.getAbsolutePath()
									+ File.separatorChar + files[i], dest,
									prefix, personURI, vin, CLOSE_CHECK, when);
						} catch (Exception e) {
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
	private static void doFile(String destDir, String fileName, String dest,
			String prefix, String personURI, String vin, double closeDef,
			String tim) throws FileNotFoundException {

		Model model = ModelFactory.createDefaultModel();

		File file = new File(fileName);
		if (file.isHidden()) {
			return;
		}
		logger.info("in doFile with destDir = " + destDir + " fileName is "
				+ fileName + "dest = " + dest + " prefix = " + prefix
				+ "personURI =" + personURI + " vin = " + vin);
		Individual person = Vcard.VCard.createIndividual(personURI);
		Individual car = LoadLocationPerPerson.createCar(model, person, vin);

		Individual route = AutoIE.Route.createIndividual(AutoIE.Route.getURI()
				+ System.currentTimeMillis());

		model.add(route, AutoIE.routeStatus, "COMPLETED");
		model.add(car, AutoIE.drives, route);
		model.add(route, AutoIE.isDrivenBy, car);

		model.add(person, AutoIE.driverDrivesARoute, route);
		model.add(route, AutoIE.routeHasAsADriver, person);

		writeModel(destDir, prefix + System.currentTimeMillis(), model);

		model = ModelFactory.createDefaultModel();
		DomHandler dom = new DomHandler(file);

		NodeList rows = dom.getElementsByTagName("trkpt");
		String plat = null;
		String plon = null;
		for (int i = 0; i < rows.getLength(); i++) {
			Node row = rows.item(i);
			String lat = row.getAttributes().getNamedItem("lat").getNodeValue();
			String lon = row.getAttributes().getNamedItem("lon").getNodeValue();

			logger.info(" lat = " + lat + " lon = " + lon);
			NodeList rowChildren = row.getChildNodes();
			String placeStatus;
			if (lat != null && lat.trim().length() > 0 && lon != null
					&& lon.trim().length() > 0) {
				for (int x = 0; x < rowChildren.getLength(); x++) {
					Node rowChild = rowChildren.item(x);
					String item = rowChild.getNodeName();
					logger.info("nodeName = " + rowChild.getNodeName()); // Tag
					if (item.equals("time")) {
						tim = dom.getFirstChildNodeValue(rowChild);
					}
					// Name
					logger.info("value = "
							+ dom.getFirstChildNodeValue(rowChild)); // Contained
					// Value
					tim = Util.datePlus(tim, 1000 * 60);
					Individual trackPoint = LoadLocationPerPerson
							.createTrackPoint(car, lat, lon, "point", tim,
									model, plat, plon, closeDef);
					if (trackPoint != null) {
						if (i == 0) {
							model.add(route, AutoIE.hasAsAPointOfOrigin,
									trackPoint);
							model.add(trackPoint, AutoIE.isOriginFor, route);
							writeModel(destDir,
									prefix + System.currentTimeMillis(), model);
							model = ModelFactory.createDefaultModel();
						} else {
							model.add(route, AutoIE.isTrackedBy, trackPoint);
						}
						writeModel(destDir,
								prefix + System.currentTimeMillis(), model);
						model = ModelFactory.createDefaultModel();
						plat = lat;
						plon = lon;
					}
				}
			}
		}

	}

	public static void writeModel(String destDir, String fileName, Model model)
			throws FileNotFoundException {
		// Output model
		File outFile = new File(destDir + fileName + ".nt");
		PrintStream ps = new PrintStream(outFile);
		model.write(ps, "N-TRIPLE");
		logger.info(" Completed processing, outputting data for file "
				+ outFile.getAbsolutePath() + " " + outFile.getName());
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

	public static Individual createCar(Model model, Individual person,
			String VIN) {

		Individual auto = AutoIE.Automobile.createIndividual(AutoIE.Automobile
				.getURI() + VIN);

		Statement s = model.createStatement(person, AutoIE.operates, auto);

		s = model.createStatement(auto, AutoIE.isOperatedBy, person);
		model.add(s);

		return auto;
	}

	public static Individual createTrackPoint(Individual auto, String latitude,
			String longitude, String pointStatus, String when, Model model,
			String plat, String plon, double closeVal) {

		Individual trackPoint = AutoIE.TrackPoint
				.createIndividual(AutoIE.TrackPoint.getURI()
						+ System.currentTimeMillis());

		if (plat != null && plon != null) {
			if (GEOPoint.isClose(latitude, longitude, plat, plon, closeVal)) {
				return null;
			}
		}

		Statement s = null;

		s = model.createStatement(trackPoint, AutoIE.trackPointWhen, when);
		model.add(s);

		s = model.createStatement(trackPoint, Vcard.latitude, latitude);
		model.add(s);

		s = model.createStatement(trackPoint, Vcard.longitude, longitude);
		model.add(s);

		s = model.createStatement(trackPoint, AutoIE.pointStatus, pointStatus);
		model.add(s);

		return trackPoint;

	}

}
