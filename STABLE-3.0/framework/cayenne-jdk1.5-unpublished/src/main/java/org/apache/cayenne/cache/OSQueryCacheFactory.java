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
package org.apache.cayenne.cache;

import java.util.Map;

/**
 * A factory for the OSCache factory. "/oscache.properties" file is read to load the
 * standard OSCache properties and also extra properties
 * 
 * @since 3.0
 */
public class OSQueryCacheFactory implements QueryCacheFactory {

    /**
     * Creates QueryCache, ignoring provided properties, and reading data from
     * "oscache.properties" file instead.
     */
    public QueryCache getQueryCache(Map<String, String> properties) {
        return new OSQueryCache();
    }
}
