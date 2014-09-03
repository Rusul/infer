package infer.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.collections.list.SetUniqueList;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import infer.util.generated.AutoIE;
import infer.util.generated.Provenance;
import infer.util.rules.AllValue;
import infer.util.rules.Blacklist;
import infer.util.rules.CalLocDest;
import infer.util.rules.CalTextDest;
import infer.util.rules.Like;
import infer.util.rules.Poi;
import infer.util.rules.PointToSegment;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RSIterator;
import com.hp.hpl.jena.rdf.model.ReifiedStatement;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinRegistry;
import com.hp.hpl.jena.reasoner.rulesys.ClauseEntry;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.reasoner.rulesys.builtins.NoValue;

/**
 * 
 * @author Scott Streit
 * 
 *         This class contains the static methods necessary to Solr. The calls
 *         to Solr are through web service calls (post) and do not require any
 *         jar files from Solr. The assumptions are that the aSor schema.xml
 *         file has dynamic fields defined as an _type suffix. For example a
 *         text field named banana in the relational database becomes banana_t
 *         in solr. There is a dependency on a file named load.properties off
 *         the classpath.
 * 
 */
public class JenaUtil {
	private static String identifierField = SolrUtil.identifierField;
	private static String hierarchySeparator = SolrUtil.hierarchySeparator;
	private static String hierarchyField = SolrUtil.hierarchyField;
	private static boolean commitEveryTriple = SolrUtil.commitEveryTriple;
	public static String name = "default";

	final static Logger logger = Logger.getLogger(JenaUtil.class.getName());

	
	public static void loadRules() {
		BuiltinRegistry.theRegistry.register(new Blacklist());
		BuiltinRegistry.theRegistry.register( new AllValue() );
		BuiltinRegistry.theRegistry.register( new Like() );
		BuiltinRegistry.theRegistry.register( new Poi() );
		BuiltinRegistry.theRegistry.register(new CalLocDest());
		BuiltinRegistry.theRegistry.register(new CalTextDest());
		BuiltinRegistry.theRegistry.register(new NoValue());
		BuiltinRegistry.theRegistry.register(new PointToSegment());
	}

	public static void loadIntoSolr(Model defModel) throws IOException {
		InputStream is;
		Properties properties = SolrUtil.getProperties();
		String val = properties.getProperty("RULE_URL");
		logger.fine(" rules are " + val);
		
		URL rulesURL = new URL(val);
		List<Rule> rules = Rule.rulesFromURL(val);
		GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);
		Model model2 = ModelFactory.createDefaultModel();
		
		//Pulling Object Property Sources from @prefixes in rules.txt
		Scanner s = new Scanner(rulesURL.openStream());
		while(s.hasNext()){
		   String prefixLine=s.nextLine();
		   	if(prefixLine.indexOf("@prefix")>=0){
		   	prefixLine=prefixLine.substring(prefixLine.indexOf(':')+1).replaceAll("\\s","").replace("#","");
		   		if(prefixLine.equals("http://www.w3.org/2006/vcard/ns")){
		   			prefixLine=prefixLine+".rdf";
		   		}
		   	
			    logger.info("PREFIX:" + prefixLine);
			    URL url2 = new URL(prefixLine);
			    is = url2.openStream();
				model2.read(is, null);
				is.close();
			}
		   
		 }
		s.close();

		InfModel sourceModel = ModelFactory.createInfModel(reasoner, defModel);
				
