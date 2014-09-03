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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.collections.list.SetUniqueList;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.simple.JSONObject;

import infer.util.generated.AutoIE;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.rulesys.BindingEnvironment;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinException;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;
import com.hp.hpl.jena.reasoner.rulesys.impl.BindingVector;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

import infer.util.SolrUtil;

/**
 * Can be used in two arg form (X, P) or three arg form (X, P, V). 
 * In three arg form it succeeds if the triple  (X, P, V) is not
 * currently present, in two arg form it succeeds if there is not value
 * for (X, P, *).
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009-06-29 08:55:36 $
 */
public class Like extends BaseBuiltin {
	final static Logger logger = Logger.getLogger(Like.class
			.getName());

    /**
     * Return a name for this builtin, normally this will be the name of the 
     * functor that will be used to invoke it.
     */
    @Override
    public String getName() {
        return "Like";
    }
    
    @Override
    public void headAction(Node[] args, int length, RuleContext context) {
    	String ruleString=context.getRule().toString();
    	String interestPred=ruleString.substring(ruleString.indexOf("(?o")+3,ruleString.indexOf("?y)"));
       
    	checkArgs(length, context);
        Node n0 = getArg(0, args, context);
        Node n1 = getArg(1, args, context);
        Node n2 = getArg(2, args, context);
        logger.fine("LIKE in head action " + n0 + " " + n1 + " " + n2);
        BindingVector bv = (BindingVector) context.getEnv();
        Node[] node = bv.getEnvironment();
        Node opinCatNode = node[2];
        
        
        ArrayList<List<String>> interestTermsList = new ArrayList <List<String>>();
        ArrayList<String> interestNames = new ArrayList <String>();
        
        SolrDocumentList docs3 = SolrUtil.getDocsbySPO(null,AutoIE.hasIndividualInterestOf.toString(), null,
				SolrUtil.MAX_DOCS);
		for (int k = 0; k < docs3.size(); k++) {
			SolrDocument doc3 = docs3.get(k);
			String object3 = (String) doc3.getFieldValue("object_t");
			List<String> interestTerms = new ArrayList<String>();
			logger.fine("individualInterest: " + object3);
			
			SolrDocumentList docs4 = SolrUtil.getDocsbySPO(object3,AutoIE.interestTerms.toString(), null,
					SolrUtil.MAX_DOCS);
			for(int m=0;m<docs4.size();m++){
				
				SolrDocument doc4 = docs4.get(m);
				String object4 = (String) doc4.getFieldValue("object_t");
				logger.fine(" object4 = "+ object4);
				String interestText = object4;
				if(interestText.indexOf(',')>0){
					String[] interest_array = interestText.split(",");
					
					for(int l = 0; l<interest_array.length-1;l++){
						if(!interestTerms.contains(interest_array[l].replaceAll(" ",""))){
							interestTerms.add(interest_array[l].replaceAll(" ",""));
						}
					}	
				}
				else if(!interestTerms.contains(interestText)){
					interestTerms.add(interestText);
				}
			}
			interestTermsList.add(interestTerms);
			interestNames.add(object3);
			
		}
        
        String subject_person = n0.toString();
		SolrDocumentList docs = SolrUtil.getDocsbySPO(subject_person,AutoIE.describesLikesAs.toString(), null,
				SolrUtil.MAX_DOCS);
		for (int i = 0; i < docs.size(); i++) {
			SolrDocument doc = docs.get(i);
			String subject_opinion = (String) doc.getFieldValue("object_t");
			SolrDocumentList docs2 = SolrUtil.getDocsbySPO(subject_opinion,interestPred, null,
					SolrUtil.MAX_DOCS);
			List<String> likes_text = SetUniqueList.decorate(new ArrayList<String>());
			for (int j = 0; j < docs2.size(); j++) {
				SolrDocument doc2 = docs2.get(j);
				String like_text = (String) doc2.getFieldValue("object_t");
				if(!likes_text.contains(like_text)){
					likes_text.add(like_text);
					List<String> synonyms = SetUniqueList.decorate(new ArrayList<String>());
					addPieces(synonyms,  like_text);
					addSynonyms(synonyms);
					for(int k=0;k<synonyms.size();k++){
						logger.fine("synonyms:"+synonyms.get(k));
						for(int l=0;l<interestTermsList.size();l++)
						if(interestTermsList.get(l).contains(synonyms.get(k))){
							Node val = Node.createLiteral(interestNames.get(l));
							context.add( new Triple( n0, n1, val ) ); 
							logger.fine("added n-triple: " + n0 + ","+n1+","+val);
						}
					}
					

				
					
				}
			}
			
		}
		
		//Checks for the interest area for the doc that actually fired the rule
			
				String like_text = opinCatNode.toString(false);
				List<String> synonyms = SetUniqueList.decorate(new ArrayList<String>());
				addPieces(synonyms,  like_text);
				addSynonyms(synonyms);
				for(int k=0;k<synonyms.size();k++){
					logger.fine("synonym:"+synonyms.get(k));
					for(int l=0;l<interestTermsList.size();l++)
					if(interestTermsList.get(l).contains(synonyms.get(k))){
						Node val = Node.createLiteral(interestNames.get(l));
						context.add( new Triple( n0, n1, val ) ); 
						logger.fine("added n-triple: " + n0 + ","+n1+","+val);
					}
					
    			}

				
					
			
			
			
		
		
		
		
		
		
		

        
    }

