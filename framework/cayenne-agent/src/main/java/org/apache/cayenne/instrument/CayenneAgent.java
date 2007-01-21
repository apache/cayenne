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

/**
 * An agent that provides access to {@link Instrumentation} instance
 * <p>
 * To enable CayenneAgent (and hence class enhancers in the Java SE environment), start
 * the JVM with the "-javaagent:" option. E.g.:
 * 
 * <pre>java -javaagent:/path/to/cayenne-agent-xxxx.jar org.example.Main</pre>
 * 
 * @author Andrus Adamchik
 */
public class CayenneAgent {

    static Instrumentation instrumentation;

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("*** CayenneAgent starting...");
        CayenneAgent.instrumentation = instrumentation;
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
