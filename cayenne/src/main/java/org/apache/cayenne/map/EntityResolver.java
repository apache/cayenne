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

package org.apache.cayenne.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;

import org.apache.cayenne.annotation.PostAdd;
import org.apache.cayenne.annotation.PostLoad;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.annotation.PostRemove;
import org.apache.cayenne.annotation.PostUpdate;
import org.apache.cayenne.annotation.PrePersist;
import org.apache.cayenne.annotation.PreRemove;
import org.apache.cayenne.annotation.PreUpdate;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.FaultFactory;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.reflect.SingletonFaultFactory;
import org.apache.cayenne.reflect.generic.PersistentObjectDescriptorFactory;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategyFactory;
import org.apache.cayenne.reflect.valueholder.ValueHolderDescriptorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a virtual shared namespace for zero or more DataMaps. Unlike
 * DataMap, EntityResolver is intended to work as a runtime container of
 * mapping. DataMaps can be added or removed dynamically at runtime.
 * <p>
 * EntityResolver is thread-safe.
 * </p>
 *
 * @since 1.1
 */
public class EntityResolver implements MappingNamespace, Serializable {

    protected static final Logger logger = LoggerFactory.getLogger(EntityResolver.class);
    protected static AtomicLong incrementer = new AtomicLong();

    protected static final Map<LifecycleEvent, Class<? extends Annotation>> LIFECYCLE_EVENT_MAP;
    static {
        LIFECYCLE_EVENT_MAP = new EnumMap<>(LifecycleEvent.class);
        LIFECYCLE_EVENT_MAP.put(LifecycleEvent.POST_ADD, PostAdd.class);
        LIFECYCLE_EVENT_MAP.put(LifecycleEvent.PRE_PERSIST, PrePersist.class);
        LIFECYCLE_EVENT_MAP.put(LifecycleEvent.POST_PERSIST, PostPersist.class);
        LIFECYCLE_EVENT_MAP.put(LifecycleEvent.PRE_UPDATE, PreUpdate.class);
        LIFECYCLE_EVENT_MAP.put(LifecycleEvent.POST_UPDATE, PostUpdate.class);
        LIFECYCLE_EVENT_MAP.put(LifecycleEvent.PRE_REMOVE, PreRemove.class);
        LIFECYCLE_EVENT_MAP.put(LifecycleEvent.POST_REMOVE, PostRemove.class);
        LIFECYCLE_EVENT_MAP.put(LifecycleEvent.POST_LOAD, PostLoad.class);
    }

    protected Collection<DataMap> maps;
    protected transient MappingNamespace mappingCache;

    // must be transient, as resolver may get deserialized in another VM, and
    // descriptor recompilation will be desired.
    protected transient volatile ClassDescriptorMap classDescriptorMap;

    // callbacks are not serializable
    protected transient LifecycleCallbackRegistry callbackRegistry;

    protected transient ValueObjectTypeRegistry valueObjectTypeRegistry;

    /**
     * @since 4.2
     */
    protected transient ValueComparisonStrategyFactory valueComparisonStrategyFactory;

    /**
     * @since 4.2
     */
    protected transient EntitySorter entitySorter;

    /**
     * @since 5.0
     */
    protected transient AdhocObjectFactory objectFactory;


    /**
     * Creates new empty EntityResolver.
     */
    public EntityResolver() {
        this(Collections.emptyList());
    }

    /**
     * Creates new EntityResolver that indexes a collection of DataMaps.
     */
    public EntityResolver(Collection<DataMap> dataMaps) {
        this.maps = new ArrayList<>(dataMaps);
        refreshMappingCache();
    }

    /**
     * Updates missing mapping artifacts that can be guessed from other mapping
     * information. This implementation creates missing reverse relationships,
     * marking newly created relationships as "runtime".
     *
     * @since 3.0
     */
    public void applyDBLayerDefaults() {

        // connect DB layer
        for (DataMap map : getDataMaps()) {

            for (DbEntity entity : map.getDbEntities()) {

                // iterate by copy to avoid concurrency modification errors on reflexive relationships
                DbRelationship[] relationships = entity.getRelationships().toArray(new DbRelationship[0]);

                for (DbRelationship relationship : relationships) {
                    if (relationship.getReverseRelationship() == null) {
                        DbRelationship reverse = relationship.createReverseRelationship();

                        DbEntity targetEntity = reverse.getSourceEntity();
                        reverse.setName(getUniqueRelationshipName(targetEntity));
                        reverse.setRuntime(true);
                        targetEntity.addRelationship(reverse);

                        logger.info("added runtime complimentary DbRelationship from " + targetEntity.getName()
                                + " to " + reverse.getTargetEntityName());
                    }
                }
            }
        }

    }

