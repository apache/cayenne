/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.unit;

import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

/**
 * A helper class to setup a shared test JNDI environment.
 */
public class JNDISetup {

    private static Logger logger = LoggerFactory.getLogger(JNDISetup.class);

    private static volatile boolean setup;

    public static void doSetup() {
        if (!setup) {

            synchronized (JNDISetup.class) {

                if (!setup) {
                    try {
                        NamingManager
                                .setInitialContextFactoryBuilder(new SimpleNamingContextBuilder());
                    }
                    catch (NamingException e) {
                        logger.error("Can't perform JNDI setup, ignoring...", e);
                    }

                    setup = true;
                }
            }
        }
    }

}
