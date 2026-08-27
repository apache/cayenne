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
package org.apache.cayenne.modeler.log;

import org.slf4j.ILoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating ModelerLogger instances.
 */
public class ModelerLogFactory implements ILoggerFactory {

    private static final LogAppender SYSTEM_ERR_APPENDER = (level, formattedMessage) -> System.err.print(formattedMessage);

    private static volatile LogAppender appender = SYSTEM_ERR_APPENDER;

    /**
     * Installs a UI-side appender. Calls are mirrored to {@code System.err} via a synthetic
     * composite, so the consumer never has to worry about the dual-sink behavior. Passing
     * {@code null} reverts to the {@code System.err}-only default.
     */
    public static void setAppender(LogAppender appender) {
        if (appender == null) {
            ModelerLogFactory.appender = SYSTEM_ERR_APPENDER;
        } else {
            ModelerLogFactory.appender = (level, formattedMessage) -> {
                appender.appendMessage(level, formattedMessage);
                SYSTEM_ERR_APPENDER.appendMessage(level, formattedMessage);
            };
        }
    }

    public static LogAppender getAppender() {
        return appender;
    }

    private final Map<String, ModelerLogger> localCache;

    public ModelerLogFactory() {
        localCache = new HashMap<>();
    }

    @Override
    public ModelerLogger getLogger(String name) {
        return localCache.computeIfAbsent(name, ModelerLogger::new);
    }
}