    private String getUniqueRelationshipName(DbEntity entity) {
        String name;

        do {
            name = "runtimeRelationship" + incrementer.getAndIncrement();
        } while(entity.getRelationship(name) != null);

        return name;
    }

    /**
     * Compiles internal callback registry.
     */
    synchronized void initCallbacks() {
        if (callbackRegistry == null) {
            LifecycleCallbackRegistry callbackRegistry = new LifecycleCallbackRegistry(this);

            // load entity callbacks
            for (ObjEntity entity : getObjEntities()) {
                Class<?> entityClass = objectFactory.getJavaClass(entity.getJavaClassName());

                // load annotated methods
                for (Method m : entityClass.getDeclaredMethods()) {
                    LIFECYCLE_EVENT_MAP.forEach((eventType, annotationType) -> {
                        if(m.getDeclaredAnnotation(annotationType) != null) {
                            callbackRegistry.addCallback(eventType, entityClass, m);
                        }
                    });
                }

                // load callback defined in the model
                CallbackDescriptor[] callbacks = entity.getCallbackMap().getCallbacks();
                for (CallbackDescriptor callback : callbacks) {
                    for (String method : callback.getCallbackMethods()) {
                        callbackRegistry.addCallback(callback.getCallbackType(), entityClass, method);
                    }
                }
            }

            this.callbackRegistry = callbackRegistry;
        }
    }

    /**
     * Returns a {@link LifecycleCallbackRegistry} for handling callbacks.
     * Registry is lazily initialized on first call.
     *
     * @since 3.0
     */
    public LifecycleCallbackRegistry getCallbackRegistry() {
        if (callbackRegistry == null) {
            initCallbacks();
        }

        return callbackRegistry;
    }

    /**
     * Sets a lifecycle callbacks registry of the EntityResolver. Users rarely
     * if ever need to call this method as Cayenne would instantiate a registry
     * itself as needed based on mapped configuration.
     *
     * @since 3.0
     */
    public void setCallbackRegistry(LifecycleCallbackRegistry callbackRegistry) {
        this.callbackRegistry = callbackRegistry;
    }

    /**
     * Returns all DbEntities.
     */
    public Collection<DbEntity> getDbEntities() {
        checkMappingCache();
        return mappingCache.getDbEntities();
    }

    public Collection<ObjEntity> getObjEntities() {
        checkMappingCache();
        return mappingCache.getObjEntities();
    }

    /**
     * @since 3.0
     */
    public Collection<Embeddable> getEmbeddables() {
        checkMappingCache();
        return mappingCache.getEmbeddables();
    }

    /**
     * @since 4.0
     */
    public Collection<SQLResult> getResults() {
        checkMappingCache();
        return mappingCache.getResults();
    }

    public Collection<Procedure> getProcedures() {
        checkMappingCache();
        return mappingCache.getProcedures();
    }

    public Collection<QueryDescriptor> getQueryDescriptors() {
        checkMappingCache();
        return mappingCache.getQueryDescriptors();
    }

    public DbEntity getDbEntity(String name) {
        checkMappingCache();

        DbEntity result = mappingCache.getDbEntity(name);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getDbEntity(name);
        }

