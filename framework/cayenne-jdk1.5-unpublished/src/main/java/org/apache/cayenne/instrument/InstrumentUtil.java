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
 * Instrumentation utilities.
 * 
 * @since 3.0
 */
public class InstrumentUtil {

    /**
     * A property specifying the name of the instrumentation factory that is used to
     * access JVM {@link Instrumentation} instance.
     */
    public static final String INSTRUMENTATION_FACTORY_PROPERTY = "org.apache.cayenne.instrument.factory";

    /**
     * Returns JVM instrumentation obtained via a preconfigured factory or from a number
     * of "standard" places known to Cayenne.
     */
    public static Instrumentation getInstrumentation() {

        InstrumentationFactory factory;

        String factoryName = System.getProperty(INSTRUMENTATION_FACTORY_PROPERTY);
        if (factoryName != null) {
            try {
                factory = (InstrumentationFactory) Class.forName(
                        factoryName,
                        true,
                        Thread.currentThread().getContextClassLoader()).newInstance();
            }
            catch (Throwable th) {
                throw new IllegalStateException("Invalid instrumentation factory: "
                        + factoryName, th);
            }
        }
        else {
            factory = new CayenneInstrumentationFactory();
        }

        return factory.getInstrumentation();
    }
}
