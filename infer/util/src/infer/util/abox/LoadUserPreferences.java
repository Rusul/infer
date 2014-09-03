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

import infer.util.generated.AutoIE;
import infer.util.generated.Provenance;
import infer.util.generated.Vcard;
import infer.util.rules.AllValue;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * LoadAwardsData
 * 
 * Based on NITRD provided NSF data in awards.xml
 */
public class LoadUserPreferences {

	final static Logger logger = Logger.getLogger(LoadUserPreferences.class
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
		String fileName = args[1];
		logger.info(" file is " + file.getName());
		logger.info(" fileName is " + fileName);

		String destDir = file.getAbsolutePath().endsWith(File.separator) ? file
				.getAbsolutePath() : file.getAbsolutePath() + File.separator;
		logger.info(" destDir = " + destDir);

		BuiltinRegistry.theRegistry.register(new AllValue());

		try {
			doFile(destDir, fileName);
		} catch (Exception e) {
			logger.severe("error processing file " + file.getName()
					+ "absolute name " + file.getAbsolutePath());
			e.printStackTrace();
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
		// Insert Data into Model
		insertWithProvenance(model);

		// Output model
		logger.fine("completed processing, outputting data");
		File outFile = new File(destDir + fileName + ".nt");
		File outFile2 = new File(destDir + fileName + ".owl");
		PrintStream ps = new PrintStream(outFile);
		PrintStream ps2 = new PrintStream(outFile2);
		logger.info("completed processing, writing out N-TRIPLE");
		model.write(ps, "N-TRIPLE");
		logger.info("completed processing, writing out RDF/XML-ABBREV");
		model.write(ps2, "RDF/XML-ABBREV");
		logger.info("process completed successfully");
	}

	/**
	 * Inserts provided data into the Jena model
	 * 
	 * @param model
	 *            - the input model.
	 * 
	 */
	public static void insertWithProvenance(InfModel model) {
		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenance, Provenance.who, "Scott Streit");
		model.add(provenance, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenance, Provenance.sourceDocument, "Test Data");

		Individual scott = createBusinessCard(model, "Scott Streit",
				"301-596-2550", "scott@scottstreit.com", "CSI",  provenance);
		Individual beachley = createBusinessCard(model, "Beachley Main",
				"510-396-4327", "Beachley@beachley.com", "Infosys", provenance);

		Individual address = createVCardAddress(model, "Beachley House",
				"Santa Barbara", "CA", "91344", "Ventura", "USA", "5",
				provenance);

		Individual educated = createInterest(model, "Bachelors", provenance);
		Individual literature = createInterest(model, "Literature", provenance);
		doSameAs(model, educated, literature, provenance);

		Individual fishing = createInterest(model, "Fishing", provenance);
		Individual bullFighting = createInterest(model, "Bull_Fighting",
				provenance);
		Individual boxing = createInterest(model, "Boxing", provenance);

		Individual linux = createInterest(model, "Linux", provenance);
		Individual windows = createInterest(model, "Windows", provenance);

		/*
		 * if you are college educated and fish, you like hemmingway
		 */

		Individual hemmingway = createGroup(model, "Hemmingway", provenance, 2);

		addInterestToGroup(model, literature, hemmingway, provenance);
		addInterestToGroup(model, fishing, hemmingway, provenance);
		addInterestToGroup(model, boxing, hemmingway, provenance);
		addInterestToGroup(model, bullFighting, hemmingway, provenance);

		Individual openSource = createGroup(model, "Open_Source", provenance);
		Individual closedSource = createGroup(model, "Closed_Source",
				provenance);

		addInterestToGroup(model, linux, openSource, provenance);
		addInterestToGroup(model, windows, closedSource, provenance);

		addInterestToPerson(model, educated, beachley, provenance);
		addInterestToPerson(model, fishing, beachley, provenance);

		addMandatoryInterestToGroup(model, educated, hemmingway, provenance);
		addMandatoryInterestToGroup(model, fishing, hemmingway, provenance);

		addPersonToGroup(model, scott, hemmingway, provenance);
		// This happens by inference.
		// addPersonToGroup(model,beachley, hemmingway, provenance);

		StmtIterator si = model.listStatements(scott,
				AutoIE.hasIndividualInterestOf, (String) null);

		logger.info(" Scott is ");
		printStatements(si);

		model = ModelFactory.createInfModel(model.getReasoner(), model);

		
		si = model.listStatements(beachley, AutoIE.hasIndividualInterestOf,
				(String) null);


		logger.info(" Beachley's " + beachley.getURI() +" individual interests are ");
		printStatements(si);

		si = model.listStatements(beachley, AutoIE.memberOf,
				(String) null);

		logger.info(" Beachley's " + beachley.getURI() + " groups are ");
		logger.info(" ******** groups ********");
		printStatements(si);
		logger.info(" ******** after groups ********");
		
		

//		si = model.getDeductionsModel().listStatements();

//		logger.info(" Derived Statements are ");
//		printStatements(si);

//		addPersonToGroup(model, scott, openSource, provenance);
//		addPersonToGroup(model, beachley, closedSource, provenance);
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
			String phone, String email, String orgName,  Individual provenance) {
		if (name == null && phone == null && email == null)
			return null;
		String uri = name + "-" + phone + "-" + email;
		uri = Integer.toString(uri.hashCode());
		Individual person = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri);
		Statement s = null;
		s = model.createStatement(person, Vcard.fn, name);
		reifyStatement(model, s, provenance);
		s = model.createStatement(person, Vcard.tel, phone);
		reifyStatement(model, s, provenance);
		s = model.createStatement(person, Vcard.email, email);
		reifyStatement(model, s, provenance);
		s = model.createStatement(person, Vcard.organization_name, orgName);
		reifyStatement(model, s, provenance);
		return person;
	}

	/**
	 * Create a full Address Listing using vcard
	 * 
	 * @param model
	 * @param street
	 * @param city
	 * @param state
	 * @param zip
	 * @param county
	 * @param country
	 * @param congDistrict
	 * @return Vcard Address Individual
	 */
	public static Individual createVCardAddress(Model model, String street,
			String city, String state, String zip, String county,
			String country, String congDistrict, Individual provenance) {
		if (street == null && city == null && state == null && zip == null
				&& county == null && country == null && congDistrict == null)
			return null;
		String uri = street + "-" + city + "-" + state + "-" + zip + "-"
				+ county + "-" + country + "-" + congDistrict;
		uri = Integer.toString(uri.hashCode());
		Individual location = Vcard.Address.createIndividual(Vcard.Address
				.getURI() + uri);
		Statement s = model.createStatement(location, Vcard.street_address,
				street);
		reifyStatement(model, s, provenance);
		s = model.createStatement(location, Vcard.locality, city);
		reifyStatement(model, s, provenance);
		s = model.createStatement(location, Vcard.region, state);
		reifyStatement(model, s, provenance);
		s = model.createStatement(location, Vcard.postal_code, zip);
		reifyStatement(model, s, provenance);
		s = model.createStatement(location, Vcard.extended_address, county); // TODO
																				// county?
		reifyStatement(model, s, provenance);
		s = model.createStatement(location, Vcard.country_name, country);
		reifyStatement(model, s, provenance);

		return location;
	}

	public static Individual createInterest(Model model, String name,
			Individual provenance) {
		Individual interest = AutoIE.InterestArea
				.createIndividual(AutoIE.InterestArea.getURI() + name);

		Statement s = model
				.createStatement(interest, AutoIE.interestName, name);
		reifyStatement(model, s, provenance);
		return interest;
	}

	public static void addMandatoryInterestToGroup(Model model,
			Individual interest, Individual group, Individual provenance) {
		Statement s = model.createStatement(group,
				AutoIE.mandatoryInterestsForGroupInclusion, interest);
		reifyStatement(model, s, provenance);
	}

	public static Individual createGroup(Model model, String name,
			Individual provenance, int minForGroupInclusion) {

		Individual group = createGroup(model, name, provenance);

		Statement s = model
				.createStatement(group, AutoIE.minInterestsForGroupInclusion,
						"" + minForGroupInclusion);
		reifyStatement(model, s, provenance);
		return group;
	}

	public static Individual createGroup(Model model, String name,
			Individual provenance) {
		Individual group = AutoIE.Group.createIndividual(AutoIE.Group.getURI()
				+ name);

		Statement s = model.createStatement(group, AutoIE.groupName, name);
		reifyStatement(model, s, provenance);
		return group;
	}

	public static void addInterestToGroup(Model model, Individual interest,
			Individual group, Individual provenance) {
		Statement s = model.createStatement(group, AutoIE.hasGroupInterestOf,
				interest);
		reifyStatement(model, s, provenance);
		s = model.createStatement(interest, AutoIE.helpsGroupDefine, group);
		reifyStatement(model, s, provenance);
	}

	public static void addInterestToPerson(Model model, Individual interest,
			Individual person, Individual provenance) {
		Statement s = model.createStatement(person,
				AutoIE.hasIndividualInterestOf, interest);
		reifyStatement(model, s, provenance);
		s = model.createStatement(interest, AutoIE.helpsIndividualDefine,
				person);
		reifyStatement(model, s, provenance);
	}

	public static void addPersonToGroup(Model model, Individual person,
			Individual group, Individual provenance) {
		Statement s = model.createStatement(person, AutoIE.memberOf, group);
		reifyStatement(model, s, provenance);
		s = model.createStatement(group, AutoIE.hasAsMember, person);
		reifyStatement(model, s, provenance);
	}

	public static void doSameAs(Model model, Individual one, Individual two,
			Individual provenance) {
		Statement s = model.createStatement(one, OWL.sameAs, two);
		reifyStatement(model, s, provenance);

	}

	public static void reifyStatement(Model model, Statement s,
			Individual provenance) {
		ReifiedStatement rs = model.createReifiedStatement(s);
		rs.addProperty(Provenance.isSourcedBy, provenance);
		model.add(s);
	}
}
