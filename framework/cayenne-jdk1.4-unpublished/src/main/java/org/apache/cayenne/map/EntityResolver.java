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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.generic.DataObjectDescriptorFactory;
import org.apache.cayenne.reflect.pojo.EnhancedPojoDescriptorFactory;
import org.apache.cayenne.reflect.valueholder.ValueHolderDescriptorFactory;
import org.apache.commons.collections.collection.CompositeCollection;

/**
 * Represents a virtual shared namespace for zero or more DataMaps. Unlike DataMap,
 * EntityResolver is intended to work as a runtime container of mapping. DataMaps can be
 * added or removed dynamically at runtime.
 * <p>
 * EntityResolver is thread-safe.
 * </p>
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public class EntityResolver implements MappingNamespace, Serializable {

    static final Object DUPLICATE_MARKER = new Object();

    protected boolean indexedByClass;

    protected transient Map queryCache;
    protected transient Map embeddableCache;
    protected transient Map dbEntityCache;
    protected transient Map objEntityCache;
    protected transient Map procedureCache;
    protected List maps;
    protected transient Map entityInheritanceCache;
    protected EntityResolver clientEntityResolver;

    // must be transient, as resolver may get deserialized in another VM, and descriptor
    // recompilation will be desired.
    protected transient ClassDescriptorMap classDescriptorMap;

    // callbacks are not serializable
    protected transient LifecycleEventCallbackMap[] lifecycleEventCallbacks;

    /**
     * Creates new EntityResolver.
     */
    public EntityResolver() {
        this.indexedByClass = true;
        this.maps = new ArrayList();
        this.embeddableCache = new HashMap();
        this.queryCache = new HashMap();
        this.dbEntityCache = new HashMap();
        this.objEntityCache = new HashMap();
        this.procedureCache = new HashMap();
        this.entityInheritanceCache = new HashMap();
    }

    /**
     * Creates new EntityResolver that indexes a collection of DataMaps.
     */
    public EntityResolver(Collection dataMaps) {
        this();
        this.maps.addAll(dataMaps); // Take a copy
        this.constructCache();
    }

    synchronized void initCallbacks() {
        if (lifecycleEventCallbacks == null) {
            LifecycleEventCallbackMap[] lifecycleEventCallbacks = new LifecycleEventCallbackMap[7];

            for (int i = 0; i < lifecycleEventCallbacks.length; i++) {
                lifecycleEventCallbacks[i] = new LifecycleEventCallbackMap() {

                    protected boolean isExcludingDefaultListeners(Class objectClass) {
                        return excludingDefaultListeners(objectClass);
                    }

                    protected boolean isExcludingSuperclassListeners(Class objectClass) {
                        return excludingSuperclassListeners(objectClass);
                    }
                };
            }

            this.lifecycleEventCallbacks = lifecycleEventCallbacks;
        }
    }

    /**
     * @since 3.0
     */
    boolean excludingDefaultListeners(Class objectClass) {
        ObjEntity entity = lookupObjEntity(objectClass);
        return entity != null && entity.isExcludingDefaultListeners();
    }

    /**
     * @since 3.0
     */
    boolean excludingSuperclassListeners(Class objectClass) {
        ObjEntity entity = lookupObjEntity(objectClass);
        return entity != null && entity.isExcludingSuperclassListeners();
    }

    /**
     * Returns a {@link LifecycleEventCallbackMap} for a given type of lifecycle events.
     * Event types are defined as constants in LifecycleEventCallback interface. E.g.
     * {@link LifecycleEventCallback#PRE_PERSIST}, etc.
     * 
     * @since 3.0
     */
    public LifecycleEventCallbackMap getCallbacks(int callbackType) {
        if (lifecycleEventCallbacks == null) {
            initCallbacks();
        }

        return lifecycleEventCallbacks[callbackType];
    }

    /**
     * Returns ClientEntityResolver with mapping information that only includes entities
     * available on CWS Client Tier.
     * 
     * @since 1.2
     */
    public EntityResolver getClientEntityResolver() {

        if (clientEntityResolver == null) {

            synchronized (this) {

                if (clientEntityResolver == null) {

                    EntityResolver resolver = new ClientEntityResolver();

                    // translate to client DataMaps
                    Iterator it = getDataMaps().iterator();
                    while (it.hasNext()) {
                        DataMap map = (DataMap) it.next();
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
    public Collection getDbEntities() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getDbEntities());
        }

        return c;
    }

    public Collection getObjEntities() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getObjEntities());
        }

        return c;
    }

    public Collection getProcedures() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getProcedures());
        }

        return c;
    }

    public Collection getQueries() {
        CompositeCollection c = new CompositeCollection();
        Iterator it = getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            c.addComposited(map.getQueries());
        }

        return c;
    }

    public DbEntity getDbEntity(String name) {
        return _lookupDbEntity(name);
    }

    public ObjEntity getObjEntity(String name) {
        return _lookupObjEntity(name);
    }

    public Procedure getProcedure(String name) {
        return lookupProcedure(name);
    }

    public Query getQuery(String name) {
        return lookupQuery(name);
    }
    
    /**
     * @since 3.0
     */
    public Embeddable getEmbeddable(String className) {
        Embeddable result = (Embeddable) embeddableCache.get(className);

        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = (Embeddable) embeddableCache.get(className);
        }
        
        return result;
    }

    /**
     * Returns ClassDescriptor for the ObjEntity matching the name. Returns null if no
     * matching entity exists.
     * 
     * @since 1.2
     */
    public synchronized ClassDescriptor getClassDescriptor(String entityName) {
        if (entityName == null) {
            throw new IllegalArgumentException("Null entityName");
        }

        return getClassDescriptorMap().getDescriptor(entityName);
    }

    public synchronized void addDataMap(DataMap map) {
        if (!maps.contains(map)) {
            maps.add(map);
            map.setNamespace(this);
            clearCache();
        }
    }

    /**
     * Removes all entity mappings from the cache. Cache can be rebuilt either explicitly
     * by calling <code>constructCache</code>, or on demand by calling any of the
     * <code>lookup...</code> methods.
     */
    public synchronized void clearCache() {
        queryCache.clear();
        dbEntityCache.clear();
        objEntityCache.clear();
        procedureCache.clear();
        entityInheritanceCache.clear();
        clientEntityResolver = null;
    }

    /**
     * Creates caches of DbEntities by ObjEntity, DataObject class, and ObjEntity name
     * using internal list of maps.
     */
    protected synchronized void constructCache() {
        clearCache();

        // rebuild index
        Iterator mapIterator = maps.iterator();
        while (mapIterator.hasNext()) {
            DataMap map = (DataMap) mapIterator.next();

            // index ObjEntities
            Iterator objEntities = map.getObjEntities().iterator();
            while (objEntities.hasNext()) {
                ObjEntity oe = (ObjEntity) objEntities.next();

                // index by name
                objEntityCache.put(oe.getName(), oe);

                // index by class
                if (indexedByClass) {
                    Class entityClass;
                    try {
                        entityClass = oe.getJavaClass();
                    }
                    catch (CayenneRuntimeException e) {
                        // DataMaps can contain all kinds of garbage...
                        // TODO (Andrus, 10/18/2005) it would be nice to log something
                        // here, but since EntityResolver is used on the client, log4J is
                        // a no-go...
                        continue;
                    }

                    // allow duplicates, but put a special marker indicating that this
                    // entity can't be looked up by class
                    Object existing = objEntityCache.get(entityClass);
                    if (existing != null) {

                        if (existing != DUPLICATE_MARKER) {
                            objEntityCache.put(entityClass, DUPLICATE_MARKER);
                        }
                    }
                    else {
                        objEntityCache.put(entityClass, oe);
                    }

                    // TODO: Andrus, 12/13/2005 - An invalid DbEntity name will cause
                    // 'getDbEntity' to go into an
                    // infinite loop as "getDbEntity" will try to resolve DbEntity via a
                    // parent namespace (which will be this resolver).
                    if (oe.getDbEntity() != null) {
                        Object existingDB = dbEntityCache.get(entityClass);
                        if (existingDB != null) {

                            if (existingDB != DUPLICATE_MARKER) {
                                dbEntityCache.put(entityClass, DUPLICATE_MARKER);
                            }
                        }
                        else {
                            dbEntityCache.put(entityClass, oe.getDbEntity());
                        }
                    }
                }
            }

            // index ObjEntity inheritance
            objEntities = map.getObjEntities().iterator();
            while (objEntities.hasNext()) {
                ObjEntity oe = (ObjEntity) objEntities.next();

                // build inheritance tree... include nodes that
                // have no children to avoid uneeded cache rebuilding on lookup...
                EntityInheritanceTree node = (EntityInheritanceTree) entityInheritanceCache
                        .get(oe.getName());
                if (node == null) {
                    node = new EntityInheritanceTree(oe);
                    entityInheritanceCache.put(oe.getName(), node);
                }

                String superOEName = oe.getSuperEntityName();
                if (superOEName != null) {
                    EntityInheritanceTree superNode = (EntityInheritanceTree) entityInheritanceCache
                            .get(superOEName);

                    if (superNode == null) {
                        // do direct entity lookup to avoid recursive cache rebuild
                        ObjEntity superOE = (ObjEntity) objEntityCache.get(superOEName);
                        if (superOE != null) {
                            superNode = new EntityInheritanceTree(superOE);
                            entityInheritanceCache.put(superOEName, superNode);
                        }
                        else {
                            // bad mapping?
                            // TODO (Andrus, 10/18/2005) it would be nice to log something
                            // here, but since EntityResolver is used on the client, log4J
                            // is a no-go...
                            continue;
                        }
                    }

                    superNode.addChildNode(node);
                }
            }

            // index DbEntities
            Iterator dbEntities = map.getDbEntities().iterator();
            while (dbEntities.hasNext()) {
                DbEntity de = (DbEntity) dbEntities.next();
                dbEntityCache.put(de.getName(), de);
            }

            // index stored procedures
            Iterator procedures = map.getProcedures().iterator();
            while (procedures.hasNext()) {
                Procedure proc = (Procedure) procedures.next();
                procedureCache.put(proc.getName(), proc);
            }

            // index queries
            Iterator queries = map.getQueries().iterator();
            while (queries.hasNext()) {
                Query query = (Query) queries.next();
                String name = query.getName();
                Object existingQuery = queryCache.put(name, query);

                if (existingQuery != null && query != existingQuery) {
                    throw new CayenneRuntimeException("More than one Query for name"
                            + name);
                }
            }
        }
    }

    /**
     * Returns a DataMap matching the name.
     */
    public synchronized DataMap getDataMap(String mapName) {
        if (mapName == null) {
            return null;
        }

        Iterator it = maps.iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            if (mapName.equals(map.getName())) {
                return map;
            }
        }

        return null;
    }

    public synchronized void setDataMaps(Collection maps) {
        this.maps.clear();
        this.maps.addAll(maps);
        clearCache();
    }

    /**
     * Returns an unmodifiable collection of DataMaps.
     */
    public Collection getDataMaps() {
        return Collections.unmodifiableList(maps);
    }

    /**
     * Looks in the DataMap's that this object was created with for the DbEntity that
     * services the specified class
     * 
     * @return the required DbEntity, or null if none matches the specifier
     */
    public synchronized DbEntity lookupDbEntity(Class aClass) {
        if (!indexedByClass) {
            throw new CayenneRuntimeException("Class index is disabled.");
        }
        return this._lookupDbEntity(aClass);
    }

    /**
     * Looks in the DataMap's that this object was created with for the DbEntity that
     * services the specified data Object
     * 
     * @return the required DbEntity, or null if none matches the specifier
     */
    public synchronized DbEntity lookupDbEntity(Persistent dataObject) {
        return this._lookupDbEntity(dataObject.getClass());
    }

    /**
     * Returns EntityInheritanceTree representing inheritance hierarchy that starts with a
     * given ObjEntity as root, and includes all its subentities. If ObjEntity has no
     * known subentities, null is returned.
     */
    public EntityInheritanceTree lookupInheritanceTree(ObjEntity entity) {

        EntityInheritanceTree tree = (EntityInheritanceTree) entityInheritanceCache
                .get(entity.getName());

        if (tree == null) {
            // since we keep inheritance trees for all entities, null means
            // unknown entity...

            // rebuild cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            tree = (EntityInheritanceTree) entityInheritanceCache.get(entity.getName());
        }

        // don't return "trivial" trees
        return (tree == null || tree.getChildrenCount() == 0) ? null : tree;
    }

    /**
     * Looks in the DataMap's that this object was created with for the ObjEntity that
     * maps to the services the specified class
     * 
     * @return the required ObjEntity or null if there is none that matches the specifier
     */
    public synchronized ObjEntity lookupObjEntity(Class aClass) {
        if (!indexedByClass) {
            throw new CayenneRuntimeException("Class index is disabled.");
        }

        return this._lookupObjEntity(aClass);
    }

    /**
     * Looks in the DataMap's that this object was created with for the ObjEntity that
     * services the specified data Object
     * 
     * @return the required ObjEntity, or null if none matches the specifier
     */
    public synchronized ObjEntity lookupObjEntity(Object object) {
        ObjectId id = null;

        if (object instanceof Persistent) {
            id = ((Persistent) object).getObjectId();
        }

        Object key = id != null ? (Object) id.getEntityName() : object.getClass();
        return this._lookupObjEntity(key);
    }

    /**
     * Looks in the DataMap's that this object was created with for the ObjEntity that
     * maps to the services the class with the given name
     * 
     * @return the required ObjEntity or null if there is none that matches the specifier
     */
    public synchronized ObjEntity lookupObjEntity(String entityName) {
        return this._lookupObjEntity(entityName);
    }

    public Procedure lookupProcedure(Query q) {
        return q.getMetaData(this).getProcedure();
    }

    public Procedure lookupProcedure(String procedureName) {

        Procedure result = (Procedure) procedureCache.get(procedureName);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = (Procedure) procedureCache.get(procedureName);
        }

        return result;
    }

    /**
     * Returns a named query or null if no query exists for a given name.
     */
    public synchronized Query lookupQuery(String name) {
        Query result = (Query) queryCache.get(name);

        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = (Query) queryCache.get(name);
        }
        return result;
    }

    public synchronized void removeDataMap(DataMap map) {
        if (maps.remove(map)) {
            clearCache();
        }
    }

    public boolean isIndexedByClass() {
        return indexedByClass;
    }

    public void setIndexedByClass(boolean b) {
        indexedByClass = b;
    }

    /**
     * Internal usage only - provides the type-unsafe implementation which services the
     * four typesafe public lookupDbEntity methods Looks in the DataMap's that this object
     * was created with for the ObjEntity that maps to the specified object. Object may be
     * a Entity name, ObjEntity, DataObject class (Class object for a class which
     * implements the DataObject interface), or a DataObject instance itself
     * 
     * @return the required DbEntity, or null if none matches the specifier
     */
    protected DbEntity _lookupDbEntity(Object object) {
        if (object instanceof DbEntity) {
            return (DbEntity) object;
        }

        Object result = dbEntityCache.get(object);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = dbEntityCache.get(object);
        }

        if (result == DUPLICATE_MARKER) {
            throw new CayenneRuntimeException(
                    "Can't perform lookup. There is more than one DbEntity mapped to "
                            + object);
        }

        return (DbEntity) result;
    }

    /**
     * Internal usage only - provides the type-unsafe implementation which services the
     * three typesafe public lookupObjEntity methods Looks in the DataMap's that this
     * object was created with for the ObjEntity that maps to the specified object. Object
     * may be a Entity name, DataObject instance or DataObject class (Class object for a
     * class which implements the DataObject interface)
     * 
     * @return the required ObjEntity or null if there is none that matches the specifier
     */
    protected ObjEntity _lookupObjEntity(Object object) {
        if (object instanceof ObjEntity) {
            return (ObjEntity) object;
        }

        if (object instanceof Persistent) {
            object = object.getClass();
        }

        Object result = objEntityCache.get(object);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = objEntityCache.get(object);
        }

        if (result == DUPLICATE_MARKER) {
            throw new CayenneRuntimeException(
                    "Can't perform lookup. There is more than one ObjEntity mapped to "
                            + object);
        }

        return (ObjEntity) result;
    }

    /**
     * Returns an object that compiles and stores {@link ClassDescriptor} instances for
     * all entities.
     * 
     * @since 3.0
     */
    public ClassDescriptorMap getClassDescriptorMap() {
        if (classDescriptorMap == null) {
            ClassDescriptorMap classDescriptorMap = new ClassDescriptorMap(this);

            // add factories in reverse of the desired chain order
            classDescriptorMap.addFactory(new ValueHolderDescriptorFactory(
                    classDescriptorMap));
            classDescriptorMap.addFactory(new EnhancedPojoDescriptorFactory(
                    classDescriptorMap));
            classDescriptorMap.addFactory(new DataObjectDescriptorFactory(
                    classDescriptorMap));

            this.classDescriptorMap = classDescriptorMap;
        }

        return classDescriptorMap;
    }
}
