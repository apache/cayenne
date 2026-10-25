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
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.ResultIterator;
import org.apache.cayenne.SelectResult;
import org.apache.cayenne.access.lifecycle.LifecycleEventManager;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.query.EmbeddableResultSegment;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.PrefetchSelectQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.QueryMetadataProxy;
import org.apache.cayenne.query.QueryRouter;
import org.apache.cayenne.query.ResultSegment;
import org.apache.cayenne.query.ResultSegments;
import org.apache.cayenne.query.ScalarResultSegment;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.DefaultConstructor;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.ReadOnlyTransaction;
import org.apache.cayenne.tx.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Performs query routing and execution. Routes the query into {@link StatementContext}s of the
 * {@link OperationContext}, executes them on their nodes, and assembles the response from their results, converting
 * rows to objects.
 *
 * @since 1.2
 */
class DataDomainQueryAction implements QueryRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataDomainQueryAction.class);

    private static final boolean DONE = true;

    private final OperationContext execContext;
    private final DataContext context;
    private final DataDomain domain;
    private final Query query;
    private final QueryMetadata metadata;
    private final AdhocObjectFactory objectFactory;

    private List<QueryResult> response;
    private List<QueryResult> fullResponse;
    private Map<CayennePath, List<?>> prefetchResultsByPath;

    // True when using a caching strategy (shared or local cache), indicating lists are immutable and need copying
    private boolean cachedResult;
    // True when results were found in the shared cache (cache hit), false when fetched from the database (cache miss
    // or explicit refresh)
    private boolean cacheHit;

    DataDomainQueryAction(DataDomain domain, OperationContext execContext) {
        ObjectContext context = execContext.getContext();
        if (context != null && !(context instanceof DataContext)) {
            throw new IllegalArgumentException("DataDomain can only work with DataContext. "
                    + "Unsupported context type: " + context);
        }

        this.execContext = execContext;
        this.domain = domain;
        this.query = execContext.getQuery();
        this.metadata = execContext.getMetadata();
        this.context = (DataContext) context;
        this.objectFactory = domain.getObjectFactory();
    }

    List<QueryResult> execute() {

        // run chain...
        if (interceptIteratedQuery() != DONE) {
            if (interceptSharedCache() != DONE) {
                runQueryInTransaction();
            }
        }

        interceptObjectConversion();

        return response;
    }

    private boolean interceptIteratedQuery() {
        if (execContext.isIteratedResult()) {
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

        // The transaction is owned by the caller, and is not aligned with the iterator scope
        if (BaseTransaction.getThreadTransaction() != null) {
            runQuery();
            firstIteratorIndex();
        }

        // The transaction will be owned by the iterator, and will outlive the query action
        else {
            Transaction tx = domain.getTransactionFactory().createTransaction();
            BaseTransaction.bindThreadTransaction(tx);
            try {
                // the transaction is unbound from the thread below, so nothing but the iterator will ever use its
                // connection
                execContext.setExclusiveConnection(true);
                runQuery();
                int index = firstIteratorIndex();
                ResultIterator<?> it = (ResultIterator<?>) fullResponse.get(index);
                fullResponse.set(index, new TransactionResultIteratorDecorator<>(it, tx));
            } catch (Throwable th) {
                // No iterator will be returned to the caller, so nothing will ever commit this transaction. Mark it
                // for rollback below, so that its connections are not leaked. Note that a DataNode marks the
                // transaction on its own, but a failure may as well originate outside of the node.
                tx.setRollbackOnly();
                throw th;
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

    private int firstIteratorIndex() {
        for (int i = 0; i < fullResponse.size(); i++) {
            if (fullResponse.get(i) instanceof ResultIterator) {
                return i;
            }
        }
        throw new CayenneRuntimeException("Iterator response expected");
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

            // the cache may hold no list at all if the query produced no select result
            if (response == null || wasResponseNull) {
                response = new ArrayList<>(1);
                if (cachedResults != null) {
                    response.add(new SelectResult<>(cachedResults));
                }
            }

            // Mark as cached result - lists need copying whether hit or miss
            cachedResult = true;

            if (cachedResults instanceof ListWithPrefetches listWithPrefetches) {
                this.prefetchResultsByPath = listWithPrefetches.getPrefetchResultsByPath();
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

            List<?> list = QueryResults.firstList(response);
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
        if (metadata.isReadOnly() && BaseTransaction.getThreadTransaction() == null) {
            domain.getTransactionManager().performInTransaction(() -> {
                runQuery();
                return null;
            }, descriptor -> new ReadOnlyTransaction());
        } else {
            domain.getTransactionManager().performInTransaction(() -> {
                runQuery();
                return null;
            });
        }
    }

    private void runQuery() {
        // whether this is null or not will driver further decisions on how to process prefetched rows
        this.prefetchResultsByPath = metadata.getPrefetchTree() != null && !metadata.isFetchingDataRows()
                ? new HashMap<>() : null;

        // categorize queries by node and by "executable" query...
        query.route(this, domain.getEntityResolver(), null);

        // run categorized queries
        execContext.getStatementContexts().forEach(DataNode::execute);

        this.fullResponse = collectResults();
        this.response = this.fullResponse;
    }

    /**
     * Assembles the response from the results of the statements, in the order they were routed. Rows selected by a
     * prefetch query are filed by their prefetch path instead, to be joined to the main result during object
     * conversion.
     */
    private List<QueryResult> collectResults() {
        List<QueryResult> results = new ArrayList<>();
        for (List<StatementContext> nodeStatementContexts : execContext.getStatementContexts().values()) {
            for (StatementContext statementContext : nodeStatementContexts) {
                collectResults(statementContext, results);
            }
        }
        return results;
    }

    private void collectResults(StatementContext statementContext, List<QueryResult> results) {
        for (QueryResult result : statementContext.getResults()) {
            if (prefetchResultsByPath != null
                    && result instanceof SelectResult<?> select
                    && statementContext.getQuery() instanceof PrefetchSelectQuery<?> prefetchQuery) {
                prefetchResultsByPath.put(prefetchQuery.getPrefetchPath(), select.objects());
            } else {
                results.add(result);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void interceptObjectConversion() {
        if (noObjectConversion()) {
            return;
        }

        ObjectConversionStrategy<?, ?> converter = getConverter();
        for (int i = 0; i < response.size(); i++) {
            switch (response.get(i)) {
                case SelectResult<?> select -> {
                    List<?> mainRows = select.objects(); // List<DataRow> or List<Object[]>
                    if (!mainRows.isEmpty()) {
                        List<?> result = converter.convert((List) mainRows);
                        if (result != mainRows) {
                            response.set(i, new SelectResult<>(result));
                        }
                    }
                }
                case ResultIterator<?> iterator -> response.set(i,
                        new ResultIteratorConverterDecorator(iterator, converter));
                default -> {
                }
            }
        }
    }

    private boolean noObjectConversion() {
        return context == null || metadata.getPageSize() > 0;
    }

    private ObjectConversionStrategy<?, ?> getConverter() {
        ObjectConversionStrategy<?, ?> converter = getDefaultConverter();
        Function<?, ?> mapper = metadata.getResultMapper();
        return mapper != null ? new MapperConversionStrategy(mapper, converter) : converter;
    }

    private ObjectConversionStrategy<?, ?> getDefaultConverter() {
        if (metadata.isFetchingDataRows()) {
            return new IdentityConversionStrategy();
        }

        ResultSegments rsMapping = metadata.getResultSegments();
        if (rsMapping == null) {
            return new SingleObjectConversionStrategy();
        }

        if (rsMapping.array()) {
            return new MixedConversionStrategy();
        }

        return switch (rsMapping.segments().getFirst()) {
            case EntityResultSegment ignored -> new SingleObjectConversionStrategy();
            case EmbeddableResultSegment ignored -> new SingleEmbeddableConversionStrategy();
            case ScalarResultSegment ignored -> new SingleScalarConversionStrategy();
        };
    }

    @Override
    public void route(DataNode node, Query query, Query substitutedQuery) {
        // the operation has already resolved the metadata of its own query; a substituted query (e.g. a prefetch
        // query) is resolved here
        EntityResolver resolver = domain.getEntityResolver();
        QueryMetadata queryMetadata = query == this.query ? metadata : query.getMetaData(resolver);
        execContext.addStatementContext(node, new StatementContext(query, queryMetadata, resolver));
    }

    @Override
    public DataNode nodeForDataMap(DataMap map) {
        if (map == null) {
            throw new NullPointerException("Null DataMap, can't determine DataNode.");
        }

        DataNode node = domain.lookupDataNode(map);

        if (node == null) {
            throw new CayenneRuntimeException("No DataNode exists for DataMap %s", map);
        }

        return node;
    }

    /**
     * @since 5.0
     */
    @Override
    public DataNode nodeForName(String name) {

        DataNode node;

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

    abstract static class ObjectConversionStrategy<T, R> {
        abstract List<? extends R> convert(List<T> mainRows);

        abstract R convert(T t);
    }

    private PrefetchProcessorNode toResultsTree(ClassDescriptor descriptor, PrefetchTreeNode prefetchTree,
                                                List<DataRow> normalizedRows) {

        // take a shortcut when no prefetches exist...
        if (prefetchTree == null) {
            // When results come from cache (not a refresh operation), don't refresh objects 
            // to avoid clobbering newer in-memory state
            boolean refresh = metadata.isRefreshingObjects() && !shouldSkipRefresh();
            return new ObjectResolver(context, descriptor, refresh)
                    .rootResultNodeFromDataRows(normalizedRows);
        } else {
            // When results come from cache (not a refresh operation), wrap metadata to prevent refreshing objects
            QueryMetadata effectiveMetadata = shouldSkipRefresh() && metadata.isRefreshingObjects()
                    ? nonRefreshingMetadata(metadata)
                    : metadata;
            return new HierarchicalObjectResolver(context, effectiveMetadata)
                    .rootResultNodeFromDataRows(prefetchTree, normalizedRows, prefetchResultsByPath);
        }
    }

    private boolean shouldSkipRefresh() {
        // Skip refresh only for cache hits to prevent stale cached data from clobbering newer in-memory state
        // For cache misses (including explicit refresh operations), cacheHit is false, so refresh happens normally
        // Prefetch relationships are resolved independently via connectToParents(), so this doesn't affect prefetch behavior
        return cacheHit;
    }

    private void performPostLoadCallbacks(PrefetchProcessorNode node, LifecycleEventManager postLoad) {

        if (node.hasChildren()) {
            for (PrefetchTreeNode child : node.getChildren()) {
                performPostLoadCallbacks((PrefetchProcessorNode) child, postLoad);
            }
        }

        List<Persistent> objects = node.getObjects();
        if (objects != null) {
            postLoad.onEvent(objects);
        }
    }

    private QueryMetadata nonRefreshingMetadata(QueryMetadata metadata) {
        return new QueryMetadataProxy(metadata) {
            @Override
            public boolean isRefreshingObjects() {
                return false;
            }
        };
    }

    class SingleObjectConversionStrategy extends ObjectConversionStrategy<DataRow, Persistent> {

        @Override
        public List<Persistent> convert(List<DataRow> mainRows) {

            PrefetchProcessorNode node = getPrefetchProcessorNode(mainRows);
            List<Persistent> objects = node.getObjects();

            // apply POST_LOAD callback
            LifecycleEventManager postLoad = domain.getLifecycleEventsManager().postLoad();
            if (postLoad.hasListeners()) {
                performPostLoadCallbacks(node, postLoad);
            }
            return objects != null ? objects : new ArrayList<>(1);
        }

        @Override
        Persistent convert(DataRow dataRow) {
            PrefetchProcessorNode node = getPrefetchProcessorNode(Collections.singletonList(dataRow));
            return node.getObjects().getFirst();
        }

        private PrefetchProcessorNode getPrefetchProcessorNode(List<DataRow> mainRows) {
            PrefetchTreeNode prefetchTree = metadata.getPrefetchTree();

            ResultSegments rsMapping = metadata.getResultSegments();
            EntityResultSegment resultSegment = null;
            if (rsMapping != null && !rsMapping.segments().isEmpty()) {
                resultSegment = (EntityResultSegment) rsMapping.segments().getFirst();
            }

            ClassDescriptor descriptor = resultSegment == null
                    ? metadata.getClassDescriptor()
                    : resultSegment.classDescriptor();

            return toResultsTree(descriptor, prefetchTree, mainRows);
        }
    }

    static class SingleScalarConversionStrategy extends ObjectConversionStrategy<Object, Object> {

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
            EmbeddableResultSegment resultSegment = (EmbeddableResultSegment) metadata.getResultSegments().segments().getFirst();
            Embeddable embeddable = resultSegment.embeddable();
            Class<? extends EmbeddableObject> embeddableClass = objectFactory.getJavaClass(embeddable.getClassName());
            DefaultConstructor<? extends EmbeddableObject> constructor = new DefaultConstructor<>(embeddableClass);
            List<EmbeddableObject> result = new ArrayList<>(mainRows.size());
            mainRows.forEach(dataRow -> {
                EmbeddableObject eo = constructor.newInstance();
                dataRow.forEach(eo::writePropertyDirectly);
                result.add(eo);
            });
            return result;
        }

        @Override
        EmbeddableObject convert(DataRow dataRow) {
            return convert(Collections.singletonList(dataRow)).getFirst();
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
            if (needConversion) {
                // reuse Object[]'s to fill them with resolved objects
                List<PrefetchProcessorNode> segmentNodes = doInPlaceConversion(result);

                // invoke callbacks now that all objects are resolved...
                LifecycleEventManager postLoad = domain.getLifecycleEventsManager().postLoad();
                if (postLoad.hasListeners()) {
                    for (PrefetchProcessorNode node : segmentNodes) {
                        performPostLoadCallbacks(node, postLoad);
                    }
                }
            }

            // distinct filtering
            if (!metadata.isSuppressingDistinct()) {
                Set<List<?>> seen = HashSet.newHashSet(result.size());
                result.removeIf(objects -> !seen.add(Arrays.asList(objects)));
            }

            return result;
        }

        @Override
        Object[] convert(Object[] objectsArray) {
            List<Object[]> objects = new ArrayList<>(1);
            objects.add(objectsArray);
            return convert(objects).getFirst();
        }

        private List<Object[]> createResultList(List<Object[]> mainRows, boolean needConversion) {
            if (!cachedResult) {
                // fast-path, we can reuse existing rows
                return mainRows;
            }

            if (!needConversion) {
                // no conversion needed, so can clone only top-level list
                return new ArrayList<>(mainRows);
            }

            // slowest path, deep copy everything
            List<Object[]> result = new ArrayList<>(mainRows.size());
            for (Object[] row : mainRows) {
                result.add(Arrays.copyOf(row, metadata.getResultSegments().segments().size()));
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
                        .rootResultNodeFromDataRows(rowsColumn);
            } else {
                HierarchicalObjectResolver resolver
                        = new HierarchicalObjectResolver(context, metadata, descriptor, true);
                return resolver.rootResultNodeFromDataRows(prefetchTree, rowsColumn, prefetchResultsByPath);
            }
        }

        private List<PrefetchProcessorNode> doInPlaceConversion(List<Object[]> result) {
            List<ResultSegment> resultSetMapping = metadata.getResultSegments().segments();
            int width = resultSetMapping.size();
            int height = result.size();
            List<PrefetchProcessorNode> segmentNodes = new ArrayList<>(width);
            for (int i = 0; i < width; i++) {
                ResultSegment mapping = resultSetMapping.get(i);
                if (mapping instanceof EntityResultSegment entitySegment) {
                    PrefetchProcessorNode nextResult = toResultsTree(entitySegment.classDescriptor(),
                            metadata.getPrefetchTree(), result, i);

                    segmentNodes.add(nextResult);

                    List<Persistent> objects = nextResult.getObjects();
                    for (int j = 0; j < height; j++) {
                        Object[] row = result.get(j);
                        row[i] = objects.get(j);
                    }
                } else if (mapping instanceof EmbeddableResultSegment resultSegment) {
                    Embeddable embeddable = resultSegment.embeddable();
                    Class<? extends EmbeddableObject> embeddableClass = objectFactory
                            .getJavaClass(embeddable.getClassName());
                    DefaultConstructor<? extends EmbeddableObject> constructor = new DefaultConstructor<>(embeddableClass);
                    for (Object[] row : result) {
                        DataRow dataRow = (DataRow) row[i];
                        EmbeddableObject eo = constructor.newInstance();
                        dataRow.forEach(eo::writePropertyDirectly);
                        row[i] = eo;
                    }
                }
            }
            return segmentNodes;
        }

        private boolean needConversion() {
            for (ResultSegment mapping : metadata.getResultSegments().segments()) {
                if (mapping instanceof EntityResultSegment
                        || mapping instanceof EmbeddableResultSegment) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class IdentityConversionStrategy extends ObjectConversionStrategy<Object, Object> {
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

    // Conversion strategy that uses mapper function to map raw result
    private static class MapperConversionStrategy extends ObjectConversionStrategy<Object, Object> {

        private final Function<Object, ?> mapper;
        private final ObjectConversionStrategy<Object, Object> parentStrategy;

        @SuppressWarnings({"unchecked", "rawtypes"})
        MapperConversionStrategy(Function<?, ?> mapper, ObjectConversionStrategy<?, ?> parentStrategy) {
            this.mapper = (Function) mapper;
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
}
