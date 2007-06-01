/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.EntityInheritanceTree;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Procedure;
import org.objectstyle.cayenne.query.ProcedureQuery;
import org.objectstyle.cayenne.query.Query;

/**
 * In 1.1 this class is replaced by {@link org.objectstyle.cayenne.map.EntityResolver}.
 * 
 * @deprecated since 1.1 this class is moved to the map package. Use {@link org.objectstyle.cayenne.map.EntityResolver}, 
 * since org.objectstyle.cayenne.access.EntityResolver is deprecated and will be removed.
 */
public class EntityResolver {
    private static Logger logObj = Logger.getLogger(EntityResolver.class);

    protected boolean indexedByClass;
    protected Map queryCache;
    protected Map dbEntityCache;
    protected Map objEntityCache;
    protected Map procedureCache;
    protected List maps;
    protected List mapsRef;
    protected Map entityInheritanceCache;

    public EntityResolver() {
        this.indexedByClass = true;
        this.maps = new ArrayList();
        this.mapsRef = Collections.unmodifiableList(maps);
        this.queryCache = new HashMap();
        this.dbEntityCache = new HashMap();
        this.objEntityCache = new HashMap();
        this.procedureCache = new HashMap();
        this.entityInheritanceCache = new HashMap();
    }

    /**
     * Constructor for EntityResolver.
     */
    public EntityResolver(Collection dataMaps) {
        this();
        this.maps.addAll(dataMaps); //Take a copy
        this.constructCache();
    }

    /**
     * Adds a DataMap to the list handled by resolver.
     */
    public synchronized void addDataMap(DataMap map) {
        if (!maps.contains(map)) {
            maps.add(map);
            clearCache();
        }
    }

