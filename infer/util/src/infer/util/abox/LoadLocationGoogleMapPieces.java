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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
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
public class LoadLocationGoogleMapPieces {

	final static Logger logger = Logger.getLogger(LoadLocationGoogleMapPieces.class.getName());
	final static int pointsPerFile = 1;

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
		logger.info(" fileName is " + fileName);


		Model model = ModelFactory.createDefaultModel();

		File file = new File(fileName);
		if (file.isHidden()) {
			return;
		}
		logger.info(" file is " + file);

		
		Individual susanSilver = LoadLocationGoogleMap.createBusinessCard(model, "Susan Silver",
				"(765) 398-1842", "susansilver2012@gmail.com", "VIPCommunity",
				"02/28/1970");
		
		Individual car = LoadLocationGoogleMap.createCar(model, "Susan Silver",
				"susansilver2012@gmail.com", "Chrysler", " Town & Country ", "2013",
				"2C4RC1BG3DR614258",
				DatatypeConverter.printDateTime(new GregorianCalendar()),
				"white", "Minivan", "IE", "driver", "TEST DATA");
		

		Individual route = AutoIE.Route.createIndividual(AutoIE.Route.getURI()+System.currentTimeMillis());
		
		model.add(route, AutoIE.routeStatus, "INPROGRESS");
		model.add(car, AutoIE.drives, route);
		
		model.add(route, AutoIE.isDrivenBy, car);
		
		model.add(susanSilver, AutoIE.driverDrivesARoute, route);
		model.add(route, AutoIE.routeHasAsADriver, susanSilver);

		DomHandler dom = new DomHandler(file);

		NodeList rows = dom.getElementsByTagName("trkpt");
		
		String previousLat = "0";
		String previousLon = "0";
		int pointCount = 0;
		int fileCount = 0;
		
		for (int i = 0; i < rows.getLength(); i++) {
			Node row = rows.item(i);
			String lat = row.getAttributes().getNamedItem("lat").getNodeValue();
			String lon = row.getAttributes().getNamedItem("lon").getNodeValue();

			logger.info(" lat = " + lat + " lon = " + lon);
			NodeList rowChildren = row.getChildNodes();
			String tim = null;
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

				Individual tp = LoadLocationGoogleMap.createTrackPoint(route, lat, lon, "point",
						Util.nowPlus(1000), model, previousLat, previousLon, LoadLocationGoogleMap.CLOSE_CHECK,x);
				if (tp != null) {
					previousLat = lat;
					previousLon = lon;
					pointCount++;
					if (pointCount >= pointsPerFile) {
						writeFile(destDir, model, fileCount++);
						pointCount = 0;
						model.removeAll();
					}
				}

			}
		}
		if (pointCount > 0) {
			writeFile(destDir, model, fileCount++);
			pointCount = 0;
			model.removeAll();	
		}
		
		
		logger.fine("completed processing, outputting data");
	}
	
	public static void writeFile(String destDir, Model model, int fileCount) throws FileNotFoundException {
		logger.info(" in writeFile with destDir = " + destDir);
		File outFile = new File(destDir + fileCount + ".nt");
		PrintStream ps = new PrintStream(outFile);
		logger.info("completed processing, writing out N-TRIPLE");
		model.write(ps, "N-TRIPLE");

	}


	private static void printStatements(StmtIterator si) {
		while (si.hasNext()) {
			Statement s = si.next();

			logger.info("Statement is " + s);
		}

	}



	


}
