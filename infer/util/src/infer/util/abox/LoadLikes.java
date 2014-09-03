package infer.util.abox;

/*
 * @author Scott Streit
 * @version 0.1
 * Input from Excel xml files financial data and place into N3 and owl files.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import infer.util.JenaUtil;
import infer.util.LoadAboxWebService;
import infer.util.SolrUtil;
import infer.util.Util;
import infer.util.generated.AutoIE;
import infer.util.generated.Provenance;
import infer.util.generated.Vcard;
import infer.util.rules.AllValue;
import infer.util.rules.CalLocDest;
import infer.util.rules.CalTextDest;
import infer.util.rules.Like;
import infer.util.rules.Poi;
import infer.util.rules.PointToSegment;

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
import com.hp.hpl.jena.reasoner.rulesys.builtins.NoValue;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * LoadAwardsData
 * 
 * Based on NITRD provided NSF data in awards.xml
 */
public class LoadLikes {

	final static Logger logger = Logger.getLogger(LoadLikes.class.getName());

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
		BuiltinRegistry.theRegistry.register(new Like());
		BuiltinRegistry.theRegistry.register(new Poi());

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
//		 Insert Data into Model
		insertWithProvenance(model);
//	 	addGroupsAndInterest(model);
//		addPerson(model);
		StmtIterator si = model.getDeductionsModel().listStatements();

		while (si.hasNext()) {
			Statement s = si.next();
			logger.info(" got deduction statement of " + s);
		}

//		 Output model
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
	
	public static void doFromServlet(String name, String email, String bday, 
			List<String> likes, List<String> cats)
			throws FileNotFoundException {
		
		JenaUtil.loadRules();
		
		List rules = Rule.rulesFromURL("rules.txt");
		Reasoner reasoner = new GenericRuleReasoner(rules);
		Model ontModel = ModelFactory.createDefaultModel();
		InfModel model = ModelFactory.createInfModel(reasoner, ontModel);

		//		 Insert Data into Model
		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		
		Individual person = createBusinessCard(model, name,
				"(301) 596-2550", email, "",
				bday, provenance);
		
		for(int i=0;i<likes.size();i++){
			addFacebookLike(model, name, email,
					likes.get(i), cats.get(i), "facebook", provenance);	
		}

		StmtIterator si = model.getDeductionsModel().listStatements();

		while (si.hasNext()) {
			Statement s = si.next();
			logger.info(" got deduction statement of " + s);
		}

//		 Output model
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		logger.info("completed processing, writing out N-TRIPLE");
		model.write(os, "N-TRIPLE");
		ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
		LoadAboxWebService.doStream(is, SolrUtil.getProperties());
		logger.info("process completed successfully");
		
	}

	public static void insertWithProvenance(InfModel model) {

		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
	
		addFacebookLike(model, "Mary Frank", "Mary_Frank_infer@hotmail.com",
				"Ruby Tuesday", "restaurant", "yelp", provenance);	
		
		addFacebookLike(model, "Mary Frank", "Mary_Frank_infer@hotmail.com",
				"Philainfera Eagles", "sport", "facebook", provenance);	
	
		addFacebookLike(model, "Mary Frank", "Mary_Frank_infer@hotmail.com",
				"books", "mixer", "facebook", provenance);	
	}

	public static void addPerson(InfModel model) {

		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenance, Provenance.who, "Facebook Likes");
		model.add(provenance, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenance, Provenance.sourceDocument,
				"http:www.facebook.com/");
		model.add(provenance, Provenance.probability, "1");

