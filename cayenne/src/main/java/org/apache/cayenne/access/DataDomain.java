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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelQueryFilterChain;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.DataChannelSyncFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.access.flush.DataDomainFlushAction;
import org.apache.cayenne.access.flush.DataDomainFlushActionFactory;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.tx.TransactionManager;
import org.apache.cayenne.util.ToStringBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DataDomain performs query routing functions in Cayenne. DataDomain creates
 * single data source abstraction hiding multiple physical data sources from the
 * user. When a child DataContext sends a query to the DataDomain, it is
 * transparently routed to an appropriate DataNode.
 */
public class DataDomain implements QueryEngine, DataChannel {

	public static final String SHARED_CACHE_ENABLED_PROPERTY = "cayenne.DataDomain.sharedCache";
	public static final boolean SHARED_CACHE_ENABLED_DEFAULT = true;

	public static final String VALIDATING_OBJECTS_ON_COMMIT_PROPERTY = "cayenne.DataDomain.validatingObjectsOnCommit";
	public static final boolean VALIDATING_OBJECTS_ON_COMMIT_DEFAULT = true;

	/**
	 * @since 3.1
	 */
	@Inject
	protected JdbcEventLogger jdbcEventLogger;

	/**
	 * @since 4.0
	 */
	@Inject
	protected TransactionManager transactionManager;

	/**
     * @since 4.0
     */
    protected DataRowStoreFactory dataRowStoreFactory;

    /**
	 * @since 3.1
	 */
	protected int maxIdQualifierSize;

	/**
	 * @since 4.1
	 */
	protected List<DataChannelQueryFilter> queryFilters;

	/**
	 * @since 4.1
	 */
	protected List<DataChannelSyncFilter> syncFilters;

	/**
	 * @since 4.2
	 */
	@Inject
	protected DataDomainFlushActionFactory flushActionFactory;

	/**
	 * @since 4.2
	 */
	@Inject
	protected AdhocObjectFactory objectFactory;

	protected Map<String, DataNode> nodes;
	protected Map<String, DataNode> nodesByDataMapName;
	protected DataNode defaultNode;
	protected Map<String, String> properties;

	protected EntityResolver entityResolver;
	protected DataRowStore sharedSnapshotCache;
	protected String name;
	protected QueryCache queryCache;

	// these are initialized from properties...
	protected boolean sharedCacheEnabled;
	protected boolean validatingObjectsOnCommit;

	/**
	 * @since 1.2
	 */
	protected EventManager eventManager;

	/**
	 * @since 1.2
	 */
	protected EntitySorter entitySorter;

	protected boolean stopped;

	/**
	 * Creates a DataDomain and assigns it a name.
	 */
	public DataDomain(String name) {
		init(name);
		resetProperties();
	}

	/**
	 * Creates new DataDomain.
	 *
	 * @param name
	 *            DataDomain name. Domain can be located using its name in the
	 *            Configuration object.
	 * @param properties
	 *            A Map containing domain configuration properties.
	 * @deprecated since 4.0 unused
	 */
	@Deprecated
	public DataDomain(String name, Map<String, String> properties) {
		init(name);
		initWithProperties(properties);
	}

	private void init(String name) {

		this.queryFilters = new CopyOnWriteArrayList<>();
		this.syncFilters = new CopyOnWriteArrayList<>();
		this.nodesByDataMapName = new ConcurrentHashMap<>();
		this.nodes = new ConcurrentHashMap<>();

		// properties are read-only, so no need for concurrent map, or any
		// specific map
		// for that matter
		this.properties = Collections.emptyMap();

		setName(name);
	}

	/**
	 * Checks that Domain is not stopped. Throws DomainStoppedException
	 * otherwise.
	 *
	 * @since 3.0
	 */
	protected void checkStopped() throws DomainStoppedException {
		if (stopped) {
			throw new DomainStoppedException("Domain " + name
					+ " was shutdown and can no longer be used to access the database");
		}
	}

