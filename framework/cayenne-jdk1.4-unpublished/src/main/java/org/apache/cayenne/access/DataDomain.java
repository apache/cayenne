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

package org.apache.cayenne.access;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.cache.MapQueryCacheFactory;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheFactory;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.AshwoodEntitySorter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.util.Util;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * DataDomain performs query routing functions in Cayenne. DataDomain creates single data
 * source abstraction hiding multiple physical data sources from the user. When a child
 * DataContext sends a query to the DataDomain, it is transparently routed to an
 * appropriate DataNode.
 * 
 * @author Andrus Adamchik
 */
public class DataDomain implements QueryEngine, DataChannel {

    public static final String SHARED_CACHE_ENABLED_PROPERTY = "cayenne.DataDomain.sharedCache";
    public static final boolean SHARED_CACHE_ENABLED_DEFAULT = true;

    public static final String VALIDATING_OBJECTS_ON_COMMIT_PROPERTY = "cayenne.DataDomain.validatingObjectsOnCommit";
    public static final boolean VALIDATING_OBJECTS_ON_COMMIT_DEFAULT = true;

    public static final String USING_EXTERNAL_TRANSACTIONS_PROPERTY = "cayenne.DataDomain.usingExternalTransactions";
    public static final boolean USING_EXTERNAL_TRANSACTIONS_DEFAULT = false;

    /**
     * Defines a property name for storing an optional DataContextFactory.
     * 
     * @since 1.2
     */
    public static final String DATA_CONTEXT_FACTORY_PROPERTY = "cayenne.DataDomain.dataContextFactory";

    /**
     * Defaines a property name for storing optional {@link QueryCacheFactory}.
     * 
     * @since 3.0
     */
    public static final String QUERY_CACHE_FACTORY_PROPERTY = "cayenne.DataDomain.queryCacheFactory";

    /** Stores mapping of data nodes to DataNode name keys. */
    protected Map nodes = Collections.synchronizedMap(new TreeMap());
    protected Map nodesByDataMapName = Collections.synchronizedMap(new HashMap());

    /**
     * Properties configured for DataDomain. These include properties of the DataRowStore
     * and remote notifications.
     */
    protected Map properties = Collections.synchronizedMap(new TreeMap());

    protected EntityResolver entityResolver;
    protected DataRowStore sharedSnapshotCache;
    protected TransactionDelegate transactionDelegate;
    protected DataContextFactory dataContextFactory;
    protected QueryCacheFactory queryCacheFactory;
    protected String name;

    // these are initializable from properties...
    protected boolean sharedCacheEnabled;
    protected boolean validatingObjectsOnCommit;
    protected boolean usingExternalTransactions;

    /**
     * @since 1.2
     */
    protected EventManager eventManager;

    /**
     * @since 1.2
     */
    EntitySorter entitySorter;

    /**
     * @since 3.0
     */
    protected QueryCache queryCache;

    /**
     * Creates a DataDomain and assigns it a name.
     */
    public DataDomain(String name) {
        DataContextFaults.init();
        setName(name);
        resetProperties();
    }

    /**
     * Creates new DataDomain.
     * 
     * @param name DataDomain name. Domain can be located using its name in the
     *            Configuration object.
     * @param properties A Map containing domain configuration properties.
     */
    public DataDomain(String name, Map properties) {
        DataContextFaults.init();
        setName(name);
        initWithProperties(properties);
    }

    /**
     * @since 1.2
     */
    // TODO: andrus, 4/12/2006 - after 1.2 API freeze is over, replace DataNode
    // EntitySorter with this one ... maybe even make it a part of server-side
    // EntityResolver?
    EntitySorter getEntitySorter() {

        if (entitySorter == null) {
            synchronized (this) {
                if (entitySorter == null) {

                    // backwards compatibility mode... only possible in a single-node case
                    // see TODO above
                    if (nodes.size() == 1) {
                        entitySorter = ((DataNode) nodes.values().iterator().next())
                                .getEntitySorter();
                    }
                    else {
                        entitySorter = new AshwoodEntitySorter(getDataMaps());
                    }
                }
            }
        }

        return entitySorter;
    }

