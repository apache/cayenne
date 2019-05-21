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

package org.slf4j.impl;

import org.apache.cayenne.modeler.util.ModelerLogFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * StaticLoggerBinder bind and replace standard SLF4J LoggerFactory to ModelerLogFactory.
 * This is used because in Cayenne Modeler we use custom logger implementation (ModelerLogger)
 * for logging in System.err and modeler logger.
 *
 * @since 4.0
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

    private final static StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
    private final static String LOGGER_FACTORY_CLASS_STR = ModelerLogFactory.class.getName();

    private final ILoggerFactory loggerFactory = new ModelerLogFactory();

    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }

    public ILoggerFactory getLoggerFactory() {
        return this.loggerFactory;
    }

    public String getLoggerFactoryClassStr() {
        return LOGGER_FACTORY_CLASS_STR;
    }
}