	/**
	 * @since 3.1
	 */
	public EntitySorter getEntitySorter() {
		return entitySorter;
	}

	/**
	 * @since 3.1
	 */
	public void setEntitySorter(EntitySorter entitySorter) {
		this.entitySorter = entitySorter;
	}

	/**
	 * @since 1.1
	 */
	protected void resetProperties() {
		properties = Collections.emptyMap();

		sharedCacheEnabled = SHARED_CACHE_ENABLED_DEFAULT;
		validatingObjectsOnCommit = VALIDATING_OBJECTS_ON_COMMIT_DEFAULT;
	}

	/**
	 * Reinitializes domain state with a new set of properties.
	 *
	 * @since 1.1
	 * @deprecated since 4.0 properties are processed by the DI provider.
	 */
	@Deprecated
	public void initWithProperties(Map<String, String> properties) {

		// clone properties to ensure that it is read-only internally
		properties = properties != null ? new HashMap<>(properties) : Collections.<String, String>emptyMap();

		String sharedCacheEnabled = properties.get(SHARED_CACHE_ENABLED_PROPERTY);
		String validatingObjectsOnCommit = properties.get(VALIDATING_OBJECTS_ON_COMMIT_PROPERTY);

		// init ivars from properties
		this.sharedCacheEnabled = (sharedCacheEnabled != null) ? "true".equalsIgnoreCase(sharedCacheEnabled)
				: SHARED_CACHE_ENABLED_DEFAULT;
		this.validatingObjectsOnCommit = (validatingObjectsOnCommit != null) ? "true"
				.equalsIgnoreCase(validatingObjectsOnCommit) : VALIDATING_OBJECTS_ON_COMMIT_DEFAULT;

		this.properties = properties;
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
	 * Returns <code>true</code> if DataContexts produced by this DataDomain are
	 * using shared DataRowStore. Returns <code>false</code> if each DataContext
	 * would work with its own DataRowStore. Note that this setting can be
	 * overwritten per DataContext.
	 */
	public boolean isSharedCacheEnabled() {
		return sharedCacheEnabled;
	}

	public void setSharedCacheEnabled(boolean sharedCacheEnabled) {
		this.sharedCacheEnabled = sharedCacheEnabled;
	}

	/**
	 * Returns whether child DataContexts default behavior is to perform object
	 * validation before commit is executed.
	 *
	 * @since 1.1
	 */
	public boolean isValidatingObjectsOnCommit() {
		return validatingObjectsOnCommit;
	}

	/**
	 * Sets the property defining whether child DataContexts should perform
	 * object validation before commit is executed.
	 *
	 * @since 1.1
	 */
	public void setValidatingObjectsOnCommit(boolean flag) {
		this.validatingObjectsOnCommit = flag;
	}

	/**
	 * @since 1.1
	 * @return a Map of properties for this DataDomain.
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Returns snapshots cache for this DataDomain, lazily initializing it on
	 * the first call if 'sharedCacheEnabled' flag is true.
	 */
	public DataRowStore getSharedSnapshotCache() {
		if (sharedSnapshotCache == null && sharedCacheEnabled) {
			this.sharedSnapshotCache = nonNullSharedSnapshotCache();
		}

		return sharedSnapshotCache;
	}

	/**
	 * Returns a guaranteed non-null shared snapshot cache regardless of the
	 * 'sharedCacheEnabled' flag setting.
	 */
	synchronized DataRowStore nonNullSharedSnapshotCache() {
		if (sharedSnapshotCache == null) {
			this.sharedSnapshotCache = dataRowStoreFactory.createDataRowStore(name);
		}

		return sharedSnapshotCache;
	}

	/**
	 * Shuts down the previous cache instance, sets cache to the new
	 * DataSowStore instance and updates two properties of the new DataSowStore:
	 * name and eventManager.
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

	public void addDataMap(DataMap dataMap) {
		getEntityResolver().addDataMap(dataMap);
		refreshEntitySorter();
	}

	/**
	 * @since 3.1
	 */
	public DataMap getDataMap(String mapName) {
		return getEntityResolver().getDataMap(mapName);
	}

	/**
	 * Removes named DataMap from this DataDomain and any underlying DataNodes
	 * that include it.
	 *
	 * @since 3.1
	 */
	public void removeDataMap(String mapName) {
		DataMap map = getDataMap(mapName);
		if (map == null) {
			return;
		}

		// remove from data nodes
		for (DataNode node : nodes.values()) {
			node.removeDataMap(mapName);
		}

		nodesByDataMapName.remove(mapName);

		// remove from EntityResolver
		getEntityResolver().removeDataMap(map);

		refreshEntitySorter();
	}

	/**
	 * Removes a DataNode from DataDomain. Any maps previously associated with
	 * this node within domain will still be kept around, however they wan't be
	 * mapped to any node.
	 */
	public void removeDataNode(String nodeName) {
		DataNode removed = nodes.remove(nodeName);
		if (removed != null) {
			removed.setEntityResolver(null);
			nodesByDataMapName.values().removeIf(dataNode -> dataNode == removed);
		}
	}

	/**
	 * Returns a collection of registered DataMaps.
	 */
	public Collection<DataMap> getDataMaps() {
		return getEntityResolver().getDataMaps();
	}

	/**
	 * Returns an unmodifiable collection of DataNodes associated with this
	 * domain.
	 */
	public Collection<DataNode> getDataNodes() {
		return Collections.unmodifiableCollection(nodes.values());
	}

	/**
	 * Adds new DataNode.
	 */
	public void addNode(DataNode node) {

		// add node to name->node map
		nodes.put(node.getName(), node);
		node.setEntityResolver(getEntityResolver());

		// add node to "ent name->node" map
		for (DataMap map : node.getDataMaps()) {
			addDataMap(map);
			nodesByDataMapName.put(map.getName(), node);
		}
	}

	/**
	 * Returns registered DataNode whose name matches <code>name</code>
	 * parameter.
	 *
	 * @since 3.1
	 */
	public DataNode getDataNode(String nodeName) {
		return nodes.get(nodeName);
	}

	/**
	 * Returns a DataNode that should handle queries for all entities in a
	 * DataMap.
	 *
	 * @since 1.1
	 */
	public DataNode lookupDataNode(DataMap map) {

		DataNode node = nodesByDataMapName.get(map.getName());
		if (node == null) {

			// see if one of the node states has changed, and the map is now
			// linked...
			for (DataNode n : getDataNodes()) {
				for (DataMap m : n.getDataMaps()) {
					if (m == map) {
						nodesByDataMapName.put(map.getName(), n);
						node = n;
						break;
					}
				}

				if (node != null) {
					break;
				}
			}

			if (node == null) {

				if (defaultNode != null) {
					nodesByDataMapName.put(map.getName(), defaultNode);
					node = defaultNode;
				} else {
					throw new CayenneRuntimeException("No DataNode configured for DataMap '%s'"
							+ " and no default DataNode set", map.getName());
				}
			}
		}

		return node;
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
	 * Shutdowns all owned data nodes and marks this domain as stopped.
	 */
	@BeforeScopeEnd
	public void shutdown() {
		if (!stopped) {
			stopped = true;

			if (sharedSnapshotCache != null) {
				sharedSnapshotCache.shutdown();
			}
		}
	}

	/**
	 * Routes queries to appropriate DataNodes for execution.
	 */
	public void performQueries(final Collection<? extends Query> queries, final OperationObserver callback) {
		transactionManager.performInTransaction(() -> {
			new DataDomainLegacyQueryAction(DataDomain.this, new QueryChain(queries), callback).execute();
			return null;
		});
	}

	// ****** DataChannel methods:

	/**
	 * Runs query returning generic QueryResponse.
	 *
	 * @since 1.2
	 */
	@Override
	public QueryResponse onQuery(final ObjectContext originatingContext, final Query query) {
		checkStopped();

		return new DataDomainQueryFilterChain().onQuery(originatingContext, query);
	}

	QueryResponse onQueryNoFilters(final ObjectContext originatingContext, final Query query) {
		// transaction note:
		// we don't wrap this code in transaction to reduce transaction scope to
		// just the DB operation for better performance ... query action will
		// start a transaction itself when and if needed
		return new DataDomainQueryAction(originatingContext, DataDomain.this, query).execute();
	}

	/**
	 * Returns an EntityResolver that stores mapping information for this
	 * domain.
	 */
	@Override
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
	@Override
	public GraphDiff onSync(final ObjectContext originatingContext, final GraphDiff changes, int syncType) {

		checkStopped();

		return new DataDomainSyncFilterChain().onSync(originatingContext, changes, syncType);
	}

	GraphDiff onSyncNoFilters(final ObjectContext originatingContext, final GraphDiff changes, int syncType) {

        GraphDiff result;
        switch (syncType) {
		case DataChannel.ROLLBACK_CASCADE_SYNC:
			result = onSyncRollback(originatingContext);
			break;
		// "cascade" and "no_cascade" are the same from the DataDomain perspective
		case DataChannel.FLUSH_NOCASCADE_SYNC:
		case DataChannel.FLUSH_CASCADE_SYNC:
			result =  onSyncFlush(originatingContext, changes);
			break;
		default:
			throw new CayenneRuntimeException("Invalid synchronization type: %d", syncType);
		}

		return result;
	}

	GraphDiff onSyncRollback(ObjectContext originatingContext) {
		// if there is a transaction in progress, roll it back

		Transaction transaction = BaseTransaction.getThreadTransaction();
		if (transaction != null) {
			transaction.setRollbackOnly();
		}

		return new CompoundDiff();
	}

	GraphDiff onSyncFlush(ObjectContext originatingContext, GraphDiff childChanges) {

		if (!(originatingContext instanceof DataContext)) {
			throw new CayenneRuntimeException("No support for committing ObjectContexts that are not DataContexts yet. "
							+ "Unsupported context: %s", originatingContext);
		}

		DataDomainFlushAction action = flushActionFactory.createFlushAction(this);
		return action.flush((DataContext) originatingContext, childChanges);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("name", name).toString();
	}

	/**
	 * Returns shared {@link QueryCache} used by this DataDomain.
	 *
	 * @since 3.0
	 */
	public QueryCache getQueryCache() {
		return queryCache;
	}

	public void setQueryCache(QueryCache queryCache) {
		this.queryCache = queryCache;
	}

	/**
	 * @since 4.0
	 */
	public DataRowStoreFactory getDataRowStoreFactory() {
		return dataRowStoreFactory;
	}

	/**
	 * @since 4.0
	 */
	public void setDataRowStoreFactory(DataRowStoreFactory dataRowStoreFactory) {
		this.dataRowStoreFactory = dataRowStoreFactory;
	}

	/**
	 * @since 3.1
	 */
	JdbcEventLogger getJdbcEventLogger() {
		return jdbcEventLogger;
	}

	void refreshEntitySorter() {
		if (entitySorter != null) {
			entitySorter.setEntityResolver(getEntityResolver());
		}
	}

	/**
	 * Returns an unmodifiable list of query filters registered with this DataDomain.
	 * <p>
	 * Filter ordering note: filters are applied in reverse order of their
	 * occurrence in the filter list. I.e. the last filter in the list called
	 * first in the chain.
	 *
	 * @since 4.1
	 */
	public List<DataChannelQueryFilter> getQueryFilters() {
		return Collections.unmodifiableList(queryFilters);
	}

	/**
	 * Returns an unmodifiable list of sync filters registered with this DataDomain.
	 * <p>
	 * Filter ordering note: filters are applied in reverse order of their
	 * occurrence in the filter list. I.e. the last filter in the list called
	 * first in the chain.
	 *
	 * @since 4.1
	 */
	public List<DataChannelSyncFilter> getSyncFilters() {
		return Collections.unmodifiableList(syncFilters);
	}

	/**
	 * Adds a new query filter.
	 * Also registers passed filter as an event listener, if any of its methods have event annotations.
	 *
	 * @since 4.1
	 */
	public void addQueryFilter(DataChannelQueryFilter filter) {
		// skip double listener registration, if filter already in sync filters list
		if(!syncFilters.contains(filter)) {
			addListener(filter);
		}
		queryFilters.add(filter);
	}

	/**
	 * Adds a new sync filter.
	 * Also registers passed filter as an event listener, if any of its methods have event annotations.
	 *
	 * @since 4.1
	 */
	public void addSyncFilter(DataChannelSyncFilter filter) {
		// skip double listener registration, if filter already in query filters list
		if(!queryFilters.contains(filter)) {
			addListener(filter);
		}
		syncFilters.add(filter);
	}

	/**
	 * Removes a query filter from the filter chain.
	 *
	 * @since 4.1
	 */
	public void removeQueryFilter(DataChannelQueryFilter filter) {
		queryFilters.remove(filter);
	}

	/**
	 * Removes a sync filter from the filter chain.
	 *
	 * @since 4.1
	 */
	public void removeSyncFilter(DataChannelSyncFilter filter) {
		syncFilters.remove(filter);
	}

	/**
	 * Adds a listener, mapping its methods to events based on annotations. This
	 * is a shortcut for
	 * 'getEntityResolver().getCallbackRegistry().addListener(listener)'.
	 *
	 * @since 4.0
	 */
	public void addListener(Object listener) {
		getEntityResolver().getCallbackRegistry().addListener(listener);
	}

	final class DataDomainQueryFilterChain implements DataChannelQueryFilterChain {

		private int idx;

		DataDomainQueryFilterChain() {
			idx = queryFilters.size();
		}

		@Override
		public QueryResponse onQuery(ObjectContext originatingContext, Query query) {
			return --idx >= 0
					? queryFilters.get(idx).onQuery(originatingContext, query, this)
					: onQueryNoFilters(originatingContext, query);
		}
	}

	final class DataDomainSyncFilterChain implements DataChannelSyncFilterChain {

		private int idx;

		DataDomainSyncFilterChain() {
			idx = syncFilters.size();
		}

		@Override
		public GraphDiff onSync(ObjectContext originatingContext, final GraphDiff changes, int syncType) {
			return --idx >= 0
					? syncFilters.get(idx).onSync(originatingContext, changes, syncType, this)
					: onSyncNoFilters(originatingContext, changes, syncType);
		}
	}

	/**
	 * An optional DataNode that is used for DataMaps that are not linked to a
	 * DataNode explicitly.
	 *
	 * @since 3.1
	 */
	public DataNode getDefaultNode() {
		return defaultNode;
	}

	/**
	 * @since 3.1
	 */
	public void setDefaultNode(DataNode defaultNode) {
		this.defaultNode = defaultNode;
	}

	/**
	 * Returns a maximum number of object IDs to match in a single query for
	 * queries that select objects based on collection of ObjectIds. This
	 * affects queries generated by Cayenne when processing paginated queries
	 * and DISJOINT_BY_ID prefetches and is intended to address database
	 * limitations on the size of SQL statements as well as to cap memory use in
	 * Cayenne when generating such queries. The default is 10000. It can be
	 * changed either by calling {@link #setMaxIdQualifierSize(int)} or changing
	 * the value for property
	 * {@link Constants#MAX_ID_QUALIFIER_SIZE_PROPERTY}.
	 *
	 * @since 3.1
	 */
	public int getMaxIdQualifierSize() {
		return maxIdQualifierSize;
	}

	/**
	 * @since 3.1
	 */
	public void setMaxIdQualifierSize(int maxIdQualifierSize) {
		this.maxIdQualifierSize = maxIdQualifierSize;
	}

	TransactionManager getTransactionManager() {
		return transactionManager;
	}

	AdhocObjectFactory getObjectFactory() {
		return objectFactory;
	}
}
