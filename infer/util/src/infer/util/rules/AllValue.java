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

import java.util.logging.Logger;
 
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
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
public class AllValue extends BaseBuiltin {
	final static Logger logger = Logger.getLogger(AllValue.class
			.getName());

    /**
     * Return a name for this builtin, normally this will be the name of the 
     * functor that will be used to invoke it.
     */
    @Override
    public String getName() {
        return "allValue";
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
        if (length !=4) {
            throw new BuiltinException(this, context, "builtin " + getName() + " requires 4 arguments but saw " + length);
        }
        Node subj = getArg(0, args, context);
        // Allow variables in subject position to correspond to wild cards
        if (subj.isVariable()) {
            subj = null;
        }
        Node pred = getArg(1, args, context);
        if (pred.isVariable()) {
            pred = null;
        }
        Node pred2 = getArg(2, args, context);
        if (pred.isVariable()) {
            pred = null;
        }
        
        Node obj = getArg(3, args, context);
        logger.info(" subject is " + subj + " pred is " + pred + " pred2  is " + pred2 + " obj is " + obj);
        
        ClosableIterator<Triple> ti =  context.find(subj, pred, (Node) null);
        while (ti.hasNext()) {
        	Triple triple = ti.next();
        	logger.info(" got a triple of " + triple);
        	logger.info(" looking for subject (triple.getObject()" + triple.getObject() + " pred2 is " + pred2 + " obj is " + obj);
        	if (!context.contains(triple.getObject(), pred2, obj)) {
        		logger.info(" returning false");
        		return false;
        	}
        }
        
         
        logger.info(" returning true");
        
        return true;
        //return !context.contains(subj, pred, obj);
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
