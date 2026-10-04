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
package org.apache.cayenne;

import java.util.Map;

/**
 * Represents a properties map for a given {@link org.apache.cayenne.CayenneRuntime}.
 *
 * @since 3.1
 */
public interface RuntimeProperties {

    String JDBC_DRIVER_PROPERTY = "cayenne.jdbc.driver";

    String JDBC_URL_PROPERTY = "cayenne.jdbc.url";

    String JDBC_USERNAME_PROPERTY = "cayenne.jdbc.username";

    String JDBC_PASSWORD_PROPERTY = "cayenne.jdbc.password";

    String JDBC_MIN_CONNECTIONS_PROPERTY = "cayenne.jdbc.min_connections";

    String JDBC_MAX_CONNECTIONS_PROPERTY = "cayenne.jdbc.max_connections";

    /**
     * Defines a maximum time in milliseconds that a connection request could
     * wait in the connection queue. After this period expires, an exception
     * will be thrown in the calling method. A value of zero will make the
     * thread wait until a connection is available with no time out. Defaults to
     * 20 seconds.
     *
     * @since 4.0
     */
    String JDBC_MAX_QUEUE_WAIT_TIME = "cayenne.jdbc.max_wait";

    /**
     * @since 4.0
     */
    String JDBC_VALIDATION_QUERY_PROPERTY = "cayenne.jdbc.validation_query";

    /**
     * An integer property defining the maximum number of entries in the query
     * cache. Note that not all QueryCache providers may respect this property.
     * MapQueryCache uses it, but the rest would use alternative configuration
     * methods.
     */
    String QUERY_CACHE_SIZE_PROPERTY = "cayenne.querycache.size";

    /**
     * An optional name of the runtime DataDomain. If not specified (which is
     * normally the case), the name is inferred from the configuration name.
     *
     * @since 4.0
     */
    String DOMAIN_NAME_PROPERTY = "cayenne.domain.name";

    /**
     * A boolean property defining whether cross-contexts synchronization is
     * enabled. Possible values are "true" or "false".
     */
    String CONTEXTS_SYNC_PROPERTY = "cayenne.contexts_sync_strategy";

    /**
     * A String property that defines how ObjectContexts should retain cached
     * committed objects. Possible values are "weak", "soft", "hard".
     */
    String OBJECT_RETAIN_STRATEGY_PROPERTY = "cayenne.object_retain_strategy";

    /**
     * A boolean property that defines whether runtime should use external
     * transactions. Possible values are "true" or "false".
     */
    String EXTERNAL_TX_PROPERTY = "cayenne.external_tx";

    /**
     * A property that defines a maximum number of ID qualifiers in where clause
     * of queries that are generated for example in
     * {@link org.apache.cayenne.access.IncrementalFaultList} or in
     * DISJOINT_BY_ID prefetch processing. This is needed to avoid where clause
     * size limitations and memory usage efficiency.
     */
    String MAX_ID_QUALIFIER_SIZE_PROPERTY = "cayenne.max_id_qualifier_size";

    /**
     * Defines if database uses case-insensitive collation
     */
    String CI_PROPERTY = "cayenne.runtime.db.collation.assume.ci";

    /**
     * An integer property defining the maximum number of batch rows whose bindings are logged in full before the
     * compact SQL logger truncates them to "first row .. elided count .. last row". Default is 20.
     *
     * @since 5.0
     */
    String JDBC_LOG_BATCH_ROW_THRESHOLD_PROPERTY = "cayenne.jdbc.log.batch.threshold";

    /**
     * Snapshot cache max size
     *
     * @see org.apache.cayenne.CoreModuleExtender#snapshotCacheSize(int)
     * @since 4.0
     */
    String SNAPSHOT_CACHE_SIZE_PROPERTY = "cayenne.DataRowStore.snapshot.size";

    /**
     * Returns a String property value for a given key.
     */
    String get(String key);

    /**
     * Returns a String property value for a given key or a default value if a
     * value is not present in properties or is null.
     * 
     * @since 4.0
     */
    String get(String key, String defaultValue);

    int getInt(String key, int defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    /**
     * Returns a snapshot of the properties as a map, with the same key resolution rules as {@link #get(String)}.
     *
     * @since 5.0
     */
    Map<String, String> toMap();
}
