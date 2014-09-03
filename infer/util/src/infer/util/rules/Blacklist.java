/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infer.util.rules;

import infer.util.SolrUtil;
import infer.util.Util;
import infer.util.generated.IDS;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.rulesys.BindingEnvironment;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinException;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;

/**
 * Can be used in two arg form (X, P) or three arg form (X, P, V). In three arg
 * form it succeeds if the triple (X, P, V) is not currently present, in two arg
 * form it succeeds if there is not value for (X, P, *).
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009-06-29 08:55:36 $
 */
public class Blacklist extends BaseBuiltin {
	final static Logger logger = Logger.getLogger(Blacklist.class.getName());

	/**
	 * Return a name for this builtin, normally this will be the name of the
	 * functor that will be used to invoke it.
	 */
	@Override
	public String getName() {
		return "Blacklist";
	}

	@Override
	public void headAction(Node[] args, int length, RuleContext context) {

		checkArgs(length, context);
		Node n0 = getArg(0, args, context);
		Node n1 = getArg(1, args, context);
		Node n2 = getArg(2, args, context);
		logger.fine("BLACKLIST in head action " + n0 + " " + n1 + " " + n2);

		SolrDocumentList docs = SolrUtil.getDocsbySPO(null,
				IDS.hasSourceDeviceId.toString(), n2.toString(), 4, true);
		logger.fine(" docs are " + docs);
		int countIncidents = 0;
		for (int k = 0; k < docs.size(); k++) {
			SolrDocument doc = docs.get(k);
			logger.fine(" doc received is " + doc);
			String subject = (String) doc.getFieldValue("subject_t");
			SolrDocumentList docs2 = SolrUtil.getDocsbySPO(subject,
					"*incidentTime*", null, 1);
			logger.fine(" docs2 are " + docs2);
			Calendar timeStamp = Util.toCalendar((String) docs2.get(0)
					.getFieldValue("object_t"));
			logger.info("TIMESTAMP: " + timeStamp.getTime());

			Calendar oldNow = Util.getNowPlus(-86400000);
			logger.fine("24 hours ago: " + Util.calendarToISO8601String(oldNow));

			if (timeStamp.compareTo(oldNow) > 0) {
				countIncidents = countIncidents + 1;
				logger.fine("CountIncidents: " + countIncidents);
			}

		}
		SolrDocumentList docs2 = SolrUtil.getDocsbySPO(null,
				IDS.isAttackedByID.toString(), n2.toString(),
				SolrUtil.MAX_DOCS, true);

		if (docs2.size() <= 0) {
			if (countIncidents == 4) {

				Node sub = NodeFactory.createURI(IDS.Attack.getURI()
						+ UUID.randomUUID());
				Node pred1 = NodeFactory.createURI(IDS.hasStatus.getURI());
				Node obj1 = NodeFactory
						.createLiteral("Multiple ID Attempt Attack");
				context.add(new Triple(sub, pred1, obj1));
				logger.fine("added n-triple: " + sub + "," + pred1 + "," + obj1);

				Node pred2 = NodeFactory.createURI(IDS.isAttackedByID.getURI());
				context.add(new Triple(sub, pred2, n2));
				logger.fine("added n-triple: " + sub + "," + pred2 + "," + n2);

				SolrDocument doc = docs.get(0);
				Date timeStamp = (Date) doc.getFieldValue("timestamp");
				Calendar cal = Calendar.getInstance();
				cal.setTime(timeStamp);
				String aTimeStart = Util.calendarToISO8601String(cal);
				Node obj2 = NodeFactory.createLiteral(aTimeStart);

				Node pred3 = NodeFactory
						.createURI(IDS.attackStartTime.getURI());
				context.add(new Triple(sub, pred3, obj2));
				logger.fine("added n-triple: " + sub + "," + pred3 + "," + obj2);

				SolrDocument doc2 = docs.get(docs.size() - 1);
				Date timeStamp2 = (Date) doc.getFieldValue("timestamp");
				Calendar cal2 = Calendar.getInstance();
				cal2.setTime(timeStamp2);
				String aTimeStart2 = Util.calendarToISO8601String(cal2);
				Node obj3 = NodeFactory.createLiteral(aTimeStart2);

				Node pred4 = NodeFactory.createURI(IDS.attackEndTime.getURI());
				context.add(new Triple(sub, pred4, obj3));
				logger.fine("added n-triple: " + sub + "," + pred4 + "," + obj3);
			}
		}

	}