    public synchronized void removeDataMap(DataMap map) {
        if (maps.remove(map)) {
            clearCache();
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
        return mapsRef;
    }

    /**
     * Removes all entity mappings from the cache.
     * Cache can be rebuilt either explicitly by calling
     * <code>constructCache</code>, or on demand by calling any of the
     * <code>lookup...</code> methods.
     */
    public synchronized void clearCache() {
        queryCache.clear();
        dbEntityCache.clear();
        objEntityCache.clear();
        procedureCache.clear();
        entityInheritanceCache.clear();
    }

    /**
     * Creates caches of DbEntities by ObjEntity,
     * DataObject class, and ObjEntity name using internal
     * list of maps.
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
                String className = oe.getClassName();
                if (indexedByClass && className != null) {
                    Class entityClass;
                    try {
                        entityClass =
                            Configuration.getResourceLoader().loadClass(className);
                    }
                    catch (ClassNotFoundException e) {
                        // print a big warning and continue... DataMaps can contain all kinds of garbage...
                        logObj.warn("*** Class '" + className + "' not found in runtime. Ignoring.");
                        continue;
                    }

                    if (objEntityCache.get(entityClass) != null) {
                        throw new CayenneRuntimeException(
                            getClass().getName()
                                + ": More than one ObjEntity ("
                                + oe.getName()
                                + " and "
                                + ((ObjEntity) objEntityCache.get(entityClass)).getName()
                                + ") uses the class "
                                + entityClass.getName());
                    }

                    objEntityCache.put(entityClass, oe);
                    if (oe.getDbEntity() != null) {
                        dbEntityCache.put(entityClass, oe.getDbEntity());
                    }
                }
            }

            // index ObjEntity inheritance
            objEntities = map.getObjEntities().iterator();
            while (objEntities.hasNext()) {
                ObjEntity oe = (ObjEntity) objEntities.next();

                // build inheritance tree... include nodes that 
                // have no children to avoid uneeded cache rebuilding on lookup...
                EntityInheritanceTree node =
                    (EntityInheritanceTree) entityInheritanceCache.get(oe.getName());
                if (node == null) {
                    node = new EntityInheritanceTree(oe);
                    entityInheritanceCache.put(oe.getName(), node);
                }

                String superOEName = oe.getSuperEntityName();
                if (superOEName != null) {
                    EntityInheritanceTree superNode =
                        (EntityInheritanceTree) entityInheritanceCache.get(superOEName);
                        
                    if (superNode == null) {
                        // do direct entity lookup to avoid recursive cache rebuild
                        ObjEntity superOE = (ObjEntity) objEntityCache.get(superOEName);
                        if (superOE != null) {
                            superNode = new EntityInheritanceTree(superOE);
                            entityInheritanceCache.put(superOEName, superNode);
                        }
                        else {
                            // bad mapping?
                            logObj.debug(
                                "Invalid superEntity '"
                                    + superOEName
                                    + "' for entity '"
                                    + oe.getName()
                                    + "'");
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
                    throw new CayenneRuntimeException(
                        "More than one Query for name" + name);
                }
            }
        }
    }

    /**
     * Looks in the DataMap's that this object was created with for the DbEntity
     * that services the specified class
     * @return the required DbEntity, or null if none matches the specifier
     */
    public synchronized DbEntity lookupDbEntity(Class aClass) {
        if (!indexedByClass) {
            throw new CayenneRuntimeException("Class index is disabled.");
        }
        return this._lookupDbEntity(aClass);
    }

    /**
     * Looks in the DataMap's that this object was created with for the DbEntity
     * that services the specified objentity
     * 
     * @return the required DbEntity, or null if none matches the specifier
     * @deprecated Since 1.1. Use 
     * {@link org.objectstyle.cayenne.map.EntityResolver#getDbEntity(String)}
     * instead.
     */
    public synchronized DbEntity lookupDbEntity(ObjEntity entity) {
        return (entity != null) ? entity.getDbEntity() : null;
    }

    /**
     * Looks in the DataMap's that this object was created with for the DbEntity
     * that services ObjEntity with the given name. Note that this method is deprecated
     * and shouldn't be used, since it is confusing - the key name is that of ObjEntity,
     * while lookup is done for ObjEntity.
     * 
     * @deprecated Since 1.1 this method is deprecated. Use 
     * {@link org.objectstyle.cayenne.map.EntityResolver#getObjEntity(String)}
     * to find ObjEntity, and then get a DbEntity from ObjEntity.
     */
    public synchronized DbEntity lookupDbEntity(String objEntityName) {
        ObjEntity objEntity = this._lookupObjEntity(objEntityName);
        return (objEntity != null) ? _lookupDbEntity(objEntity.getDbEntityName()) : null;
    }

    /**
     * Looks in the DataMap's that this object was created with for the DbEntity
     * that services the specified data Object
     * @return the required DbEntity, or null if none matches the specifier
     */
    public synchronized DbEntity lookupDbEntity(DataObject dataObject) {
        return this._lookupDbEntity(dataObject.getClass());
    }

    /**
     * Internal usage only - provides the type-unsafe implementation which services
     * the four typesafe public lookupDbEntity methods
     * Looks in the DataMap's that this object was created with for the ObjEntity that maps to the
     * specified object.  Object may be a Entity name, ObjEntity, DataObject class
     * (Class object for a class which implements the DataObject interface), or a DataObject
     * instance itself
     *
     * @return the required DbEntity, or null if none matches the specifier
     */
    protected DbEntity _lookupDbEntity(Object object) {
        if (object instanceof DbEntity) {
            return (DbEntity) object;
        }

        DbEntity result = (DbEntity) dbEntityCache.get(object);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = (DbEntity) dbEntityCache.get(object);
        }
        return result;
    }

    /**
     * Looks up the DbEntity for the given query by using the query's getRoot method and passing to lookupDbEntity
     * @return the root DbEntity of the query
     */
    public synchronized DbEntity lookupDbEntity(Query q) {
        Object root = q.getRoot();
        if (root instanceof DbEntity) {
            return (DbEntity) root;
        }
        else if (root instanceof Class) {
            return this.lookupDbEntity((Class) root);
        }
        else if (root instanceof ObjEntity) {
            return ((ObjEntity) root).getDbEntity();
        }
        else if (root instanceof String) {
            ObjEntity objEntity = this.lookupObjEntity((String) root);
            return (objEntity != null) ? objEntity.getDbEntity() : null;
        }
        else if (root instanceof DataObject) {
            return this.lookupDbEntity((DataObject) root);
        }
        return null;
    }