    /**
     * This method is invoked when the builtin is called in a rule body.
     * @param args the array of argument values for the builtin, this is an array 
     * of Nodes, some of which may be Node_RuleVariables.
     * @param length the length of the argument list, may be less than the length of the args array
     * for some rule engines
     * @param context an execution context giving access to other relevant data
     * @return return true if the buildin predicate is deemed to have succeeded in
     * the current environment
     */
    @Override
    public boolean bodyCall(Node[] args, int length, RuleContext context) {
        if (length !=3) {
            throw new BuiltinException(this, context, "builtin " + getName() + " requires 3 arguments but saw " + length);
        }
        
        BindingEnvironment env = context.getEnv();

        
        Node subj = getArg(0, args, context);
        Node pred = getArg(1, args, context);       
        Node obj = getArg(2, args, context); 
        
        logger.fine("LIKE subject is " + subj + " pred is " + pred + " obj is " + obj);
        
   		List<String> synonyms = SetUniqueList.decorate(new ArrayList<String>());
   		addPieces(synonyms, obj.toString());    
   		addSynonyms(synonyms);
		String synonymString = synonyms.toString().toLowerCase();
        logger.fine("LIKE val is " + synonymString);
 
        Node val = Node.createLiteral(synonyms.toString().toLowerCase());
        
        env.bind(obj, val);
        return true;
    }
    
	public static List<String> addSynonyms(List<String> terms) {
		List<String> retList = terms;
		for (int i = 0; i < terms.size(); i++) {
			List<String> temp = WordNetUtil.getSynonyms(terms.get(i));
			retList.addAll(temp);
		}
		return retList;
	}

    
    
	public static void addPieces(List<String> categories, String term) {
		int indexOf = term.indexOf("&");
		if (indexOf > 0) {
			String pieceOne = term.substring(0, indexOf);
			String pieceTwo = term.substring(indexOf + 1);
			String[] one = pieceOne.split("&| |;|\\/");
			String[] two = pieceTwo.trim().split("&| |;|\\/");

			categories.add(one[one.length - 1].toLowerCase());
			categories.add(two[0].toLowerCase());
			return;
		} else if (term.contains(";")) {
			int where = term.indexOf(";");
			String temp = term.substring(0, where);
			String[] x = temp.split("&| |;|\\/");
			if (x.length == 1) {
				categories.add(x[0].toLowerCase());
			}
			return;
		} else if (term.contains("/")) {
			int where = term.indexOf("/");
			String temp = term.substring(0, where);
			String[] x = temp.split("&| |;|\\/");
			if (x.length == 1) {
				categories.add(x[0].toLowerCase());
			}
			return;
		}
		
		String[] parts = term.split("&| |;|\\/");
		if (parts.length > 1) {
			categories.add(term.replaceAll(" ","").toLowerCase());
			return;
		}
		term = term.replace("general|community", "");
		term = term.trim();
		if (term.length() > 0) {
			categories.add(term.toLowerCase());
		}
	}
	   
    /**
     * Flag as non-monotonic so the guard clause will get rerun after deferal
     * as part of a non-trivial conflict set.
     */
    @Override
    public boolean isMonotonic() {
        return false;
    }
    
}
