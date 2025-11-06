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
import org.apache.cayenne.DataRow;
import org.apache.cayenne.EmbeddableObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.EmbeddableResultSegment;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.IteratedQueryDecorator;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.PrefetchSelectQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryMetadataProxy;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.RefreshQuery;
import org.apache.cayenne.query.RelationshipQuery;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.Transaction;
import org.apache.cayenne.util.GenericResponse;
import org.apache.cayenne.util.ListResponse;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Performs query routing and execution. During execution phase intercepts
 * callbacks to the OperationObserver, remapping results to the original
 * pre-routed queries.
 * 
 * @since 1.2
 */
class DataDomainQueryAction implements QueryRouter, OperationObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataDomainQueryAction.class);

    private static final boolean DONE = true;

    private final DataContext context;
    final DataDomain domain;
    final Query query;
    private final QueryMetadata metadata;
    private final AdhocObjectFactory objectFactory;

    private DataRowStore cache;
    private QueryResponse response;
    private GenericResponse fullResponse;
    private Map<CayennePath, List<?>> prefetchResultsByPath;
    private Map<QueryEngine, Collection<Query>> queriesByNode;
    private boolean noObjectConversion;
    // True when using a caching strategy (shared or local cache), indicating lists are immutable and need copying
    private boolean cachedResult;
    // True when results were found in cache (cache hit), false when fetched from database (cache miss or explicit refresh)
    private boolean cacheHit;

    /*
     * A constructor for the "new" way of performing a query via 'execute' with
     * QueryResponse created internally.
     */
    DataDomainQueryAction(ObjectContext context, DataDomain domain, Query query) {
        if (context != null && !(context instanceof DataContext)) {
            throw new IllegalArgumentException("DataDomain can only work with DataContext. "
                    + "Unsupported context type: " + context);
        }

        this.domain = domain;
        this.query = query;
        this.metadata = query.getMetaData(domain.getEntityResolver());
        this.context = (DataContext) context;
        this.objectFactory = domain.getObjectFactory();

        // cache may be shared or unique for the ObjectContext
        if (context != null) {
            this.cache = this.context.getObjectStore().getDataRowCache();
        }

        if (this.cache == null) {
            this.cache = domain.getSharedSnapshotCache();
        }
    }

    QueryResponse execute() {

        // run chain...
        if (interceptIteratedQuery() != DONE) {
            if (interceptOIDQuery() != DONE) {
                if (interceptRelationshipQuery() != DONE) {
                    if (interceptRefreshQuery() != DONE) {
                        if (interceptSharedCache() != DONE) {
                            if (interceptDataDomainQuery() != DONE) {
                                runQueryInTransaction();
                            }
                        }
                    }
                }
            }
        }

        interceptObjectConversion();

        return response;
    }

    private boolean interceptIteratedQuery() {
        if (query instanceof IteratedQueryDecorator) {
            noObjectConversion = ((IteratedQueryDecorator) query).isFetchingDataRows();
            validateIteratedQuery();
            performIteratedQuery();
            return DONE;
        }
        return !DONE;
    }

    private void validateIteratedQuery() {
        if (metadata.getPageSize() > 0) {
            throw new CayenneRuntimeException("Pagination is not supported with iterator");
        }
        if (metadata.getPrefetchTree() != null) {
            for (PrefetchTreeNode prefetchTreeNode : metadata.getPrefetchTree().getChildren()) {
                if (prefetchTreeNode.isDisjointPrefetch()) {
                    throw new CayenneRuntimeException("\"Disjoint\" semantic doesn't work with iterator. Use \"Joint\" instead");
                }
                if (prefetchTreeNode.isDisjointByIdPrefetch()) {
                    LOGGER.warn("A separate select query will be created for each iterated item");
                }
            }
        }
    }

    private void performIteratedQuery() {
        Transaction tx = BaseTransaction.getThreadTransaction();
        if (tx != null) {
            runIteratedQuery(tx);
        } else {
            tx = context.getTransactionFactory().createTransaction();
            BaseTransaction.bindThreadTransaction(tx);
            try {
                runIteratedQuery(tx);
            } catch (Exception e) {
                throw new CayenneRuntimeException(e);
            } finally {
                BaseTransaction.bindThreadTransaction(null);
                if (tx.isRollbackOnly()) {
                    try {
                        tx.rollback();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private boolean interceptDataDomainQuery() {
        if (query instanceof DataDomainQuery) {
            response = new ListResponse(domain);
            return DONE;
        }

        return !DONE;
    }

    private boolean interceptOIDQuery() {
        if (query instanceof ObjectIdQuery) {

            ObjectIdQuery oidQuery = (ObjectIdQuery) query;
            ObjectId oid = oidQuery.getObjectId();

            // special handling of temp ids...
            // Return an empty list immediately so that upstream code could throw FaultFailureException, etc.
            // Don't attempt to translate and run the query. See for instance CAY-1651
            if (oid.isTemporary() && !oid.isReplacementIdAttached()) {
                response = new ListResponse();
                return DONE;
            }

            DataRow row = null;

            if (cache != null && !oidQuery.isFetchMandatory()) {
                row = polymorphicRowFromCache(oid);
            }

            // refresh is forced or not found in cache
            if (row == null) {
                if (oidQuery.isFetchAllowed()) {
                    runQueryInTransaction();
                } else {
                    response = new ListResponse();
                }
            } else {
                response = new ListResponse(row);
            }

            return DONE;
        }

        return !DONE;
    }

    private DataRow polymorphicRowFromCache(ObjectId superOid) {
        DataRow row = cache.getCachedSnapshot(superOid);
        if (row != null) {
            return row;
        }

        EntityInheritanceTree inheritanceTree = domain.getEntityResolver().getInheritanceTree(superOid.getEntityName());
        if (!inheritanceTree.getChildren().isEmpty()) {
            row = polymorphicRowFromCache(inheritanceTree, superOid);
        }

        return row;
    }

    private DataRow polymorphicRowFromCache(EntityInheritanceTree superNode, ObjectId superOid) {

        for (EntityInheritanceTree child : superNode.getChildren()) {
            ObjectId id = ObjectId.of(child.getEntity().getName(), superOid);
            DataRow row = cache.getCachedSnapshot(id);
            if (row != null) {
                return row;
            }

            row = polymorphicRowFromCache(child, superOid);
            if (row != null) {
                return row;
            }
        }

        return null;
    }

    private boolean interceptRelationshipQuery() {

        if (query instanceof RelationshipQuery) {

            RelationshipQuery relationshipQuery = (RelationshipQuery) query;
            if (relationshipQuery.isRefreshing()) {
                return !DONE;
            }

            ObjRelationship relationship = relationshipQuery.getRelationship(domain.getEntityResolver());

            // check if we can derive target PK from FK...
            if (relationship.isSourceIndependentFromTargetChange()) {
                return !DONE;
            }

            // we can assume that there is one and only one DbRelationship as
            // we previously checked that "!isSourceIndependentFromTargetChange"
            DbRelationship dbRelationship = relationship.getDbRelationships().get(0);

            // FK pointing to a unique field that is a 'fake' PK (CAY-1755)...
            // It is not sufficient to generate target ObjectId.
            DbEntity targetEntity = dbRelationship.getTargetEntity();
            if (dbRelationship.getJoins().size() < targetEntity.getPrimaryKeys().size()) {
                return !DONE;
            }

            if (cache == null) {
                return !DONE;
            }

            DataRow sourceRow = cache.getCachedSnapshot(relationshipQuery.getObjectId());
            if (sourceRow == null) {
                return !DONE;
            }

            ObjectId targetId = sourceRow.createTargetObjectId(relationship.getTargetEntityName(), dbRelationship);

            // null id means that FK is null...
            if (targetId == null) {
                this.response = new GenericResponse(Collections.emptyList());
                return DONE;
            }

            // target id resolution (unlike source) should be polymorphic
            DataRow targetRow = polymorphicRowFromCache(targetId);

            if (targetRow != null) {
                this.response = new GenericResponse(Collections.singletonList(targetRow));
                return DONE;
            }

            // check whether a non-null FK is enough to assume non-null target, and if so, create a fault
            if (context != null && relationship.isSourceDefiningTargetPrecenseAndType(domain.getEntityResolver())) {

                // prevent passing partial snapshots to ObjectResolver per CAY-724.
                // Create a hollow object right here and skip object conversion downstream
                this.noObjectConversion = true;
                Object object = context.findOrCreateObject(targetId);

                this.response = new GenericResponse(Collections.singletonList(object));
                return DONE;
            }
        }

        return !DONE;
    }

    /**
     * @since 3.0
     */
    @SuppressWarnings("deprecation")
    private boolean interceptRefreshQuery() {

        if (query instanceof RefreshQuery) {
            RefreshQuery refreshQuery = (RefreshQuery) query;

            if (refreshQuery.isRefreshAll()) {

                // not sending any events - peer contexts will not get refreshed
                if (domain.getSharedSnapshotCache() != null) {
                    domain.getSharedSnapshotCache().clear();
                } else {
                    // remove snapshots from local ObjectStore only
                    context.getObjectStore().getDataRowCache().clear();
                }
                context.getQueryCache().clear();

                GenericResponse response = new GenericResponse();
                response.addUpdateCount(1);
                this.response = response;
                return DONE;
            }

            @SuppressWarnings("unchecked")
            Collection<Persistent> objects = (Collection<Persistent>) refreshQuery.getObjects();
            if (objects != null && !objects.isEmpty()) {

                Collection<ObjectId> ids = new ArrayList<>(objects.size());
                for (final Persistent object : objects) {
                    ids.add(object.getObjectId());
                }

                if (domain.getSharedSnapshotCache() != null) {
                    // send an event for removed snapshots
                    domain.getSharedSnapshotCache().processSnapshotChanges(context.getObjectStore(),
                            Collections.emptyMap(), Collections.emptyList(), ids, Collections.emptyList());
                } else {
                    // remove snapshots from local ObjectStore only
                    context.getObjectStore()
                            .getDataRowCache()
                            .processSnapshotChanges(context.getObjectStore(), Collections.emptyMap(),
                                    Collections.emptyList(), ids, Collections.emptyList());
                }

                GenericResponse response = new GenericResponse();
                response.addUpdateCount(1);
                this.response = response;
                return DONE;
            }

            // 3. refresh query - this shouldn't normally happen as child datacontext
            // usually does a cascading refresh
            if (refreshQuery.getQuery() != null) {
                Query cachedQuery = refreshQuery.getQuery();

                String cacheKey = cachedQuery.getMetaData(context.getEntityResolver()).getCacheKey();
                context.getQueryCache().remove(cacheKey);

                this.response = domain.onQuery(context, cachedQuery);
                return DONE;
            }

            // 4. refresh groups...
            if (refreshQuery.getGroupKeys() != null && refreshQuery.getGroupKeys().length > 0) {

                String[] groups = refreshQuery.getGroupKeys();
                for (String group : groups) {
                    domain.getQueryCache().removeGroup(group);
                }

                GenericResponse response = new GenericResponse();
                response.addUpdateCount(1);
                this.response = response;
                return DONE;
            }
        }

        return !DONE;
    }

    /*
     * Wraps execution in shared cache checks
     */
    private boolean interceptSharedCache() {

        if (metadata.getCacheKey() == null) {
            return !DONE;
        }

        boolean cache = QueryCacheStrategy.SHARED_CACHE == metadata.getCacheStrategy();
        boolean cacheOrCacheRefresh = cache || QueryCacheStrategy.SHARED_CACHE_REFRESH == metadata.getCacheStrategy();

        if (!cacheOrCacheRefresh) {
            return !DONE;
        }

        QueryCache queryCache = domain.getQueryCache();
        QueryCacheEntryFactory factory = getCacheObjectFactory();

        if (cache) {
            boolean wasResponseNull = (response == null);
            List<?> cachedResults = queryCache.get(metadata, factory);

            // response may already be initialized by the factory above ...
            // it is null if there was a preexisting cache entry
            cacheHit = (response == null);
            
            if (response == null || wasResponseNull) {
                response = new ListResponse(cachedResults);
            }
            
            // Mark as cached result - lists need copying whether hit or miss
            cachedResult = true;

            if (cachedResults instanceof ListWithPrefetches) {
                this.prefetchResultsByPath = ((ListWithPrefetches) cachedResults).getPrefetchResultsByPath();
            }
        } else {
            // on cache-refresh request, fetch without blocking and fill the cache
            queryCache.put(metadata, factory.createObject());
            cachedResult = true; // Still a cached path, lists need copying
            cacheHit = false; // Not a cache hit, we're refreshing
        }

        return DONE;
    }

    private QueryCacheEntryFactory getCacheObjectFactory() {
        return () -> {
            runQueryInTransaction();

            List<?> list = response.firstList();
            if (list != null) {

                // make an immutable list to make sure callers don't mess it up
                list = Collections.unmodifiableList(list);

                // include prefetches in the cached result
                if (prefetchResultsByPath != null) {
                    list = new ListWithPrefetches(list, prefetchResultsByPath);
                }
            }

            return list;
        };
    }

    /*
     * Gets response from the underlying DataNodes.
     */
    void runQueryInTransaction() {
        domain.getTransactionManager().performInTransaction(() -> {
            runQuery();
            return null;
        });
    }

    private void runQuery() {
        // reset
        this.fullResponse = new GenericResponse();
        this.response = this.fullResponse;
        this.queriesByNode = null;

        // whether this is null or not will driver further decisions on how to process prefetched rows
        this.prefetchResultsByPath = metadata.getPrefetchTree() != null && !metadata.isFetchingDataRows()
                ? new HashMap<>() : null;

        // categorize queries by node and by "executable" query...
        query.route(this, domain.getEntityResolver(), null);

        // run categorized queries
        if (queriesByNode != null) {
            for (Map.Entry<QueryEngine, Collection<Query>> entry : queriesByNode.entrySet()) {
                QueryEngine nextNode = entry.getKey();
                Collection<Query> nodeQueries = entry.getValue();
                nextNode.performQueries(nodeQueries, this);
            }
        }
    }

    private void runIteratedQuery(Transaction tx) {
        runQuery();
        ResultIterator<?> iterator = fullResponse.firstIterator();
        if (iterator == null) {
            throw new IllegalStateException("Iterator response expected");
        }
        fullResponse.replaceResult(iterator, new TransactionResultIteratorDecorator<>(iterator, tx));
        fullResponse.reset();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void interceptObjectConversion() {
        if (noObjectConversion()) {
            return;
        }

        ObjectConversionStrategy<?, ?> converter = getConverter();
        // local copy because it can change while iterating
        QueryResponse response = this.response;
        for (response.reset(); response.next();) {
            if (response.isList()) {
                List<?> mainRows = response.currentList(); // List<DataRow> or List<Object[]>
                if (mainRows != null && !mainRows.isEmpty()) {
                    List<?> result = converter.convert((List) mainRows);
                    if(result != mainRows) {
                        updateResponse(mainRows, result);
                    }
                }
            } else if (response.isIterator()) {
                // iterator should be a part of full response
                ResultIterator<?> iterator = fullResponse.currentIterator();
                fullResponse.replaceResult(iterator, new ResultIteratorConverterDecorator(iterator, converter));
            }
        }
        response.reset();
    }

    private boolean noObjectConversion() {
        return context == null
                || noObjectConversion
                || metadata.getPageSize() > 0;
    }

    private ObjectConversionStrategy<?, ?> getConverter() {
        ObjectConversionStrategy<?, ?> converter;

        if (metadata.isFetchingDataRows()) {
            converter = new IdentityConversionStrategy();
        } else {
            List<Object> rsMapping = metadata.getResultSetMapping();
            if (rsMapping == null) {
                converter = new SingleObjectConversionStrategy();
            } else {
                if (metadata.isSingleResultSetMapping()) {
                    if (rsMapping.get(0) instanceof EntityResultSegment) {
                        converter = new SingleObjectConversionStrategy();
                    } else if (rsMapping.get(0) instanceof EmbeddableResultSegment) {
                        converter = new SingleEmbeddableConversionStrategy();
                    } else {
                        converter = new SingleScalarConversionStrategy();
                    }
                } else {
                    converter = new MixedConversionStrategy();
                }
            }
        }

        if (metadata.getResultMapper() != null) {
            converter = new MapperConversionStrategy(converter);
        }
        return converter;
    }

    @Override
    public void route(QueryEngine engine, Query query, Query substitutedQuery) {
        Collection<Query> queries = null;
        if (queriesByNode == null) {
            queriesByNode = new HashMap<>();
        } else {
            queries = queriesByNode.get(engine);
        }

        if (queries == null) {
            queries = new ArrayList<>(5);
            queriesByNode.put(engine, queries);
        }

        queries.add(query);
    }

    @Override
    public QueryEngine engineForDataMap(DataMap map) {
        if (map == null) {
            throw new NullPointerException("Null DataMap, can't determine DataNode.");
        }

        QueryEngine node = domain.lookupDataNode(map);

        if (node == null) {
            throw new CayenneRuntimeException("No DataNode exists for DataMap %s", map);
        }

        return node;
    }

    /**
     * @since 4.0
     */
    @Override
    public QueryEngine engineForName(String name) {

        QueryEngine node;

        if (name != null) {
            node = domain.getDataNode(name);
            if (node == null) {
                throw new CayenneRuntimeException("No DataNode exists for name %s", name);
            }
        } else {
            node = domain.getDefaultNode();
            if (node == null) {
                throw new CayenneRuntimeException("No default DataNode exists.");
            }
        }

        return node;
    }

    @Override
    public void nextCount(Query query, int resultCount) {
        fullResponse.addUpdateCount(resultCount);
    }

    @Override
    public void nextBatchCount(Query query, int[] resultCount) {
        fullResponse.addBatchUpdateCount(resultCount);
    }

    @Override
    public void nextRows(Query query, List<?> dataRows) {
        // exclude prefetched rows in the main result
        if (prefetchResultsByPath != null && query instanceof PrefetchSelectQuery) {
            PrefetchSelectQuery<?> prefetchQuery = (PrefetchSelectQuery<?>) query;
            prefetchResultsByPath.put(prefetchQuery.getPrefetchPath(), dataRows);
        } else {
            fullResponse.addResultList(dataRows);
        }
    }

    @Override
    public void nextRows(Query q, ResultIterator<?> it) {
        // exclude prefetched rows in the main result
        if (prefetchResultsByPath != null && query instanceof PrefetchSelectQuery) {
            PrefetchSelectQuery<?> prefetchQuery = (PrefetchSelectQuery<?>) query;
            prefetchResultsByPath.put(prefetchQuery.getPrefetchPath(), (List<?>) it);
        } else {
            this.fullResponse.addResultIterator(it);
        }
    }

    @Override
    public void nextGeneratedRows(Query query, ResultIterator<?> keys, List<ObjectId> idsToUpdate) {
        if (keys != null) {
            try (keys) {
                nextRows(query, keys.allRows());
            }
        }
    }

    @Override
    public void nextQueryException(Query query, Exception ex) {
        throw new CayenneRuntimeException("Query exception.", Util.unwindException(ex));
    }

    @Override
    public void nextGlobalException(Exception e) {
        throw new CayenneRuntimeException("Global exception.", Util.unwindException(e));
    }

    @Override
    public boolean isIteratedResult() {
        return (query instanceof IteratedQueryDecorator);
    }

    protected <T, R> void updateResponse(List<T> sourceObjects, List<? extends R> targetObjects) {
        if (response instanceof GenericResponse) {
            ((GenericResponse) response).replaceResult(sourceObjects, targetObjects);
        } else if (response instanceof ListResponse) {
            response = new ListResponse(targetObjects);
        } else {
            throw new IllegalStateException("Unknown response object: " + response);
        }
    }

    abstract class ObjectConversionStrategy<T, R> {
        abstract List<? extends R> convert(List<T> mainRows);

        abstract R convert(T t);

        protected PrefetchProcessorNode toResultsTree(ClassDescriptor descriptor, PrefetchTreeNode prefetchTree,
                                                      List<DataRow> normalizedRows) {

            // take a shortcut when no prefetches exist...
            if (prefetchTree == null) {
                // When results come from cache (not a refresh operation), don't refresh objects 
                // to avoid clobbering newer in-memory state
                boolean refresh = metadata.isRefreshingObjects() && !shouldSkipRefresh();
                return new ObjectResolver(context, descriptor, refresh)
                        .synchronizedRootResultNodeFromDataRows(normalizedRows);
            } else {
                // When results come from cache (not a refresh operation), wrap metadata to prevent refreshing objects
                QueryMetadata effectiveMetadata = shouldSkipRefresh() && metadata.isRefreshingObjects() 
                        ? new NonRefreshingQueryMetadataWrapper(metadata)
                        : metadata;
                HierarchicalObjectResolver resolver = new HierarchicalObjectResolver(context, effectiveMetadata);
                return resolver
                        .synchronizedRootResultNodeFromDataRows(prefetchTree, normalizedRows, prefetchResultsByPath);
            }
        }
        
        private boolean shouldSkipRefresh() {
            // Skip refresh only for cache hits to prevent stale cached data from clobbering newer in-memory state
            // For cache misses (including explicit refresh operations), cacheHit is false, so refresh happens normally
            // Prefetch relationships are resolved independently via connectToParents(), so this doesn't affect prefetch behavior
            return cacheHit;
        }

        protected void performPostLoadCallbacks(PrefetchProcessorNode node, LifecycleCallbackRegistry callbackRegistry) {

            if (node.hasChildren()) {
                for (PrefetchTreeNode child : node.getChildren()) {
                    performPostLoadCallbacks((PrefetchProcessorNode) child, callbackRegistry);
                }
            }

            List<Persistent> objects = node.getObjects();
            if (objects != null) {
                callbackRegistry.performCallbacks(LifecycleEvent.POST_LOAD, objects);
            }
        }
    }

    class SingleObjectConversionStrategy extends ObjectConversionStrategy<DataRow, Persistent> {

        @Override
        public List<Persistent> convert(List<DataRow> mainRows) {

            PrefetchProcessorNode node = getPrefetchProcessorNode(mainRows);
            List<Persistent> objects = node.getObjects();

            // apply POST_LOAD callback
            LifecycleCallbackRegistry callbackRegistry = context.getEntityResolver().getCallbackRegistry();

            if (!callbackRegistry.isEmpty(LifecycleEvent.POST_LOAD)) {
                performPostLoadCallbacks(node, callbackRegistry);
            }
            return objects != null ? objects : new ArrayList<>(1);
        }

        @Override
        Persistent convert(DataRow dataRow) {
            PrefetchProcessorNode node = getPrefetchProcessorNode(Collections.singletonList(dataRow));
            return node.getObjects().get(0);
        }

        private PrefetchProcessorNode getPrefetchProcessorNode(List<DataRow> mainRows) {
            PrefetchTreeNode prefetchTree = metadata.getPrefetchTree();

            List<Object> rsMapping = metadata.getResultSetMapping();
            EntityResultSegment resultSegment = null;
            if (rsMapping != null && !rsMapping.isEmpty()) {
                resultSegment = (EntityResultSegment) rsMapping.get(0);
            }

            ClassDescriptor descriptor = resultSegment == null
                    ? metadata.getClassDescriptor()
                    : resultSegment.getClassDescriptor();

            return toResultsTree(descriptor, prefetchTree, mainRows);
        }
    }

    class SingleScalarConversionStrategy extends ObjectConversionStrategy<Object, Object> {

        @Override
        List<Object> convert(List<Object> mainRows) {
            // noop... scalars require no further processing
            return mainRows;
        }

        @Override
        Object convert(Object o) {
            // noop... scalars require no further processing
            return o;
        }
    }

    class SingleEmbeddableConversionStrategy extends ObjectConversionStrategy<DataRow, EmbeddableObject> {

        @Override
        List<EmbeddableObject> convert(List<DataRow> mainRows) {
            EmbeddableResultSegment resultSegment = (EmbeddableResultSegment) metadata.getResultSetMapping().get(0);
            Embeddable embeddable = resultSegment.getEmbeddable();
            Class<? extends EmbeddableObject> embeddableClass = objectFactory.getJavaClass(embeddable.getClassName());
            List<EmbeddableObject> result = new ArrayList<>(mainRows.size());
            mainRows.forEach(dataRow -> {
                EmbeddableObject eo;
                try {
                    eo = embeddableClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new CayenneRuntimeException("Unable to materialize embeddable '%s'", e,
                            embeddable.getClassName());
                }
                dataRow.forEach(eo::writePropertyDirectly);
                result.add(eo);
            });
            return result;
        }

        @Override
        EmbeddableObject convert(DataRow dataRow) {
            return convert(Collections.singletonList(dataRow)).get(0);
        }
    }

    class MixedConversionStrategy extends ObjectConversionStrategy<Object[], Object[]> {

        @Override
        List<Object[]> convert(List<Object[]> mainRows) {
            if (mainRows.isEmpty()) {
                // just a sanity check, should not be a valid case
                return mainRows;
            }

            // do we have anything to convert to objects inside mainRows?
            boolean needConversion = needConversion();

            // create result list
            List<Object[]> result = createResultList(mainRows, needConversion);

            // no conversions needed for scalar positions;
            if(needConversion) {
                // reuse Object[]'s to fill them with resolved objects
                List<PrefetchProcessorNode> segmentNodes = doInPlaceConversion(result);

                // invoke callbacks now that all objects are resolved...
                LifecycleCallbackRegistry callbackRegistry = context.getEntityResolver().getCallbackRegistry();
                if (!callbackRegistry.isEmpty(LifecycleEvent.POST_LOAD)) {
                    for (PrefetchProcessorNode node : segmentNodes) {
                        performPostLoadCallbacks(node, callbackRegistry);
                    }
                }
            }

            // distinct filtering
            if (!metadata.isSuppressingDistinct()) {
                Set<List<?>> seen = new HashSet<>(result.size());
                result.removeIf(objects -> !seen.add(Arrays.asList(objects)));
            }

            return result;
        }

        @Override
        Object[] convert(Object[] objectsArray) {
            List<Object[]> objects = new ArrayList<>(1);
            objects.add(objectsArray);
            return convert(objects).get(0);
        }

        private List<Object[]> createResultList(List<Object[]> mainRows, boolean needConversion) {
            if(!cachedResult) {
                // fast-path, we can reuse existing rows
                return mainRows;
            }

            if(!needConversion) {
                // no conversion needed, so can clone only top-level list
                return new ArrayList<>(mainRows);
            }

            // slowest path, deep copy everything
            List<Object[]> result = new ArrayList<>(mainRows.size());
            for(Object[] row : mainRows) {
                result.add(Arrays.copyOf(row, metadata.getResultSetMapping().size()));
            }
            return result;
        }

        protected PrefetchProcessorNode toResultsTree(ClassDescriptor descriptor, PrefetchTreeNode prefetchTree,
                                                      List<Object[]> rows, int position) {

            List<DataRow> rowsColumn = new ArrayList<>(rows.size());
            for (Object[] row : rows) {
                rowsColumn.add((DataRow) row[position]);
            }

            if (prefetchTree != null) {
                PrefetchTreeNode prefetchTreeNode = null;
                for (PrefetchTreeNode prefetch : prefetchTree.getChildren()) {
                    if (descriptor.getEntity().getName().equals(prefetch.getEntityName())) {
                        if (prefetchTreeNode == null) {
                            prefetchTreeNode = new PrefetchTreeNode();
                        }
                        PrefetchTreeNode addPath = prefetchTreeNode.addPath(prefetch.getPath());
                        addPath.setSemantics(prefetch.getSemantics());
                        addPath.setPhantom(false);
                    }
                }
                prefetchTree = prefetchTreeNode;
            }

            if (prefetchTree == null) {
                return new ObjectResolver(context, descriptor, metadata.isRefreshingObjects())
                        .synchronizedRootResultNodeFromDataRows(rowsColumn);
            } else {
                HierarchicalObjectResolver resolver
                        = new HierarchicalObjectResolver(context, metadata, descriptor, true);
                return resolver.synchronizedRootResultNodeFromDataRows(prefetchTree, rowsColumn, prefetchResultsByPath);
            }
        }

        private List<PrefetchProcessorNode> doInPlaceConversion(List<Object[]> result) {
            List<Object> resultSetMapping = metadata.getResultSetMapping();
            int width = resultSetMapping.size();
            int height  = result.size();
            List<PrefetchProcessorNode> segmentNodes = new ArrayList<>(width);
            for (int i = 0; i < width; i++) {
                Object mapping = resultSetMapping.get(i);
                if (mapping instanceof EntityResultSegment) {
                    EntityResultSegment entitySegment = (EntityResultSegment) mapping;
                    PrefetchProcessorNode nextResult = toResultsTree(entitySegment.getClassDescriptor(),
                            metadata.getPrefetchTree(), result, i);

                    segmentNodes.add(nextResult);

                    List<Persistent> objects = nextResult.getObjects();
                    for (int j = 0; j < height; j++) {
                        Object[] row = result.get(j);
                        row[i] = objects.get(j);
                    }
                } else if (mapping instanceof EmbeddableResultSegment) {
                    EmbeddableResultSegment resultSegment = (EmbeddableResultSegment) mapping;
                    Embeddable embeddable = resultSegment.getEmbeddable();
                    Class<? extends EmbeddableObject> embeddableClass = objectFactory
                            .getJavaClass(embeddable.getClassName());
                    try {
                        Constructor<? extends EmbeddableObject> declaredConstructor = embeddableClass
                                .getDeclaredConstructor();
                        for (Object[] row : result) {
                            DataRow dataRow = (DataRow) row[i];
                            EmbeddableObject eo = declaredConstructor.newInstance();
                            dataRow.forEach(eo::writePropertyDirectly);
                            row[i] = eo;
                        }
                    } catch (Exception e) {
                        throw new CayenneRuntimeException("Unable to materialize embeddable '%s'", e, embeddable.getClassName());
                    }
                }
            }
            return segmentNodes;
        }

        private boolean needConversion() {
            for (Object mapping : metadata.getResultSetMapping()) {
                if (mapping instanceof EntityResultSegment
                        || mapping instanceof EmbeddableResultSegment) {
                    return true;
                }
            }
            return false;
        }
    }

    private class IdentityConversionStrategy extends ObjectConversionStrategy<Object, Object> {
        @Override
        List<Object> convert(List<Object> mainRows) {
            //noop
            return mainRows;
        }

        @Override
        public Object convert(Object object) {
            return object;
        }
    }

    /**
     * Conversion strategy that uses mapper function to map raw result
     */
    private class MapperConversionStrategy extends ObjectConversionStrategy<Object, Object> {

        private final Function<Object, ?> mapper;
        private final ObjectConversionStrategy<Object, Object> parentStrategy;

        @SuppressWarnings({"unchecked", "rawtypes"})
        MapperConversionStrategy(ObjectConversionStrategy<?, ?> parentStrategy) {
            this.mapper = (Function) metadata.getResultMapper();
            this.parentStrategy = (ObjectConversionStrategy) parentStrategy;
        }

        @Override
        List<Object> convert(List<Object> mainRows) {
            parentStrategy.convert(mainRows);
            mainRows.replaceAll(mapper::apply);
            return mainRows;
        }

        @Override
        Object convert(Object object) {
            return mapper.apply(parentStrategy.convert(object));
        }
    }

    /**
     * Wrapper that overrides isRefreshingObjects() to return false, preventing cached
     * query results from clobbering newer in-memory object state.
     */
    static class NonRefreshingQueryMetadataWrapper extends QueryMetadataProxy {
        NonRefreshingQueryMetadataWrapper(QueryMetadata delegate) {
            super(delegate);
        }

        @Override
        public boolean isRefreshingObjects() {
            return false;
        }
    }
}
