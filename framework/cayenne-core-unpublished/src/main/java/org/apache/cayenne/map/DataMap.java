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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.util.ToStringBuilder;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Stores a collection of related mapping objects that describe database and
 * object layers of an application. DataMap contains DbEntities mapping database
 * tables, ObjEntities - mapping persistent Java classes, Procedures - mapping
 * database stored procedures.
 */
public class DataMap implements Serializable, ConfigurationNode, XMLSerializable, MappingNamespace, DbEntityListener,
        ObjEntityListener, Comparable<DataMap> {

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
     * Defines the name of the property for default client Java superclass.
     * 
     * @since 3.0
     */
    public static final String DEFAULT_CLIENT_SUPERCLASS_PROPERTY = "defaultClientSuperclass";

    /**
     * Defines the name of the property for default DB catalog.
     * 
     * @since 3.2
     */
    public static final String DEFAULT_CATALOG_PROPERTY = "defaultCatalog";

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
     * Defines the name of the property for default Java superclass.
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

    public static final String DEFAULT_QUOTE_SQL_IDENTIFIERS_PROPERTY = "quoteSqlIdentifiers";

    /**
     * The namespace in which the data map XML file will be created. This is
     * also the URI to locate a copy of the schema document.
     */
    public static final String SCHEMA_XSD = "http://cayenne.apache.org/schema/7/modelMap";

    protected String name;
    protected String location;
    protected MappingNamespace namespace;

    protected Boolean quotingSQLIdentifiers;

    protected String defaultCatalog;
    protected String defaultSchema;
    protected String defaultPackage;

    protected String defaultSuperclass;
    protected int defaultLockType;

    protected boolean clientSupported;
    protected String defaultClientPackage;
    protected String defaultClientSuperclass;

    private SortedMap<String, Embeddable> embeddablesMap;
    private SortedMap<String, ObjEntity> objEntityMap;
    private SortedMap<String, DbEntity> dbEntityMap;
    private SortedMap<String, Procedure> procedureMap;
    private SortedMap<String, Query> queryMap;
    private SortedMap<String, SQLResult> results;

    /**
     * @deprecated since 3.2 unused as listeners are no longer tied to a
     *             DataMap.
     */
    private List<EntityListener> defaultEntityListeners;

    /**
     * @since 3.1
     */
    protected Resource configurationSource;

    /**
     * @since 3.1
     */
    protected DataChannelDescriptor dataChannelDescriptor;

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

    public DataMap(String mapName, Map<String, Object> properties) {
        embeddablesMap = new TreeMap<String, Embeddable>();
        objEntityMap = new TreeMap<String, ObjEntity>();
        dbEntityMap = new TreeMap<String, DbEntity>();
        procedureMap = new TreeMap<String, Procedure>();
        queryMap = new TreeMap<String, Query>();
        defaultEntityListeners = new ArrayList<EntityListener>(3);
        results = new TreeMap<String, SQLResult>();
        setName(mapName);
        initWithProperties(properties);
    }

    /**
     * @since 3.1
     */
    public DataChannelDescriptor getDataChannelDescriptor() {
        return dataChannelDescriptor;
    }

    /**
     * @since 3.1
     */
    public void setDataChannelDescriptor(DataChannelDescriptor dataChannelDescriptor) {
        this.dataChannelDescriptor = dataChannelDescriptor;
    }

    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitDataMap(this);
    }

    /**
     * @since 3.1
     */
    public int compareTo(DataMap o) {
        String o1 = getName();
        String o2 = o.getName();

        if (o1 == null) {
            return (o2 != null) ? -1 : 0;
        } else if (o2 == null) {
            return 1;
        } else {
            return o1.compareTo(o2);
        }
    }

    /**
     * @since 3.0
     */
    public boolean isQuotingSQLIdentifiers() {
        return quotingSQLIdentifiers;
    }

    /**
     * @since 3.0
     */
    public void setQuotingSQLIdentifiers(boolean quotingSqlIdentifiers) {
        this.quotingSQLIdentifiers = quotingSqlIdentifiers;
    }

    /**
     * Performs DataMap initialization from a set of properties, using defaults
     * for the missing properties.
     * 
     * @since 1.1
     */
    public void initWithProperties(Map<String, Object> properties) {
        // must init defaults even if properties are empty
        if (properties == null) {
            properties = Collections.EMPTY_MAP;
        }

        Object lockType = properties.get(DEFAULT_LOCK_TYPE_PROPERTY);
        Object packageName = properties.get(DEFAULT_PACKAGE_PROPERTY);
        Object catalog = properties.get(DEFAULT_CATALOG_PROPERTY);
        Object schema = properties.get(DEFAULT_SCHEMA_PROPERTY);
        Object superclass = properties.get(DEFAULT_SUPERCLASS_PROPERTY);
        Object clientEntities = properties.get(CLIENT_SUPPORTED_PROPERTY);
        Object clientPackageName = properties.get(DEFAULT_CLIENT_PACKAGE_PROPERTY);
        Object clientSuperclass = properties.get(DEFAULT_CLIENT_SUPERCLASS_PROPERTY);
        Object quoteSqlIdentifier = properties.get(DEFAULT_QUOTE_SQL_IDENTIFIERS_PROPERTY);

        this.defaultLockType = "optimistic".equals(lockType) ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                : ObjEntity.LOCK_TYPE_NONE;

        this.defaultPackage = (packageName != null) ? packageName.toString() : null;
        this.quotingSQLIdentifiers = (quoteSqlIdentifier != null) ? "true".equalsIgnoreCase(quoteSqlIdentifier
                .toString()) : false;
        this.defaultSchema = (schema != null) ? schema.toString() : null;
        this.defaultCatalog = (catalog != null) ? catalog.toString() : null;
        this.defaultSuperclass = (superclass != null) ? superclass.toString() : null;
        this.clientSupported = (clientEntities != null) ? "true".equalsIgnoreCase(clientEntities.toString()) : false;
        this.defaultClientPackage = (clientPackageName != null) ? clientPackageName.toString() : null;
        this.defaultClientSuperclass = (clientSuperclass != null) ? clientSuperclass.toString() : null;
    }

    /**
     * Returns a DataMap stripped of any server-side information, such as
     * DbEntity mapping, or ObjEntities that are not allowed in the client tier.
     * Returns null if this DataMap as a whole does not support client tier
     * persistence.
     * 
     * @since 1.2
     */
    public DataMap getClientDataMap(EntityResolver serverResolver) {
        if (!isClientSupported()) {
            return null;
        }

        DataMap clientMap = new DataMap(getName());

        // create client entities for entities
        for (ObjEntity entity : getObjEntities()) {
            if (entity.isClientAllowed()) {
                clientMap.addObjEntity(entity.getClientEntity());
            }
        }

        // create proxies for named queries
        for (Query q : getQueries()) {
            NamedQuery proxy = new NamedQuery(q.getName());
            proxy.setName(q.getName());
            proxy.setDataMap(clientMap);

            // resolve metadata so that client can have access to it without
            // knowing about
            // the server query.
            proxy.initMetadata(q.getMetaData(serverResolver));
            clientMap.addQuery(proxy);
        }

        return clientMap;
    }

    /**
     * Prints itself as XML to the provided PrintWriter.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.println("<data-map xmlns=\"http://cayenne.apache.org/schema/7/modelMap\"");

        encoder.indent(1);
        encoder.println(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        encoder.println(" xsi:schemaLocation=\"" + SCHEMA_XSD + " " + SCHEMA_XSD + ".xsd\"");

        encoder.printProjectVersion();
        encoder.println(">");

        // properties
        if (defaultLockType == ObjEntity.LOCK_TYPE_OPTIMISTIC) {
            encoder.printProperty(DEFAULT_LOCK_TYPE_PROPERTY, "optimistic");
        }

        if (!Util.isEmptyString(defaultPackage)) {
            encoder.printProperty(DEFAULT_PACKAGE_PROPERTY, defaultPackage);
        }

        if (!Util.isEmptyString(defaultCatalog)) {
            encoder.printProperty(DEFAULT_CATALOG_PROPERTY, defaultCatalog);
        }

        if (!Util.isEmptyString(defaultSchema)) {
            encoder.printProperty(DEFAULT_SCHEMA_PROPERTY, defaultSchema);
        }

        if (!Util.isEmptyString(defaultSuperclass)) {
            encoder.printProperty(DEFAULT_SUPERCLASS_PROPERTY, defaultSuperclass);
        }

        if (quotingSQLIdentifiers) {
            encoder.printProperty(DEFAULT_QUOTE_SQL_IDENTIFIERS_PROPERTY, quotingSQLIdentifiers);
        }

        if (clientSupported) {
            encoder.printProperty(CLIENT_SUPPORTED_PROPERTY, "true");
        }

        if (!Util.isEmptyString(defaultClientPackage)) {
            encoder.printProperty(DEFAULT_CLIENT_PACKAGE_PROPERTY, defaultClientPackage);
        }

        if (!Util.isEmptyString(defaultClientSuperclass)) {
            encoder.printProperty(DEFAULT_CLIENT_SUPERCLASS_PROPERTY, defaultClientSuperclass);
        }

        // embeddables
        encoder.print(getEmbeddableMap());

        // procedures
        encoder.print(getProcedureMap());

        // DbEntities
        for (DbEntity dbe : getDbEntityMap().values()) {
            dbe.encodeAsXML(encoder);
        }

        // others...
        encoder.print(getObjEntityMap());
        encodeDBRelationshipsAsXML(getDbEntityMap(), encoder);
        encodeOBJRelationshipsAsXML(getObjEntityMap(), encoder);

        // since Queries are not XMLSerializable by default, check for
        // non-serilaizable
        // queries and throws if they are not..
        for (Query query : getQueries()) {
            if (query instanceof XMLSerializable) {
                ((XMLSerializable) query).encodeAsXML(encoder);
            } else {
                throw new CayenneRuntimeException("Query is not XMLSerilaizable: " + query);
            }
        }

        encoder.indent(-1);
        encoder.println("</data-map>");
    }

    // stores relationships for the map of entities
    private final void encodeDBRelationshipsAsXML(Map<String, DbEntity> entityMap, XMLEncoder encoder) {
        for (Entity entity : entityMap.values()) {
            for (Relationship relationship : entity.getRelationships()) {
                // filter out synthetic
                if (!relationship.isRuntime()) {
                    relationship.encodeAsXML(encoder);
                }
            }
        }
    }

    // stores relationships for the map of entities
    private final void encodeOBJRelationshipsAsXML(Map<String, ObjEntity> entityMap, XMLEncoder encoder) {
        for (ObjEntity entity : entityMap.values()) {
            for (Relationship relationship : entity.getDeclaredRelationships()) {
                // filter out synthetic
                if (!relationship.isRuntime()) {
                    relationship.encodeAsXML(encoder);
                }
            }
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).toString();
    }

    /**
     * Returns the name of this DataMap.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this DataMap.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Adds all Object and DB entities and Queries from another map to this map.
     * Overwrites all existing entities and queries with the new ones.
     * <p>
     * <i>TODO: will need to implement advanced merge that allows different
     * policies for overwriting entities / queries. </i>
     * </p>
     */
    public void mergeWithDataMap(DataMap map) {
        for (DbEntity ent : new ArrayList<DbEntity>(map.getDbEntities())) {
            this.removeDbEntity(ent.getName());
            this.addDbEntity(ent);
        }

        for (ObjEntity ent : new ArrayList<ObjEntity>(map.getObjEntities())) {
            this.removeObjEntity(ent.getName());
            this.addObjEntity(ent);
        }

        for (Query query : new ArrayList<Query>(map.getQueries())) {
            this.removeQuery(query.getName());
            this.addQuery(query);
        }
    }

    /**
     * Returns "location" property value. Location is abstract and can depend on
     * how the DataMap was loaded. E.g. location can be a File on the filesystem
     * or a location within a JAR.
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
     * Returns a sorted unmodifiable map of ObjEntities contained in this
     * DataMap, keyed by ObjEntity name.
     */
    public SortedMap<String, ObjEntity> getObjEntityMap() {
        return Collections.unmodifiableSortedMap(objEntityMap);
    }

    /**
     * Returns a sorted unmodifiable map of DbEntities contained in this
     * DataMap, keyed by DbEntity name.
     */
    public SortedMap<String, DbEntity> getDbEntityMap() {
        return Collections.unmodifiableSortedMap(dbEntityMap);
    }

    /**
     * Returns a named query associated with this DataMap.
     * 
     * @since 1.1
     */
    public Query getQuery(String queryName) {
        Query query = queryMap.get(queryName);
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

        // TODO: change method signature to return replaced procedure and make
        // sure the
        // Modeler handles it...
        Object existingQuery = queryMap.get(query.getName());
        if (existingQuery != null) {
            if (existingQuery == query) {
                return;
            } else {
                throw new IllegalArgumentException("An attempt to override entity '" + query.getName());
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
     * @since 3.0
     */
    public void clearResultSets() {
        results.clear();
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
    public SortedMap<String, Query> getQueryMap() {
        return Collections.unmodifiableSortedMap(queryMap);
    }

    /**
     * Returns an unmodifiable collection of mapped queries.
     * 
     * @since 1.1
     */
    public Collection<Query> getQueries() {
        return Collections.unmodifiableCollection(queryMap.values());
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
            throw new NullPointerException("Attempt to add Embeddable with no class name.");
        }

        // TODO: change method signature to return replaced entity and make sure
        // the
        // Modeler handles it...
        Object existing = embeddablesMap.get(embeddable.getClassName());
        if (existing != null) {
            if (existing == embeddable) {
                return;
            } else {
                throw new IllegalArgumentException("An attempt to override embeddable '" + embeddable.getClassName());
            }
        }

        embeddablesMap.put(embeddable.getClassName(), embeddable);
        embeddable.setDataMap(this);
    }

    /**
     * Adds a named SQLResultSet to the DataMap.
     * 
     * @since 3.0
     */
    public void addResult(SQLResult result) {
        if (result == null) {
            throw new NullPointerException("Null result");
        }

        if (result.getName() == null) {
            throw new NullPointerException("Attempt to add resultSetMapping with no name.");
        }

        Object existing = results.get(result.getName());
        if (existing != null) {
            if (existing == result) {
                return;
            } else {
                throw new IllegalArgumentException("An attempt to override resultSetMapping '" + result.getName());
            }
        }

        results.put(result.getName(), result);
    }

    /**
     * Adds a new ObjEntity to this DataMap.
     */
    public void addObjEntity(ObjEntity entity) {
        if (entity.getName() == null) {
            throw new NullPointerException("Attempt to add ObjEntity with no name.");
        }

        // TODO: change method signature to return replaced entity and make sure
        // the
        // Modeler handles it...
        Object existingEntity = objEntityMap.get(entity.getName());
        if (existingEntity != null) {
            if (existingEntity == entity) {
                return;
            } else {
                throw new IllegalArgumentException("An attempt to override entity '" + entity.getName());
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

        // TODO: change method signature to return replaced entity and make sure
        // the
        // Modeler handles it...
        Object existingEntity = dbEntityMap.get(entity.getName());
        if (existingEntity != null) {
            if (existingEntity == entity) {
                return;
            } else {
                throw new IllegalArgumentException("An attempt to override entity '" + entity.getName());
            }
        }

        dbEntityMap.put(entity.getName(), entity);
        entity.setDataMap(this);
    }

    /**
     * Returns an unmodifiable collection of ObjEntities stored in this DataMap.
     */
    public Collection<ObjEntity> getObjEntities() {
        return Collections.unmodifiableCollection(objEntityMap.values());
    }

    /**
     * @since 3.0
     */
    public Map<String, Embeddable> getEmbeddableMap() {
        return Collections.unmodifiableMap(embeddablesMap);
    }

    /**
     * Returns a collection of {@link Embeddable} mappings stored in the
     * DataMap.
     * 
     * @since 3.0
     */
    public Collection<Embeddable> getEmbeddables() {
        return Collections.unmodifiableCollection(embeddablesMap.values());
    }

    /**
     * @since 3.0
     */
    public Map<String, SQLResult> getResultsMap() {
        return Collections.unmodifiableMap(results);
    }

    /**
     * @since 3.0
     */
    public Collection<SQLResult> getResults() {
        return Collections.unmodifiableCollection(results.values());
    }

    /**
     * @since 3.0
     */
    public Embeddable getEmbeddable(String className) {
        Embeddable e = embeddablesMap.get(className);
        if (e != null) {
            return e;
        }

        return namespace != null ? namespace.getEmbeddable(className) : null;
    }

    /**
     * @since 3.0
     */
    public SQLResult getResult(String name) {
        SQLResult rsMapping = results.get(name);
        if (rsMapping != null) {
            return rsMapping;
        }

        return namespace != null ? namespace.getResult(name) : null;
    }

    /**
     * Returns an unmodifiable list of default {@link EntityListener} objects.
     * Note that since the order of listeners is significant a list, not just a
     * generic Collection is returned.
     * 
     * @since 3.0
     * @deprecated since 3.2 unused as listeners are no longer tied to a
     *             DataMap.
     */
    @Deprecated
    public List<EntityListener> getDefaultEntityListeners() {
        return Collections.unmodifiableList(defaultEntityListeners);
    }

    /**
     * Adds a new EntityListener.
     * 
     * @since 3.0
     * @throws IllegalArgumentException
     *             if a listener for the same class name is already registered.
     * @deprecated since 3.2 unused as listeners are no longer tied to a
     *             DataMap.
     */
    @Deprecated
    public void addDefaultEntityListener(EntityListener listener) {
        for (EntityListener next : defaultEntityListeners) {
            if (listener.getClassName().equals(next.getClassName())) {
                throw new IllegalArgumentException("Duplicate default listener for " + next.getClassName());
            }
        }

        defaultEntityListeners.add(listener);
    }

    /**
     * Removes a listener matching class name.
     * 
     * @since 3.0
     * @deprecated since 3.2 unused as listeners are no longer tied to a
     *             DataMap.
     */
    @Deprecated
    public void removeDefaultEntityListener(String className) {
        Iterator<EntityListener> it = defaultEntityListeners.iterator();
        while (it.hasNext()) {
            EntityListener next = it.next();
            if (className.equals(next.getClassName())) {
                it.remove();
                break;
            }
        }
    }

    /**
     * @since 3.0
     * @deprecated since 3.2 unused, as listeners are no longer tied to a
     *             DataMap.
     */
    @Deprecated
    public EntityListener getDefaultEntityListener(String className) {
        for (EntityListener listener : defaultEntityListeners) {
            if (className.equals(listener.getClassName())) {
                return listener;
            }
        }

        return null;
    }

    /**
     * Returns all DbEntities in this DataMap.
     */
    public Collection<DbEntity> getDbEntities() {
        return Collections.unmodifiableCollection(dbEntityMap.values());
    }

    /**
     * Returns DbEntity matching the <code>name</code> parameter. No
     * dependencies will be searched.
     */
    public DbEntity getDbEntity(String dbEntityName) {
        DbEntity entity = dbEntityMap.get(dbEntityName);

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

        for (ObjEntity entity : getObjEntities()) {
            if (javaClassName.equals(entity.getClassName())) {
                return entity;
            }
        }

        return null;
    }

    /**
     * Returns an ObjEntity for a given name. If it is not found in this
     * DataMap, it will search a parent EntityNamespace.
     */
    public ObjEntity getObjEntity(String objEntityName) {
        ObjEntity entity = objEntityMap.get(objEntityName);
        if (entity != null) {
            return entity;
        }

        return namespace != null ? namespace.getObjEntity(objEntityName) : null;
    }

    /**
     * Returns all ObjEntities mapped to the given DbEntity.
     */
    public Collection<ObjEntity> getMappedEntities(DbEntity dbEntity) {
        if (dbEntity == null) {
            return Collections.EMPTY_LIST;
        }

        Collection<ObjEntity> allEntities = (namespace != null) ? namespace.getObjEntities() : getObjEntities();

        if (allEntities.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        Collection<ObjEntity> result = new ArrayList<ObjEntity>();
        for (ObjEntity entity : allEntities) {
            if (entity.getDbEntity() == dbEntity) {
                result.add(entity);
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
        // TODO: andrus, 1/25/2007 - clean up references like removeDbEntity
        // does.
        embeddablesMap.remove(className);
    }

    /**
     * @since 3.0
     */
    public void removeResult(String name) {
        results.remove(name);
    }

    /**
     * "Dirty" remove of the DbEntity from the data map.
     */
    public void removeDbEntity(String dbEntityName) {
        removeDbEntity(dbEntityName, false);
    }

    /**
     * Removes DbEntity from the DataMap. If <code>clearDependencies</code> is
     * true, all DbRelationships that reference this entity are also removed.
     * ObjEntities that rely on this entity are cleaned up.
     * 
     * @since 1.1
     */
    public void removeDbEntity(String dbEntityName, boolean clearDependencies) {
        DbEntity dbEntityToDelete = dbEntityMap.remove(dbEntityName);

        if (dbEntityToDelete != null && clearDependencies) {
            for (DbEntity dbEnt : this.getDbEntities()) {
                // take a copy since we're going to modify the entity
                for (Relationship rel : new ArrayList<Relationship>(dbEnt.getRelationships())) {
                    if (dbEntityName.equals(rel.getTargetEntityName())) {
                        dbEnt.removeRelationship(rel.getName());
                    }
                }
            }

            // Remove all obj relationships referencing removed DbRelationships.
            for (ObjEntity objEnt : this.getObjEntities()) {
                if (dbEntityToDelete.getName().equals(objEnt.getDbEntityName())) {
                    objEnt.clearDbMapping();
                } else {
                    for (ObjRelationship rel : objEnt.getRelationships()) {
                        for (DbRelationship dbRel : rel.getDbRelationships()) {
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
     * Removes ObjEntity from the DataMap. If <code>clearDependencies</code> is
     * true, all ObjRelationships that reference this entity are also removed.
     * 
     * @since 1.1
     */
    public void removeObjEntity(String objEntityName, boolean clearDependencies) {
        ObjEntity entity = objEntityMap.remove(objEntityName);

        if (entity != null && clearDependencies) {

            // remove relationships that point to this entity
            for (ObjEntity ent : getObjEntities()) {
                // take a copy since we're going to modify the entity
                for (Relationship relationship : new ArrayList<Relationship>(ent.getRelationships())) {
                    if (objEntityName.equals(relationship.getTargetEntityName())
                            || objEntityName.equals(relationship.getTargetEntityName())) {
                        ent.removeRelationship(relationship.getName());
                    }
                }
            }
        }
    }

    /**
     * Returns stored procedures associated with this DataMap.
     */
    public Collection<Procedure> getProcedures() {
        return Collections.unmodifiableCollection(procedureMap.values());
    }

    /**
     * Returns a Procedure for a given name or null if no such procedure exists.
     * If Procedure is not found in this DataMap, a parent EntityNamcespace is
     * searched.
     */
    public Procedure getProcedure(String procedureName) {
        Procedure procedure = procedureMap.get(procedureName);
        if (procedure != null) {
            return procedure;
        }

        return namespace != null ? namespace.getProcedure(procedureName) : null;
    }

    /**
     * Adds stored procedure to the list of procedures. If there is another
     * procedure registered under the same name, throws an
     * IllegalArgumentException.
     */
    public void addProcedure(Procedure procedure) {
        if (procedure.getName() == null) {
            throw new NullPointerException("Attempt to add procedure with no name.");
        }

        // TODO: change method signature to return replaced procedure and make
        // sure the
        // Modeler handles it...
        Object existingProcedure = procedureMap.get(procedure.getName());
        if (existingProcedure != null) {
            if (existingProcedure == procedure) {
                return;
            } else {
                throw new IllegalArgumentException("An attempt to override procedure '" + procedure.getName());
            }
        }

        procedureMap.put(procedure.getName(), procedure);
        procedure.setDataMap(this);
    }

    public void removeProcedure(String name) {
        procedureMap.remove(name);
    }

    /**
     * Returns a sorted unmodifiable map of Procedures in this DataMap keyed by
     * name.
     */
    public SortedMap<String, Procedure> getProcedureMap() {
        return Collections.unmodifiableSortedMap(procedureMap);
    }

    /**
     * Returns a parent namespace where this DataMap resides. Parent
     * EntityNamespace is used to establish relationships with entities in other
     * DataMaps.
     * 
     * @since 1.1
     */
    public MappingNamespace getNamespace() {
        return namespace;
    }

    /**
     * Sets a parent namespace where this DataMap resides. Parent
     * EntityNamespace is used to establish relationships with entities in other
     * DataMaps.
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
     * Returns default client superclass.
     * 
     * @since 3.0
     */
    public String getDefaultClientSuperclass() {
        return defaultClientSuperclass;
    }

    /**
     * @since 3.0
     */
    public void setDefaultClientSuperclass(String defaultClientSuperclass) {
        this.defaultClientSuperclass = defaultClientSuperclass;
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
     * DbEntity property changed. May be name, attribute or relationship added
     * or removed, etc. Attribute and relationship property changes are handled
     * in respective listeners.
     * 
     * @since 1.2
     */
    public void dbEntityChanged(EntityEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof DbEntity) {

            DbEntity dbEntity = (DbEntity) entity;

            dbEntity.dbEntityChanged(e);

            // finish up the name change here because we
            // do not have direct access to the dbEntityMap
            if (e.isNameChange()) {
                // remove the entity from the map with the old name
                dbEntityMap.remove(e.getOldName());

                // add the entity back in with the new name
                dbEntityMap.put(e.getNewName(), dbEntity);

                // important - clear parent namespace:
                MappingNamespace ns = getNamespace();
                if (ns instanceof EntityResolver) {
                    ((EntityResolver) ns).refreshMappingCache();
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

    /**
     * ObjEntity property changed. May be name, attribute or relationship added
     * or removed, etc. Attribute and relationship property changes are handled
     * in respective listeners.
     * 
     * @since 1.2
     */
    public void objEntityChanged(EntityEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof ObjEntity) {

            ObjEntity objEntity = (ObjEntity) entity;
            objEntity.objEntityChanged(e);

            // finish up the name change here because we
            // do not have direct access to the objEntityMap
            if (e.isNameChange()) {
                // remove the entity from the map with the old name
                objEntityMap.remove(e.getOldName());

                // add the entity back in with the new name
                objEntityMap.put(e.getNewName(), objEntity);

                // important - clear parent namespace:
                MappingNamespace ns = getNamespace();
                if (ns instanceof EntityResolver) {
                    ((EntityResolver) ns).refreshMappingCache();
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

    /**
     * @since 3.1
     */
    public Resource getConfigurationSource() {
        return configurationSource;
    }

    /**
     * @since 3.1
     */
    public void setConfigurationSource(Resource configurationSource) {
        this.configurationSource = configurationSource;
    }

    /**
     * @since 3.2
     */
    public String getDefaultCatalog() {
        return defaultCatalog;
    }

    /**
     * @since 3.2
     */
    public void setDefaultCatalog(String defaultCatalog) {
        this.defaultCatalog = defaultCatalog;
    }

    /**
     * @since 3.2
     */
    public EntityInheritanceTree getInheritanceTree(String entityName) {
        // TODO: we should support that
        throw new UnsupportedOperationException();
    }

    /**
     * @since 3.2
     */
    public ObjEntity getObjEntity(Class<?> entityClass) {
        if (entityClass == null) {
            return null;
        }

        String className = entityClass.getName();

        for (ObjEntity e : objEntityMap.values()) {
            if (className.equals(e.getClassName())) {
                return e;
            }
        }

        return null;
    }

    public ObjEntity getObjEntity(Persistent object) {
        ObjectId id = object.getObjectId();
        if (id != null) {
            return getObjEntity(id.getEntityName());
        } else {
            return getObjEntity(object.getClass());
        }
    }
}
