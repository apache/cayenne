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

package org.apache.cayenne.configuration.server;

import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.tx.TransactionFilter;

/**
 * A builder of extensions for {@link ServerModule}.
 *
 * @see ServerModule#extend(Binder)
 * @since 5.0
 */
public class ServerModuleExtender {

    private final Binder binder;

    private MapBuilder<String> properties;
    private ListBuilder<String> projectLocations;
    private ListBuilder<DbAdapterDetector> adapterDetectors;
    private MapBuilder<PkGenerator> pkGenerators;
    private ListBuilder<DataChannelQueryFilter> queryFilters;
    private ListBuilder<DataChannelSyncFilter> syncFilters;
    private ListBuilder<Object> listeners;
    private ListBuilder<ExtendedType> defaultExtendedTypes;
    private ListBuilder<ExtendedType> userExtendedTypes;
    private ListBuilder<ExtendedTypeFactory> extendedTypeFactories;
    private ListBuilder<ValueObjectType> valueObjectTypes;

    protected ServerModuleExtender(Binder binder) {
        this.binder = binder;
    }

    protected ServerModuleExtender initAllExtensions() {
        contributeProperties();
        contributeProjectLocations();
        contributeAdapterDetectors();
        contributePkGenerators();
        contributeQueryFilters();
        contributeSyncFilters();
        contributeListeners();
        contributeDefaultExtendedTypes();
        contributeUserExtendedTypes();
        contributeExtendedTypeFactories();
        contributeValueObjectTypes();
        return this;
    }

    /**
     * Sets Cayenne runtime property. Property names known to Cayenne are defined in the {@link Constants} interface.
     */
    public ServerModuleExtender setProperty(String key, Object value) {
        contributeProperties().put(key, value != null ? value.toString() : null);
        return this;
    }

    /**
     * Configures the stack to synchronize data between ObjectContexts. This is false by default.
     */
    public ServerModuleExtender syncContexts() {
        contributeProperties().put(Constants.SERVER_CONTEXTS_SYNC_PROPERTY, "true");
        return this;
    }

    /**
     * Sets transaction management to either external. By default, transactions are internally managed by Cayenne.
     */
    public ServerModuleExtender externalTransactions() {
        contributeProperties().put(Constants.SERVER_EXTERNAL_TX_PROPERTY, "true");
        return this;
    }

    /**
     * Sets max size of snapshot cache.
     *
     * @param size max size of snapshot cache
     */
    public ServerModuleExtender snapshotCacheSize(int size) {
        contributeProperties().put(Constants.SNAPSHOT_CACHE_SIZE_PROPERTY, Integer.toString(size));
        return this;
    }

    /**
     * Adds a custom project location.
     */
    public ServerModuleExtender addProjectLocation(String location) {
        contributeProjectLocations().add(location);
        return this;
    }

    /**
     * Adds a custom PK generator per DbAdapter
     */
    public ServerModuleExtender addPkGenerator(Class<? extends DbAdapter> adapter, PkGenerator pkGenerator) {
        contributePkGenerators().put(adapter.getName(), pkGenerator);
        return this;
    }

    /**
     * Adds a custom PK generator per DbAdapter
     */
    public ServerModuleExtender addPkGenerator(Class<? extends DbAdapter> adapter, Class<? extends PkGenerator> pkGeneratorType) {
        contributePkGenerators().put(adapter.getName(), pkGeneratorType);
        return this;
    }

    /**
     * Adds a custom query filter to the end of the existing filter list
     */
    public ServerModuleExtender addQueryFilter(DataChannelQueryFilter queryFilter) {
        contributeQueryFilters().add(queryFilter);
        return this;
    }

    /**
     * Adds a custom query filter to the end of the existing filter list
     */
    public ServerModuleExtender addQueryFilter(Class<? extends DataChannelQueryFilter> queryFilterType) {
        contributeQueryFilters().add(queryFilterType);
        return this;
    }

    /**
     * Adds a custom sync filter.
     */
    public ServerModuleExtender addSyncFilter(DataChannelSyncFilter syncFilter) {
        contributeSyncFilters().add(syncFilter);
        return this;
    }

    /**
     * Adds a custom sync filter.
     */
    public ServerModuleExtender addSyncFilter(Class<? extends DataChannelSyncFilter> syncFilterType) {
        contributeSyncFilters().add(syncFilterType);
        return this;
    }

    /**
     * Adds a custom sync filter. Depending on the "includeInTransaction" parameter value it is added either
     * before or after the {@link TransactionFilter}.
     */
    public ServerModuleExtender addSyncFilter(DataChannelSyncFilter syncFilter, boolean includeInTransaction) {
        if (includeInTransaction) {
            contributeSyncFilters().insertBefore(syncFilter, TransactionFilter.class);
        } else {
            contributeSyncFilters().addAfter(syncFilter, TransactionFilter.class);
        }

        return this;
    }

    /**
     * Adds a custom sync filter. Depending on the "includeInTransaction" parameter value it is added either
     * before or after the {@link TransactionFilter}.
     */
    public ServerModuleExtender addSyncFilter(Class<? extends DataChannelSyncFilter> syncFilterType, boolean includeInTransaction) {
        if (includeInTransaction) {
            contributeSyncFilters().insertBefore(syncFilterType, TransactionFilter.class);
        } else {
            contributeSyncFilters().addAfter(syncFilterType, TransactionFilter.class);
        }

        return this;
    }

    /**
     * Registers an annotated event listener.
     */
    public ServerModuleExtender addListener(Object listener) {
        contributeListeners().add(listener);
        return this;
    }

    /**
     * Registers an annotated event listener of a given type
     */
    public ServerModuleExtender addListenerType(Class<?> listenerType) {
        contributeListeners().add(listenerType);
        return this;
    }

