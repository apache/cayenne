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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

import org.apache.cayenne.CayenneRuntimeException;

/**
 * Instrumentation utilities.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class InstrumentUtil {

    /**
     * Registers class transformer with the instrumentation agent. Throws an exception if
     * the application wasn't started with CayenneAgent.
     */
    public static void addTransformer(ClassFileTransformer transformer) {
        Instrumentation instrumentation;
        try {
            instrumentation = getInstrumentation();
        }
        catch (Throwable th) {
            throw new CayenneRuntimeException("CayenneAgent is not started", th);
        }

        if (instrumentation == null) {
            throw new CayenneRuntimeException("CayenneAgent is not started");
        }

        instrumentation.addTransformer(transformer);
    }

    /**
     * Checks whether the JVM was started with CayenneAgent.
     */
    public static boolean isAgentLoaded() {

        // check whether CayenneAgent class is initialized and instrumentation is set.
        try {
            return getInstrumentation() != null;
        }
        catch (Throwable th) {
            return false;
        }
    }

    /**
     * Returns CayenneAgent instrumentation.
     */
    static Instrumentation getInstrumentation() throws Exception {
        Class agent = Class.forName(
                "org.apache.cayenne.instrument.CayenneAgent",
                false,
                Thread.currentThread().getContextClassLoader());

        Method getInstrumentation = agent.getDeclaredMethod("getInstrumentation");
        return (Instrumentation) getInstrumentation.invoke(null);
    }
}
