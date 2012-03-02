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

/**
 * Defines the names of runtime properties and DI collections used in DI modules used to
 * configure server and client runtime.
 * 
 * @since 3.1
 */
public interface Constants {

    // DI "collections"

    /**
     * A DI container key for the properties map used to configure either ROP or server
     * tiers.
     */
    public static final String PROPERTIES_MAP = "cayenne.properties";

    /**
     * A DI container key for the List<DbAdapterDetector> that contains objects that can
     * discover the type of current database and install the correct DbAdapter in runtime.
     */
    public static final String SERVER_ADAPTER_DETECTORS_LIST = "cayenne.server.adapter_detectors";

    /**
     * A DI container key for the list storing DataDomain filters.
     */
    public static final String SERVER_DOMAIN_FILTERS_LIST = "cayenne.server.domain_filters";

    /**
     * A DI container key for the list storing locations of the one of more project
     * configuration files.
     */
    public static final String SERVER_PROJECT_LOCATIONS_LIST = "cayenne.server.project_locations";

    /**
     * A DI container key for the List<ExtendedType> storing default adapter-agnostic
     * ExtendedTypes.
     */
    public static final String SERVER_DEFAULT_TYPES_LIST = "cayenne.server.default_types";

    /**
     * A DI container key for the List<ExtendedType> storing a user-provided
     * ExtendedTypes.
     */
    public static final String SERVER_USER_TYPES_LIST = "cayenne.server.user_types";

    /**
     * A DI container key for the List<ExtendedTypeFactory> storing default and
     * user-provided ExtendedTypeFactories.
     */
    public static final String SERVER_TYPE_FACTORIES_LIST = "cayenne.server.type_factories";

}