		logger.fine("Pre Add Abox Source Model Size: " + sourceModel.listStatements().toList().size());
		addAboxBasedOnRulesAndTBox(sourceModel,model2);
		logger.fine("Post Add Abox Source Model Size: " + sourceModel.listStatements().toList().size());
		indexRDF(sourceModel,name);
	}


	public static String modelToString(Model model) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		model.write(baos, "N-TRIPLE");
		return baos.toString();
	}

	public static void loadIntoSolr(String abox) throws IOException {
		InputStream is;
		Properties properties = SolrUtil.getProperties();
		String val = properties.getProperty("RULE_URL");
		logger.fine(" rules are " + val);

		
		
		
		URL rulesURL = new URL(val);
		List<Rule> rules = Rule.rulesFromURL(val);
		GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);
		Model model2 = ModelFactory.createDefaultModel();
		
		//Pulling Object Property Sources from @prefixes in rules.txt
		Scanner s = new Scanner(rulesURL.openStream());
		while(s.hasNext()){
		   String prefixLine=s.nextLine();
		   	if(prefixLine.indexOf("@prefix")>=0){
		   	prefixLine=prefixLine.substring(prefixLine.indexOf(':')+1).replaceAll("\\s","").replace("#","");
		   		if(prefixLine.equals("http://www.w3.org/2006/vcard/ns")){
		   			prefixLine=prefixLine+".rdf";
		   		}
		   	
			    logger.info("PREFIX:" + prefixLine);
			    URL url2 = new URL(prefixLine);
			    is = url2.openStream();
				model2.read(is, null);
				is.close();
			}
		   
		 }
		s.close();

		Model defModel = ModelFactory.createDefaultModel();
		InfModel sourceModel = ModelFactory.createInfModel(reasoner, defModel);
				
		is = new ByteArrayInputStream(abox.getBytes());
		sourceModel.read(is, null, "N-TRIPLE");
		logger.fine("Pre Add Abox Source Model Size: " + sourceModel.listStatements().toList().size());
		addAboxBasedOnRulesAndTBox(sourceModel,model2);
		logger.fine("Post Add Abox Source Model Size: " + sourceModel.listStatements().toList().size());
		indexRDF(sourceModel,name);
		is.close();
	}
	public static void reifyStatement(Model model, Statement s,
			Individual provenance) {
		ReifiedStatement rs = model.createReifiedStatement(s);
		rs.addProperty(Provenance.isSourcedBy, provenance);
		model.add(s);
	}

	/**
	 * This method takes Jena model from memory create lucene documents. Each
	 * RDF triple in the model consist of subject, predicate, and object. It
	 * iterates over the Jena model and creates solr document for each
	 * statement. Each solr document consist of three fields defined as subject,
	 * predicate, object.
	 * 
	 * Before adding to a document, each keyword is converted to lucene format
	 * (fieldname: valuename) For example, object=2009 will be converted to
	 * (object:2009)
	 * 
	 * @param model
	 *            this is in memory Jena model that contains RDF data to be
	 *            indexed.
	 * @param name
	 *            , the name for the graph (named graph).
	 * 
	 */
	public static void indexRDF(Model model, String name) {

		logger.fine("in indexRdf with  graph name = " + name);

		

		try {
			
			SolrServer server = SolrUtil.getSolrServer();

			logger.fine("Indexing...");

			Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
			SolrInputDocument documentSearch = new SolrInputDocument();
			StmtIterator iter = model.listStatements();
		
			while (iter.hasNext()) {

				Statement stmt = iter.nextStatement(); // get next statement
				logger.fine(" got a statement of " + stmt);
			
				
				SolrInputDocument document = new SolrInputDocument();
				String subject = stmt.getSubject().toString();
				String object = stmt.getObject().toString();
				String timestamp = null;
								
				document.addField("subject_t", stmt.getSubject().toString());
				
				logger.fine(" identiferField = "+ identifierField);
				logger.fine(" Predicate is " + stmt.getPredicate().toString());
				
				document.addField("predicate_t", stmt.getPredicate().toString());
				if (identifierField.contains(stmt.getPredicate().toString())) {

					String value = stmt.getSubject().toString();
					if (documentSearch.getField("id") != null) {
						docs.add(documentSearch);
						documentSearch = new SolrInputDocument();
					}
					documentSearch.addField("id", value);
				}
				if (stmt.getPredicate().toString().equals(hierarchyField)) {
					String value = "";
					logger.finer(" have hierarchy field");
					String[] pieces = stmt.getObject().toString().trim()
							.split(hierarchySeparator, -1);
					for (int i = 0; i < pieces.length; i++) {
						// remove non printable characters
						String clearString = pieces[i].trim().replaceAll(
								"\\p{Cntrl}", "");
						logger.finer(" piece is " + clearString);
						if (value.length() > 0) {
							value += "/";
						}
						value += clearString;
						documentSearch.addField("cat", clearString);
					}
					String key = "category_path_ht";
					logger.finer(" category_path_ht is " + value);
					// String value =
					// addIndexfinermationToHierarchy(stmt.getObject().toString());

					documentSearch.addField(key, value);
				}
				document.addField("object_t", stmt.getObject().toString()
						.trim());
				document.addField("modelName_t", name);
				document.addField("id", stmt.toString());
				String reifiedId = null;
				if (stmt.isReified()) {
					RSIterator rsi = model.listReifiedStatements(stmt);
					while (rsi.hasNext()) {
						ReifiedStatement rs = rsi.next();
						reifiedId = ClientUtils.escapeQueryChars(rs.getId()
								+ "");
						logger.finer(" reifiedId is " + reifiedId);
						SolrInputDocument rDocument = new SolrInputDocument();
						rDocument.addField("r_subject_t",
								ClientUtils.escapeQueryChars(rs.getId() + ""));
						StmtIterator si = rs.listProperties();
						while (si.hasNext()) {
							Statement s = si.next();
							if (s.getPredicate().toString()
									.contains("Provenance")) {
								rDocument.addField("r_predicate_t", s
										.getPredicate().toString());
								rDocument.addField("r_object_t", s.getObject()
										.toString());
								rDocument.addField("r_modelName_t", name);
								rDocument.addField("id", rs.toString());
								docs.add(rDocument);
							}
						}

					}
				}
				document.addField("reifiedId_t", reifiedId);
				logger.fine(" writing triple document out " + document);
				docs.add(document);
				
				if (commitEveryTriple) {
					server.add(docs);
					server.commit();
				}


				Set<Resource> s = new HashSet<Resource>();
				
				if (stmt.getObject().isLiteral()) {
					logger.finer(" object is a literal ");
					processObjectLiteral(documentSearch, stmt, stmt);
				} else if (stmt.getObject().isAnon()) {
					logger.fine(" object is a anon ");

					// Case of indexing blank nodes
					boolean done = false;
					Resource bNode = stmt.getResource();
					if (!bNode.toString().contains(
							"http://www.w3.org/2000/01/rdf-schema#Class")) {

						while (!done) {
							StmtIterator si = model.listStatements(bNode,
									(Property) null, (RDFNode) null);
							if (si.hasNext()) {
								Statement blank = si.nextStatement(); // get
																		// next
																		// statement
								if (blank.getObject().isLiteral()) {

									processObjectLiteral(documentSearch, blank,
											stmt);
									done = true;
								} else if (stmt.getObject().isAnon()) {
									if (s.contains(bNode)) {
										done = true;
									} else {
										s.add(bNode);

										bNode = blank.getResource();
										if (bNode
												.toString()
												.contains(
														"http://www.w3.org/2000/01/rdf-schema#Class")
												|| bNode.toString().contains(
														"rdf-syntax-ns#")) {
											done = true;
										}
									}
								}
							} else {
								done = true;
							}
						}
					}
				}
			}
			if (documentSearch.containsKey("id")) {
				docs.add(documentSearch);
			} else {
				logger.fine(" could not add search document because no id field.  Document that would have been added is "
						+ documentSearch);
			}
			long startTime = System.currentTimeMillis();
			server.add(docs);
			long endTime = System.currentTimeMillis();

			logger.fine("after adding document and before commit milliseconds are "
					+ (endTime - startTime));

			startTime = System.currentTimeMillis();
			server.commit();
			// server.commit(false, false, true);

			endTime = System.currentTimeMillis();

			logger.fine(" after commit milliseconds are "
					+ (endTime - startTime));

		} catch (IOException ex) {
			logger.finer("logging" + ex.getMessage());
			throw new RuntimeException(ex);

		} catch (SolrServerException ex) {
			logger.finer("logging" + ex.getMessage());
			throw new RuntimeException(ex);
		}

	}

	public static void addAboxBasedOnRulesAndTBox(InfModel infModel,Model m) {
	
		GenericRuleReasoner reasoner = (GenericRuleReasoner) infModel
				.getReasoner();
		Map<String, RulePOJO> ruleMap = new HashMap<String, RulePOJO>();
			List<Rule> rules = reasoner.getRules();
			for (int i = 0; i < rules.size(); i++) {
				Rule rule = rules.get(i);
				List<TriplePattern> triples = SetUniqueList
						.decorate(new ArrayList<TriplePattern>());

				ClauseEntry[] body = rule.getBody();
				addClauses(body, triples);
				for (int j = 0; j < triples.size(); j++) {
					TriplePattern tp = triples.get(j);
					RulePOJO rulePOJO = new RulePOJO();
					rulePOJO.setTp(tp);
					String predicate = tp.getPredicate().toString();
				/*	RulePOJO rulePOJOPredicate = ruleMap.get(predicate);

					if (rulePOJOPredicate != null) {
						rulePOJO.getParents().addAll(
								rulePOJOPredicate.getParents());
						rulePOJO.getChildren().addAll(
								rulePOJOPredicate.getChildren());
					}*/
					ruleMap.put(predicate, rulePOJO);
				}
			}
			logger.fine(" rules data structure is " + ruleMap);


			RulePOJO arr[] = ruleMap.values().toArray(new RulePOJO[0]);
			Map<String, DomainPredicateRange> dprMap = new HashMap<String, DomainPredicateRange>();
			for (int i = 0; i < arr.length; i++) {
				RulePOJO rulePOJO = arr[i];
				DomainPredicateRange dpr = null;
				dpr = convertRulePOJOToDomainPredicateRange(m, rulePOJO, dprMap);
			}
			
			logger.fine(" DomainPredicateRangeMap is " + dprMap);

			Model addedModel = ModelFactory.createDefaultModel();
			DomainPredicateRange[] dprArr = dprMap.values().toArray(
					new DomainPredicateRange[0]);
			for (int i = 0; i < dprArr.length; i++) {
				DomainPredicateRange dpr = dprArr[i];
				addRelatedStatements(infModel, dpr, addedModel);

			}
			logger.fine("Added Model Size: " + addedModel.listStatements().toList().size());

			// logModel(addedModel);

			infModel.add(addedModel);
			// if (g != null) {
			// model.remove(m);
			// };
			// logModel(model);

		}


	/**
	 * A POJO containing a set of parent and child rules.
	 * 
	 * @author scott
	 * 
	 */

	
	public static void addClauses(Object[] body, List<TriplePattern> triples) {
		for (int j = 0; j < body.length; j++) {
			if (body[j] instanceof TriplePattern) {
				TriplePattern tp = (TriplePattern) body[j];
				triples.add(tp);
			}
		}
	}
	
	static class RulePOJO {
		final static Logger log = Logger.getLogger(RulePOJO.class.getName());

		List<RulePOJO> parents = SetUniqueList
				.decorate(new ArrayList<RulePOJO>());
		List<RulePOJO> children = SetUniqueList
				.decorate(new ArrayList<RulePOJO>());
		TriplePattern tp = null;

		public TriplePattern getTp() {
			return tp;
		}

		public void setTp(TriplePattern tp) {
			this.tp = tp;
		}

		public List<RulePOJO> getParents() {
			return parents;
		}

		public void setParents(List<RulePOJO> parents) {
			this.parents = parents;
		}

		public List<RulePOJO> getChildren() {
			return children;
		}

		public void setChildren(List<RulePOJO> children) {
			this.children = children;
		}

		/**
		 * Determine whether fields are equal. Solely considers field.
		 */
		@Override
		public boolean equals(Object obj) {
	
			RulePOJO c = (RulePOJO) obj;
			if (c == null || c.getTp() == null) {
				return false;
			}

			return (this.getTp().equals(c.getTp()));
		}

		public String toString() {
			StringBuffer st = new StringBuffer();
			st.append(" tp is " + tp);
			for (int i = 0; i < this.getParents().size(); i++) {
				st.append("*** parent [" + i + "]"
						+ this.getParents().get(i).getTp() + "***");
			}
			for (int i = 0; i < this.getChildren().size(); i++) {
				st.append("--- child [" + i + "]"
						+ this.getChildren().get(i).getTp() + "---");
			}
			return st.toString();
		}

	}

	static class DomainPredicateRange {
		final static Logger log = Logger.getLogger(RulePOJO.class.getName());

		List<DomainPredicateRange> parents = SetUniqueList
				.decorate(new ArrayList<DomainPredicateRange>());
		List<DomainPredicateRange> children = SetUniqueList
				.decorate(new ArrayList<DomainPredicateRange>());

		String subject;
		String predicate;
		String object;

		public String getSubject() {
			return subject;
		}

		public void setSubject(String subject) {
			this.subject = subject;
		}

		public String getPredicate() {
			return predicate;
		}

		public void setPredicate(String predicate) {
			this.predicate = predicate;
		}

		public String getObject() {
			return object;
		}

		public void setObject(String object) {
			this.object = object;
		}

		public List<DomainPredicateRange> getParents() {
			return parents;
		}

		public void setParents(List<DomainPredicateRange> parents) {
			this.parents = parents;
		}

		public List<DomainPredicateRange> getChildren() {
			return children;
		}

		public void setChildren(List<DomainPredicateRange> children) {
			this.children = children;
		}

		@Override
		public int hashCode() {
			int ctr = 0;
			if (subject != null) {
				ctr += subject.hashCode();
			}
			if (predicate != null) {
				ctr += predicate.hashCode();
			}
			if (object != null) {
				ctr += object.hashCode();
			}
			return ctr;
		}

		/**
		 * Determine whether fields are equal. Solely considers field.
		 */
		@Override
		public boolean equals(Object obj) {
			log.fine(" in equals with obj = " + obj + "this  =" + this);

			DomainPredicateRange c = (DomainPredicateRange) obj;
			if (c == null || c.getSubject() == null || c.getObject() == null
					|| c.getPredicate() == null) {
				return false;
			}
			boolean sub = this.getSubject().equals(c.getSubject());
			boolean prop = this.getPredicate().equals(c.getPredicate());
			boolean obje = this.getObject().equals(c.getObject());

			return sub && prop && obje;

		}

		public String toString() {
			StringBuffer st = new StringBuffer();
			st.append("\nSTART \n  Subject is " + getSubject()
					+ "\n Predicate is " + getPredicate() + "\n Object is "
					+ getObject() + "\n");
			for (int i = 0; i < this.getParents().size(); i++) {

				st.append("\n*** parent [" + i + "] ");
				st.append(" Subject is " + getParents().get(i).getSubject()
						+ "\n Predicate is "
						+ getParents().get(i).getPredicate() + "\n Object is "
						+ getParents().get(i).getObject());
				st.append("*** \n");
			}
			for (int i = 0; i < this.getChildren().size(); i++) {
				st.append("\n--- child [" + i + "]");
				st.append(" Subject is " + getChildren().get(i).getSubject()
						+ "\n Predicate is "
						+ getChildren().get(i).getPredicate() + "\n Object is "
						+ getChildren().get(i).getObject());
				st.append("---\n");
			}
			st.append(" END\n\n");
			return st.toString();
		}

		/**
		 * Return a String representation of the Object
		 * 
		 * @return the representing String.
		 */

	}

	public static DomainPredicateRange convertRulePOJOToDomainPredicateRange(
			Model m, RulePOJO rulePOJO, Map<String, DomainPredicateRange> dprMap) {
		TriplePattern tp = rulePOJO.getTp();
		StmtIterator domain = m
				.listStatements(
						m.createResource(tp.getPredicate().toString()),
						m.createProperty("http://www.w3.org/2000/01/rdf-schema#domain"),
						(RDFNode) null);
		
		StmtIterator range = m.listStatements(
				m.createResource(tp.getPredicate().toString()),
				m.createProperty("http://www.w3.org/2000/01/rdf-schema#range"),
				(RDFNode) null);
		
		DomainPredicateRange dpr = new DomainPredicateRange();
		if (domain.hasNext()) {
			Statement s = domain.next();
			dpr.setSubject(s.getObject().toString());
			dpr.setPredicate(tp.getPredicate().toString());
		}
		if (range.hasNext()) {
			Statement s = range.next();
			dpr.setObject(s.getObject().toString());
		}
		// If it already exists then merge.
		if (dpr.getPredicate() != null) {
			DomainPredicateRange dpr2 = dprMap.get(dpr.getPredicate()
					.toString());
			if (dpr2 != null) {
				dpr.getParents().addAll(dpr2.getParents());
				dpr.getChildren().addAll(dpr2.getChildren());
			}

			// dprMap.put(dpr.getSubject().toString(), dpr);
			dprMap.put(dpr.getPredicate().toString(), dpr);
		}
		// dprMap.put(dpr.getObject().toString(), dpr);
		return dpr;
	}

	/**
	 * Get the solr server based on a URI
	 * 
	 * @return
	 * @throws MalformedURLException
	 */

	/**
	 * 
	 * @param model is a model that only contains tbox.
	 * @param dpr gives us the domains, properties and ranges we care about for inferencing.
	 * @param addedModel using disc, the tbox and dpr, add data.
	 */
	public static void addRelatedStatements(Model model,
			DomainPredicateRange dpr, Model addedModel) {
		//possibly check if SolrServer is empty first to save on first load
		StmtIterator si = model
				.listStatements(
						(Resource) null,
						model.createProperty(dpr.getPredicate().toString()),
						(RDFNode) null);
		Set<Statement> set = si.toSet();
		Iterator<Statement> sIterator = set.iterator();
		logger.fine("dpr predicate: "+dpr.getPredicate().toString());
		logger.fine("dpr subject: "+dpr.getSubject().toString());
		//1. query sourceModel for dpr.predicate
		//2. query solr with statement
		while (sIterator.hasNext()) {
			Statement s = sIterator.next();
			/*
			 *  Object of the statement itself is a subject.
			 */
			if (s.getSubject().toString().contains(dpr.getSubject())) {
				// We have a row with the subject on disk is the subject we
				// need.
			
						//GET PARENTS
						SolrDocumentList docs = SolrUtil.getDocsbySPO(null,
								null,
								s.getSubject().toString(),10000);
								for (int k=0;k<docs.size();k++) {
									SolrDocument doc = docs.get(k);
									String subject = (String) doc.getFieldValue("subject_t");
									String predicate = (String) doc.getFieldValue("predicate_t");
									String object = (String) doc.getFieldValue("object_t");
									Resource r = addedModel.createResource(subject);
									Property p = addedModel.createProperty(predicate);
									addedModel.add(addedModel.createStatement(r, p, object));
								}
					
					//GET CHILDREN
						SolrDocumentList docs2 = SolrUtil.getDocsbySPO(s.getObject().toString(),
							null,
							null,10000);
							for (int k=0;k<docs2.size();k++) {
								SolrDocument doc2 = docs2.get(k);
								String subject2 = (String) doc2.getFieldValue("subject_t");
								String predicate2 = (String) doc2.getFieldValue("predicate_t");
								String object2 = (String) doc2.getFieldValue("object_t");
								Resource r2 = addedModel.createResource(subject2);
								Property p2 = addedModel.createProperty(predicate2);
								addedModel.add(addedModel.createStatement(r2, p2, object2));
							}
					
				

			}
		}
	}
	
	public static void processObjectLiteral(SolrInputDocument documentSearch,
			Statement stmt, Statement root) {
		logger.fine(" subject is " + root.getSubject());
		logger.fine(" namespace is " + root.getSubject().getNameSpace());
		logger.fine(" uri is " + root.getSubject().getURI());
		logger.fine(" local name  is " + root.getSubject().getLocalName());
		String predicate = stmt.getPredicate().getLocalName();
		String datatype = null;
		if (stmt.getObject().asNode().getLiteralDatatype() != null) {
			datatype = stmt.getObject().asNode().getLiteralDatatype()
					.toString().toUpperCase();
		}
		if (predicate.endsWith("price")) {
			datatype = "FLOAT";
		}
		logger.fine("datatype is " + datatype);

		if (datatype != null && datatype.contains("FLOAT")) {
			if (documentSearch.getField(predicate + "_f") != null) {
				return;
			}

			documentSearch.addField(predicate + "_f", stmt.getObject()
					.asLiteral().getValue()
					+ "");
		} else if (datatype != null && datatype.contains("INT")) {
			if (documentSearch.getField(predicate + "_l") != null) {
				return;
			}
			documentSearch.addField(predicate + "_l", stmt.getObject()
					.asLiteral().getValue()
					+ "");
		} else {
			if (documentSearch.getField(predicate + "_t") != null) {
				return;
			}
			documentSearch.addField(predicate + "_t", stmt.getObject()
					.toString().trim());
		}
		//logger.fine(" writing search document out " + documentSearch);
	}
	



}