    /**
     * Exists as a backdoor to override domain sorter until the sorter API is moved from
     * DataNode.
     * 
     * @since 1.2
     */
    void setEntitySorter(EntitySorter entitySorter) {
        this.entitySorter = entitySorter;
    }

    /**
     * @since 1.1
     */
    protected void resetProperties() {
        if (properties != null) {
            properties.clear();
        }

        sharedCacheEnabled = SHARED_CACHE_ENABLED_DEFAULT;
        validatingObjectsOnCommit = VALIDATING_OBJECTS_ON_COMMIT_DEFAULT;
        usingExternalTransactions = USING_EXTERNAL_TRANSACTIONS_DEFAULT;
        dataContextFactory = null;
    }

    /**
     * Reinitializes domain state with a new set of properties.
     * 
     * @since 1.1
     */
    public void initWithProperties(Map properties) {
        // create map with predictable modification and synchronization behavior
        Map localMap = new HashMap();
        if (properties != null) {
            localMap.putAll(properties);
        }

        this.properties = localMap;

        Object sharedCacheEnabled = localMap.get(SHARED_CACHE_ENABLED_PROPERTY);
        Object validatingObjectsOnCommit = localMap
                .get(VALIDATING_OBJECTS_ON_COMMIT_PROPERTY);
        Object usingExternalTransactions = localMap
                .get(USING_EXTERNAL_TRANSACTIONS_PROPERTY);

        Object dataContextFactory = localMap.get(DATA_CONTEXT_FACTORY_PROPERTY);
        Object queryCacheFactory = localMap.get(QUERY_CACHE_FACTORY_PROPERTY);

        // init ivars from properties
        this.sharedCacheEnabled = (sharedCacheEnabled != null)
                ? "true".equalsIgnoreCase(sharedCacheEnabled.toString())
                : SHARED_CACHE_ENABLED_DEFAULT;
        this.validatingObjectsOnCommit = (validatingObjectsOnCommit != null)
                ? "true".equalsIgnoreCase(validatingObjectsOnCommit.toString())
                : VALIDATING_OBJECTS_ON_COMMIT_DEFAULT;
        this.usingExternalTransactions = (usingExternalTransactions != null)
                ? "true".equalsIgnoreCase(usingExternalTransactions.toString())
                : USING_EXTERNAL_TRANSACTIONS_DEFAULT;

        if (dataContextFactory != null
                && !Util.isEmptyString(dataContextFactory.toString())) {
            this.dataContextFactory = (DataContextFactory) createInstance(
                    dataContextFactory.toString(),
                    DataContextFactory.class);
        }
        else {
            this.dataContextFactory = null;
        }

        if (queryCacheFactory != null
                && !Util.isEmptyString(dataContextFactory.toString())) {
            queryCacheFactory = (QueryCacheFactory) createInstance(queryCacheFactory
                    .toString(), QueryCacheFactory.class);
        }
        else {
            queryCacheFactory = null;
        }
    }

    private Object createInstance(String className, Class implementedInterface) {
        Class aClass;
        try {
            aClass = Class.forName(className, true, Thread
                    .currentThread()
                    .getContextClassLoader());
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error loading '" + className + "'", e);
        }

        if (!implementedInterface.isAssignableFrom(aClass)) {
            throw new CayenneRuntimeException("Failed to load '"
                    + className
                    + "' - it is expected to implement "
                    + implementedInterface);
        }

        try {
            return aClass.newInstance();
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error instantiating " + className, e);
        }
    }

    /**
     * Returns EventManager used by this DataDomain.
     * 
     * @since 1.2
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Sets EventManager used by this DataDomain.
     * 
     * @since 1.2
     */
    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;

