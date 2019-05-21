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
package org.apache.cayenne.cache;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;

/**
 * @since 3.1
 */
public class MapQueryCacheProvider implements Provider<QueryCache> {

    protected RuntimeProperties properties;

    public MapQueryCacheProvider(@Inject RuntimeProperties properties) {
        this.properties = properties;
    }

    public QueryCache get() throws ConfigurationException {

        int size = properties.getInt(
                Constants.QUERY_CACHE_SIZE_PROPERTY,
                MapQueryCache.DEFAULT_CACHE_SIZE);
        return new MapQueryCache(size);
    }
}
