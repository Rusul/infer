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
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import infer.util.Util;
import infer.util.generated.AutoIE;
import infer.util.generated.Provenance;
import infer.util.generated.Vcard;

/**
 * LoadAwardsData
 * 
 * Based on NITRD provided NSF data in awards.xml
 */
public class LoadLikesNoInf {

	final static Logger logger = Logger.getLogger(LoadLikesNoInf.class.getName());

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
		insertWithProvenance(model);

	
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
	
		addFacebookLike(model, "Mary Frank", "Mary_Frank_infer@hotmail.com",
				"Ruby Tuesday", "restaurant", "yelp", provenance);	
		
		addFacebookLike(model, "Mary Frank", "Mary_Frank_infer@hotmail.com",
				"Philainfera Eagles", "sport", "facebook", provenance);	
	
		addFacebookLike(model, "Mary Frank", "Mary_Frank_infer@hotmail.com",
				"books", "mixer", "facebook", provenance);	
	}


	private static void printStatements(StmtIterator si) {
		while (si.hasNext()) {
			Statement s = si.next();

			logger.info("Statement is " + s);
		}

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
