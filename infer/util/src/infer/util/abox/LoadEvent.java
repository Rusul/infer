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
import java.io.PrintStream;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

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

import infer.util.JenaUtil;
import infer.util.LoadAboxWebService;
import infer.util.SolrUtil;
import infer.util.Util;
import infer.util.generated.AutoIE;
import infer.util.generated.LinkedEvents;
import infer.util.generated.Provenance;
import infer.util.generated.Vcard;
import infer.util.rules.AllValue;
import infer.util.rules.CalLocDest;
import infer.util.rules.CalTextDest;
import infer.util.rules.Like;
import infer.util.rules.Poi;
import infer.util.rules.PointToSegment;

/**
 * LoadAwardsData
 * 
 * Based on NITRD provided NSF data in awards.xml
 */
public class LoadEvent {

	final static Logger logger = Logger.getLogger(LoadEvent.class.getName());

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
		insert(model);

//		 Output model
		logger.fine("completed processing, outputting data");
		File outFile = new File(destDir + fileName + ".nt");
		PrintStream ps = new PrintStream(outFile);
		logger.info("completed processing, writing out N-TRIPLE");
		model.write(ps, "N-TRIPLE");
		logger.info("process completed successfully");
	}
	/**
	 * Process from the servlet. 
	 * 
	 * Strips data from the servlet and passes on for processing into a jena
	 * model.
	 * 
	 * @param uri parameters
	 *            - the input parameters.
	 * @param destDir
	 *            - the destination directory to place the Abox results.
	 */
	public static void doFromServlet(String name, String email, 
			String eventDateStart, String eventDateEnd, String location, 
			String description)
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
		

//		 Output model
		String uri = Util.getPerson(name, email);
		Individual person = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ uri);
		//TODO: Ensure that multiple names aren't associated with one email account.
		
		//Create Person
		SolrDocumentList docs = SolrUtil.getDocsbySPO(null, Vcard.email.toString(), email, 1);
		if(docs.size()==0){
			
			
			Statement s = null;
			
			s = model.createStatement(person, Vcard.fn, name);
			ReifiedStatement rs = model.createReifiedStatement(s);
			rs.addProperty(Provenance.isSourcedBy, provenance);
			model.add(s);
			
			s = model.createStatement(person, Vcard.email, email);
			ReifiedStatement rs2 = model.createReifiedStatement(s);
			rs2.addProperty(Provenance.isSourcedBy, provenance);
			model.add(s);
	
			//Give Provenance
			Individual provenanceScott = Provenance.Provenance
					.createIndividual(Provenance.getURI()
							+ System.currentTimeMillis());
			model.add(provenanceScott, Provenance.who, "GoogleCalendar");
			model.add(provenanceScott, Provenance.when,
					DatatypeConverter.printDateTime(new GregorianCalendar()));
			model.add(provenanceScott, Provenance.sourceDocument,
					"https://www.google.com/calendar");
			model.add(provenanceScott, Provenance.probability, "1");
		}
	
		//Create Event
		//TODO: Prevent duplicate events.

		
		boolean duplicateEvent=false;
		Individual event = LinkedEvents.Event.createIndividual(LinkedEvents.Event.getURI()+"#"
		+uri+System.currentTimeMillis());
		
		SolrDocumentList eventStartDocs = SolrUtil.getDocsbySPO(null, AutoIE.eventStartTime.toString(), 
				"\""+eventDateStart+"\"",SolrUtil.MAX_DOCS);
		logger.fine("eventDateStart: "+eventStartDocs);
		for(int i=0;i<eventStartDocs.size();i++){
			SolrDocument eventDoc = eventStartDocs.get(i);
			String eventString = (String) eventDoc.getFieldValue("subject_t");
			logger.fine("Event " + eventString);
			
			SolrDocumentList eventComDocs = SolrUtil.getDocsbySPO(eventString, 
					AutoIE.eventCommittedByPerson.toString(), person.toString(),1);
			if(eventComDocs.size()>0){
				logger.fine("FOUND");
				SolrDocumentList eventLocDocs = SolrUtil.getDocsbySPO(eventString, 
						AutoIE.eventHasAsALocation.toString(), location,1);
				if(eventLocDocs.size()>0){
					duplicateEvent=true;
				}
			}
		}
		
		if(!duplicateEvent){
			model.add(event, AutoIE.eventCommittedByPerson, person);
			model.add(person, AutoIE.personCommitsToEvent, event);
			
			model.add(event, AutoIE.eventStartTime, eventDateStart);
			model.add(event, AutoIE.eventEndTime, eventDateEnd);
	
			model.add(event, AutoIE.eventHasAsALocation, location);
			model.add(event, AutoIE.eventDescription, description);
		}
		StmtIterator si = model.getDeductionsModel().listStatements();

		while (si.hasNext()) {
			Statement t = si.next();
			logger.info(" got deduction statement of " + t);
		}

//		 Output model
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		logger.info("completed processing, writing out N-TRIPLE");
		model.write(os, "N-TRIPLE");
		ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
		LoadAboxWebService.doStream(is, SolrUtil.getProperties());
		logger.info("process completed successfully");
	}
	/**
	 * Inserts provided data into the Jena model
	 * 
	 * @param model
	 *            - the input model.
	 * 
	 */
	public static void insert(Model model) {
		String person = Util.getPerson("John Williams", "john.williams.inferces@gmail.com");
		

		//  Susan Siliver has a flight at 4pm heading to Boston. She has a business meeting at 1pm. 
		//  She's at the office at 11:00am, Lunch at noon. Flight at 4pm. 
		//  She works at the Delphi Auburn Hills office. Lunch appoint at Spargos Coney Island. 
		//  Business meeting at Chrysler, Auburn Hills. Flight from Detroit Metro at 4pm.	
		
		Individual john = Vcard.VCard.createIndividual(Vcard.VCard.getURI()
				+ person);
		
		String eventDateStart = Util.nowPlus(1000*60*10);
		String eventDateEnd = Util.nowPlus(1000*60*70);
		Individual doctorAppointment = LinkedEvents.Event.createIndividual(LinkedEvents.Event.getURI()+"#"+person+System.currentTimeMillis());
		model.add(doctorAppointment, AutoIE.eventCommittedByPerson, john);
		model.add(john, AutoIE.personCommitsToEvent, doctorAppointment);
		
		model.add(doctorAppointment, AutoIE.eventStartTime, eventDateStart);
		model.add(doctorAppointment, AutoIE.eventEndTime, eventDateEnd);

		model.add(doctorAppointment, AutoIE.eventHasAsALocation, "3095 E Walton Blvd, Auburn Hills, MI, 48326");
		model.add(doctorAppointment, AutoIE.eventDescription, "Dr. Appointment");
		
		Individual trackPoint = AutoIE.TrackPoint
				.createIndividual(AutoIE.TrackPoint.getURI()
						+ System.currentTimeMillis());
		model.add(doctorAppointment, AutoIE.eventHasAsADestination, trackPoint);
		model.add(trackPoint, AutoIE.DestinationHasAsAnEvent, doctorAppointment);
		

		model.add(trackPoint, Vcard.latitude, "42.678858");

		model.add(trackPoint, Vcard.longitude, "-83.22841");
		model.add(trackPoint, AutoIE.trackPointWhen, eventDateStart);
		model.add(trackPoint, AutoIE.pointStatus, "point");

		
		

	}



	
}
