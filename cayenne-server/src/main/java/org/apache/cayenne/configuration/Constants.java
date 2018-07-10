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

import org.apache.cayenne.di.Binder;

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
     * @see org.apache.cayenne.configuration.server.ServerModule#contributeProperties(Binder).
     */
    String PROPERTIES_MAP = "cayenne.properties";

    /**
     * A DI container key for the List&lt;DbAdapterDetector&gt; that contains
     * objects that can discover the type of current database and install the
     * correct DbAdapter in runtime.
     */
    String SERVER_ADAPTER_DETECTORS_LIST = "cayenne.server.adapter_detectors";

    /**
     * A DI container key for the List&lt;DataChannelFilter&gt; storing
     * DataDomain filters.
     *
     * @see org.apache.cayenne.configuration.server.ServerModule#contributeDomainQueryFilters(Binder)
     * @see org.apache.cayenne.configuration.server.ServerModule#contributeDomainSyncFilters(Binder)
     * @deprecated since 4.1 domain filters replaced with query and sync filters
     */
    @Deprecated
    String SERVER_DOMAIN_FILTERS_LIST = "cayenne.server.domain_filters";

    /**
     * A DI container key for the List&lt;Object&gt; storing lifecycle events listeners.
     *
     * @see org.apache.cayenne.configuration.server.ServerModule#contributeDomainListeners(Binder).
     */
    String SERVER_DOMAIN_LISTENERS_LIST = "cayenne.server.domain_listeners";

    /**
     * A DI container key for the List&lt;String&gt; storing locations of the
     * one of more project configuration files.
     */
    String SERVER_PROJECT_LOCATIONS_LIST = "cayenne.server.project_locations";

    /**
     * A DI container key for the List&lt;ExtendedType&gt; storing default
     * adapter-agnostic ExtendedTypes.
     *
     * @see org.apache.cayenne.configuration.server.ServerModule#contributeDefaultTypes(Binder).
     */
    String SERVER_DEFAULT_TYPES_LIST = "cayenne.server.default_types";

    /**
     * A DI container key for the List&lt;ExtendedType&gt; storing a
     * user-provided ExtendedTypes.
     *
     * @see org.apache.cayenne.configuration.server.ServerModule#contributeUserTypes(Binder).
     */
    String SERVER_USER_TYPES_LIST = "cayenne.server.user_types";

    /**
     * A DI container key for the List&lt;ExtendedTypeFactory&gt; storing
     * default and user-provided ExtendedTypeFactories.
     *
     * @see org.apache.cayenne.configuration.server.ServerModule#contributeTypeFactories(Binder).
     */
    String SERVER_TYPE_FACTORIES_LIST = "cayenne.server.type_factories";

    /**
     * A server-side DI container key for binding {@link org.apache.cayenne.resource.ResourceLocator}
     */
    String SERVER_RESOURCE_LOCATOR = "cayenne.server.resource_locator";

    /**
     * A server-side DI container key for the Map&lt;String, String&gt; storing
     * event bridge properties passed to the ROP client on bootstrap.
     */
    String SERVER_ROP_EVENT_BRIDGE_PROPERTIES_MAP = "cayenne.server.rop_event_bridge_properties";

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
    String SERVER_DOMAIN_NAME_PROPERTY = "cayenne.server.domain.name";

    /**
     * A boolean property defining whether cross-contexts synchronization is
     * enabled. Possible values are "true" or "false".
     */
    String SERVER_CONTEXTS_SYNC_PROPERTY = "cayenne.server.contexts_sync_strategy";

    /**
     * A String property that defines how ObjectContexts should retain cached
     * committed objects. Possible values are "weak", "soft", "hard".
     */
    String SERVER_OBJECT_RETAIN_STRATEGY_PROPERTY = "cayenne.server.object_retain_strategy";

    /**
     * A boolean property that defines whether runtime should use external
     * transactions. Possible values are "true" or "false".
     */
    String SERVER_EXTERNAL_TX_PROPERTY = "cayenne.server.external_tx";

    /**
     * The name of the {@link org.apache.cayenne.event.EventBridgeFactory} that
     * is passed from the ROP server to the client. Client would instantiate the
     * factory to receive events from the server. Note that this property is
     * stored in {@link #SERVER_ROP_EVENT_BRIDGE_PROPERTIES_MAP}, not
     * {@link #PROPERTIES_MAP}.
     */
    String SERVER_ROP_EVENT_BRIDGE_FACTORY_PROPERTY = "cayenne.server.rop_event_bridge_factory";

    /**
     * A property that defines a maximum number of ID qualifiers in where clause
     * of queries that are generated for example in
     * {@link org.apache.cayenne.access.IncrementalFaultList} or in
     * DISJOINT_BY_ID prefetch processing. This is needed to avoid where clause
     * size limitations and memory usage efficiency.
     */
    String SERVER_MAX_ID_QUALIFIER_SIZE_PROPERTY = "cayenne.server.max_id_qualifier_size";

    /**
     * Defines a maximum time in milliseconds that a connection request could
     * wait in the connection queue. After this period expires, an exception
     * will be thrown in the calling method. A value of zero will make the
     * thread wait until a connection is available with no time out. Defaults to
     * 20 seconds.
     *
     * @deprecated since 4.0 renamed to {@link #JDBC_MAX_QUEUE_WAIT_TIME}. Property name is preserved.
     */
    String SERVER_MAX_QUEUE_WAIT_TIME = JDBC_MAX_QUEUE_WAIT_TIME;

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
    String QUERY_EXECUTION_TIME_LOGGING_THRESHOLD_PROPERTY = "cayenne.server.query_execution_time_logging_threshold";

    /**
     * Snapshot cache max size
     *
     * @see org.apache.cayenne.configuration.server.ServerModule#setSnapshotCacheSize(Binder, int)
     * @since 4.0
     */
    String SNAPSHOT_CACHE_SIZE_PROPERTY = "cayenne.DataRowStore.snapshot.size";

}
