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
package org.apache.cayenne.access;

import org.apache.cayenne.CoreModule;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.flush.DataDomainFlushActionFactory;
import org.apache.cayenne.access.lifecycle.LifecycleEventManager;
import org.apache.cayenne.access.lifecycle.LifecycleEventsManager;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.cache.NestedQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.project.ProjectLoader;
import org.apache.cayenne.project.ProjectMerger;
import org.apache.cayenne.project.DataNodeDescriptor;
import org.apache.cayenne.project.DataNodeDescriptors;
import org.apache.cayenne.RuntimeProperties;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.lifecycle.PostAddAware;
import org.apache.cayenne.lifecycle.PostLoadAware;
import org.apache.cayenne.lifecycle.PostPersistAware;
import org.apache.cayenne.lifecycle.PostRemoveAware;
import org.apache.cayenne.lifecycle.PostUpdateAware;
import org.apache.cayenne.lifecycle.PrePersistAware;
import org.apache.cayenne.lifecycle.PreRemoveAware;
import org.apache.cayenne.lifecycle.PreUpdateAware;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.EntitySorterFactory;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategyFactory;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.tx.TransactionFactory;
import org.apache.cayenne.tx.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A {@link DataChannel} provider that provides a single instance of DataDomain
 * configured per configuration supplied via injected
 * {@link ProjectLoader}.
 *
 * @since 3.1
 */
