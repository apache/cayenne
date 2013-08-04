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

package org.apache.cayenne.map;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.FaultFactory;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.reflect.SingletonFaultFactory;
import org.apache.cayenne.reflect.generic.DataObjectDescriptorFactory;
import org.apache.cayenne.reflect.valueholder.ValueHolderDescriptorFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    protected static final Log logger = LogFactory.getLog(EntityResolver.class);

    @Deprecated
    protected boolean indexedByClass;

    protected Collection<DataMap> maps;
    protected transient MappingNamespace mappingCache;
    protected EntityResolver clientEntityResolver;

    // must be transient, as resolver may get deserialized in another VM, and
    // descriptor recompilation will be desired.
    protected transient volatile ClassDescriptorMap classDescriptorMap;

    // callbacks are not serializable
    protected transient LifecycleCallbackRegistry callbackRegistry;

    /**
     * Creates new empty EntityResolver.
     */
    public EntityResolver() {
        this(Collections.<DataMap> emptyList());
    }

    /**
     * Creates new EntityResolver that indexes a collection of DataMaps.
     */
    public EntityResolver(Collection<DataMap> dataMaps) {
        this.maps = new ArrayList<DataMap>(dataMaps);
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

                // iterate by copy to avoid concurrency modification errors on
                // reflexive relationships
                DbRelationship[] relationships = entity.getRelationships().toArray(
                        new DbRelationship[entity.getRelationships().size()]);

                for (DbRelationship relationship : relationships) {
                    if (relationship.getReverseRelationship() == null) {
                        DbRelationship reverse = relationship.createReverseRelationship();

                        Entity targetEntity = reverse.getSourceEntity();
                        reverse.setName(makeUniqueRelationshipName(targetEntity));
                        reverse.setRuntime(true);
                        targetEntity.addRelationship(reverse);

                        logger.info("added runtime complimentary DbRelationship from " + targetEntity.getName()
                                + " to " + reverse.getTargetEntityName());
                    }
                }
            }
        }

    }

    /**
     * @since 3.0
     * @deprecated since 3.2 does nothing. Previously it used to create runtime
     *             ObjRelationships, that broke a lot of things.
     */
    @Deprecated
    public void applyObjectLayerDefaults() {
        // noop
    }

    private String makeUniqueRelationshipName(Entity entity) {
        for (int i = 0; i < 1000; i++) {
            String name = "runtimeRelationship" + i;
            if (entity.getRelationship(name) == null) {
                return name;
            }
        }

        throw new CayenneRuntimeException("Could not come up with a unique relationship name");
    }

    /**
     * Compiles internal callback registry.
     */
    synchronized void initCallbacks() {
        if (callbackRegistry == null) {
            LifecycleCallbackRegistry callbackRegistry = new LifecycleCallbackRegistry(this);

            // load entity callbacks
            for (ObjEntity entity : getObjEntities()) {
                Class<?> entityClass = entity.getJavaClass();

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
     * Returns ClientEntityResolver with mapping information that only includes
     * entities available on CWS Client Tier.
     * 
     * @since 1.2
     */
    public EntityResolver getClientEntityResolver() {

        if (clientEntityResolver == null) {

            synchronized (this) {

                if (clientEntityResolver == null) {

                    EntityResolver resolver = new ClientEntityResolver();

                    // translate to client DataMaps
                    for (DataMap map : getDataMaps()) {
                        DataMap clientMap = map.getClientDataMap(this);

                        if (clientMap != null) {
                            resolver.addDataMap(clientMap);
                        }
                    }

                    clientEntityResolver = resolver;
                }
            }
        }

        return clientEntityResolver;
    }

    /**
     * Returns all DbEntities.
     */
    public Collection<DbEntity> getDbEntities() {
        return mappingCache.getDbEntities();
    }

    public Collection<ObjEntity> getObjEntities() {
        return mappingCache.getObjEntities();
    }

    /**
     * @since 3.0
     */
    public Collection<Embeddable> getEmbeddables() {
        return mappingCache.getEmbeddables();
    }

    /**
     * @deprecated since 3.2 use {@link #getResults()}.
     */
    @Deprecated
    public Collection<SQLResult> getResultSets() {
        return getResults();
    }

    /**
     * @since 3.2
     */
    public Collection<SQLResult> getResults() {
        return mappingCache.getResults();
    }

    public Collection<Procedure> getProcedures() {
        return mappingCache.getProcedures();
    }

    public Collection<Query> getQueries() {
        return mappingCache.getQueries();
    }

    public DbEntity getDbEntity(String name) {
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
    public Query getQuery(String name) {
        Query result = mappingCache.getQuery(name);

        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getQuery(name);
        }
        return result;
    }

    /**
     * @since 3.0
     */
    public Embeddable getEmbeddable(String className) {
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

    /**
     * Removes all entity mappings from the cache.
     * 
     * @deprecated since 3.2 in favor of {@link #refreshMappingCache()}.
     */
    @Deprecated
    public void clearCache() {
        refreshMappingCache();
    }

    /**
     * Refreshes entity cache to reflect the current state of the DataMaps in
     * the EntityResolver.
     * 
     * @since 3.2
     */
    public void refreshMappingCache() {
        mappingCache = new ProxiedMappingNamespace() {

            @Override
            protected MappingCache createDelegate() {
                return new MappingCache(maps);
            }
        };

        clientEntityResolver = null;
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
     * @since 3.2
     */
    public EntityInheritanceTree getInheritanceTree(String entityName) {

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
     * @deprecated since 3.2 use {@link #getInheritanceTree(String)}.
     */
    @Deprecated
    public EntityInheritanceTree lookupInheritanceTree(String entityName) {
        return getInheritanceTree(entityName);
    }

    /**
     * Looks in the DataMap's that this object was created with for the
     * ObjEntity that maps to the services the specified class
     * 
     * @return the required ObjEntity or null if there is none that matches the
     *         specifier
     * 
     * @since 3.2
     */
    public ObjEntity getObjEntity(Class<?> entityClass) {
        ObjEntity result = mappingCache.getObjEntity(entityClass);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getObjEntity(entityClass);
        }

        return result;
    }

    /**
     * @deprecated since 3.2, use {@link #getObjEntity(Class)}.
     */
    public ObjEntity lookupObjEntity(Class<?> entityClass) {
        return getObjEntity(entityClass);
    }

    public ObjEntity getObjEntity(Persistent object) {
        return mappingCache.getObjEntity(object);
    }

    /**
     * Looks in the DataMap's that this object was created with for the
     * ObjEntity that services the specified data Object
     * 
     * @return the required ObjEntity, or null if none matches the specifier
     * @since 3.2 a corresponding getObjEntity method should be used.
     */
    @Deprecated
    public ObjEntity lookupObjEntity(Object object) {
        if (object instanceof ObjEntity) {
            return (ObjEntity) object;
        }

        if (object instanceof Persistent) {
            ObjectId id = ((Persistent) object).getObjectId();
            if (id != null) {
                return getObjEntity(id.getEntityName());
            }
        } else if (object instanceof Class) {
            return getObjEntity((Class<?>) object);
        }

        return getObjEntity(object.getClass());
    }

    /**
     * @deprecated since 3.2. Use q.getMetaData(resolver).getProcedure()
     */
    @Deprecated
    public Procedure lookupProcedure(Query q) {
        return q.getMetaData(this).getProcedure();
    }

    /**
     * @deprecated since 3.2 use {@link #getProcedure(String)}.
     */
    @Deprecated
    public Procedure lookupProcedure(String procedureName) {
        return getProcedure(procedureName);
    }

    /**
     * @deprecated since 3.2 use {@link #getQuery(String)}.
     */
    @Deprecated
    public Query lookupQuery(String name) {
        return getQuery(name);
    }

    public synchronized void removeDataMap(DataMap map) {
        if (maps.remove(map)) {
            refreshMappingCache();
        }
    }

    /**
     * @deprecated since 3.2. There's no replacement. This property is
     *             meaningless and is no longer respected by the code.
     */
    @Deprecated
    public boolean isIndexedByClass() {
        return indexedByClass;
    }

    /**
     * @deprecated since 3.2. There's no replacement. This property is
     *             meaningless.
     */
    public void setIndexedByClass(boolean b) {
        indexedByClass = b;
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
                    classDescriptorMap.addFactory(new DataObjectDescriptorFactory(classDescriptorMap, faultFactory));

                    // since ClassDescriptorMap is not synchronized, we need to
                    // prefill
                    // it with entity proxies here.
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
     * @since 3.0
     * @deprecated since 3.2 this method does nothing, as EntityResolver no
     *             longer loads listeners from its DataMaps.
     */
    @Deprecated
    public void setEntityListenerFactory(EntityListenerFactory entityListenerFactory) {
        // noop
    }

    /**
     * Java default deserialization seems not to invoke constructor by default -
     * invoking it manually
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        refreshMappingCache();
    }
}