        return result;
    }

    public ObjEntity getObjEntity(String name) {
        checkMappingCache();

        ObjEntity result = mappingCache.getObjEntity(name);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getObjEntity(name);
        }

        return result;
    }

    public Procedure getProcedure(String procedureName) {
        checkMappingCache();

        Procedure result = mappingCache.getProcedure(procedureName);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getProcedure(procedureName);
        }

        return result;
    }

    /**
     * Returns a named query or null if no query exists for a given name.
     */
    public QueryDescriptor getQueryDescriptor(String name) {
        checkMappingCache();

        QueryDescriptor result = mappingCache.getQueryDescriptor(name);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getQueryDescriptor(name);
        }
        return result;
    }

    /**
     * @since 3.0
     */
    public Embeddable getEmbeddable(String className) {
        checkMappingCache();

        Embeddable result = mappingCache.getEmbeddable(className);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getEmbeddable(className);
        }

        return result;
    }

    /**
     * @since 3.0
     */
    public SQLResult getResult(String name) {
        checkMappingCache();

        SQLResult result = mappingCache.getResult(name);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getResult(name);
        }

        return result;
    }

    /**
     * Returns ClassDescriptor for the ObjEntity matching the name. Returns null
     * if no matching entity exists.
     *
     * @since 1.2
     */
    public ClassDescriptor getClassDescriptor(String entityName) {
        if (entityName == null) {
            throw new IllegalArgumentException("Null entityName");
        }

        return getClassDescriptorMap().getDescriptor(entityName);
    }

    public synchronized void addDataMap(DataMap map) {
        if (!maps.contains(map)) {
            maps.add(map);
            map.setNamespace(this);
            refreshMappingCache();
        }
    }

    private void checkMappingCache() {
        if (mappingCache == null) {
            refreshMappingCache();
        }
    }

    /**
     * Refreshes entity cache to reflect the current state of the DataMaps in
     * the EntityResolver.
     *
     * @since 4.0
     */
    public void refreshMappingCache() {
        mappingCache = new ProxiedMappingNamespace() {

            @Override
            protected MappingCache createDelegate() {
                return new MappingCache(maps);
            }
        };
    }

    /**
     * Returns a DataMap matching the name.
     */
    public DataMap getDataMap(String mapName) {
        if (mapName == null) {
            return null;
        }

        for (DataMap map : maps) {
            if (mapName.equals(map.getName())) {
                return map;
            }
        }

        return null;
    }

    public synchronized void setDataMaps(Collection<DataMap> maps) {
        this.maps.clear();
        this.maps.addAll(maps);
        refreshMappingCache();
    }

    /**
     * Returns an unmodifiable collection of DataMaps.
     */
    public Collection<DataMap> getDataMaps() {
        return Collections.unmodifiableCollection(maps);
    }

    /**
     * @since 4.0
     */
    public EntityInheritanceTree getInheritanceTree(String entityName) {
        checkMappingCache();

        EntityInheritanceTree tree = mappingCache.getInheritanceTree(entityName);
        if (tree == null) {
            // since we keep inheritance trees for all entities, null means
            // unknown entity...

            // rebuild cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            tree = mappingCache.getInheritanceTree(entityName);
        }

        return tree;

    }

    /**
     * Looks in the DataMap's that this object was created with for the
     * ObjEntity that maps to the services the specified class
     *
     * @return the required ObjEntity or null if there is none that matches the
     *         specifier
     *
     * @since 4.0
     */
    public ObjEntity getObjEntity(Class<?> entityClass) {
        checkMappingCache();

        ObjEntity result = mappingCache.getObjEntity(entityClass);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getObjEntity(entityClass);
        }

        return result;
    }

    public ObjEntity getObjEntity(Persistent object) {
        checkMappingCache();
        return mappingCache.getObjEntity(object);
    }

    public synchronized void removeDataMap(DataMap map) {
        if (maps.remove(map)) {
            refreshMappingCache();
        }
    }

    /**
     * Returns an object that compiles and stores {@link ClassDescriptor}
     * instances for all entities.
     * 
     * @since 3.0
     */
    public ClassDescriptorMap getClassDescriptorMap() {
        if (classDescriptorMap == null) {

            synchronized (this) {

                if (classDescriptorMap == null) {

                    ClassDescriptorMap classDescriptorMap = new ClassDescriptorMap(this);
                    FaultFactory faultFactory = new SingletonFaultFactory();

                    // add factories in reverse of the desired chain order
                    classDescriptorMap.addFactory(new ValueHolderDescriptorFactory(classDescriptorMap));
                    classDescriptorMap.addFactory(new PersistentObjectDescriptorFactory(classDescriptorMap, faultFactory, valueComparisonStrategyFactory));

                    // since ClassDescriptorMap is not synchronized, we need to prefill it with entity proxies here.
                    for (DataMap map : maps) {
                        for (String entityName : map.getObjEntityMap().keySet()) {
                            classDescriptorMap.getDescriptor(entityName);
                        }
                    }

                    this.classDescriptorMap = classDescriptorMap;
                }
            }
        }

        return classDescriptorMap;
    }

    /**
     * Java default deserialization seems not to invoke constructor by default -
     * invoking it manually
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        refreshMappingCache();
    }

    public ValueObjectTypeRegistry getValueObjectTypeRegistry() {
        return valueObjectTypeRegistry;
    }

    public void setValueObjectTypeRegistry(ValueObjectTypeRegistry valueObjectTypeRegistry) {
        this.valueObjectTypeRegistry = valueObjectTypeRegistry;
    }

    /**
     * @since 4.2
     */
    public void setValueComparisonStrategyFactory(ValueComparisonStrategyFactory valueComparisonStrategyFactory) {
        this.valueComparisonStrategyFactory = valueComparisonStrategyFactory;
    }

    /**
     * @since 4.2
     */
    public void setEntitySorter(EntitySorter entitySorter) {
        this.entitySorter = entitySorter;
    }

    /**
     * @since 4.2
     */
    public EntitySorter getEntitySorter() {
        return entitySorter;
    }

    /**
     * @since 5.0
     */
    public void setObjectFactory(AdhocObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    /**
     * @since 5.0
     */
    public AdhocObjectFactory getObjectFactory() {
        return objectFactory;
    }
}