		Individual provenanceScott = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceScott, Provenance.who, "Test");
		model.add(provenanceScott, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceScott, Provenance.sourceDocument,
				"http:www.scottstreit.com");
		model.add(provenanceScott, Provenance.probability, "1");

		Individual scott = createBusinessCard(model, "Scott Streit",
				"(301) 596-2550", "scott@scottstreit.com", "VIPCommunity",
				"11/15/1962", provenanceScott);
		Individual techie = createGroup(model, "Techie", provenance);

		addPersonToGroup(model, scott, techie, provenanceScott);

	}

	public static void addGroupsAndInterest(InfModel model) {
		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenance, Provenance.who, "Facebook Likes");
		model.add(provenance, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenance, Provenance.sourceDocument,
				"http:www.facebook.com/");
		model.add(provenance, Provenance.probability, "1");

		Individual techie = createGroup(model, "Techie", provenance);
		Individual professional = createGroup(model, "Professional", provenance);
		Individual adventurous = createGroup(model, "Adventurous", provenance);
		Individual sporty = createGroup(model, "Sporty", provenance);
		Individual minimalist = createGroup(model, "Minimalist", provenance);
		Individual luxurious = createGroup(model, "Luxurious", provenance);
		Individual spirited = createGroup(model, "Spirited", provenance);
		Individual friendly = createGroup(model, "Friendly", provenance);

		Individual foodie = createGroup(model, "Foodie", provenance);
		Individual cultural = createGroup(model, "Cultural", provenance);
		Individual frugal = createGroup(model, "Frugal", provenance);
		Individual fitness = createGroup(model, "Fitness", provenance);
		Individual outdoors = createGroup(model, "Outdoors", provenance);
		Individual fashion = createGroup(model, "Fashion", provenance);
		Individual kids = createGroup(model, "Kids", provenance);
		Individual homeImprovement = createGroup(model, "HomeImprovement",
				provenance);

		Individual technologyUser = createInterest(model, "technologyUser",
				"techie, gadget, computer, technology", "paper, manual",
				provenance);
		Individual technologyResearcher = createInterest(model,
				"technologyResearcher", " blog, wiki, rss, feed, wired",
				"paper, manual", provenance);
		Individual gagetOwner = createInterest(
				model,
				"gagetOwner",
				"mp3, ipad, smartphone, bluetooth, mobile, device, linked, network, connected",
				"paper, manual", provenance);
		Individual introverted = createInterest(model, "introverted",
				" shy, secluded, quiet", "party, noise", provenance);
		addInterestToGroup(model, technologyUser, techie, provenance);
		addInterestToGroup(model, technologyResearcher, techie, provenance);
		addInterestToGroup(model, gagetOwner, techie, provenance);

		addMandatoryInterestToGroup(model, technologyUser, techie, provenance);
		addMandatoryInterestToGroup(model, technologyResearcher, techie,
				provenance);

		Individual bachelorOfArts = createInterest(model, "bachelorsOfArts",
				"educated, read, art, experience, seasoned, accomplish",
				"technical, math, ology", provenance);
		Individual bachelorOfScience = createInterest(
				model,
				"bachelorsOfScience",
				"educated, read, math, science, experience, seasoned, accomplish, ology",
				"poetry", provenance);

		Individual ownsHome = createInterest(model, "ownsHome",
				" financial, credit, secure", "bankruptcy, debt", provenance);
		Individual livesAbovePovertyLine = createInterest(model,
				"livesAbovePovertyLine", " credit, financial ",
				"poor, poverty", provenance);
		addInterestToGroup(model, bachelorOfArts, professional, provenance);
		addInterestToGroup(model, bachelorOfScience, professional, provenance);
		addInterestToGroup(model, ownsHome, professional, provenance);
		addInterestToGroup(model, livesAbovePovertyLine, professional,
				provenance);

		addMandatoryInterestToGroup(model, ownsHome, professional, provenance);

		Individual outdoorPerson = createInterest(
				model,
				"outdoorPerson",
				"outdoor, camping, nature, animal, plant, tent, canopy, fresh, bicycle",
				"inside, indoor", provenance);
		Individual travel = createInterest(model, "travel",
				" adventure, airplane, fare, train, automobile, cruise",
				"stationary", provenance);
		addInterestToGroup(model, outdoorPerson, adventurous, provenance);
		addInterestToGroup(model, travel, adventurous, provenance);
		addInterestToGroup(model, livesAbovePovertyLine, adventurous,
				provenance);

		addMandatoryInterestToGroup(model, outdoorPerson, adventurous,
				provenance);
		addMandatoryInterestToGroup(model, travel, adventurous, provenance);

		Individual playsBasketball = createInterest(model, "playsBasketball",
				" fit, exercise, team, aerobic",
				"blind, stationary, fat, overweight", provenance);
		Individual playsSoftball = createInterest(model, "playsSoftball",
				" team, glove, bat, field, park", "blind", provenance);
		Individual playsFantasyFootball = createInterest(model,
				" social, statistics, stats, teams, fan, competitive",
				"sewing", "playsFantasyFootball", provenance);
		Individual playsFantasyBaseball = createInterest(model,
				" social, statistics, stats, teams, fan, competitive",
				"sewing", "playsFantasyBaseball", provenance);
		Individual attendsSports = createInterest(model, "attendsSports",
				"stadium, arena, team, professional, college", "cooking",
				provenance);
		Individual worksOut = createInterest(
				model,
				"worksOut",
				" gym, exercise, aerobic, weights, fitness, treadmil, ecliptical, yoga, pilate, train",
				"fat", provenance);
		Individual likesSnacks = createInterest(model, "likesSnacks",
				"eat, wings, chips, dip, soda, salsa, cola", "bistro",
				provenance);
		Individual extraverted = createInterest(model, "extraverted",
				" social, talk, outgoing, opinion ", "quiet", provenance);

		addInterestToGroup(model, playsBasketball, sporty, provenance);
		addInterestToGroup(model, playsSoftball, sporty, provenance);
		addInterestToGroup(model, playsFantasyFootball, sporty, provenance);
		addInterestToGroup(model, playsFantasyBaseball, sporty, provenance);
		addInterestToGroup(model, attendsSports, sporty, provenance);
		addInterestToGroup(model, worksOut, sporty, provenance);
		addInterestToGroup(model, likesSnacks, sporty, provenance);
		addInterestToGroup(model, extraverted, sporty, provenance);

		addMandatoryInterestToGroup(model, attendsSports, sporty, provenance);

		Individual avoidsGagets = createInterest(model, "avoidsGagets",
				"simple, paper, manual, pen, pencil, journal",
				"automatic, iphone, ipad, android", provenance);
		Individual reads = createInterest(model, "reads",
				"books, author, kindle", "hiphop, rap", provenance);
		addInterestToGroup(model, introverted, minimalist, provenance);
		addInterestToGroup(model, avoidsGagets, minimalist, provenance);
		addInterestToGroup(model, reads, minimalist, provenance);

		addMandatoryInterestToGroup(model, avoidsGagets, minimalist, provenance);
		addMandatoryInterestToGroup(model, reads, minimalist, provenance);

		Individual fineDining = createInterest(
				model,
				"fineDining",
				"cuisine, house, bakery,  cafe, bistro, french, fine, dining, chef, five, pub ",
				"donald, Burger, King, bar, wendy, sport, subway, dunkin, taco, doninos, hut, drive",
				provenance);
		Individual expensiveCar = createInterest(
				model,
				"expensiveCar",
				" luxury, sporty, mercedes, audi, bugatti, maserati, rolls, porche, jaguar",
				"saturn", provenance);
		Individual shopping = createInterest(
				model,
				"shopping",
				"amazon, commerce, mall, outlet, shipping, factory, design, fashion ",
				"shorts", provenance);
		Individual upperIncome = createInterest(model, "upperIncome",
				" credit, financial, stock, bond, trade, home, vacation",
				"welfare", provenance);
		addInterestToGroup(model, fineDining, luxurious, provenance);
		addInterestToGroup(model, expensiveCar, luxurious, provenance);
		addInterestToGroup(model, shopping, luxurious, provenance);
		addInterestToGroup(model, travel, luxurious, provenance);
		addInterestToGroup(model, upperIncome, luxurious, provenance);

		addMandatoryInterestToGroup(model, fineDining, luxurious, provenance);
		addMandatoryInterestToGroup(model, shopping, luxurious, provenance);

		Individual social = createInterest(model, "social",
				"party, social, invite, talk, group, outgoing", "quiet",
				provenance);
		Individual connected = createInterest(model, "connected",
				"outgoing, social, talk, social, wired, network", "quiet",
				provenance);
		addInterestToGroup(model, extraverted, spirited, provenance);
		addInterestToGroup(model, social, spirited, provenance);
		addInterestToGroup(model, connected, spirited, provenance);

		addMandatoryInterestToGroup(model, social, spirited, provenance);
		addMandatoryInterestToGroup(model, connected, spirited, provenance);

		doSameAs(model, spirited, friendly, provenance);

		Individual nonFranchiseFoodEater = createInterest(
				model,
				"NonFranchiseFoodEater",
				"mom, pop, recipe, local, downtown, dining, farmer",
				"donald, Burger, King, bar, wendy, sport, subway, dunkin, taco, doninos, hut, starbuck, drive",
				provenance);
		Individual recipes = createInterest(model, "recipes",
				"steward, child, better, homes, recipes.com",
				"McDonalds, burger, king", provenance);
		addInterestToGroup(model, nonFranchiseFoodEater, foodie, provenance);
		addInterestToGroup(model, social, foodie, provenance);
		addInterestToGroup(model, fineDining, foodie, provenance);
		addInterestToGroup(model, recipes, foodie, provenance);

		addMandatoryInterestToGroup(model, nonFranchiseFoodEater, foodie,
				provenance);
		addMandatoryInterestToGroup(model, recipes, foodie, provenance);

		Individual classicalMusic = createInterest(
				model,
				"classicalMusic",
				" bach, bethoven, classical, choppin, orchestra, cello, violin, flute, wind, instrument, brass, harmonic, philharmonic, tchaikovsky",
				"rock, rap, hiphop, blues", provenance);
		Individual jazzMusic = createInterest(
				model,
				"jazzMusic",
				" jazz, ellington, harmony, coltrane, basie, sinatra, sax, piano, trio. quartet, club, festival",
				"rock, rap, hiphop", provenance);
		Individual sociallyLiberal = createInterest(
				model,
				"sociallyLiberal",
				" obama, democrat, nader, green, redistribution, taxes, welfare",
				"adam, smith, friedman, Reagan", provenance);
		Individual independentMovies = createInterest(
				model,
				"independentMovies",
				" subtitles, allen, independent, movie, foreign, art, bergman ",
				"action, spielberg", provenance);
		Individual read = createInterest(
				model,
				"read",
				" book, kindle, novel, author, bookstore, barnes, amazon, half",
				"cartoons", provenance);

		addInterestToGroup(model, classicalMusic, cultural, provenance);
		addInterestToGroup(model, jazzMusic, cultural, provenance);
		addInterestToGroup(model, sociallyLiberal, cultural, provenance);
		addInterestToGroup(model, independentMovies, cultural, provenance);
		addInterestToGroup(model, read, cultural, provenance);

		addMandatoryInterestToGroup(model, classicalMusic, cultural, provenance);
		addMandatoryInterestToGroup(model, independentMovies, cultural,
				provenance);

		Individual coupons = createInterest(model, "coupons",
				"frugal, cheap, coupon, cutter, wallet, groupon ", "expensive",
				provenance);
		Individual bigBoxStores = createInterest(model, "bigBoxStores",
				" costco, sam, bj, bestbuy, sears, kmart, walmart", "mall",
				provenance);
		Individual dollarStore = createInterest(model, "dollarStore",
				" dollar, tree, 99", "mall", provenance);
		Individual avoidTolls = createInterest(model, "avoidTolls",
				"freeway, road", "toll", provenance);

		addInterestToGroup(model, coupons, frugal, provenance);
		addInterestToGroup(model, bigBoxStores, frugal, provenance);
		addInterestToGroup(model, dollarStore, frugal, provenance);
		addInterestToGroup(model, avoidTolls, frugal, provenance);

		addMandatoryInterestToGroup(model, coupons, frugal, provenance);
		addMandatoryInterestToGroup(model, bigBoxStores, frugal, provenance);

		Individual eatHealthy = createInterest(
				model,
				"eatHealthy",
				"vegetarian, calorie, gluten, vegan, carb, protein, chicken, fish",
				"candy", provenance);
		Individual sports = createInterest(model, "sports",
				" nfl, nba, nl, al, nhl, ncaa, tournament, big, sec, pac",
				"swiming", provenance);
		Individual vain = createInterest(model, "vain",
				"plastic, surgery, groom, fashion", "natural", provenance);
		Individual sportingGoods = createInterest(model, "sportingGoods",
				" big, 5, bass, pro, boat, gun, knife, hike, deer", "mall",
				provenance);

		addInterestToGroup(model, eatHealthy, fitness, provenance);

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
			String phone, String email, String orgName, String birthDate,
			Individual provenance) {
		if (name == null && phone == null && email == null)
			return null;
		String uri = Util.getPerson(name, email);
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
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		Date date = null;
		try {
			date = df.parse(birthDate);
		} catch (ParseException e) {

			e.printStackTrace();
			throw new RuntimeException(e);
		}
		Calendar cal = new GregorianCalendar();
		cal.setTime(date);
		s = model.createStatement(person, Vcard.bday,
				DatatypeConverter.printDateTime(cal));
		logger.info(" bday property is " + Vcard.bday.toString());
		reifyStatement(model, s, provenance);
		s = model.createStatement(person, AutoIE.preferences, "userLikes");
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
		s = model.createStatement(location, Vcard.extended_address, county);  
																				
		reifyStatement(model, s, provenance);
		s = model.createStatement(location, Vcard.country_name, country);
		reifyStatement(model, s, provenance);

		return location;
	}

	public static Individual createInterest(Model model, String name,
			String terms, String notTerms, Individual provenance) {
		Individual interest = AutoIE.InterestArea
				.createIndividual(AutoIE.InterestArea.getURI() + name);

		Statement s = model
				.createStatement(interest, AutoIE.interestName, name);
		reifyStatement(model, s, provenance);

		s = model.createStatement(interest, AutoIE.interestTerms, terms);
		reifyStatement(model, s, provenance);
		if (notTerms != null) {

			s = model.createStatement(interest, AutoIE.notInterestTerms,
					notTerms);
			reifyStatement(model, s, provenance);
		}

		return interest;
	}

	public static void addMandatoryInterestToGroup(Model model,
			Individual interest, Individual group, Individual provenance) {
		Statement s = model.createStatement(group,
				AutoIE.mandatoryInterestsForGroupInclusion, interest);
		reifyStatement(model, s, provenance);
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

	public static void addUniqueInterestToPerson(Model model, String name,
			String email, String interest, String terms) {
		String uri = Util.getPerson(name, email);
		Individual person = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri);

		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());

		Individual i = createInterest(model, interest + terms, terms, null,
				provenance);

		Statement s = model.createStatement(person,
				AutoIE.hasIndividualInterestOf, i);
		reifyStatement(model, s, provenance);
	}

	public static void addPersonToGroup(Model model, Individual person,
			Individual group, Individual provenance) {
		Statement s = model.createStatement(person, AutoIE.memberOf, group);
		reifyStatement(model, s, provenance);
		s = model.createStatement(group, AutoIE.hasAsMember, person);
		reifyStatement(model, s, provenance);
	}

	public static void addPersonToGroup(Model model, String name, String email,
			String groupName, String who, String what, String source) {
		String uri = Util.getPerson(name, email);
		Individual person = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri);
		Individual group = AutoIE.Group.createIndividual(AutoIE.Group.getURI()
				+ groupName);

		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenance, Provenance.who, who);
		model.add(provenance, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenance, Provenance.sourceDocument, source);
		model.add(provenance, Provenance.probability, "1");

		Statement s = model.createStatement(person, AutoIE.memberOf, group);
		reifyStatement(model, s, provenance);
		s = model.createStatement(group, AutoIE.hasAsMember, person);
		reifyStatement(model, s, provenance);
	}

	public static Individual addFacebookLike(Model model, String name,
			String email, String whatTheyLike, String category, String who,
			String what, String source) {
		String uri = Util.getPerson(name, email);
		Individual person = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri);

		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenance, Provenance.who, who);
		model.add(provenance, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenance, Provenance.sourceDocument, source);
		model.add(provenance, Provenance.probability, "1");

		Statement s = model.createStatement(provenance, Provenance.how, source);
		model.add(s);

		Individual o = AutoIE.Opinion.createIndividual(AutoIE.Opinion.getURI()
				+ System.currentTimeMillis());
		s = model.createStatement(o, AutoIE.opinionText, whatTheyLike);
		reifyStatement(model, s, provenance);

		s = model.createStatement(o, AutoIE.opinionCategory, category);
		reifyStatement(model, s, provenance);

		s = model.createStatement(person, AutoIE.describesLikesAs, o);
		reifyStatement(model, s, provenance);

		s = model.createStatement(o, AutoIE.referencesLikesFor, person);
		reifyStatement(model, s, provenance);

		return o;
	}

	public static Individual addFacebookLike(Model model, String name,
			String email, String whatTheyLike, String category, String source,
			Individual provenance) {
		String uri = Util.getPerson(name, email);
		Individual person = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri);

		Statement s = model.createStatement(provenance, Provenance.how, source);
		model.add(s);

		Individual o = AutoIE.Opinion.createIndividual(AutoIE.Opinion.getURI()
				+ System.currentTimeMillis());
		s = model.createStatement(o, AutoIE.opinionText, whatTheyLike);
		reifyStatement(model, s, provenance);

		s = model.createStatement(o, AutoIE.opinionCategory, category);
		reifyStatement(model, s, provenance);

		s = model.createStatement(person, AutoIE.describesLikesAs, o);
		reifyStatement(model, s, provenance);

		s = model.createStatement(o, AutoIE.referencesLikesFor, person);
		reifyStatement(model, s, provenance);

		return o;
	}

	public static Individual addFacebookLike(Model model, String name,
			String email, String interestName, String whatTheyLike,
			String category, String location, String source,
			Individual provenance) {

		Individual o = addFacebookLike(model, name, email, whatTheyLike,
				category, source, provenance);

		Individual interest = AutoIE.InterestArea
				.createIndividual(AutoIE.InterestArea.getURI() + interestName);

		Statement s = model.createStatement(o, AutoIE.opinesFor, interest);
		reifyStatement(model, s, provenance);

		s = model.createStatement(interest, AutoIE.isOpinedAbout, o);
		reifyStatement(model, s, provenance);

		return o;
	}

	public static void doSameAs(Model model, Individual one, Individual two,
			Individual provenance) {
		Statement s = model.createStatement(one, OWL.sameAs, two);
		reifyStatement(model, s, provenance);

	}

	public static void assignFriend(Model model, String name1, String email1,
			String name2, String email2, Individual provenance) {
		String uri = Util.getPerson(name1, email1);
		Individual person1 = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri);
		String uri2 = Util.getPerson(name2, email2);
		Individual person2 = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri2);
		assignFriend(model, person1, person2, provenance);
	}

	public static void assignFriend(Model model, Individual one,
			Individual two, Individual provenance) {
		Statement s = model.createStatement(one, AutoIE.isFriendOf, two);
		reifyStatement(model, s, provenance);

	}


	public static void searchResult(Model model, String name, String email,
			String term, String who, String what, String source) {
		String uri = Util.getPerson(name, email);
		Individual person = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri);

		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenance, Provenance.who, who);
		model.add(provenance, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenance, Provenance.sourceDocument, source);
		model.add(provenance, Provenance.probability, "1");

		Statement s = model.createStatement(provenance, Provenance.how, source);
		model.add(s);

		Individual o = AutoIE.SearchResult.createIndividual(AutoIE.SearchResult
				.getURI()
				+ uri
				+ "-"
				+ term.replaceAll(" ", "_")
				+ "-"
				+ System.currentTimeMillis());
		s = model.createStatement(person, AutoIE.storesAsHistogram, o);
		reifyStatement(model, s, provenance);
		s = model.createStatement(o, AutoIE.selectedTerms, term);
		reifyStatement(model, s, provenance);
	}

	public static void addPreferences(Model model, String name, String email,
			String preference, String who, String what, String source) {
		String uri = Util.getPerson(name, email);
		Individual person = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri);

		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenance, Provenance.who, who);
		model.add(provenance, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenance, Provenance.sourceDocument, source);
		model.add(provenance, Provenance.probability, "1");

		Statement s = model.createStatement(provenance, Provenance.how, source);
		model.add(s);

		s = model.createStatement(person, AutoIE.preferences, preference);
		reifyStatement(model, s, provenance);
	}

	public static Individual createProvenance(Model model, String who,
			String source) {
		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenance, Provenance.who, who);
		model.add(provenance, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenance, Provenance.sourceDocument, source);
		model.add(provenance, Provenance.probability, "1");
		return provenance;
	}

	public static void reifyStatement(Model model, Statement s,
			Individual provenance) {
		ReifiedStatement rs = model.createReifiedStatement(s);
		rs.addProperty(Provenance.isSourcedBy, provenance);
		model.add(s);
	}
}
