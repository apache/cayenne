/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.collection.CompositeCollection;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.ClassDescriptorFactory;
import org.objectstyle.cayenne.query.Query;

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

    protected Map queryCache;
    protected Map dbEntityCache;
    protected Map objEntityCache;
    protected Map procedureCache;
    protected List maps;
    protected Map entityInheritanceCache;
    protected EntityResolver clientEntityResolver;

    // must be transient, as resolver may get deserialized in another VM, and descriptor
    // recompilation will be desired.
    protected transient ClassDescriptorFactory classDescriptorFactory;

    /**
     * Creates new EntityResolver.
     */
    public EntityResolver() {
        this.indexedByClass = true;
        this.maps = new ArrayList();
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

                    EntityResolver resolver = new EntityResolver();

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
     * Returns ClassDescriptor for the ObjEntity matching the name. Returns null if no
     * matching entity exists.
     * 
     * @since 1.2
     */
    public synchronized ClassDescriptor getClassDescriptor(String entityName) {
        if (entityName == null) {
            throw new IllegalArgumentException("Null entityName");
        }

        return getClassDescriptorFactory().getDescriptor(entityName);
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

        // index DbEntities separatly and before ObjEntities to avoid infinite loops when
        // looking up DbEntities during ObjEntity index op
        Iterator mapIterator0 = maps.iterator();
        while (mapIterator0.hasNext()) {
            DataMap map = (DataMap) mapIterator0.next();

            Iterator dbEntities = map.getDbEntities().iterator();
            while (dbEntities.hasNext()) {
                DbEntity de = (DbEntity) dbEntities.next();
                dbEntityCache.put(de.getName(), de);
            }
        }

        Iterator mapIterator1 = maps.iterator();
        while (mapIterator1.hasNext()) {
            DataMap map = (DataMap) mapIterator1.next();

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

                    // lookup DbEntity in EntityResolver cache to take into account all
                    // DataMaps in the namespace
                    Object dbEntity = dbEntityCache.get(oe.getDbEntityName());
                    if (dbEntity != null) {
                        Object existingDB = dbEntityCache.get(entityClass);
                        if (existingDB != null) {

                            if (existingDB != DUPLICATE_MARKER) {
                                dbEntityCache.put(entityClass, DUPLICATE_MARKER);
                            }
                        }
                        else {
                            dbEntityCache.put(entityClass, dbEntity);
                        }
                    }
                }
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

        // restart the map iterator to index inheritance
        Iterator mapIterator2 = maps.iterator();
        while (mapIterator2.hasNext()) {
            DataMap map = (DataMap) mapIterator2.next();

            // index ObjEntity inheritance
            Iterator objEntities = map.getObjEntities().iterator();
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
     * Searches for DataMap that holds Query root object.
     * 
     * @deprecated since 1.2 use 'Query.getMetaData(EntityResolver).getDataMap()'.
     */
    public synchronized DataMap lookupDataMap(Query q) {
        return q.getMetaData(this).getDataMap();
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
     * Looks up the DbEntity for the given query by using the query's getRoot method and
     * passing to lookupDbEntity
     * 
     * @return the root DbEntity of the query
     * @deprecated since 1.2 use 'Query.getMetaData(EntityResolver).getDbEntity()'
     */
    public synchronized DbEntity lookupDbEntity(Query q) {
        return q.getMetaData(this).getDbEntity();
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
    public synchronized ObjEntity lookupObjEntity(Persistent dataObject) {
        ObjectId id = dataObject.getObjectId();
        Object key = id != null ? (Object) id.getEntityName() : dataObject.getClass();
        return this._lookupObjEntity(key);
    }

    /**
     * Looks up the ObjEntity for the given query by using the query's getRoot method and
     * passing to lookupObjEntity
     * 
     * @return the root ObjEntity of the query
     * @throws CayenneRuntimeException if the root of the query is a DbEntity (it is not
     *             reliably possible to map from a DbEntity to an ObjEntity as a DbEntity
     *             may be the source for multiple ObjEntities. It is not safe to rely on
     *             such behaviour).
     * @deprecated since 1.2 use 'Query.getMetaData(EntityResolver).getObjEntity()'.
     */
    public synchronized ObjEntity lookupObjEntity(Query q) {
        return q.getMetaData(this).getObjEntity();
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
     * Returns a factory for ClassDescriptors used by Cayenne stack. This method is
     * guaranteed to return non null value. If the factory hasn't been set explicitly, it
     * initializes default {@link EntityDescriptorFactory}.
     * 
     * @since 1.2
     */
    public ClassDescriptorFactory getClassDescriptorFactory() {
        if (classDescriptorFactory == null) {
            this.classDescriptorFactory = new EntityDescriptorFactory(this);
        }
        return classDescriptorFactory;
    }

    /**
     * Sets a factory for ClassDescriptors used by Cayenne stack.
     * 
     * @since 1.2
     */
    public void setClassDescriptorFactory(ClassDescriptorFactory descriptorFactory) {
        this.classDescriptorFactory = descriptorFactory;
    }
}