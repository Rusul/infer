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
import com.hp.hpl.jena.reasoner.rulesys.RuleContext;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;

/**
 * Can be used in two arg form (X, P) or three arg form (X, P, V). 
 * In three arg form it succeeds if the triple  (X, P, V) is not
 * currently present, in two arg form it succeeds if there is not value
 * for (X, P, *).
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.1 $ on $Date: 2009-06-29 08:55:36 $
 */
public class Timeout extends BaseBuiltin {
	final static Logger logger = Logger.getLogger(Timeout.class
			.getName());

    /**
     * Return a name for this builtin, normally this will be the name of the 
     * functor that will be used to invoke it.
     */
    @Override
    public String getName() {
        return "Timeout";
    }
    
    @Override
    public void headAction(Node[] args, int length, RuleContext context) {
 
        checkArgs(length, context);
        Node n0 = getArg(0, args, context);
        Node n1 = getArg(1, args, context);
        Node n2 = getArg(2, args, context);
        logger.info(" in head action " + n0 + n1 + n2);
        Object mLock = new Object();

        synchronized (mLock) {
        	try {
        		mLock.wait(30000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        } 
        
// 
//        Node val = Node.createLiteral(synonyms.toString().toLowerCase());
//        context.add( new Triple( n0, n1, val ) );
        
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
