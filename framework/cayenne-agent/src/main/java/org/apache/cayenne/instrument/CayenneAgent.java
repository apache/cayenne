/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.instrument;

import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.StringTokenizer;

import javax.persistence.Persistence;

/**
 * An agent that provides access to {@link Instrumentation} instance
 * <p>
 * To enable CayenneAgent (and hence class enhancers in the Java SE environment), start
 * the JVM with the "-javaagent:" option. E.g.:
 * 
 * <pre>
 *           java -javaagent:/path/to/cayenne-agent-xxxx.jar org.example.Main
 *           java -javaagent:/path/to/cayenne-agent-xxxx.jar=arg1,arg2 org.example.Main
 * </pre>
 * 
 * Supported arguments:
 * <ul>
 * <li>jpa-eager-load: loads all JPA units available on CLASSPATH. This option ensures
 * correct enhancement of all persistent classes, regardless of the provider loading
 * order. Without this option, classes loaded before their persistence unit was
 * instantiated will not be properly enhanced. However use with caution as this option can
 * slow down the application significantly.</li>
 * </ul>
 * 
 */
public class CayenneAgent {

    private static final String JPA_EAGER_LOAD_ARG = "jpa-eager-load";
    
    static Instrumentation instrumentation;

    public static void premain(String agentArgs, Instrumentation instrumentation) {

        if (agentArgs != null) {
            System.out.println("*** CayenneAgent starting with arguments: " + agentArgs);
        }
        else {
            System.out.println("*** CayenneAgent starting...");
        }

        CayenneAgent.instrumentation = instrumentation;

        AgentOptions options = new AgentOptions(agentArgs);

        if (options.jpaEagerLoad) {
            Collection<String> jpaUnitNames = new JpaUnitNameParser().getUnitNames();
            for (String unit : jpaUnitNames) {
                Persistence.createEntityManagerFactory(unit).close();
            }
        }
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    static final class AgentOptions {

        boolean jpaEagerLoad;

        AgentOptions(String args) {
            if (args != null) {
                StringTokenizer toks = new StringTokenizer(args, ",");
                while(toks.hasMoreTokens()) {
                    String option = toks.nextToken();
                    if(JPA_EAGER_LOAD_ARG.equals(option)) {
                        jpaEagerLoad = true;
                    }
                }
            }
        }
    }
}
