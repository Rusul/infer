package infer.util.abox;

/*
 * @author Scott Streit
 * @version 0.1
 * Input from Excel xml files financial data and place into N3 and owl files.
 */

import infer.util.Util;
import infer.util.generated.IDS;
import infer.util.generated.Provenance;
import infer.util.rules.Blacklist;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * Load Incidents
 * 
 * Based on URI data provided by websec
 */
public class LoadIncidents {

	final static Logger logger = Logger
			.getLogger(LoadIncidents.class.getName());

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
		BuiltinRegistry.theRegistry.register(new Blacklist());

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
	public static void insertWithProvenance(Model model) {

		Individual provenance = Provenance.Provenance
				.createIndividual(Provenance.getURI()
						+ System.currentTimeMillis());
		model.add(provenance, Provenance.who, "IDS");
		model.add(provenance, Provenance.when,
				DatatypeConverter.printDateTime(new GregorianCalendar()));
		model.add(provenance, Provenance.sourceDocument, "websec");
		model.add(provenance, Provenance.probability, "1");

		String incTime = "";
		for (int i = 0; i < 8; i++) {

			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MONTH, 8);
			cal.set(Calendar.DAY_OF_MONTH, 5);
			cal.set(Calendar.YEAR, 2014);
			cal.set(Calendar.HOUR_OF_DAY, 2 * i);
			incTime = Util.calendarToISO8601String(cal);

			Individual incident = createIncident(model, "192.168.3.112",
					"domain1", "255.255.0", "123456", "none", "field1",
					"6666666", "1234567", incTime, provenance);
		}
		String incTime2 = "";
		for (int i = 0; i < 23; i++) {

			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MONTH, 4);
			cal.set(Calendar.DAY_OF_MONTH, 12);
			cal.set(Calendar.YEAR, 2013);
			cal.set(Calendar.HOUR_OF_DAY, i);
			incTime2 = Util.calendarToISO8601String(cal);

			Individual incident = createIncident(model, "192.168.3.112",
					"domain1", "255.255.0", "123456", "none", "field1",
					"6666666", "1234567", incTime2, provenance);
		}

		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, 11);
		cal.set(Calendar.DAY_OF_MONTH, 18);
		cal.set(Calendar.YEAR, 2013);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		String attackStartTime = Util.calendarToISO8601String(cal);
		Individual attack1 = createAttack(model, "192.168.3.116","1233367",
				attackStartTime, incTime, "Multiple IP Attempt Attack",
				provenance);

		Calendar cal2 = Calendar.getInstance();
		cal2.set(Calendar.MONTH, 4);
		cal2.set(Calendar.DAY_OF_MONTH, 12);
		cal2.set(Calendar.YEAR, 2013);
		cal2.set(Calendar.HOUR_OF_DAY, 0);
		String attackStartTime2 = Util.calendarToISO8601String(cal2);
		Individual attack2 = createAttack(model, "192.168.3.118","1233567",
				attackStartTime2, incTime2, "Multiple IP Attempt Attack",
				provenance);

	}

	private static void printStatements(StmtIterator si) {
		while (si.hasNext()) {
			Statement s = si.next();

			logger.info("Statement is " + s);
		}

	}

	/**
	 * Create an incident
	 * 
	 * @param model
	 * @param ipAddress
	 * @param domain
	 * @param macAddress
	 * @param deviceID
	 * @param failedField
	 * @param actualValue
	 * @param desiredValue
	 * @param status
	 * @return Incident Individual
	 */
	public static Individual createIncident(Model model, String sourceIP,
			String sourceDomain, String sourceMAC, String sourceDeviceId,
			String status, String failedField, String actualValue,
			String desiredValue, String incidentTime, Individual provenance) {
		if (sourceIP == null && sourceDomain == null)
			return null;
		Statement s = null;
		Individual incident1 = IDS.Incident.createIndividual(IDS.Incident
				.getURI() + UUID.randomUUID());

		if (sourceIP != null) {
			s = model.createStatement(incident1, IDS.hasSourceIP, sourceIP);
			reifyStatement(model, s, provenance);
		}
		s = model.createStatement(incident1, IDS.hasSourceDeviceId,
				sourceDeviceId);
		reifyStatement(model, s, provenance);
		if (failedField != null) {
			s = model.createStatement(incident1, IDS.hasFailedField,
					failedField);
			reifyStatement(model, s, provenance);
		}
		if (actualValue != null) {
			s = model.createStatement(incident1, IDS.hasActualValue,
					actualValue);
			reifyStatement(model, s, provenance);
		}
		if (desiredValue != null) {
			s = model.createStatement(incident1, IDS.hasDesiredValue,
					desiredValue);
			reifyStatement(model, s, provenance);
		}
		if (incidentTime != null) {
			s = model
					.createStatement(incident1, IDS.incidentTime, incidentTime);
			reifyStatement(model, s, provenance);
		}
		
		return incident1;
	}

	public static Individual createAttack(Model model, String sourceIP, String sourceDeviceId,
			String startTime, String endTime, String status,
			Individual provenance) {
		Statement s = null;
		Individual attack1 = IDS.Attack.createIndividual(IDS.Attack.getURI()
				+ UUID.randomUUID());

		s = model.createStatement(attack1, IDS.hasStatus, status);
		reifyStatement(model, s, provenance);

		s = model.createStatement(attack1, IDS.attackStartTime, startTime);
		reifyStatement(model, s, provenance);

		s = model.createStatement(attack1, IDS.attackEndTime, endTime);
		reifyStatement(model, s, provenance);

		s = model.createStatement(attack1, IDS.isAttackedByID, sourceDeviceId);
		reifyStatement(model, s, provenance);

		return attack1;
	}

	public static void doSameAs(Model model, Individual one, Individual two,
			Individual provenance) {
		Statement s = model.createStatement(one, OWL.sameAs, two);
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
