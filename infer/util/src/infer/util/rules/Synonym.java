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
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.collections.list.SetUniqueList;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.rulesys.BindingEnvironment;
import com.hp.hpl.jena.reasoner.rulesys.BuiltinException;
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

/**
 * Can be used in two arg form (X, P) or three arg form (X, P, V). 
 * In three arg form it succeeds if the triple  (X, P, V) is not
 * currently present, in two arg form it succeeds if there is not value
 * for (X, P, *).
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009-06-29 08:55:36 $
 */
public class Synonym extends BaseBuiltin {
	final static Logger logger = Logger.getLogger(Synonym.class
			.getName());

    /**
     * Return a name for this builtin, normally this will be the name of the 
     * functor that will be used to invoke it.
     */
    @Override
    public String getName() {
        return "synonym";
    }
    
    @Override
    public void headAction(Node[] args, int length, RuleContext context) {
 
        checkArgs(length, context);
        Node n0 = getArg(0, args, context);
        Node n1 = getArg(1, args, context);
        Node n2 = getArg(2, args, context);
        logger.info(" in head action " + n0 + n1 + n2);
        
  		List<String> synonyms = SetUniqueList.decorate(new ArrayList<String>());
   		addPieces(synonyms,  n2.toString());    
   		addSynonyms(synonyms);
		String synonymString = synonyms.toString().toLowerCase();
        logger.info(" val is " + synonymString);
 
        Node val = Node.createLiteral(synonyms.toString().toLowerCase());
        context.add( new Triple( n0, n1, val ) );
        
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
        
        logger.info(" subject is " + subj + " pred is " + pred + " obj is " + obj);
        
   		List<String> synonyms = SetUniqueList.decorate(new ArrayList<String>());
   		addPieces(synonyms, obj.toString());    
   		addSynonyms(synonyms);
		String synonymString = synonyms.toString().toLowerCase();
        logger.info(" val is " + synonymString);
 
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
		String[] parts = term.split("&| |;|\\/");
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
		}
		if (parts.length > 1) {
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
