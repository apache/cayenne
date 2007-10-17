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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.DbAttributeListener;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.DbRelationshipListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjAttributeListener;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.map.event.ObjRelationshipListener;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Stores a collection of related mapping objects that describe database and object layers
 * of an application. DataMap contains DbEntities mapping database tables, ObjEntities -
 * mapping persistent Java classes, Procedures - mapping database stored procedures.
 * 
 * @author Michael Shengaout
 * @author Andrus Adamchik
 * @author Craig Miskell
 */
public class DataMap implements Serializable, XMLSerializable, MappingNamespace,
        DbEntityListener, DbAttributeListener, DbRelationshipListener, ObjEntityListener,
        ObjAttributeListener, ObjRelationshipListener {

    /**
     * Defines whether a DataMap supports client entities.
     * 
     * @since 1.2
     */
    public static final String CLIENT_SUPPORTED_PROPERTY = "clientSupported";

    /**
     * Defines the name of the property for default client Java class package.
     * 
     * @since 1.2
     */
    public static final String DEFAULT_CLIENT_PACKAGE_PROPERTY = "defaultClientPackage";

    /**
     * Defines the name of the property for default DB schema.
     * 
     * @since 1.1
     */
    public static final String DEFAULT_SCHEMA_PROPERTY = "defaultSchema";

    /**
     * Defines the name of the property for default Java class package.
     * 
     * @since 1.1
     */
    public static final String DEFAULT_PACKAGE_PROPERTY = "defaultPackage";

    /**
     * Defines the name of the property for default DB schema.
     * 
     * @since 1.1
     */
    public static final String DEFAULT_SUPERCLASS_PROPERTY = "defaultSuperclass";

    /**
     * Defines the name of the property for default DB schema.
     * 
     * @since 1.1
     */
    public static final String DEFAULT_LOCK_TYPE_PROPERTY = "defaultLockType";

    protected String name;
    protected String location;
    protected MappingNamespace namespace;

    protected String defaultSchema;
    protected String defaultPackage;
    protected String defaultSuperclass;
    protected int defaultLockType;

    protected boolean clientSupported;
    protected String defaultClientPackage;

    private SortedMap embeddablesMap;
    private SortedMap entityListenersMap;
    private SortedMap objEntityMap;
    private SortedMap dbEntityMap;
    private SortedMap procedureMap;
    private SortedMap queryMap;

    private List defaultEntityListeners;

    /**
     * Creates a new unnamed DataMap.
     */
    public DataMap() {
        this(null);
    }

    /**
     * Creates a new named DataMap.
     */
    public DataMap(String mapName) {
        this(mapName, Collections.EMPTY_MAP);
    }

    public DataMap(String mapName, Map properties) {
        embeddablesMap = new TreeMap();
        entityListenersMap = new TreeMap();
        objEntityMap = new TreeMap();
        dbEntityMap = new TreeMap();
        procedureMap = new TreeMap();
        queryMap = new TreeMap();
        defaultEntityListeners = new ArrayList(3);

        setName(mapName);
        initWithProperties(properties);
    }

    /**
     * Performs DataMap initialization from a set of properties, using defaults for the
     * missing properties.
     * 
     * @since 1.1
     */
    public void initWithProperties(Map properties) {
        // must init defaults even if properties are empty
        if (properties == null) {
            properties = Collections.EMPTY_MAP;
        }

        Object lockType = properties.get(DEFAULT_LOCK_TYPE_PROPERTY);
        Object packageName = properties.get(DEFAULT_PACKAGE_PROPERTY);
        Object schema = properties.get(DEFAULT_SCHEMA_PROPERTY);
        Object superclass = properties.get(DEFAULT_SUPERCLASS_PROPERTY);
        Object clientEntities = properties.get(CLIENT_SUPPORTED_PROPERTY);
        Object clientPackageName = properties.get(DEFAULT_CLIENT_PACKAGE_PROPERTY);

        this.defaultLockType = "optimistic".equals(lockType)
                ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                : ObjEntity.LOCK_TYPE_NONE;

        this.defaultPackage = (packageName != null) ? packageName.toString() : null;
        this.defaultSchema = (schema != null) ? schema.toString() : null;
        this.defaultSuperclass = (superclass != null) ? superclass.toString() : null;
        this.clientSupported = (clientEntities != null) ? "true"
                .equalsIgnoreCase(clientEntities.toString()) : false;
        this.defaultClientPackage = (clientPackageName != null) ? clientPackageName
                .toString() : null;
    }

    /**
     * Returns a DataMap stripped of any server-side information, such as DbEntity
     * mapping, or ObjEntities that are not allowed in the client tier. Returns null if
     * this DataMap as a whole does not support client tier persistence.
     * 
     * @since 1.2
     */
    public DataMap getClientDataMap(EntityResolver serverResolver) {
        if (!isClientSupported()) {
            return null;
        }

        DataMap clientMap = new DataMap(getName());

        // create client entities for entities
        Iterator entities = getObjEntities().iterator();
        while (entities.hasNext()) {
            ObjEntity entity = (ObjEntity) entities.next();
            if (entity.isClientAllowed()) {
                clientMap.addObjEntity(entity.getClientEntity());
            }
        }

        // create proxies for named queries
        Iterator queries = getQueries().iterator();
        while (queries.hasNext()) {
            Query q = (Query) queries.next();
            NamedQuery proxy = new NamedQuery(q.getName());
            proxy.setName(q.getName());

            // resolve metadata so that client can have access to it without knowing about
            // the server query.
            proxy.initMetadata(q.getMetaData(serverResolver));
            clientMap.addQuery(proxy);
        }

        return clientMap;
    }

    /**
     * Prints itself as a well-formed complete XML document. In comparison,
     * {@link #encodeAsXML(XMLEncoder)}stores DataMap assuming it is a part of a bigger
     * document.
     * 
     * @since 1.1
     */
    public void encodeAsXML(PrintWriter pw) {
        XMLEncoder encoder = new XMLEncoder(pw, "\t");
        encoder.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        encodeAsXML(encoder);
    }

    /**
     * Prints itself as XML to the provided PrintWriter.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<data-map project-version=\"");
        encoder.print(String.valueOf(Project.CURRENT_PROJECT_VERSION));
        encoder.println("\">");

        encoder.indent(1);

        // properties
        if (defaultLockType == ObjEntity.LOCK_TYPE_OPTIMISTIC) {
            encoder.printProperty(DEFAULT_LOCK_TYPE_PROPERTY, "optimistic");
        }

        if (!Util.isEmptyString(defaultPackage)) {
            encoder.printProperty(DEFAULT_PACKAGE_PROPERTY, defaultPackage);
        }

        if (!Util.isEmptyString(defaultSchema)) {
            encoder.printProperty(DEFAULT_SCHEMA_PROPERTY, defaultSchema);
        }

        if (!Util.isEmptyString(defaultSuperclass)) {
            encoder.printProperty(DEFAULT_SUPERCLASS_PROPERTY, defaultSuperclass);
        }

        if (clientSupported) {
            encoder.printProperty(CLIENT_SUPPORTED_PROPERTY, "true");
        }

        if (!Util.isEmptyString(defaultClientPackage)) {
            encoder.printProperty(DEFAULT_CLIENT_PACKAGE_PROPERTY, defaultClientPackage);
        }

        // embeddables
        encoder.print(getEmbeddableMap());

        // procedures
        encoder.print(getProcedureMap());

        // DbEntities
        boolean hasDerived = false;
        Iterator dbEntities = getDbEntityMap().entrySet().iterator();
        while (dbEntities.hasNext()) {
            Map.Entry entry = (Map.Entry) dbEntities.next();
            DbEntity dbe = (DbEntity) entry.getValue();

            // skip derived, store them after regular DbEntities
            if (dbe instanceof DerivedDbEntity) {
                hasDerived = true;
            }
            else {
                dbe.encodeAsXML(encoder);
            }
        }

        // DerivedDbEntities
        if (hasDerived) {
            Iterator derivedDbEntities = getDbEntityMap().entrySet().iterator();
            while (derivedDbEntities.hasNext()) {
                Map.Entry entry = (Map.Entry) derivedDbEntities.next();
                DbEntity dbe = (DbEntity) entry.getValue();

                // only store derived...
                if (dbe instanceof DerivedDbEntity) {
                    dbe.encodeAsXML(encoder);
                }
            }
        }

        // others...
        encoder.print(getObjEntityMap());
        encodeDBRelationshipsAsXML(getDbEntityMap(), encoder);
        encodeOBJRelationshipsAsXML(getObjEntityMap(), encoder);
        encoder.print(getQueryMap());

        encoder.indent(-1);
        encoder.println("</data-map>");
    }

    // stores relationships of for the map of entities
    private final void encodeDBRelationshipsAsXML(Map entityMap, XMLEncoder encoder) {
        Iterator it = entityMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Entity entity = (Entity) entry.getValue();

            // filter out synthetic
            Iterator relationships = entity.getRelationships().iterator();
            while (relationships.hasNext()) {
                Relationship relationship = (Relationship) relationships.next();
                if (!relationship.isRuntime()) {
                    relationship.encodeAsXML(encoder);
                }
            }
        }
    }

    // stores relationships of for the map of entities
    private final void encodeOBJRelationshipsAsXML(Map entityMap, XMLEncoder encoder) {
        Iterator it = entityMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ObjEntity entity = (ObjEntity) entry.getValue();
            
            // filter out synthetic
            Iterator relationships = entity.getDeclaredRelationships().iterator();
            while (relationships.hasNext()) {
                Relationship relationship = (Relationship) relationships.next();
                if (!relationship.isRuntime()) {
                    relationship.encodeAsXML(encoder);
                }
            }
        }
    }

    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).toString();
    }

    /**
     * Returns the name of this DataMap.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Adds all Object and DB entities and Queries from another map to this map.
     * Overwrites all existing entities and queries with the new ones.
     * <p>
     * <i>TODO: will need to implement advanced merge that allows different policies for
     * overwriting entities / queries. </i>
     * </p>
     */
    public void mergeWithDataMap(DataMap map) {
        Iterator dbs = new ArrayList(map.getDbEntities()).iterator();
        while (dbs.hasNext()) {
            DbEntity ent = (DbEntity) dbs.next();
            this.removeDbEntity(ent.getName());
            this.addDbEntity(ent);
        }

        Iterator objs = new ArrayList(map.getObjEntities()).iterator();
        while (objs.hasNext()) {
            ObjEntity ent = (ObjEntity) objs.next();
            this.removeObjEntity(ent.getName());
            this.addObjEntity(ent);
        }

        Iterator queries = new ArrayList(map.getQueries()).iterator();
        while (queries.hasNext()) {
            Query query = (Query) queries.next();
            this.removeQuery(query.getName());
            this.addQuery(query);
        }
    }

    /**
     * Returns "location" property value. Location is abstract and can depend on how the
     * DataMap was loaded. E.g. location can be a File on the filesystem or a location
     * within a JAR.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets "location" property.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns a sorted unmodifiable map of ObjEntities contained in this DataMap, keyed
     * by ObjEntity name.
     */
    public SortedMap getObjEntityMap() {
        return Collections.unmodifiableSortedMap(objEntityMap);
    }

    /**
     * Returns a sorted unmodifiable map of DbEntities contained in this DataMap, keyed by
     * DbEntity name.
     */
    public SortedMap getDbEntityMap() {
        return Collections.unmodifiableSortedMap(dbEntityMap);
    }

    /**
     * Returns a named query associated with this DataMap.
     * 
     * @since 1.1
     */
    public Query getQuery(String queryName) {
        Query query = (Query) queryMap.get(queryName);
        if (query != null) {
            return query;
        }

        return namespace != null ? namespace.getQuery(queryName) : null;
    }

    /**
     * Stores a query under its name.
     * 
     * @since 1.1
     */
    public void addQuery(Query query) {
        if (query == null) {
            throw new NullPointerException("Can't add null query.");
        }

        if (query.getName() == null) {
            throw new NullPointerException("Query name can't be null.");
        }

        // TODO: change method signature to return replaced procedure and make sure the
        // Modeler handles it...
        Object existingQuery = queryMap.get(query.getName());
        if (existingQuery != null) {
            if (existingQuery == query) {
                return;
            }
            else {
                throw new IllegalArgumentException("An attempt to override entity '"
                        + query.getName());
            }
        }

        queryMap.put(query.getName(), query);
    }

    /**
     * Removes a named query from the DataMap.
     * 
     * @since 1.1
     */
    public void removeQuery(String queryName) {
        queryMap.remove(queryName);
    }

    /**
     * Removes all stored embeddable objects from the map.
     * 
     * @since 3.0
     */
    public void clearEmbeddables() {
        embeddablesMap.clear();
    }

    /**
     * Removes all stored entity listeners from the map.
     * 
     * @since 3.0
     */
    public void clearEntityListeners() {
        entityListenersMap.clear();
    }

    /**
     * @since 1.1
     */
    public void clearQueries() {
        queryMap.clear();
    }

    /**
     * @since 1.2
     */
    public void clearObjEntities() {
        objEntityMap.clear();
    }

    /**
     * @since 1.2
     */
    public void clearDbEntities() {
        dbEntityMap.clear();
    }

    /**
     * @since 1.2
     */
    public void clearProcedures() {
        procedureMap.clear();
    }

    /**
     * @since 1.1
     */
    public SortedMap getQueryMap() {
        return Collections.unmodifiableSortedMap(queryMap);
    }

    /**
     * Returns an unmodifiable collection of mapped queries.
     * 
     * @since 1.1
     */
    public Collection getQueries() {
        return Collections.unmodifiableCollection(queryMap.values());
    }

    /**
     * Adds a default entity listener that should be notified of certain events on all
     * entities.
     */
    public void addEntityListener(EntityListener listener) {
        if (listener == null) {
            throw new NullPointerException("Null EntityListener");
        }

        if (listener.getClassName() == null) {
            throw new NullPointerException(
                    "Attempt to add EntityListener with no class name.");
        }

        // TODO: change method signature to return replaced el and make sure the
        // Modeler handles it...
        Object existing = embeddablesMap.get(listener.getClassName());
        if (existing != null) {
            if (existing == listener) {
                return;
            }
            else {
                throw new IllegalArgumentException("An attempt to override listener '"
                        + listener.getClassName());
            }
        }

        entityListenersMap.put(listener.getClassName(), listener);
    }

    /**
     * Removes an {@link EntityListener} descriptor with matching class name from entity
     * listeners and default entity listeners.
     * 
     * @since 3.0
     */
    public void removeEntityListener(String className) {
        if (entityListenersMap.remove(className) != null) {
            removeDefaultEntityListener(className);
        }
    }

    /**
     * Adds an embeddable object to the DataMap.
     * 
     * @since 3.0
     */
    public void addEmbeddable(Embeddable embeddable) {
        if (embeddable == null) {
            throw new NullPointerException("Null embeddable");
        }

        if (embeddable.getClassName() == null) {
            throw new NullPointerException(
                    "Attempt to add Embeddable with no class name.");
        }

        // TODO: change method signature to return replaced entity and make sure the
        // Modeler handles it...
        Object existing = embeddablesMap.get(embeddable.getClassName());
        if (existing != null) {
            if (existing == embeddable) {
                return;
            }
            else {
                throw new IllegalArgumentException("An attempt to override embeddable '"
                        + embeddable.getClassName());
            }
        }

        embeddablesMap.put(embeddable.getClassName(), embeddable);
    }

    /**
     * Adds a new ObjEntity to this DataMap.
     */
    public void addObjEntity(ObjEntity entity) {
        if (entity.getName() == null) {
            throw new NullPointerException("Attempt to add ObjEntity with no name.");
        }

        // TODO: change method signature to return replaced entity and make sure the
        // Modeler handles it...
        Object existingEntity = objEntityMap.get(entity.getName());
        if (existingEntity != null) {
            if (existingEntity == entity) {
                return;
            }
            else {
                throw new IllegalArgumentException("An attempt to override entity '"
                        + entity.getName());
            }
        }

        objEntityMap.put(entity.getName(), entity);
        entity.setDataMap(this);
    }

    /**
     * Adds a new DbEntity to this DataMap.
     */
    public void addDbEntity(DbEntity entity) {
        if (entity.getName() == null) {
            throw new NullPointerException("Attempt to add DbEntity with no name.");
        }

        // TODO: change method signature to return replaced entity and make sure the
        // Modeler handles it...
        Object existingEntity = dbEntityMap.get(entity.getName());
        if (existingEntity != null) {
            if (existingEntity == entity) {
                return;
            }
            else {
                throw new IllegalArgumentException("An attempt to override entity '"
                        + entity.getName());
            }
        }

        dbEntityMap.put(entity.getName(), entity);
        entity.setDataMap(this);
    }

    /**
     * Returns an unmodifiable collection of ObjEntities stored in this DataMap.
     */
    public Collection getObjEntities() {
        return Collections.unmodifiableCollection(objEntityMap.values());
    }

    /**
     * @since 3.0
     */
    public Map getEmbeddableMap() {
        return Collections.unmodifiableMap(embeddablesMap);
    }

    /**
     * Returns a collection of {@link Embeddable} mappings stored in the DataMap.
     * 
     * @since 3.0
     */
    public Collection getEmbeddables() {
        return Collections.unmodifiableCollection(embeddablesMap.values());
    }

    /**
     * @since 3.0
     */
    public Embeddable getEmbeddable(String className) {
        Embeddable e = (Embeddable) embeddablesMap.get(className);
        if (e != null) {
            return e;
        }

        return namespace != null ? namespace.getEmbeddable(className) : null;
    }

    /**
     * @since 3.0
     */
    public Map getEntityListenersMap() {
        return Collections.unmodifiableMap(entityListenersMap);
    }

    /**
     * Returns a collection of {@link EntityListener} mappings stored in the DataMap.
     * 
     * @since 3.0
     */
    public Collection getEntityListeners() {
        return Collections.unmodifiableCollection(entityListenersMap.values());
    }

    /**
     * @since 3.0
     */
    public EntityListener getEntityListener(String className) {
        EntityListener e = (EntityListener) entityListenersMap.get(className);
        if (e != null) {
            return e;
        }

        return namespace != null ? namespace.getEntityListener(className) : null;
    }

    /**
     * Returns an unmodifiable list of default {@link EntityListener} objects. Note that
     * since the order of listeners is significant a list, not just a generic Collection
     * is returned.
     * 
     * @since 3.0
     */
    public List getDefaultEntityListeners() {
        return Collections.unmodifiableList(defaultEntityListeners);
    }

    /**
     * Adds a new EntityListener.
     * 
     * @since 3.0
     * @throws IllegalArgumentException if a listener for the same class name is already
     *             registered.
     */
    public void addDefaultEntityListener(EntityListener listener) {
        Iterator it = defaultEntityListeners.iterator();
        while (it.hasNext()) {
            EntityListener next = (EntityListener) it.next();
            if (listener.getClassName().equals(next.getClassName())) {
                throw new IllegalArgumentException("Duplicate default listener for "
                        + next.getClassName());
            }
        }

        defaultEntityListeners.add(listener);
    }

    /**
     * Removes a listener matching class name.
     * 
     * @since 3.0
     */
    public void removeDefaultEntityListener(String className) {
        Iterator it = defaultEntityListeners.iterator();
        while (it.hasNext()) {
            EntityListener next = (EntityListener) it.next();
            if (className.equals(next.getClassName())) {
                it.remove();
                break;
            }
        }
    }

    /**
     * @since 3.0
     */
    public EntityListener getDefaultEntityListener(String className) {
        Iterator it = defaultEntityListeners.iterator();
        while (it.hasNext()) {
            EntityListener next = (EntityListener) it.next();
            if (className.equals(next.getClassName())) {
                return next;
            }
        }

        return null;
    }

    /**
     * Returns all DbEntities in this DataMap.
     */
    public Collection getDbEntities() {
        return Collections.unmodifiableCollection(dbEntityMap.values());
    }

    /**
     * Returns DbEntity matching the <code>name</code> parameter. No dependencies will
     * be searched.
     */
    public DbEntity getDbEntity(String dbEntityName) {
        DbEntity entity = (DbEntity) dbEntityMap.get(dbEntityName);

        if (entity != null) {
            return entity;
        }

        return namespace != null ? namespace.getDbEntity(dbEntityName) : null;
    }

    /**
     * Returns an ObjEntity for a DataObject class name.
     * 
     * @since 1.1
     */
    public ObjEntity getObjEntityForJavaClass(String javaClassName) {
        if (javaClassName == null) {
            return null;
        }

        Iterator it = getObjEntityMap().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ObjEntity entity = (ObjEntity) entry.getValue();
            if (javaClassName.equals(entity.getClassName())) {
                return entity;
            }
        }

        return null;
    }

    /**
     * Returns an ObjEntity for a given name. If it is not found in this DataMap, it will
     * search a parent EntityNamespace.
     */
    public ObjEntity getObjEntity(String objEntityName) {
        ObjEntity entity = (ObjEntity) objEntityMap.get(objEntityName);
        if (entity != null) {
            return entity;
        }

        return namespace != null ? namespace.getObjEntity(objEntityName) : null;
    }

    /**
     * Returns all ObjEntities mapped to the given DbEntity.
     */
    public Collection getMappedEntities(DbEntity dbEntity) {
        if (dbEntity == null) {
            return Collections.EMPTY_LIST;
        }

        Collection allEntities = (namespace != null)
                ? namespace.getObjEntities()
                : getObjEntities();

        if (allEntities.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        Collection result = new ArrayList();
        Iterator iter = allEntities.iterator();
        while (iter.hasNext()) {
            ObjEntity objEnt = (ObjEntity) iter.next();
            if (objEnt.getDbEntity() == dbEntity) {
                result.add(objEnt);
            }
        }

        return result;
    }

    /**
     * Removes an {@link Embeddable} descriptor with matching class name.
     * 
     * @since 3.0
     */
    public void removeEmbeddable(String className) {
        // TODO: andrus, 1/25/2007 - clean up references like removeDbEntity does.
        embeddablesMap.remove(className);
    }

    /**
     * "Dirty" remove of the DbEntity from the data map.
     */
    public void removeDbEntity(String dbEntityName) {
        removeDbEntity(dbEntityName, false);
    }

    /**
     * Removes DbEntity from the DataMap. If <code>clearDependencies</code> is true, all
     * DbRelationships that reference this entity are also removed. ObjEntities that rely
     * on this entity are cleaned up.
     * 
     * @since 1.1
     */
    public void removeDbEntity(String dbEntityName, boolean clearDependencies) {
        DbEntity dbEntityToDelete = (DbEntity) dbEntityMap.remove(dbEntityName);

        if (dbEntityToDelete != null && clearDependencies) {
            Iterator dbEnts = this.getDbEntities().iterator();
            while (dbEnts.hasNext()) {
                DbEntity dbEnt = (DbEntity) dbEnts.next();
                // take a copy since we're going to modifiy the entity
                Iterator rels = new ArrayList(dbEnt.getRelationships()).iterator();
                while (rels.hasNext()) {
                    DbRelationship rel = (DbRelationship) rels.next();
                    if (dbEntityName.equals(rel.getTargetEntityName())) {
                        dbEnt.removeRelationship(rel.getName());
                    }
                }
            }

            // Remove all obj relationships referencing removed DbRelationships.
            Iterator objEnts = this.getObjEntities().iterator();
            while (objEnts.hasNext()) {
                ObjEntity objEnt = (ObjEntity) objEnts.next();
                if (objEnt.getDbEntity() == dbEntityToDelete) {
                    objEnt.clearDbMapping();
                }
                else {
                    Iterator iter = objEnt.getRelationships().iterator();
                    while (iter.hasNext()) {
                        ObjRelationship rel = (ObjRelationship) iter.next();
                        Iterator dbRels = rel.getDbRelationships().iterator();
                        while (dbRels.hasNext()) {
                            DbRelationship dbRel = (DbRelationship) dbRels.next();
                            if (dbRel.getTargetEntity() == dbEntityToDelete) {
                                rel.clearDbRelationships();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * "Dirty" remove of the ObjEntity from the data map.
     */
    public void removeObjEntity(String objEntityName) {
        removeObjEntity(objEntityName, false);
    }

    /**
     * Removes ObjEntity from the DataMap. If <code>clearDependencies</code> is true,
     * all ObjRelationships that reference this entity are also removed.
     * 
     * @since 1.1
     */
    public void removeObjEntity(String objEntityName, boolean clearDependencies) {
        ObjEntity entity = (ObjEntity) objEntityMap.remove(objEntityName);

        if (entity != null && clearDependencies) {

            // remove relationships that point to this entity
            Iterator entities = getObjEntities().iterator();
            while (entities.hasNext()) {
                ObjEntity ent = (ObjEntity) entities.next();
                // take a copy since we're going to modifiy the entity
                Iterator rels = new ArrayList(ent.getRelationships()).iterator();
                while (rels.hasNext()) {
                    ObjRelationship rel = (ObjRelationship) rels.next();
                    if (objEntityName.equals(rel.getTargetEntityName())
                            || objEntityName.equals(rel.getTargetEntityName())) {
                        ent.removeRelationship(rel.getName());
                    }
                }
            }
        }
    }

    /**
     * Returns stored procedures associated with this DataMap.
     */
    public Collection getProcedures() {
        return Collections.unmodifiableCollection(procedureMap.values());
    }

    /**
     * Returns a Procedure for a given name or null if no such procedure exists. If
     * Procedure is not found in this DataMap, a parent EntityNamcespace is searched.
     */
    public Procedure getProcedure(String procedureName) {
        Procedure procedure = (Procedure) procedureMap.get(procedureName);
        if (procedure != null) {
            return procedure;
        }

        return namespace != null ? namespace.getProcedure(procedureName) : null;
    }

    /**
     * Adds stored procedure to the list of procedures. If there is another procedure
     * registered under the same name, throws an IllegalArgumentException.
     */
    public void addProcedure(Procedure procedure) {
        if (procedure.getName() == null) {
            throw new NullPointerException("Attempt to add procedure with no name.");
        }

        // TODO: change method signature to return replaced procedure and make sure the
        // Modeler handles it...
        Object existingProcedure = procedureMap.get(procedure.getName());
        if (existingProcedure != null) {
            if (existingProcedure == procedure) {
                return;
            }
            else {
                throw new IllegalArgumentException("An attempt to override procedure '"
                        + procedure.getName());
            }
        }

        procedureMap.put(procedure.getName(), procedure);
        procedure.setDataMap(this);
    }

    public void removeProcedure(String name) {
        procedureMap.remove(name);
    }

    /**
     * Returns a sorted unmodifiable map of Procedures in this DataMap keyed by name.
     */
    public SortedMap getProcedureMap() {
        return Collections.unmodifiableSortedMap(procedureMap);
    }

    /**
     * Returns a parent namespace where this DataMap resides. Parent EntityNamespace is
     * used to establish relationships with entities in other DataMaps.
     * 
     * @since 1.1
     */
    public MappingNamespace getNamespace() {
        return namespace;
    }

    /**
     * Sets a parent namespace where this DataMap resides. Parent EntityNamespace is used
     * to establish relationships with entities in other DataMaps.
     * 
     * @since 1.1
     */
    public void setNamespace(MappingNamespace namespace) {
        this.namespace = namespace;
    }

    /**
     * @since 1.1
     */
    public int getDefaultLockType() {
        return defaultLockType;
    }

    /**
     * @since 1.1
     */
    public void setDefaultLockType(int defaultLockType) {
        this.defaultLockType = defaultLockType;
    }

    /**
     * @since 1.2
     */
    public boolean isClientSupported() {
        return clientSupported;
    }

    /**
     * @since 1.2
     */
    public void setClientSupported(boolean clientSupport) {
        this.clientSupported = clientSupport;
    }

    /**
     * Returns default client package.
     * 
     * @since 1.2
     */
    public String getDefaultClientPackage() {
        return defaultClientPackage;
    }

    /**
     * @since 1.2
     */
    public void setDefaultClientPackage(String defaultClientPackage) {
        this.defaultClientPackage = defaultClientPackage;
    }

    /**
     * @since 1.1
     */
    public String getDefaultPackage() {
        return defaultPackage;
    }

    /**
     * @since 1.1
     */
    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }

    /**
     * @since 1.1
     */
    public String getDefaultSchema() {
        return defaultSchema;
    }

    /**
     * @since 1.1
     */
    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }

    /**
     * @since 1.1
     */
    public String getDefaultSuperclass() {
        return defaultSuperclass;
    }

    /**
     * @since 1.1
     */
    public void setDefaultSuperclass(String defaultSuperclass) {
        this.defaultSuperclass = defaultSuperclass;
    }

    /**
     * DbEntity property changed. May be name, attribute or relationship added or removed,
     * etc. Attribute and relationship property changes are handled in respective
     * listeners.
     * 
     * @since 1.2
     */
    public void dbEntityChanged(EntityEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof DbEntity) {
            ((DbEntity) entity).dbEntityChanged(e);

            // finish up the name change here because we
            // do not have direct access to the dbEntityMap
            if (e.isNameChange()) {
                // remove the entity from the map with the old name
                dbEntityMap.remove(e.getOldName());

                // add the entity back in with the new name
                dbEntityMap.put(e.getNewName(), entity);

                // important - clear parent namespace:
                MappingNamespace ns = getNamespace();
                if (ns instanceof EntityResolver) {
                    ((EntityResolver) ns).clearCache();
                }
            }
        }
    }

    /** New entity has been created/added. */
    public void dbEntityAdded(EntityEvent e) {
        // does nothing currently
    }

    /** Entity has been removed. */
    public void dbEntityRemoved(EntityEvent e) {
        // does nothing currently
    }

    /** Attribute property changed. */
    public void dbAttributeChanged(AttributeEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof DbEntity) {
            ((DbEntity) entity).dbAttributeChanged(e);
        }
    }

    /** New attribute has been created/added. */
    public void dbAttributeAdded(AttributeEvent e) {
        // does nothing currently
    }

    /** Attribute has been removed. */
    public void dbAttributeRemoved(AttributeEvent e) {
        // does nothing currently
    }

    /** Relationship property changed. */
    public void dbRelationshipChanged(RelationshipEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof DbEntity) {
            ((DbEntity) entity).dbRelationshipChanged(e);
        }
    }

    /** Relationship has been created/added. */
    public void dbRelationshipAdded(RelationshipEvent e) {
        // does nothing currently
    }

    /** Relationship has been removed. */
    public void dbRelationshipRemoved(RelationshipEvent e) {
        // does nothing currently
    }

    /**
     * ObjEntity property changed. May be name, attribute or relationship added or
     * removed, etc. Attribute and relationship property changes are handled in respective
     * listeners.
     * 
     * @since 1.2
     */
    public void objEntityChanged(EntityEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof ObjEntity) {
            ((ObjEntity) entity).objEntityChanged(e);

            // finish up the name change here because we
            // do not have direct access to the objEntityMap
            if (e.isNameChange()) {
                // remove the entity from the map with the old name
                objEntityMap.remove(e.getOldName());

                // add the entity back in with the new name
                objEntityMap.put(e.getNewName(), entity);

                // important - clear parent namespace:
                MappingNamespace ns = getNamespace();
                if (ns instanceof EntityResolver) {
                    ((EntityResolver) ns).clearCache();
                }
            }
        }
    }

    /** New entity has been created/added. */
    public void objEntityAdded(EntityEvent e) {
        // does nothing currently
    }

    /** Entity has been removed. */
    public void objEntityRemoved(EntityEvent e) {
        // does nothing currently
    }

    /** Attribute property changed. */
    public void objAttributeChanged(AttributeEvent e) {
        // does nothing currently
    }

    /** New attribute has been created/added. */
    public void objAttributeAdded(AttributeEvent e) {
        // does nothing currently
    }

    /** Attribute has been removed. */
    public void objAttributeRemoved(AttributeEvent e) {
        // does nothing currently
    }

    /** Relationship property changed. */
    public void objRelationshipChanged(RelationshipEvent e) {
        // does nothing currently
    }

    /** Relationship has been created/added. */
    public void objRelationshipAdded(RelationshipEvent e) {
        // does nothing currently
    }

    /** Relationship has been removed. */
    public void objRelationshipRemoved(RelationshipEvent e) {
        // does nothing currently
    }
}