        if (sharedSnapshotCache != null) {
            sharedSnapshotCache.setEventManager(eventManager);
        }
    }

    /**
     * Returns "name" property value.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets "name" property to a new value.
     */
    public synchronized void setName(String name) {
        this.name = name;
        if (sharedSnapshotCache != null) {
            this.sharedSnapshotCache.setName(name);
        }
    }

    /**
     * Returns <code>true</code> if DataContexts produced by this DataDomain are using
     * shared DataRowStore. Returns <code>false</code> if each DataContext would work
     * with its own DataRowStore.
     */
    public boolean isSharedCacheEnabled() {
        return sharedCacheEnabled;
    }

    public void setSharedCacheEnabled(boolean sharedCacheEnabled) {
        this.sharedCacheEnabled = sharedCacheEnabled;
    }

    /**
     * Returns whether child DataContexts default behavior is to perform object validation
     * before commit is executed.
     * 
     * @since 1.1
     */
    public boolean isValidatingObjectsOnCommit() {
        return validatingObjectsOnCommit;
    }

    /**
     * Sets the property defining whether child DataContexts should perform object
     * validation before commit is executed.
     * 
     * @since 1.1
     */
    public void setValidatingObjectsOnCommit(boolean flag) {
        this.validatingObjectsOnCommit = flag;
    }

    /**
     * Returns whether this DataDomain should internally commit all transactions, or let
     * container do that.
     * 
     * @since 1.1
     */
    public boolean isUsingExternalTransactions() {
        return usingExternalTransactions;
    }

    /**
     * Sets a property defining whether this DataDomain should internally commit all
     * transactions, or let container do that.
     * 
     * @since 1.1
     */
    public void setUsingExternalTransactions(boolean flag) {
        this.usingExternalTransactions = flag;
    }

    /**
     * @since 1.1
     * @return a Map of properties for this DataDomain. There is no guarantees of specific
     *         synchronization behavior of this map.
     */
    public Map getProperties() {
        return properties;
    }

    /**
     * @since 1.1
     * @return TransactionDelegate associated with this DataDomain, or null if no delegate
     *         exist.
     */
    public TransactionDelegate getTransactionDelegate() {
        return transactionDelegate;
    }

    /**
     * Initializes TransactionDelegate used by all DataContexts associated with this
     * DataDomain.
     * 
     * @since 1.1
     */
    public void setTransactionDelegate(TransactionDelegate transactionDelegate) {
        this.transactionDelegate = transactionDelegate;
    }

    /**
     * Returns snapshots cache for this DataDomain, lazily initializing it on the first
     * call.
     */
    public synchronized DataRowStore getSharedSnapshotCache() {
        if (sharedSnapshotCache == null && sharedCacheEnabled) {
            this.sharedSnapshotCache = new DataRowStore(name, properties, eventManager);
        }

        return sharedSnapshotCache;
    }

    /**
     * Shuts down the previous cache instance, sets cache to the new DataSowStore instance
     * and updates two properties of the new DataSowStore: name and eventManager.
     */
    public synchronized void setSharedSnapshotCache(DataRowStore snapshotCache) {
        if (this.sharedSnapshotCache != snapshotCache) {
            if (this.sharedSnapshotCache != null) {
                this.sharedSnapshotCache.shutdown();
            }
            this.sharedSnapshotCache = snapshotCache;

            if (snapshotCache != null) {
                snapshotCache.setEventManager(getEventManager());
                snapshotCache.setName(getName());
            }
        }
    }

    public DataContextFactory getDataContextFactory() {
        return dataContextFactory;
    }

    public void setDataContextFactory(DataContextFactory dataContextFactory) {
        this.dataContextFactory = dataContextFactory;
    }

    /** Registers new DataMap with this domain. */
    public void addMap(DataMap map) {
        getEntityResolver().addDataMap(map);
        entitySorter = null;
    }

    /** Returns DataMap matching <code>name</code> parameter. */
    public DataMap getMap(String mapName) {
        return getEntityResolver().getDataMap(mapName);
    }

    /**
     * Removes named DataMap from this DataDomain and any underlying DataNodes that
     * include it.
     */
    public synchronized void removeMap(String mapName) {
        DataMap map = getMap(mapName);
        if (map == null) {
            return;
        }

        // remove from data nodes
        Iterator it = nodes.values().iterator();
        while (it.hasNext()) {
            DataNode node = (DataNode) it.next();
            node.removeDataMap(mapName);
        }

        // remove from EntityResolver
        getEntityResolver().removeDataMap(map);
        entitySorter = null;

        // reindex nodes to remove references on removed map entities
        reindexNodes();
    }

    /**
     * Removes a DataNode from DataDomain. Any maps previously associated with this node
     * within domain will still be kept around, however they wan't be mapped to any node.
     */
    public synchronized void removeDataNode(String nodeName) {
        DataNode removed = (DataNode) nodes.remove(nodeName);
        if (removed != null) {

            removed.setEntityResolver(null);

            Iterator it = nodesByDataMapName.values().iterator();
            while (it.hasNext()) {
                if (it.next() == removed) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Returns a collection of registered DataMaps.
     */
    public Collection getDataMaps() {
        return getEntityResolver().getDataMaps();
    }

    /**
     * Returns an unmodifiable collection of DataNodes associated with this domain.
     */
    public Collection getDataNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * Closes all data nodes, removes them from the list of available nodes.
     */
    public void reset() {
        synchronized (nodes) {
            nodes.clear();
            nodesByDataMapName.clear();

            if (entityResolver != null) {
                entityResolver.clearCache();
                entityResolver = null;
            }
        }
    }

    /**
     * Clears the list of internal DataMaps. In most cases it is wise to call "reset"
     * before doing that.
     */
    public void clearDataMaps() {
        getEntityResolver().setDataMaps(Collections.EMPTY_LIST);
    }

    /**
     * Adds new DataNode.
     */
    public synchronized void addNode(DataNode node) {

        // add node to name->node map
        nodes.put(node.getName(), node);
        node.setEntityResolver(this.getEntityResolver());

        // add node to "ent name->node" map
        Iterator nodeMaps = node.getDataMaps().iterator();
        while (nodeMaps.hasNext()) {
            DataMap map = (DataMap) nodeMaps.next();
            this.addMap(map);
            this.nodesByDataMapName.put(map.getName(), node);
        }

        entitySorter = null;
    }

    /**
     * Creates and returns a new DataContext. If this DataDomain is configured to use
     * shared cache, returned DataContext will use shared cache as well. Otherwise a new
     * instance of DataRowStore will be used as its local cache.
     */
    public DataContext createDataContext() {
        return createDataContext(isSharedCacheEnabled());
    }

    /**
     * Creates a new DataContext.
     * 
     * @param useSharedCache determines whether resulting DataContext should use shared
     *            vs. local cache. This setting overrides default behavior configured for
     *            this DataDomain via {@link #SHARED_CACHE_ENABLED_PROPERTY}.
     * @since 1.1
     */
    public DataContext createDataContext(boolean useSharedCache) {
        // for new dataRowStores use the same name for all stores
        // it makes it easier to track the event subject
        DataRowStore snapshotCache = (useSharedCache)
                ? getSharedSnapshotCache()
                : new DataRowStore(name, properties, eventManager);

        DataContext context;
        if (null == dataContextFactory) {
            context = new DataContext((DataChannel) this, new ObjectStore(snapshotCache));
        }
        else {
            context = dataContextFactory.createDataContext(this, new ObjectStore(
                    snapshotCache));
        }
        context.setValidatingObjectsOnCommit(isValidatingObjectsOnCommit());
        return context;
    }

    /**
     * Creates and returns a new inactive transaction. Returned transaction is bound to
     * the current execution thread.
     * <p>
     * If there is a TransactionDelegate, adds the delegate to the newly created
     * Transaction. Behavior of the returned Transaction depends on
     * "usingInternalTransactions" property setting.
     * </p>
     * 
     * @since 1.1
     */
    public Transaction createTransaction() {
        return (isUsingExternalTransactions()) ? Transaction
                .externalTransaction(getTransactionDelegate()) : Transaction
                .internalTransaction(getTransactionDelegate());
    }

    /**
     * Returns registered DataNode whose name matches <code>name</code> parameter.
     */
    public DataNode getNode(String nodeName) {
        return (DataNode) nodes.get(nodeName);
    }

    /**
     * Updates internal index of DataNodes stored by the entity name.
     */
    public synchronized void reindexNodes() {
        nodesByDataMapName.clear();

        Iterator nodes = this.getDataNodes().iterator();
        while (nodes.hasNext()) {
            DataNode node = (DataNode) nodes.next();
            Iterator nodeMaps = node.getDataMaps().iterator();
            while (nodeMaps.hasNext()) {
                DataMap map = (DataMap) nodeMaps.next();
                addMap(map);
                nodesByDataMapName.put(map.getName(), node);
            }
        }
    }

    /**
     * Returns a DataNode that should handle queries for all entities in a DataMap.
     * 
     * @since 1.1
     */
    public DataNode lookupDataNode(DataMap map) {
        synchronized (nodesByDataMapName) {
            DataNode node = (DataNode) nodesByDataMapName.get(map.getName());
            if (node == null) {
                reindexNodes();
                return (DataNode) nodesByDataMapName.get(map.getName());
            }
            else {
                return node;
            }
        }
    }

    /**
     * Sets EntityResolver. If not set explicitly, DataDomain creates a default
     * EntityResolver internally on demand.
     * 
     * @since 1.1
     */
    public void setEntityResolver(EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    // creates default entity resolver if there is none set yet
    private synchronized void createEntityResolver() {
        if (entityResolver == null) {
            // entity resolver will be self-indexing as we add all our maps
            // to it as they are added to the DataDomain
            entityResolver = new EntityResolver();
        }
    }

    /**
     * Shutdowns all owned data nodes. Invokes DataNode.shutdown().
     */
    public void shutdown() {
        if (sharedSnapshotCache != null) {
            this.sharedSnapshotCache.shutdown();
        }

        Collection dataNodes = getDataNodes();
        for (Iterator i = dataNodes.iterator(); i.hasNext();) {
            DataNode node = (DataNode) i.next();
            try {
                node.shutdown();
            }
            catch (Exception ex) {
            }
        }
    }

    /**
     * Routes queries to appropriate DataNodes for execution.
     */
    public void performQueries(final Collection queries, final OperationObserver callback) {

        runInTransaction(new Transformer() {

            public Object transform(Object input) {
                new DataDomainLegacyQueryAction(
                        DataDomain.this,
                        new QueryChain(queries),
                        callback).execute();
                return null;
            }
        });
    }

    // ****** DataChannel methods:

    /**
     * Runs query returning generic QueryResponse.
     * 
     * @since 1.2
     */
    public QueryResponse onQuery(final ObjectContext context, final Query query) {
        // transaction note:
        // we don't wrap this code in transaction to reduce transaction scope to
        // just the DB operation for better performance ... query action will start a
        // transaction itself when and if needed
        return new DataDomainQueryAction(context, DataDomain.this, query).execute();
    }

    /**
     * Returns an EntityResolver that stores mapping information for this domain.
     */
    public EntityResolver getEntityResolver() {
        if (entityResolver == null) {
            createEntityResolver();
        }

        return entityResolver;
    }

    /**
     * Only handles commit-type synchronization, ignoring any other type.
     * 
     * @since 1.2
     */
    public GraphDiff onSync(
            final ObjectContext originatingContext,
            final GraphDiff changes,
            int syncType) {

        switch (syncType) {
            case DataChannel.ROLLBACK_CASCADE_SYNC:
                return onSyncRollback(originatingContext);
                // "cascade" and "no_cascade" are the same from the DataDomain
                // perspective,
                // including transaction handling logic
            case DataChannel.FLUSH_NOCASCADE_SYNC:
            case DataChannel.FLUSH_CASCADE_SYNC:
                return (GraphDiff) runInTransaction(new Transformer() {

                    public Object transform(Object input) {
                        return onSyncFlush(originatingContext, changes);
                    }
                });
            default:
                throw new CayenneRuntimeException("Invalid synchronization type: "
                        + syncType);
        }
    }

    GraphDiff onSyncRollback(ObjectContext originatingContext) {
        // if there is a transaction in progress, roll it back

        Transaction transaction = Transaction.getThreadTransaction();
        if (transaction != null) {
            transaction.setRollbackOnly();
        }

        return new CompoundDiff();
    }

    GraphDiff onSyncFlush(ObjectContext originatingContext, GraphDiff childChanges) {

        if (!(originatingContext instanceof DataContext)) {
            throw new CayenneRuntimeException(
                    "No support for committing ObjectContexts that are not DataContexts yet. "
                            + "Unsupported context: "
                            + originatingContext);
        }

        return new DataDomainFlushAction(this).flush(
                (DataContext) originatingContext,
                childChanges);
    }

    /**
     * Executes Transformer.transform() method in a transaction. Transaction policy is to
     * check for the thread transaction, and use it if one exists. If it doesn't, a new
     * transaction is created, with a scope limited to this method.
     */
    // WARNING: (andrus) if we ever decide to make this method protected or public, we
    // need to change the signature to avoid API dependency on commons-collections
    Object runInTransaction(Transformer operation) {

        // user or container-managed or nested transaction
        if (Transaction.getThreadTransaction() != null) {
            return operation.transform(null);
        }

        // Cayenne-managed transaction

        Transaction transaction = createTransaction();
        Transaction.bindThreadTransaction(transaction);

        try {
            // implicit begin..
            Object result = operation.transform(null);
            transaction.commit();
            return result;
        }
        catch (Exception ex) {
            transaction.setRollbackOnly();

            // must rethrow
            if (ex instanceof CayenneRuntimeException) {
                throw (CayenneRuntimeException) ex;
            }
            else {
                throw new CayenneRuntimeException(ex);
            }
        }
        finally {
            Transaction.bindThreadTransaction(null);
            if (transaction.getStatus() == Transaction.STATUS_MARKED_ROLLEDBACK) {
                try {
                    transaction.rollback();
                }
                catch (Exception rollbackEx) {
                    // although we don't expect an exception here, print the stack, as
                    // there have been some Cayenne bugs already (CAY-557) that were
                    // masked by this 'catch' clause.
                    QueryLogger.logQueryError(rollbackEx);
                }
            }
        }
    }

    public String toString() {
        return new ToStringBuilder(this).append("name", name).toString();
    }

    /**
     * Returns a non-null {@link QueryCacheFactory}.
     * 
     * @since 3.0
     */
    public QueryCacheFactory getQueryCacheFactory() {
        return queryCacheFactory != null ? queryCacheFactory : new MapQueryCacheFactory();
    }

    /**
     * @since 3.0
     */
    public void setQueryCacheFactory(QueryCacheFactory queryCacheFactory) {
        this.queryCacheFactory = queryCacheFactory;
    }

    /**
     * Returns shared {@link QueryCache} used by this DataDomain, creating it on the fly
     * if needed. Uses factory obtained via {@link #getQueryCacheFactory()} to initialize
     * the cache for the first time.
     * 
     * @since 3.0
     */
    public QueryCache getQueryCache() {

        if (queryCache == null) {
            synchronized (this) {
                if (queryCache == null) {
                    queryCache = getQueryCacheFactory().getQueryCache(
                            Collections.EMPTY_MAP);
                }
            }
        }

        return queryCache;
    }

    /**
     * @since 3.0
     */
    QueryCache getQueryCacheInternal() {
        return queryCache;
    }
}
