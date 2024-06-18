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
package org.apache.cayenne.configuration;

import org.apache.cayenne.access.types.ExtendedType;

/**
 * Defines the names of runtime properties and named collections used in DI modules.
 *
 * @since 3.1
 */
public interface Constants {

    // DI "collections"

    /**
     * A DI container key for the Map&lt;String, String&gt; storing properties
     * used by built-in Cayenne service.
     *
     * @see org.apache.cayenne.configuration.runtime.CoreModuleExtender#setProperty(String, Object)
     */
    String PROPERTIES_MAP = "cayenne.properties";

    /**
     * A DI container key for the List&lt;DbAdapterDetector&gt; that contains
     * objects that can discover the type of current database and install the
     * correct DbAdapter in runtime.
     */
    String ADAPTER_DETECTORS_LIST = "cayenne.adapter_detectors";

    /**
     * @deprecated since 5.0, use {@link #ADAPTER_DETECTORS_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_ADAPTER_DETECTORS_LIST = ADAPTER_DETECTORS_LIST;

    /**
     * A DI container key for the List&lt;Object&gt; storing lifecycle events listeners.
     *
     * @see org.apache.cayenne.configuration.runtime.CoreModuleExtender#addListener(Object)
     */
    String DOMAIN_LISTENERS_LIST = "cayenne.domain_listeners";

    /**
     * @deprecated since 5.0, use {@link #DOMAIN_LISTENERS_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_DOMAIN_LISTENERS_LIST = DOMAIN_LISTENERS_LIST;

    /**
     * A DI container key for the List&lt;String&gt; storing locations of the
     * one of more project configuration files.
     */
    String PROJECT_LOCATIONS_LIST = "cayenne.project_locations";

    /**
     * @deprecated since 5.0, use {@link #PROJECT_LOCATIONS_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_PROJECT_LOCATIONS_LIST = PROJECT_LOCATIONS_LIST;

    /**
     * A DI container key for the List&lt;ExtendedType&gt; storing default
     * adapter-agnostic ExtendedTypes.
     *
     * @see org.apache.cayenne.configuration.runtime.CoreModuleExtender#addDefaultExtendedType(ExtendedType)
     */
    String DEFAULT_TYPES_LIST = "cayenne.default_types";

    /**
     * @deprecated since 5.0, use {@link #DEFAULT_TYPES_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_DEFAULT_TYPES_LIST = DEFAULT_TYPES_LIST;

    /**
     * A DI container key for the List&lt;ExtendedType&gt; storing a
     * user-provided ExtendedTypes.
     *
     * @see org.apache.cayenne.configuration.runtime.CoreModuleExtender#addUserExtendedType(ExtendedType)
     */
    String USER_TYPES_LIST = "cayenne.user_types";

    /**
     * @deprecated since 5.0, use {@link #USER_TYPES_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_USER_TYPES_LIST = USER_TYPES_LIST;

    /**
     * A DI container key for the List&lt;ExtendedTypeFactory&gt; storing
     * default and user-provided ExtendedTypeFactories.
     *
     * @see org.apache.cayenne.configuration.runtime.CoreModuleExtender#addExtendedTypeFactory(Class)
     */
    String TYPE_FACTORIES_LIST = "cayenne.type_factories";

    /**
     * @deprecated since 5.0, use {@link #TYPE_FACTORIES_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_TYPE_FACTORIES_LIST = TYPE_FACTORIES_LIST;

    /**
     * A DI container key for binding {@link org.apache.cayenne.resource.ResourceLocator}
     */
    String RESOURCE_LOCATOR = "cayenne.resource_locator";

    /**
     * @deprecated since 5.0, use {@link #RESOURCE_LOCATOR}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_RESOURCE_LOCATOR = RESOURCE_LOCATOR;

    // Runtime properties

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
     * @deprecated since 5.0, use {@link #DOMAIN_NAME_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_DOMAIN_NAME_PROPERTY = DOMAIN_NAME_PROPERTY;

    /**
     * A boolean property defining whether cross-contexts synchronization is
     * enabled. Possible values are "true" or "false".
     */
    String CONTEXTS_SYNC_PROPERTY = "cayenne.contexts_sync_strategy";

    /**
     * @deprecated since 5.0, use {@link #CONTEXTS_SYNC_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_CONTEXTS_SYNC_PROPERTY = CONTEXTS_SYNC_PROPERTY;

    /**
     * A String property that defines how ObjectContexts should retain cached
     * committed objects. Possible values are "weak", "soft", "hard".
     */
    String OBJECT_RETAIN_STRATEGY_PROPERTY = "cayenne.object_retain_strategy";

    /**
     * @deprecated since 5.0, use {@link #CONTEXTS_SYNC_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_OBJECT_RETAIN_STRATEGY_PROPERTY = OBJECT_RETAIN_STRATEGY_PROPERTY;

    /**
     * A boolean property that defines whether runtime should use external
     * transactions. Possible values are "true" or "false".
     */
    String EXTERNAL_TX_PROPERTY = "cayenne.external_tx";

    /**
     * @deprecated since 5.0, use {@link #EXTERNAL_TX_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_EXTERNAL_TX_PROPERTY = EXTERNAL_TX_PROPERTY;

    /**
     * A property that defines a maximum number of ID qualifiers in where clause
     * of queries that are generated for example in
     * {@link org.apache.cayenne.access.IncrementalFaultList} or in
     * DISJOINT_BY_ID prefetch processing. This is needed to avoid where clause
     * size limitations and memory usage efficiency.
     */
    String MAX_ID_QUALIFIER_SIZE_PROPERTY = "cayenne.max_id_qualifier_size";

    /**
     * @deprecated since 5.0, use {@link #MAX_ID_QUALIFIER_SIZE_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_MAX_ID_QUALIFIER_SIZE_PROPERTY = MAX_ID_QUALIFIER_SIZE_PROPERTY;

    /**
     * Defines if database uses case-insensitive collation
     */
    String CI_PROPERTY = "cayenne.runtime.db.collation.assume.ci";

    /**
     * A integer property that enables logging for just long running queries
     * (rather than all queries). The value is the minimum number of
     * milliseconds a query must run before is logged. A value less than or
     * equal to zero (the default) disables this feature.
     *
     * @since 4.0
     */
    String QUERY_EXECUTION_TIME_LOGGING_THRESHOLD_PROPERTY = "cayenne.query_execution_time_logging_threshold";

    /**
     * Snapshot cache max size
     *
     * @see org.apache.cayenne.configuration.runtime.CoreModuleExtender#snapshotCacheSize(int)
     * @since 4.0
     */
    String SNAPSHOT_CACHE_SIZE_PROPERTY = "cayenne.DataRowStore.snapshot.size";

}
