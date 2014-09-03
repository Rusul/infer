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
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import infer.util.Util;
import infer.util.generated.AutoIE;
import infer.util.generated.Vcard;
import infer.util.rules.AllValue;
import infer.util.rules.Poi;
import infer.util.rules.Synonym;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * LoadAwardsData
 * 
 * Based on NITRD provided NSF data in awards.xml
 */
public class LoadLocation {

	final static Logger logger = Logger.getLogger(LoadLocation.class.getName());

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
		String destDir = file.getAbsolutePath().endsWith(File.separator) ? file
				.getAbsolutePath() : file.getAbsolutePath() + File.separator;
		logger.info(" file is " + file.getName());
		logger.info(" destDir = " + destDir);

		BuiltinRegistry.theRegistry.register(new AllValue());
		BuiltinRegistry.theRegistry.register(new Synonym());
		BuiltinRegistry.theRegistry.register(new Poi());

		if (file.canRead()) {
			if (file.isDirectory()) {
				destDir = args[1] + File.separatorChar;
				String[] files = file.list();
				// an IO error could occur
				if (files != null) {
					for (int i = 0; i < files.length; i++) {

						try {

							doFile(destDir, file.getAbsolutePath() + File.separatorChar
									+ files[i]);
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
	private static void doFile(String destDir, String fileName)
			throws FileNotFoundException {
		
		
		List rules = Rule.rulesFromURL("rules.txt");
		Reasoner reasoner = new GenericRuleReasoner(rules);

		Model ontModel = ModelFactory.createDefaultModel();
		InfModel model = ModelFactory.createInfModel(reasoner, ontModel);
		
		File file = new File(fileName);
		if (file.isHidden()) {
			return;
		}
		logger.info(" file is " + file);
		
		Individual maryFrank = createBusinessCard(model, "Mary Frank",
				"(765) 432-6375", "Mary_Frank_infer@hotmail.com",
				"VIPCommunity", "03/01/1972");

		Individual car = LoadLocation.createCar(model, "Mary Frank",
				"Mary_Frank_infer@hotmail.com", "GM", "Saturn", "2006",
				"12345",
				DatatypeConverter.printDateTime(new GregorianCalendar()),
				"white", "sedan", "IE", "driver", "TEST DATA");

		
		DomHandler dom = new DomHandler(file);

		NodeList rows = dom.getElementsByTagName("trkpt");
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
				if (tim != null) {
					if (i%100 == 0) {
						placeStatus = "places";
					} else {
						placeStatus = "point";
					}
					LoadLocation.createTrackPoint(car, lat, lon, placeStatus, tim, model);
					// Kokomo High School

					tim = null;
				}
			}
		}

		// insert(model);
		// StmtIterator si = model.getDeductionsModel().listStatements();
		//
		// while (si.hasNext()) {
		// Statement s = si.next();
		// logger.info(" got deduction statement of " + s);
		// }

		// Output model
		logger.fine("completed processing, outputting data");
		File outFile = new File(destDir + file.getName() + ".nt");
		File outFile2 = new File(destDir + file.getName() + ".owl");
		PrintStream ps = new PrintStream(outFile);
		PrintStream ps2 = new PrintStream(outFile2);
		logger.info("completed processing, writing out N-TRIPLE");
		model.write(ps, "N-TRIPLE");
		logger.info("completed processing, writing out RDF/XML-ABBREV");
		model.write(ps2, "RDF/XML-ABBREV");
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

	public static Individual createTrackPoint(Model model, Individual auto,
			String latitude, String longitude, String speed, String direction) {

		Individual trackPoint = AutoIE.TrackPoint
				.createIndividual(AutoIE.TrackPoint.getURI()
						+ System.currentTimeMillis());

		Statement s = null;

		s = model.createStatement(trackPoint, Vcard.latitude, latitude);
		model.add(s);

		s = model.createStatement(trackPoint, Vcard.longitude, longitude);
		model.add(s);
		s = model.createStatement(auto, AutoIE.isTrackedBy, trackPoint);

		model.add(s);
		s = model.createStatement(trackPoint, AutoIE.tracks, auto);
		model.add(s);

		s = model.createStatement(trackPoint, AutoIE.pointStatus, "places");
		model.add(s);

		// trackPoint = getPOI(model, trackPoint, latitude, longitude);

		return trackPoint;

	}

	public static Individual createTrackPoint(Individual auto,
			String latitude, String longitude, String pointStatus, String when, Model model) {

		Individual trackPoint = AutoIE.TrackPoint
				.createIndividual(AutoIE.TrackPoint.getURI()
						+ when);

		Statement s = null;

		s = model.createStatement(trackPoint, Vcard.latitude, latitude);
		model.add(s);

		s = model.createStatement(trackPoint, Vcard.longitude, longitude);
		model.add(s);

		s = model.createStatement(trackPoint, AutoIE.pointStatus, pointStatus);
		model.add(s);


		return trackPoint;

	}

	
	public static Individual createTrackPoint(Model model, Individual auto,
			String latitude, String longitude, String speed, String direction,
			String enterpriseShortName, String enterpriseLongName,
			String streetAddress, String city, String state, String zip) {

		Individual trackPoint = createTrackPoint(model, auto, latitude,
				longitude, speed, direction);

		Statement s = null;
		String address = streetAddress + "," + latitude + longitude;
		String term = address.replaceAll(" ", "_");
		Individual vcardAddress = Vcard.Address.createIndividual(Vcard.Address
				.getURI() + term);
		s = model.createStatement(vcardAddress, Vcard.street_address, address);
		model.add(s);
		s = model.createStatement(trackPoint, AutoIE.shortNameOfPOI,
				enterpriseShortName);
		model.add(s);
		s = model.createStatement(trackPoint, AutoIE.longNameOfPOI,
				enterpriseLongName);
		model.add(s);
		return trackPoint;

	}

 
}