    /**
     * Adds a custom DbAdapterDetector
     */
    public ServerModuleExtender addAdapterDetector(DbAdapterDetector adapterDetector) {
        contributeAdapterDetectors().add(adapterDetector);
        return this;
    }

    /**
     * Adds a custom DbAdapterDetector
     */
    public ServerModuleExtender addAdapterDetector(Class<? extends DbAdapterDetector> adapterDetectorType) {
        contributeAdapterDetectors().add(adapterDetectorType);
        return this;
    }

    /**
     * Adds a default adapter-agnostic ExtendedType. "Default" types are loaded before adapter-provided or "user"
     * types, so they may be overridden by those.
     */
    public ServerModuleExtender addDefaultExtendedType(ExtendedType<?> type) {
        contributeDefaultExtendedTypes().add(type);
        return this;
    }

    /**
     * Adds a default adapter-agnostic ExtendedType. "Default" types are loaded before adapter-provided or "user"
     * types, so they may be overridden by those.
     */
    public ServerModuleExtender addDefaultExtendedType(Class<? extends ExtendedType<?>> type) {
        contributeDefaultExtendedTypes().add(type);
        return this;
    }

    /**
     * Adds an adapter-agnostic ExtendedType. "User" types are loaded after default and adapter-provided types and
     * can override those.
     */
    public ServerModuleExtender addUserExtendedType(ExtendedType<?> type) {
        contributeUserExtendedTypes().add(type);
        return this;
    }

    /**
     * Adds an adapter-agnostic ExtendedType. "User" types are loaded after default and adapter-provided types and
     * can override those.
     */
    public ServerModuleExtender addUserExtendedType(Class<? extends ExtendedType<?>> type) {
        contributeUserExtendedTypes().add(type);
        return this;
    }

    /**
     * Adds an ExtendedTypeFactory used for dynamic extended type creation.
     */
    public ServerModuleExtender addExtendedTypeFactory(ExtendedTypeFactory factory) {
        contributeExtendedTypeFactories().add(factory);
        return this;
    }

    /**
     * Adds an ExtendedTypeFactory used for dynamic extended type creation.
     */
    public ServerModuleExtender addExtendedTypeFactory(Class<? extends ExtendedTypeFactory> factoryType) {
        contributeExtendedTypeFactories().add(factoryType);
        return this;
    }

    /**
     * Adds a custom {@link ValueObjectType}.
     */
    public ServerModuleExtender addValueObjectType(ValueObjectType<?, ?> type) {
        contributeValueObjectTypes().add(type);
        return this;
    }

    /**
     * Adds a custom {@link ValueObjectType}.
     */
    public ServerModuleExtender addValueObjectType(Class<? extends ValueObjectType<?, ?>> type) {
        contributeValueObjectTypes().add(type);
        return this;
    }

    private ListBuilder<String> contributeProjectLocations() {
        if (projectLocations == null) {
            projectLocations = binder.bindList(String.class, Constants.SERVER_PROJECT_LOCATIONS_LIST);
        }
        return projectLocations;
    }

    private MapBuilder<String> contributeProperties() {
        if (properties == null) {
            properties = binder.bindMap(String.class, Constants.PROPERTIES_MAP);
        }
        return properties;
    }

    private ListBuilder<DataChannelQueryFilter> contributeQueryFilters() {
        if (queryFilters == null) {
            queryFilters = binder.bindList(DataChannelQueryFilter.class);
        }
        return queryFilters;
    }

    private ListBuilder<DataChannelSyncFilter> contributeSyncFilters() {
        if (syncFilters == null) {
            syncFilters = binder.bindList(DataChannelSyncFilter.class);
        }
        return syncFilters;
    }

    private ListBuilder<Object> contributeListeners() {
        if (listeners == null) {
            listeners = binder.bindList(Object.class, Constants.SERVER_DOMAIN_LISTENERS_LIST);
        }
        return listeners;
    }

    private ListBuilder<DbAdapterDetector> contributeAdapterDetectors() {
        if (adapterDetectors == null) {
            adapterDetectors = binder.bindList(DbAdapterDetector.class,
                                               Constants.SERVER_ADAPTER_DETECTORS_LIST);
        }
        return adapterDetectors;
    }

    private ListBuilder<ExtendedType> contributeDefaultExtendedTypes() {
        if (defaultExtendedTypes == null) {
            defaultExtendedTypes = binder.bindList(ExtendedType.class,
                                                   Constants.SERVER_DEFAULT_TYPES_LIST);
        }
        return defaultExtendedTypes;
    }

    private ListBuilder<ExtendedType> contributeUserExtendedTypes() {
        if (userExtendedTypes == null) {
            userExtendedTypes = binder.bindList(ExtendedType.class,
                                                Constants.SERVER_USER_TYPES_LIST);
        }
        return userExtendedTypes;
    }

    private ListBuilder<ExtendedTypeFactory> contributeExtendedTypeFactories() {
        if (extendedTypeFactories == null) {
            extendedTypeFactories = binder.bindList(ExtendedTypeFactory.class,
                                                    Constants.SERVER_TYPE_FACTORIES_LIST);
        }
        return extendedTypeFactories;
    }

    private ListBuilder<ValueObjectType> contributeValueObjectTypes() {
        if (valueObjectTypes == null) {
            valueObjectTypes = binder.bindList(ValueObjectType.class);
        }
        return valueObjectTypes;
    }

    private MapBuilder<PkGenerator> contributePkGenerators() {
        if (pkGenerators == null) {
            pkGenerators = binder.bindMap(PkGenerator.class);
        }
        return pkGenerators;
    }
}