	/**
	 * This method is invoked when the builtin is called in a rule body.
	 * 
	 * @param args
	 *            the array of argument values for the builtin, this is an array
	 *            of Nodes, some of which may be Node_RuleVariables.
	 * @param length
	 *            the length of the argument list, may be less than the length
	 *            of the args array for some rule engines
	 * @param context
	 *            an execution context giving access to other relevant data
	 * @return return true if the buildin predicate is deemed to have succeeded
	 *         in the current environment
	 */
	@Override
	public boolean bodyCall(Node[] args, int length, RuleContext context) {
		if (length != 3) {
			throw new BuiltinException(this, context, "builtin " + getName()
					+ " requires 3 arguments but saw " + length);
		}

		BindingEnvironment env = context.getEnv();

		Node subj = getArg(0, args, context);
		Node pred = getArg(1, args, context);
		Node obj = getArg(2, args, context);

		logger.fine("BLACKLIST Body Call subject is " + subj + " pred is "
				+ pred + " obj is " + obj);
		checkArgs(length, context);
		Node n0 = getArg(0, args, context);
		Node n1 = getArg(1, args, context);
		Node n2 = getArg(2, args, context);
		logger.fine("BLACKLIST in Body Call " + n0 + " " + n1.getLiteralValue()
				+ " " + n2.getLiteralValue());

		int numberToGet = (Integer) n1.getLiteralValue();
		int hoursSince = (Integer) n2.getLiteralValue();

		logger.fine(" BLACKLIST numberToGet = " + numberToGet
				+ " hours since = " + hoursSince);

		//String deviceId = (String) n0.getLiteralValue();
		String SourceIP = (String) n0.getLiteralValue();
		
		
		//logger.fine(" deviceId = " + deviceId);
		logger.fine(" SourceIP = " + SourceIP);
		
		SolrDocumentList docs = SolrUtil.getDocsbySPO(null,
				IDS.hasSourceIP.toString(),SourceIP, numberToGet,
				true);
		logger.fine(" docs are " + docs);
		if (docs.size() >= numberToGet) {
			int countIncidents = 0;
			Calendar oldNow = Util.getNowPlus(-1 * ((hoursSince * 60)*60*1000));
			logger.fine("24 hours ago: " + Util.calendarToISO8601String(oldNow));

			for (int k = 0; k < docs.size(); k++) {
				SolrDocument doc = docs.get(k);
				logger.fine(" doc received is " + doc);
				String subject = (String) doc.getFieldValue("subject_t");
				SolrDocumentList docs2 = SolrUtil.getDocsbySPO(subject,
						"*incidentTime*", null, 1);
				logger.fine(" docs2 are " + docs2);
				Calendar timeStamp = Util.toCalendar((String) docs2.get(0)
						.getFieldValue("object_t"));
				logger.info("TIMESTAMP: " + timeStamp.getTime());

				if (timeStamp.compareTo(oldNow) > 0) {
					countIncidents = countIncidents + 1;
					logger.fine("CountIncidents: " + countIncidents);
				}

			}
			logger.fine(" countIncidents = " + countIncidents + " number to get = " + numberToGet);
			if (countIncidents >= numberToGet) {
				logger.fine(" returning TRUE");
				return true;
			}
		} else {

			return false;
		}
		return false;
	}

	/**
	 * Flag as non-monotonic so the guard clause will get rerun after deferal as
	 * part of a non-trivial conflict set.
	 */
	@Override
	public boolean isMonotonic() {
		return false;
	}

}
