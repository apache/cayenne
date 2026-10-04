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

import org.apache.cayenne.CoreModule;
import org.apache.cayenne.RuntimeProperties;

/**
 * Defines the names of runtime properties and named collections used in DI modules.
 *
 * @since 3.1
 * @deprecated use the DI keys defined in {@link CoreModule} and the property names defined in
 * {@link RuntimeProperties}
 */
@Deprecated(since = "5.0", forRemoval = true)
public interface Constants {

    /**
     * @deprecated use {@link CoreModule#PROPERTIES_MAP}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String PROPERTIES_MAP = CoreModule.PROPERTIES_MAP;

    /**
     * @deprecated use {@link CoreModule#ADAPTER_DETECTORS_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_ADAPTER_DETECTORS_LIST = CoreModule.ADAPTER_DETECTORS_LIST;

    /**
     * @deprecated use {@link CoreModule#DOMAIN_LISTENERS_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_DOMAIN_LISTENERS_LIST = CoreModule.DOMAIN_LISTENERS_LIST;

    /**
     * @deprecated use {@link CoreModule#PROJECT_LOCATIONS_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_PROJECT_LOCATIONS_LIST = CoreModule.PROJECT_LOCATIONS_LIST;

    /**
     * @deprecated use {@link CoreModule#DEFAULT_TYPES_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_DEFAULT_TYPES_LIST = CoreModule.DEFAULT_TYPES_LIST;

    /**
     * @deprecated use {@link CoreModule#USER_TYPES_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_USER_TYPES_LIST = CoreModule.USER_TYPES_LIST;

    /**
     * @deprecated use {@link CoreModule#TYPE_FACTORIES_LIST}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_TYPE_FACTORIES_LIST = CoreModule.TYPE_FACTORIES_LIST;

    /**
     * @deprecated use {@link RuntimeProperties#JDBC_DRIVER_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String JDBC_DRIVER_PROPERTY = RuntimeProperties.JDBC_DRIVER_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#JDBC_URL_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String JDBC_URL_PROPERTY = RuntimeProperties.JDBC_URL_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#JDBC_USERNAME_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String JDBC_USERNAME_PROPERTY = RuntimeProperties.JDBC_USERNAME_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#JDBC_PASSWORD_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String JDBC_PASSWORD_PROPERTY = RuntimeProperties.JDBC_PASSWORD_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#JDBC_MIN_CONNECTIONS_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String JDBC_MIN_CONNECTIONS_PROPERTY = RuntimeProperties.JDBC_MIN_CONNECTIONS_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#JDBC_MAX_CONNECTIONS_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String JDBC_MAX_CONNECTIONS_PROPERTY = RuntimeProperties.JDBC_MAX_CONNECTIONS_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#JDBC_MAX_QUEUE_WAIT_TIME}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String JDBC_MAX_QUEUE_WAIT_TIME = RuntimeProperties.JDBC_MAX_QUEUE_WAIT_TIME;

    /**
     * @deprecated use {@link RuntimeProperties#JDBC_VALIDATION_QUERY_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String JDBC_VALIDATION_QUERY_PROPERTY = RuntimeProperties.JDBC_VALIDATION_QUERY_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#QUERY_CACHE_SIZE_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String QUERY_CACHE_SIZE_PROPERTY = RuntimeProperties.QUERY_CACHE_SIZE_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#DOMAIN_NAME_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_DOMAIN_NAME_PROPERTY = RuntimeProperties.DOMAIN_NAME_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#CONTEXTS_SYNC_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_CONTEXTS_SYNC_PROPERTY = RuntimeProperties.CONTEXTS_SYNC_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#OBJECT_RETAIN_STRATEGY_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_OBJECT_RETAIN_STRATEGY_PROPERTY = RuntimeProperties.OBJECT_RETAIN_STRATEGY_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#EXTERNAL_TX_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_EXTERNAL_TX_PROPERTY = RuntimeProperties.EXTERNAL_TX_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#MAX_ID_QUALIFIER_SIZE_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_MAX_ID_QUALIFIER_SIZE_PROPERTY = RuntimeProperties.MAX_ID_QUALIFIER_SIZE_PROPERTY;

    /**
     * @deprecated use {@link RuntimeProperties#JDBC_MAX_QUEUE_WAIT_TIME}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SERVER_MAX_QUEUE_WAIT_TIME = RuntimeProperties.JDBC_MAX_QUEUE_WAIT_TIME;

    /**
     * @deprecated use {@link RuntimeProperties#CI_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String CI_PROPERTY = RuntimeProperties.CI_PROPERTY;

    /**
     * @deprecated since 5.0 this property is ignored. The slow-query threshold warning was removed as part of the
     * compact SQL logger redesign
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String QUERY_EXECUTION_TIME_LOGGING_THRESHOLD_PROPERTY = "cayenne.query_execution_time_logging_threshold";

    /**
     * @deprecated use {@link RuntimeProperties#SNAPSHOT_CACHE_SIZE_PROPERTY}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    String SNAPSHOT_CACHE_SIZE_PROPERTY = RuntimeProperties.SNAPSHOT_CACHE_SIZE_PROPERTY;
}