public class DataDomainProvider implements Provider<DataDomain> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataDomainProvider.class);

    @Inject
    protected ResourceLocator resourceLocator;

    @Inject
    protected ProjectMerger descriptorMerger;

    @Inject
    protected ProjectLoader loader;

    /**
     * @since 4.1
     */
    @Inject
    protected List<DataChannelQueryFilter> queryFilters;

    /**
     * @since 4.1
     */
    @Inject
    protected List<DataChannelSyncFilter> syncFilters;

    @Inject(CoreModule.DOMAIN_LISTENERS_LIST)
    protected List<Consumer<LifecycleEventsManager>> listeners;

    @Inject(CoreModule.PROJECT_LOCATIONS_LIST)
    protected List<String> locations;

    @Inject
    protected Injector injector;

    @Inject
    protected QueryCache queryCache;

    @Inject
    protected RuntimeProperties runtimeProperties;

    @Inject
    protected DataNodeFactory dataNodeFactory;

    @Inject
    protected TransactionManager transactionManager;

    @Inject
    protected TransactionFactory transactionFactory;

    @Inject
    protected DataDomainFlushActionFactory flushActionFactory;

    @Inject
    protected AdhocObjectFactory objectFactory;

    @Inject
    protected EventManager eventManager;

    @Inject
    protected EntitySorterFactory entitySorterFactory;

    @Inject
    protected DataNodeDescriptors dataNodeDescriptors;

    @Override
    public DataDomain get() {

        Project project = loadDescriptor();
        EntityResolver entityResolver = createEntityResolver(project);
        EntitySorter entitySorter = entitySorterFactory.createEntitySorter(entityResolver);
        LifecycleEventsManager lifecycleEventsManager = createLifecycleEventsManager(entityResolver);

        DataDomain domain = new DataDomain(
                project.getName(),
                transactionManager,
                transactionFactory,
                flushActionFactory,
                objectFactory,
                eventManager,
                new NestedQueryCache(queryCache),
                createSharedSnapshotCache(project),
                runtimeProperties.getInt(RuntimeProperties.MAX_ID_QUALIFIER_SIZE_PROPERTY, -1),
                project.isValidatingObjectsOnCommit(),
                entityResolver,
                entitySorter,
                lifecycleEventsManager
        );

        dataNodeDescriptors.mapsByNode().forEach((n, maps) -> addDataNode(domain, n, maps));

        if (dataNodeDescriptors.defaultNode() != null) {
            Set<String> linkedMapNames = dataNodeDescriptors.mapsByNode().values().stream()
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            addDefaultDataNode(domain, dataNodeDescriptors.defaultNode(), linkedMapNames);
        }

        for (DataChannelQueryFilter filter : queryFilters) {
            domain.addQueryFilter(filter);
        }

        for (DataChannelSyncFilter filter : syncFilters) {
            domain.addSyncFilter(filter);
        }

        for (Consumer<LifecycleEventsManager> listener : listeners) {
            listener.accept(lifecycleEventsManager);
        }

        return domain;
    }

    protected void addDataNode(DataDomain domain, DataNodeDescriptor descriptor, Set<String> mapNames) {
        List<DataMap> dataMaps = new ArrayList<>();
        for (String mn : mapNames) {
            DataMap dataMap = domain.getDataMap(mn);
            if (dataMap == null) {
                LOGGER.info("DataNode '{}' is linked to a non-existing DataMap '{}', ignoring", descriptor.name(), mn);
            } else {
                dataMaps.add(dataMap);
            }
        }

        DataNode node = dataNodeFactory.createDataNode(domain.getName(), descriptor, dataMaps);
        domain.addNode(node, dataMaps);
    }

    /**
     * Adds the default node, which takes ownership of every DataMap not explicitly linked to another node.
     */
    protected void addDefaultDataNode(DataDomain domain, DataNodeDescriptor descriptor, Set<String> linkedMapNames) {
        LOGGER.info("setting DataNode '{}' as default, used by all unlinked DataMaps", descriptor.name());

        List<DataMap> dataMaps = domain.getDataMaps().stream()
                .filter(m -> !linkedMapNames.contains(m.getName()))
                .toList();

        DataNode node = dataNodeFactory.createDataNode(domain.getName(), descriptor, dataMaps);
        domain.addNode(node, dataMaps);
        domain.setDefaultNode(node);
    }

    /**
     * Returns a snapshot cache shared by all DataContexts of the domain, or null if the descriptor turns the shared
     * cache off, and each DataContext is to use a cache of its own.
     *
     * @since 5.0
     */
    protected DataRowStore createSharedSnapshotCache(Project project) {

        return project.isSharedCacheEnabled()
                ? injector.getInstance(DataRowStoreFactory.class).createDataRowStore(project.getName())
                : null;
    }

    /**
     * Creates the lifecycle event manager of the domain, registering the callbacks of the mapped entities, i.e. the
     * {@code org.apache.cayenne.lifecycle} interfaces implemented by their classes. Listeners contributed via DI are
     * added by the caller, after the entity callbacks.
     *
     * @since 6.0
     */
    protected LifecycleEventsManager createLifecycleEventsManager(EntityResolver entityResolver) {

        LifecycleEventsManager eventManager = new LifecycleEventsManager();

        // listeners of a class are invoked for its subclasses too, so a callback is registered once per class
        // hierarchy, on the topmost class implementing the interface, which may be an unmapped superclass shared by
        // several entities. The set tracks the classes registered with each event manager
        Map<LifecycleEventManager, Set<Class<?>>> registered = new HashMap<>();

        for (ObjEntity entity : entityResolver.getObjEntities()) {
            Class<?> entityClass = entityResolver.getClassDescriptor(entity.getName()).getObjectClass();

            addEntityCallback(eventManager.postAdd(), registered, entityClass, PostAddAware.class,
                    o -> ((PostAddAware) o).onPostAdd());
            addEntityCallback(eventManager.prePersist(), registered, entityClass, PrePersistAware.class,
                    o -> ((PrePersistAware) o).onPrePersist());
            addEntityCallback(eventManager.postPersist(), registered, entityClass, PostPersistAware.class,
                    o -> ((PostPersistAware) o).onPostPersist());
            addEntityCallback(eventManager.preUpdate(), registered, entityClass, PreUpdateAware.class,
                    o -> ((PreUpdateAware) o).onPreUpdate());
            addEntityCallback(eventManager.postUpdate(), registered, entityClass, PostUpdateAware.class,
                    o -> ((PostUpdateAware) o).onPostUpdate());
            addEntityCallback(eventManager.preRemove(), registered, entityClass, PreRemoveAware.class,
                    o -> ((PreRemoveAware) o).onPreRemove());
            addEntityCallback(eventManager.postRemove(), registered, entityClass, PostRemoveAware.class,
                    o -> ((PostRemoveAware) o).onPostRemove());
            addEntityCallback(eventManager.postLoad(), registered, entityClass, PostLoadAware.class,
                    o -> ((PostLoadAware) o).onPostLoad());
        }

        return eventManager;
    }

    private static void addEntityCallback(
            LifecycleEventManager eventManager,
            Map<LifecycleEventManager, Set<Class<?>>> registered,
            Class<?> entityClass,
            Class<?> callbackType,
            Consumer<Persistent> callback) {

        if (!callbackType.isAssignableFrom(entityClass)) {
            return;
        }

        Class<?> topmost = entityClass;
        while (callbackType.isAssignableFrom(topmost.getSuperclass())
                && Persistent.class.isAssignableFrom(topmost.getSuperclass())) {
            topmost = topmost.getSuperclass();
        }

        if (registered.computeIfAbsent(eventManager, m -> new HashSet<>()).add(topmost)) {
            eventManager.addListener(callback, topmost.asSubclass(Persistent.class));
        }
    }

    protected EntityResolver createEntityResolver(Project project) {

        EntityResolver entityResolver = new EntityResolver();

        // must go through "addDataMap" - unlike the Collection constructor, it sets the map namespace
        for (DataMap dataMap : project.getDataMaps()) {
            entityResolver.addDataMap(dataMap);
        }

        entityResolver.applyDBLayerDefaults();
        entityResolver.setValueObjectTypeRegistry(injector.getInstance(ValueObjectTypeRegistry.class));
        entityResolver.setValueComparisonStrategyFactory(injector.getInstance(ValueComparisonStrategyFactory.class));
        entityResolver.setObjectFactory(objectFactory);

        return entityResolver;
    }

    /**
     * @since 4.0
     */
    protected Project loadDescriptor() {
        Project project = locations.isEmpty() ? new Project() : loadDescriptorFromConfigs();

        String nameOverride = runtimeProperties.get(RuntimeProperties.DOMAIN_NAME_PROPERTY);
        if (nameOverride != null) {
            project.setName(nameOverride);
        }

        return project;
    }

    private Project loadDescriptorFromConfigs() {

        long t0 = System.currentTimeMillis();

        LOGGER.debug("starting configuration loading: {}", locations);

        Project[] descriptors = new Project[locations.size()];

        for (int i = 0; i < locations.size(); i++) {

            String location = locations.get(i);

            Collection<Resource> configurations = resourceLocator.findResources(location);
            if (configurations.isEmpty()) {
                throw new DIRuntimeException("Configuration resource \"%s\" is not found.", location);
            }

            Resource configurationResource = configurations.iterator().next();

            // no support for multiple configs yet, but this is not a hard error
            if (configurations.size() > 1) {
                LOGGER.info("found {} configurations for {}, will use the first one: {}",
                        configurations.size(),
                        location,
                        configurationResource.getURL());
            }

            descriptors[i] = loader.load(configurationResource);
        }

        long t1 = System.currentTimeMillis();

        LOGGER.debug("finished configuration loading in {} ms.", (t1 - t0));

        return descriptorMerger.merge(descriptors);
    }
}