    /**
     * Returns EntityInheritanceTree representing inheritance hierarchy 
     * that starts with a given ObjEntity as root, and includes all its subentities.
     * If ObjEntity has no known subentities, null is returned.
     * 
     * @since 1.1
     */
    public EntityInheritanceTree lookupInheritanceTree(ObjEntity entity) {

        EntityInheritanceTree tree =
            (EntityInheritanceTree) entityInheritanceCache.get(entity.getName());

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
     * Looks in the DataMap's that this object was created with for the ObjEntity that maps to the
     * services the specified class
     * @return the required ObjEntity or null if there is none that matches the specifier
     */
    public synchronized ObjEntity lookupObjEntity(Class aClass) {
        if (!indexedByClass) {
            throw new CayenneRuntimeException("Class index is disabled.");
        }

        return this._lookupObjEntity(aClass);
    }

    /**
     * Looks in the DataMap's that this object was created with for the ObjEntity that maps to the
     * services the class with the given name
     * @return the required ObjEntity or null if there is none that matches the specifier
     */
    public synchronized ObjEntity lookupObjEntity(String entityName) {
        return this._lookupObjEntity(entityName);
    }

    /**
     * Looks in the DataMap's that this object was created with for the ObjEntity
     * that services the specified data Object
     * @return the required ObjEntity, or null if none matches the specifier
     */
    public synchronized ObjEntity lookupObjEntity(DataObject dataObject) {
        return this._lookupObjEntity(dataObject.getClass());
    }

    /**
     * Internal usage only - provides the type-unsafe implementation which services
     * the three typesafe public lookupObjEntity methods
     * Looks in the DataMap's that this object was created with for the ObjEntity that maps to the
     * specified object. Object may be a Entity name, DataObject instance or DataObject class
     * (Class object for a class which implements the DataObject interface)
     *
     * @return the required ObjEntity or null if there is none that matches the specifier
     */
    protected ObjEntity _lookupObjEntity(Object object) {
        if (object instanceof ObjEntity) {
            return (ObjEntity) object;
        }

        if (object instanceof DataObject) {
            object = object.getClass();
        }

        ObjEntity result = (ObjEntity) objEntityCache.get(object);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            constructCache();
            result = (ObjEntity) objEntityCache.get(object);
        }
        return result;
    }

    /**
     * Looks up the ObjEntity for the given query by using the query's getRoot method and passing to lookupObjEntity
     * @return the root ObjEntity of the query
     * @throws CayenneRuntimeException if the root of the query is a DbEntity (it is not reliably possible to map
     * from a DbEntity to an ObjEntity as a DbEntity may be the source for multiple ObjEntities.  It is not safe
     * to rely on such behaviour).
     */
    public synchronized ObjEntity lookupObjEntity(Query q) {

        // a special case of ProcedureQuery ...
        // TODO: should really come up with some generic way of doing this...
        // e.g. all queries may separate the notion of root from the notion
        // of result type

        Object root = (q instanceof ProcedureQuery) ? ((ProcedureQuery) q)
                .getResultClass(Configuration.getResourceLoader()) : q.getRoot();

        if (root instanceof DbEntity) {
            throw new CayenneRuntimeException(
                    "Cannot safely resolve the ObjEntity for the query "
                            + q
                            + " because the root of the query is a DbEntity");
        }
        else if (root instanceof ObjEntity) {
            return (ObjEntity) root;
        }
        else if (root instanceof Class) {
            return this.lookupObjEntity((Class) root);
        }
        else if (root instanceof String) {
            return this.lookupObjEntity((String) root);
        }
        else if (root instanceof DataObject) {
            return this.lookupObjEntity((DataObject) root);
        }

        return null;
    }

    /**
     * Searches for the named query associated with the ObjEntity corresponding to the
     * Java class specified. Returns such query if found, null otherwise.
     * 
     * @since 1.1 return type is Query instead of SelectQuery
     * @deprecated Since 1.1 use {@link #lookupQuery(String)}, since queries may not be
     *             associated with Entities.
     */
    public Query lookupQuery(Class queryRoot, String queryName) {
        Entity ent = lookupObjEntity(queryRoot);
        return (ent != null) ? ent.getQuery(queryName) : null;
    }

    /**
     * Returns a named query or null if no query exists for a given name.
     * 
     * @since 1.1
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

    public Procedure lookupProcedure(Query q) {
        Object root = q.getRoot();
        if (root instanceof Procedure) {
            return (Procedure) root;
        }
        else if (root instanceof String) {
            return this.lookupProcedure((String) root);
        }
        return null;
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
     * Searches for DataMap that holds Query root object.
     * 
     * @since 1.1 
     */
    public synchronized DataMap lookupDataMap(Query q) {
        if(q.getRoot() instanceof DataMap) {
            return (DataMap) q.getRoot();
        }
        
        DbEntity entity = lookupDbEntity(q);
        if (entity != null) {
            return entity.getDataMap();
        }

        // try procedure
        Procedure procedure = lookupProcedure(q);
        return (procedure != null) ? procedure.getDataMap() : null;
    }

    /**
     * @since 1.1
     */
    public boolean isIndexedByClass() {
        return indexedByClass;
    }

    /**
     * @since 1.1
     */
    public void setIndexedByClass(boolean b) {
        indexedByClass = b;
    }
}
