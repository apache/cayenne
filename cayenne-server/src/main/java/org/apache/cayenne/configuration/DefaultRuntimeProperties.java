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
package org.apache.cayenne.configuration;

import java.util.Map;

import org.apache.cayenne.di.Inject;

/**
 * An implementation of {@link RuntimeProperties} that returns properties that were
 * injected via a map in constructor. Each property can be overridden via -D command line
 * option (i.e. in this implementation JVM system properties take precedence over any
 * other property configuration mechanism).
 * 
 * @since 3.1
 */
public class DefaultRuntimeProperties implements RuntimeProperties {

    protected Map<String, String> properties;

    public DefaultRuntimeProperties(@Inject(Constants.PROPERTIES_MAP) Map<String, String> properties) {
        this.properties = properties;
    }

    public String get(String key) {

        // TODO: note that System.getProperty uses a synchronized hashtable internally as
        // of Java 1.6. So this method suddenly becomes a synchronization bottleneck.
        String property = System.getProperty(key);

        if (property != null) {
            return property;
        }

        return properties.get(key);
    }
    
    public long getLong(String key, long defaultValue) {
        String string = get(key);
        if (string == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(string);
        }
        catch (NumberFormatException e) {
            // incorrect property format, should we rethrow?
            return defaultValue;
        }
    }

    public int getInt(String key, int defaultValue) {
        String string = get(key);
        if (string == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(string);
        }
        catch (NumberFormatException e) {
            // incorrect property format, should we rethrow?
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String string = get(key);
        return string != null ? "true".equalsIgnoreCase(string) : defaultValue;
    }
}
