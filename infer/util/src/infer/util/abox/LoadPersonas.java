package infer.util.abox;

/*
 * @author Scott Streit
 * @version 0.1
 * Input from Excel xml files financial data and place into N3 and owl files.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import infer.util.Util;
import infer.util.generated.AutoIE;
import infer.util.generated.Provenance;
import infer.util.generated.Vcard;
import infer.util.rules.AllValue;
import infer.util.rules.Like;
import infer.util.rules.Poi;

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
public class LoadPersonas {

	final static Logger logger = Logger.getLogger(LoadPersonas.class.getName());

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

		Model model = ModelFactory.createDefaultModel();
//		 Insert Data into Model
		insertWithProvenance(model);
//	 	addGroupsAndInterest(model);
//		addPerson(model);
//		StmtIterator si = model.getDeductionsModel().listStatements();

//		while (si.hasNext()) {
//			Statement s = si.next();
//			logger.info(" got deduction statement of " + s);
//		}

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
	public static void insertWithProvenance(Model model) {

		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenance, Provenance.who, "Facebook Likes");
		model.add(provenance, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenance, Provenance.sourceDocument,
				"http:www.facebook.com/");
		model.add(provenance, Provenance.probability, "1");

		Individual provenanceSteve = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceSteve, Provenance.who, "Facebook");
		model.add(provenanceSteve, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceSteve, Provenance.sourceDocument,
				"http:www.facebook.com/?sk=welcome#!/steve.silver.108");
		model.add(provenanceSteve, Provenance.probability, "1");

		Individual provenanceSusan = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceSusan, Provenance.who, "Facebook");
		model.add(provenanceSusan, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceSusan, Provenance.sourceDocument,
				"http:www.facebook.com/?sk=welcome#!/susan.silver.7965");
		model.add(provenanceSusan, Provenance.probability, "1");

		Individual provenanceJohn = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceJohn, Provenance.who, "Facebook");
		model.add(provenanceJohn, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceJohn, Provenance.sourceDocument, "");
		model.add(provenanceJohn, Provenance.probability, "1");

		Individual provenanceSarah = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceJohn, Provenance.who, "Facebook");
		model.add(provenanceJohn, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceJohn, Provenance.sourceDocument, "");
		model.add(provenanceJohn, Provenance.probability, "1");

		Individual provenanceRobert = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceRobert, Provenance.who, "Facebook");
		model.add(provenanceRobert, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceRobert, Provenance.sourceDocument, "");
		model.add(provenanceRobert, Provenance.probability, "1");

		Individual provenanceJeff = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceJeff, Provenance.who, "Facebook");
		model.add(provenanceJeff, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceJeff, Provenance.sourceDocument, "");
		model.add(provenanceJeff, Provenance.probability, "1");

		Individual provenanceKlaus = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceKlaus, Provenance.who, "Facebook");
		model.add(provenanceKlaus, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceKlaus, Provenance.sourceDocument,
				"http:www.facebook.com/klausbusse");
		model.add(provenanceKlaus, Provenance.probability, "1");

		Individual provenanceJohnFrank = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceJohnFrank, Provenance.who, "Facebook");
		model.add(provenanceJohnFrank, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceJohnFrank, Provenance.sourceDocument,
				"http:www.facebook.com/johnfrank");
		model.add(provenanceJohnFrank, Provenance.probability, "1");

		Individual provenanceMaryFrank = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceMaryFrank, Provenance.who, "Facebook");
		model.add(provenanceMaryFrank, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceMaryFrank, Provenance.sourceDocument,
				"http:www.facebook.com/maryfrank");
		model.add(provenanceMaryFrank, Provenance.probability, "1");
		
		Individual provenanceJohnWilliams = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceJohnWilliams, Provenance.who, "Facebook");
		model.add(provenanceJohnWilliams, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceJohnWilliams, Provenance.sourceDocument,
				"http:www.facebook.com/maryfrank");
		model.add(provenanceJohnWilliams, Provenance.probability, "1");

		Individual provenanceLisaWilliams = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenanceLisaWilliams, Provenance.who, "Facebook");
		model.add(provenanceLisaWilliams, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenanceLisaWilliams, Provenance.sourceDocument,
				"http:www.facebook.com/maryfrank");
		model.add(provenanceLisaWilliams, Provenance.probability, "1");

		
		Individual steve = createBusinessCard(model, "Steve Silver",
				"(765) 398-1593", "stevesilver2012@gmail.com", "VIPCommunity",
				"01/01/1965", provenanceSteve);
		Individual susan = createBusinessCard(model, "Susan Silver",
				"(765) 398-1842", "susansilver2012@gmail.com", "VIPCommunity",
				"02/28/1970", provenanceSusan);
		Individual john = createBusinessCard(model, "John Silver", "NO PHONE",
				"adamgold2013@gmail.com", "VIPCommunity", "05/01/1998",
				provenanceJohn);
		Individual sarah = createBusinessCard(model, "Sarah Silver",
				"NO PHONE", "ryansilver2013@gmail.com", "VIPCommunity",
				"04/01/1998", provenanceSarah);
		Individual robert = createBusinessCard(model, "Robert Silver",
				"NO PHONE", "erinplatinum2012@gmail.com", "VIPCommunity",
				"06/01/1997", provenanceRobert);

		Individual jeff = createBusinessCard(model, "Jeff Platnum",
				"(765) 432-6375", "jeffplatinum2013@gmail.com", "VIPCommunity",
				"03/01/1967", provenanceJeff);

		Individual klaus = createBusinessCard(model, "Klaus Busse",
				"(765) 432-6375", "http:www.facebook.com/klausbusse",
				"VIPCommunity", "03/01/1972", provenanceKlaus);

		Individual johnFrank = createBusinessCard(model, "John Frank",
				"(765) 432-6375", "john.frank.inferces@gmail.com",
				"VIPCommunity", "03/01/1972", provenanceJohnFrank);

		Individual maryFrank = createBusinessCard(model, "Mary Frank",
				"(765) 432-6375", "mary.frank.inferces@gmail.com",
				"VIPCommunity", "03/01/1972", provenanceMaryFrank);

		Individual johnWilliams = createBusinessCard(model, "John Williams",
				"(765) 432-8000", "john.williams.inferces@gmail.com",
				"VIPCommunity", "06/28/1975", provenanceJohnWilliams);

		Individual lisaWilliams = createBusinessCard(model, "Lisa Williams",
				"(765) 432-9000", "lisa.williams.inferces@gmail.com",
				"VIPCommunity", "04/05/1985", provenanceLisaWilliams);

		
		
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
		Individual homeImprovement = createGroup(model, "HomeImprovement",provenance);
		Individual familyOriented = createGroup(model, "FamilyOriented", provenance);
		Individual cars = createGroup(model, "Cars", provenance);
		Individual health = createGroup(model, "Health", provenance);
		Individual vices = createGroup(model, "Vices", provenance);
		
		//TECHIE-----------------------------------------------------------------------
		Individual technologyUser = createInterest(model, "technologyUser",
				"electronics_store,atm","",
				provenance);
		Individual technologyResearcher = createInterest(model,
				"technologyResearcher", "electronics_store,atm",
				"", provenance);
		Individual gadgetOwner = createInterest(
				model,
				"gadgetOwner",
				"electronics_store,atm",
				"", provenance);
		
		addInterestToGroup(model, technologyUser, techie, provenance);
		addInterestToGroup(model, technologyResearcher, techie, provenance);
		addInterestToGroup(model, gadgetOwner, techie, provenance);

		addMandatoryInterestToGroup(model, technologyUser, techie, provenance);
		addMandatoryInterestToGroup(model, technologyResearcher, techie,
				provenance);
		//--------------------------------------------------------------------
		//PROFESSIONAL--------------------------------------------------------
		Individual bachelorOfArts = createInterest(model, "bachelorsOfArts",
				"art_gallery,museum",
				"", provenance);
		Individual bachelorOfScience = createInterest(
				model,
				"bachelorsOfScience",
				"electronics_store",
				"", provenance);
		Individual ownsHome = createInterest(model, "ownsHome",
				"bank, furniture_store, general_contractor, home_goods_store", "",provenance);
		Individual livesAbovePovertyLine = createInterest(model,
				"livesAbovePovertyLine", "atm, bank","", provenance);
		Individual historian = createInterest(model,
				"historian", "city_hall, church, place_of_worship, hindu_temple, mosque, courthouse, embassy, fire_station, local_government_office, museum",
				"", provenance);
		
		addInterestToGroup(model, bachelorOfArts, professional, provenance);
		addInterestToGroup(model, bachelorOfScience, professional, provenance);
		addInterestToGroup(model, ownsHome, professional, provenance);
		addInterestToGroup(model, livesAbovePovertyLine, professional,
				provenance);
		addInterestToGroup(model, historian, professional, provenance);
		
		addMandatoryInterestToGroup(model, ownsHome, professional, provenance);
		//--------------------------------------------------------------------
		//ADVENTUROUS---------------------------------------------------------
		Individual outdoorPerson = createInterest(
				model,
				"outdoorPerson",
				"bicycle_store, campground, park",
				"shopping_mall", provenance);
		Individual travels = createInterest(model, "travels",
				"airport, bus_station, car_rental, embassy, lodging, subway_station, taxi_stand, train_station, travel_agency",
				"", provenance);
		Individual animalLover = createInterest(model, "animalLover",
				"park, veterinary_care, zoo",
				"", provenance);
		
		addInterestToGroup(model, animalLover, adventurous, provenance);
		addInterestToGroup(model, outdoorPerson, adventurous, provenance);
		addInterestToGroup(model, travels, adventurous, provenance);
		addInterestToGroup(model, livesAbovePovertyLine, adventurous,
				provenance);
		
		addMandatoryInterestToGroup(model, outdoorPerson, adventurous,
				provenance);
		addMandatoryInterestToGroup(model, travels, adventurous, provenance);
		//--------------------------------------------------------------------
		//SPORTY--------------------------------------------------------------
		Individual playsBasketball = createInterest(model, "playsBasketball",
				"bar, gym, heatlh",
				"", provenance);
		Individual playsSoftball = createInterest(model, "playsSoftball",
				"bar, gym, health", "", provenance);
		Individual playsFantasyFootball = createInterest(model,
				"bar",
				"", "playsFantasyFootball", provenance);
		Individual playsFantasyBaseball = createInterest(model,
				"playsFantasyBaseball",
				"bar", "", provenance);
		Individual attendsSports = createInterest(model, "attendsSports",
				"bar, bowling_alley, stadium", "",
				provenance);
		Individual worksOut = createInterest(
				model,
				"worksOut",
				"gym, health",
				"", provenance);
		Individual likesSnacks = createInterest(model, "likesSnacks",
				"bar, grocery_or_supermarket", "",
				provenance);
		Individual extraverted = createInterest(model, "extraverted",
				"bar, restaurant", "", provenance);
		
		addInterestToGroup(model, playsBasketball, sporty, provenance);
		addInterestToGroup(model, playsSoftball, sporty, provenance);
		addInterestToGroup(model, playsFantasyFootball, sporty, provenance);
		addInterestToGroup(model, playsFantasyBaseball, sporty, provenance);
		addInterestToGroup(model, attendsSports, sporty, provenance);
		addInterestToGroup(model, worksOut, sporty, provenance);
		addInterestToGroup(model, likesSnacks, sporty, provenance);
		addInterestToGroup(model, extraverted, sporty, provenance);
		
		addMandatoryInterestToGroup(model, attendsSports, sporty, provenance);
		//--------------------------------------------------------------------
		//MINIMALIST
		Individual introverted = createInterest(model, "introverted",
				"book_store", "", provenance);
		Individual avoidsGadgets = createInterest(model, "avoidsGadgets",
				"library, book_store","electronics_store", provenance);
		Individual reads = createInterest(model, "reads",
				"book_store, library", "", provenance);
		
		addInterestToGroup(model, introverted, minimalist, provenance);
		addInterestToGroup(model, avoidsGadgets, minimalist, provenance);
		addInterestToGroup(model, reads, minimalist, provenance);
		
		addMandatoryInterestToGroup(model, avoidsGadgets, minimalist, provenance);
		addMandatoryInterestToGroup(model, reads, minimalist, provenance);
		//--------------------------------------------------------------------
		//LUXORIOUS
		Individual fineDining = createInterest(
				model,
				"fineDining",
				"bakery, café, finance, food, restaurant",
				"meal_takeaway",
				provenance);
		Individual expensiveCar = createInterest(
				model,
				"expensiveCar",
				"car_dealer, finance",
				"", provenance);
		Individual shopping = createInterest(
				model,
				"shopping",
				"atm, clothing_store, department_store, food",
				"", provenance);
		Individual upperIncome = createInterest(model, "upperIncome",
				"car_dealer, finance",
				"", provenance);
		
		addInterestToGroup(model, fineDining, luxurious, provenance);
		addInterestToGroup(model, expensiveCar, luxurious, provenance);
		addInterestToGroup(model, shopping, luxurious, provenance);
		addInterestToGroup(model, travels, luxurious, provenance);
		addInterestToGroup(model, upperIncome, luxurious, provenance);
		
		addMandatoryInterestToGroup(model, fineDining, luxurious, provenance);
		addMandatoryInterestToGroup(model, shopping, luxurious, provenance);
		//--------------------------------------------------------------------
		//FRIENDLY/SPIRITED
		Individual social = createInterest(model, "social",
				"bar, restaurant, shopping_mall", "",
				provenance);
		Individual connected = createInterest(model, "connected",
				"bar, restaurant, shopping_mall", "",
				provenance);
		
		addInterestToGroup(model, extraverted, spirited, provenance);
		addInterestToGroup(model, social, spirited, provenance);
		addInterestToGroup(model, connected, spirited, provenance);
		
		addMandatoryInterestToGroup(model, social, spirited, provenance);
		addMandatoryInterestToGroup(model, connected, spirited, provenance);

		doSameAs(model, spirited, friendly, provenance);
		//--------------------------------------------------------------------
		//FOODIE
		Individual nonFranchiseFoodEater = createInterest(
				model,
				"NonFranchiseFoodEater",
				"bakery",
				"bakery, bar, café, food",
				provenance);
		Individual recipes = createInterest(model, "recipes",
				"bakery, food, grocery_or_supermarket",
				"", provenance);
		addInterestToGroup(model, nonFranchiseFoodEater, foodie, provenance);
		addInterestToGroup(model, social, foodie, provenance);
		addInterestToGroup(model, fineDining, foodie, provenance);
		addInterestToGroup(model, recipes, foodie, provenance);

		addMandatoryInterestToGroup(model, nonFranchiseFoodEater, foodie,
				provenance);
		addMandatoryInterestToGroup(model, recipes, foodie, provenance);
		//--------------------------------------------------------------------
		//CULTURAL
		Individual classicalMusic = createInterest(
				model,
				"classicalMusic",
				"art_gallery, museum",
				"", provenance);
		Individual jazzMusic = createInterest(
				model,
				"jazzMusic",
				"café,art_gallery",
				"", provenance);
		Individual sociallyLiberal = createInterest(
				model,
				"sociallyLiberal",
				"park, campground",
				"", provenance);
		Individual independentMovies = createInterest(
				model,
				"independentMovies",
				"art_gallery, book_store, musuem",
				"", provenance);
		Individual art = createInterest(
				model,
				"art",
				"art_gallery",
				"", provenance);
		Individual europeanReligion = createInterest(
				model,
				"europeanReligion",
				"cemetery, church, funeral_home, place_of_worship, florist",
				"", provenance);
		Individual middleEasternReligion = createInterest(
				model,
				"middleEasternReligion",
				"cemetery, mosque, funeral_home, hindu_temple, place_of_worship",
				"", provenance);
		Individual romance = createInterest(
				model,
				"romance",
				"florist, jewelry_store, movie_theater",
				"", provenance);
		
		addInterestToGroup(model, classicalMusic, cultural, provenance);
		addInterestToGroup(model, jazzMusic, cultural, provenance);
		addInterestToGroup(model, sociallyLiberal, cultural, provenance);
		addInterestToGroup(model, independentMovies, cultural, provenance);
		addInterestToGroup(model, reads, cultural, provenance);
		addInterestToGroup(model, art, cultural, provenance);
		addInterestToGroup(model, europeanReligion, cultural, provenance);
		addInterestToGroup(model, middleEasternReligion, cultural, provenance);
		addInterestToGroup(model, romance, cultural, provenance);
		addMandatoryInterestToGroup(model, classicalMusic, cultural, provenance);
		addMandatoryInterestToGroup(model, independentMovies, cultural,
				provenance);
		//--------------------------------------------------------------------
		//FRUGAL
		Individual usesCoupons = createInterest(model, "usesCoupons",
				"grocery_or_supermarket, convenience_store", "",
				provenance);
		Individual enjoysBigBoxStores = createInterest(model, "enjoysBigBoxStores",
				"accounting, finance, grocery_or_supermarket", "mall",
				provenance);
		Individual enjoysDollarStores = createInterest(model, "enjoysDollarStores",
				"accounting, finance, convenience_store", "mall", provenance);
		Individual franchiseFoodEater = createInterest(model, "franchiseFoodEater",
				"convenience_store, food, meal_takeaway, restaurant", "café, bakery, restaurant", provenance);
		Individual avoidTolls = createInterest(model, "avoidTolls",
				"accounting, finance", "", provenance);

		addInterestToGroup(model, usesCoupons, frugal, provenance);
		addInterestToGroup(model, enjoysBigBoxStores, frugal, provenance);
		addInterestToGroup(model, enjoysDollarStores, frugal, provenance);
		addInterestToGroup(model, avoidTolls, frugal, provenance);

		addMandatoryInterestToGroup(model, usesCoupons, frugal, provenance);
		addMandatoryInterestToGroup(model, enjoysBigBoxStores, frugal, provenance);
		//--------------------------------------------------------------------
		//FAMILYORIENTED------------------------------------------------------
		Individual family = createInterest(
				model,
				"family",
				"amusement_park, aquarium, dentist, doctor, fire_station, food, furniture_store, grocery_or_supermarket, hair_care, health, hospital, laundry, library, lodging, meal_takeaway, museum, movie_theater, park, restaurant, shopping_mall, zoo",
				"casino", provenance);
		
		addInterestToGroup(model, animalLover, familyOriented, provenance);
		addInterestToGroup(model, family, familyOriented, provenance);
		addInterestToGroup(model, franchiseFoodEater, familyOriented, provenance);
		addInterestToGroup(model, enjoysBigBoxStores, familyOriented, provenance);
		addInterestToGroup(model, enjoysDollarStores, familyOriented, provenance);
		
		addMandatoryInterestToGroup(model, family, familyOriented, provenance);
		//--------------------------------------------------------------------
		//FITNESS-------------------------------------------------------------
		Individual eatsHealthy = createInterest(
				model,
				"eatsHealthy",
				"food, gym, restaurant",
				"", provenance);
		Individual enjoysSports = createInterest(model, "enjoysSports",
				"bar, stadium, bowling_alley",
				"", provenance);
		Individual vain = createInterest(model, "vain",
				"beauty_salon, clothing_store, department_store, hair_care, jewelry_store, shopping_mall, spa", 
				"", provenance);
		Individual sportingGoods = createInterest(model, "sportingGoods",
				"bicycle_store, campground, park", "",
				provenance);
		
		addInterestToGroup(model, eatsHealthy, fitness, provenance);
		addInterestToGroup(model, enjoysSports, fitness, provenance);
		addInterestToGroup(model, vain, fitness, provenance);
		addInterestToGroup(model, sportingGoods, fitness, provenance);
		addMandatoryInterestToGroup(model, eatsHealthy, fitness, provenance);
		addMandatoryInterestToGroup(model, enjoysSports, fitness, provenance);
		//--------------------------------------------------------------------
		//OUTDOORS-------------------------------------------------------------
		addInterestToGroup(model, outdoorPerson, outdoors, provenance);
		addInterestToGroup(model, livesAbovePovertyLine, outdoors, provenance);
		addMandatoryInterestToGroup(model, outdoorPerson, outdoors, provenance);
		//--------------------------------------------------------------------
		//FASHION-------------------------------------------------------------
		addInterestToGroup(model, shopping, fashion, provenance);
		addInterestToGroup(model, extraverted, fashion, provenance);
		addInterestToGroup(model, vain, fashion, provenance);
		addInterestToGroup(model, livesAbovePovertyLine, fashion, provenance);
		addMandatoryInterestToGroup(model, shopping, fashion, provenance);
		addMandatoryInterestToGroup(model, vain, fashion, provenance);
		//--------------------------------------------------------------------
		//KIDS----------------------------------------------------------------
		Individual elementarySchool = createInterest(
				model,
				"elementarySchool",
				"amusement_park, aquarium",
				"casino, liqour_store, bar", provenance);
		Individual cartoons = createInterest(model, "cartoons",
				"amusement_park, aquarium", "casino, liqour_store, bar",
				provenance);
		Individual videoGames = createInterest(model, "videoGames",
				"amusement_park, aquarium, electronics_store", "",
				provenance);
		Individual sweets = createInterest(model, "sweets",
				"bakery, food, grocery_or_supermarket", "",
				provenance);

		addInterestToGroup(model, elementarySchool, kids, provenance);
		addInterestToGroup(model, cartoons, kids, provenance);
		addInterestToGroup(model, videoGames, kids, provenance);
		addInterestToGroup(model, sweets, kids, provenance);
		addMandatoryInterestToGroup(model, elementarySchool, kids, provenance);
		addMandatoryInterestToGroup(model, sweets, kids, provenance);
		//--------------------------------------------------------------------
		//HOMEIMPROVEMENT-----------------------------------------------------
		Individual buysTools = createInterest(
				model,
				"buysTools",
				"home, depot, craftsman, sears, ace, hardware, kobalkt, lowes, air",
				"couch", provenance);
		Individual buysSupplies = createInterest(
				model,
				"buysSupplies",
				"electrician, hardware_store",
				"", provenance);
		Individual ownsLargeCargoVehicle = createInterest(
				model,
				"ownsLargeCargoVehicle",
				"gas_station, general_contractor", "", provenance);
		
		addInterestToGroup(model, buysTools, homeImprovement, provenance);
		addInterestToGroup(model, buysSupplies, homeImprovement, provenance);
		addInterestToGroup(model, ownsLargeCargoVehicle, homeImprovement,
				provenance);
	
		addMandatoryInterestToGroup(model, buysSupplies, homeImprovement,
				provenance);
		addMandatoryInterestToGroup(model, ownsLargeCargoVehicle,
				homeImprovement, provenance);
		//--------------------------------------------------------------------
		//CARS----------------------------------------------------------------
		Individual gearHead = createInterest(
				model,
				"gearHead",
				"car_dealer, car_rental, car_repair, car_wash, gas_station", "", provenance);
		
		addInterestToGroup(model, gearHead, cars, provenance);
		addInterestToGroup(model, ownsLargeCargoVehicle, cars, provenance);
		addInterestToGroup(model, expensiveCar, cars, provenance);
	
		addMandatoryInterestToGroup(model, gearHead,
				cars, provenance);
		//--------------------------------------------------------------------
		//VICES---------------------------------------------------------------
		Individual impiety = createInterest(
				model,
				"staysHealthy",
				"casino,liqour_store", "", provenance);
		
		addInterestToGroup(model, impiety, vices, provenance);
		addInterestToGroup(model, likesSnacks, vices, provenance);
		addInterestToGroup(model, attendsSports, vices, provenance);
	
		addMandatoryInterestToGroup(model, impiety, vices,
				provenance);
		
		//--------------------------------------------------------------------
		//HEALTH--------------------------------------------------------------
		Individual staysHealthy = createInterest(
				model,
				"staysHealthy",
				"dentist, doctor, health, hospital", "", provenance);
		
		addInterestToGroup(model, eatsHealthy, health, provenance);
		addInterestToGroup(model, staysHealthy, health, provenance);
		addInterestToGroup(model, nonFranchiseFoodEater, health, provenance);
		addInterestToGroup(model, playsBasketball, health, provenance);
		addInterestToGroup(model, playsSoftball, health, provenance);
	
		addMandatoryInterestToGroup(model, eatsHealthy, health,
				provenance);
		addMandatoryInterestToGroup(model, staysHealthy,
				health, provenance);
		//--------------------------------------------------------------------
		//Add People----------------------------------------------------------
				
		
		addPersonToGroup(model, lisaWilliams, frugal, provenanceLisaWilliams);
		addPersonToGroup(model, johnWilliams, fitness, provenanceJohnWilliams);
		addPersonToGroup(model, johnWilliams, fashion, provenanceJohnWilliams);
		addPersonToGroup(model, johnWilliams, sporty, provenanceJohnWilliams);
		
		addPersonToGroup(model, steve, techie, provenanceSteve);
		addPersonToGroup(model, steve, fitness, provenanceSteve);
		addPersonToGroup(model, steve, sporty, provenanceSteve);

		addPersonToGroup(model, susan, techie, provenanceSusan);
		addPersonToGroup(model, susan, foodie, provenanceSusan);

		addPersonToGroup(model, jeff, professional, provenanceJeff);
		addPersonToGroup(model, jeff, cultural, provenanceJeff);

		addPersonToGroup(model, klaus, fashion, provenanceKlaus);
		addPersonToGroup(model, klaus, fitness, provenanceKlaus);
		addPersonToGroup(model, klaus, sporty, provenanceKlaus);

		addPersonToGroup(model, johnFrank, techie, provenanceJohnFrank);
		addPersonToGroup(model, johnFrank, fitness, provenanceJohnFrank);
		addPersonToGroup(model, johnFrank, sporty, provenanceJohnFrank);
		addPersonToGroup(model, johnFrank, outdoors, provenanceJohnFrank);

		addPersonToGroup(model, maryFrank, foodie, provenanceMaryFrank);
		addPersonToGroup(model, maryFrank, cultural, provenanceMaryFrank);
		addPersonToGroup(model, maryFrank, outdoors, provenanceMaryFrank);

		assignFriend(model, jeff, steve, provenance);
		assignFriend(model, "John Frank", "John_Frank_infer1@hotmail.com",
				"Klaus Busse", "http:www.facebook.com/klausbusse", provenance);
		
		

		LoadPersonas.searchResult(model, "Mary Frank",
				"Mary_Frank_infer@hotmail.com", "cafe presse", "Auto",
				"Search Result", "Auto");

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
				"atm", "paper, manual",
				provenance);
		Individual technologyResearcher = createInterest(model,
				"technologyResearcher", " blog, wiki, rss, feed, wired",
				"paper, manual", provenance);
		Individual gadgetOwner = createInterest(
				model,
				"gadgetOwner",
				"mp3, ipad, smartphone, bluetooth, mobile, device, linked, network, connected",
				"paper, manual", provenance);
		Individual introverted = createInterest(model, "introverted",
				" shy, secluded, quiet", "party, noise", provenance);
		addInterestToGroup(model, technologyUser, techie, provenance);
		addInterestToGroup(model, technologyResearcher, techie, provenance);
		addInterestToGroup(model, gadgetOwner, techie, provenance);

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
				"atm ",
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
