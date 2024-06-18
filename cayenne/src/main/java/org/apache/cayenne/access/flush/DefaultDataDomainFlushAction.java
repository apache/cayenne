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

package org.apache.cayenne.access.flush;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.ObjectStore;
import org.apache.cayenne.access.ObjectStoreGraphDiff;
import org.apache.cayenne.access.OperationObserver;
import org.apache.cayenne.access.flush.operation.DbRowOpMerger;
import org.apache.cayenne.access.flush.operation.DbRowOpSorter;
import org.apache.cayenne.access.flush.operation.DbRowOp;
import org.apache.cayenne.access.flush.operation.DbRowOpVisitor;
import org.apache.cayenne.access.flush.operation.OpIdFactory;
import org.apache.cayenne.access.flush.operation.UpdateDbRowOp;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

/**
 * Default implementation of {@link DataDomainFlushAction}.
 *
 * @since 4.2
 */
public class DefaultDataDomainFlushAction implements DataDomainFlushAction {

    protected final DataDomain dataDomain;
    protected final DbRowOpSorter dbRowOpSorter;
    protected final JdbcEventLogger jdbcEventLogger;
    protected final OperationObserver observer;

    protected DefaultDataDomainFlushAction(DataDomain dataDomain, DbRowOpSorter dbRowOpSorter, JdbcEventLogger jdbcEventLogger) {
        this.dataDomain = dataDomain;
        this.dbRowOpSorter = dbRowOpSorter;
        this.jdbcEventLogger = jdbcEventLogger;
        this.observer = new FlushObserver(jdbcEventLogger);
    }

    @Override
    public GraphDiff flush(DataContext context, GraphDiff changes) {
        CompoundDiff afterCommitDiff = new CompoundDiff();
        if (changes == null) {
            return afterCommitDiff;
        }
        if(!(changes instanceof ObjectStoreGraphDiff)) {
            throw new CayenneRuntimeException("Instance of ObjectStoreGraphDiff expected, got %s", changes.getClass());
        }

        ObjectStore objectStore = context.getObjectStore();
        ObjectStoreGraphDiff objectStoreGraphDiff = (ObjectStoreGraphDiff) changes;

        List<DbRowOp> dbRowOps = createDbRowOps(objectStore, objectStoreGraphDiff);
        updateObjectIds(dbRowOps);
        List<DbRowOp> deduplicatedOps = mergeSameObjectIds(dbRowOps);
        List<DbRowOp> filteredOps = filterOps(deduplicatedOps);
        List<DbRowOp> sortedOps = sort(filteredOps);
        List<? extends Query> queries = createQueries(sortedOps);
        executeQueries(queries);
        createReplacementIds(objectStore, afterCommitDiff, sortedOps);
        postprocess(context, objectStoreGraphDiff, afterCommitDiff, sortedOps);

        return afterCommitDiff;
    }

    /**
     * Create ops based on incoming graph changes
     * @param objectStore originating object store
     * @param changes object graph diff
     * @return collection of {@link DbRowOp}
     */
    protected List<DbRowOp> createDbRowOps(ObjectStore objectStore, ObjectStoreGraphDiff changes) {
        EntityResolver resolver = dataDomain.getEntityResolver();

        Map<Object, ObjectDiff> changesByObjectId = changes.getChangesByObjectId();
        List<DbRowOp> ops = new ArrayList<>(changesByObjectId.size());
        Set<ArcTarget> processedArcs = new HashSet<>();

        DbRowOpFactory factory = new DbRowOpFactory(resolver, objectStore, processedArcs);
        // ops.addAll() method is slower in this case as it will allocate new array for all values
        //noinspection UseBulkOperation
        changesByObjectId.forEach((obj, diff) -> factory.createRows(diff).forEach(ops::add));

        return ops;
    }

    /**
     * Fill in replacement IDs' data for given operations
     * @param dbRowOps collection of {@link DbRowOp}
     */
    protected void updateObjectIds(Collection<DbRowOp> dbRowOps) {
        DbRowOpVisitor<Void> permIdVisitor = new PermanentObjectIdVisitor(dataDomain);
        dbRowOps.forEach(row -> row.accept(permIdVisitor));
    }

    /**
     * @param dbRowOps collection of {@link DbRowOp}
     * @return collection of ops with merged duplicates
     */
    protected List<DbRowOp> mergeSameObjectIds(List<DbRowOp> dbRowOps) {
        Map<ObjectId, DbRowOp> index = new HashMap<>(dbRowOps.size());
        // new EffectiveOpId()
        dbRowOps.forEach(row -> index.merge(OpIdFactory.idForOperation(row.getChangeId()), row, new DbRowOpMerger()));
        // reuse list
        dbRowOps.clear();
        dbRowOps.addAll(index.values());
        return dbRowOps;
    }

    protected List<DbRowOp> filterOps(List<DbRowOp> dbRowOps) {
        // clear phantom update (this can be from insert/delete of arc with transient object)
        dbRowOps.forEach(row -> row.accept(PhantomDbRowOpCleaner.INSTANCE));
        return dbRowOps;
    }

    /**
     * Sort all operations
     * @param dbRowOps collection of {@link DbRowOp}
     * @return sorted collection of operations
     * @see DbRowOpSorter interface and it's default implementation
     */
    protected List<DbRowOp> sort(List<DbRowOp> dbRowOps) {
        return dbRowOpSorter.sort(dbRowOps);
    }

    /**
     *
     * @param dbRowOps collection of {@link DbRowOp}
     * @return collection of {@link Query} to perform
     */
    protected List<? extends Query> createQueries(List<DbRowOp> dbRowOps) {
        QueryCreatorVisitor queryCreator = new QueryCreatorVisitor(dbRowOps.size());
        dbRowOps.forEach(row -> row.accept(queryCreator));
        return queryCreator.getQueryList();
    }

    /**
     * Execute queries, grouping them by nodes
     * @param queries to execute
     */
    protected void executeQueries(List<? extends Query> queries) {
        EntityResolver entityResolver = dataDomain.getEntityResolver();
        queries.stream()
                .collect(Collectors.groupingBy(query
                        -> dataDomain.lookupDataNode(query.getMetaData(entityResolver).getDataMap())))
                .forEach((node, nodeQueries)
                        -> node.performQueries(nodeQueries, observer));
    }

    /**
     * Set final {@link ObjectId} for persistent objects
     *
     * @param store object store
     * @param afterCommitDiff result graph diff
     * @param dbRowOps collection of {@link DbRowOp}
     */
    protected void createReplacementIds(ObjectStore store, CompoundDiff afterCommitDiff, List<DbRowOp> dbRowOps) {
        ReplacementIdVisitor visitor = new ReplacementIdVisitor(store, dataDomain.getEntityResolver(), afterCommitDiff);
        dbRowOps.forEach(row -> row.accept(visitor));
    }

    /**
     * Notify {@link ObjectStore} and it's data row cache about actual changes we performed.
     *
     * @param context originating context
     * @param changes incoming diff
     * @param afterCommitDiff resulting diff
     * @param dbRowOps collection of {@link DbRowOp}
     */
    protected void postprocess(DataContext context, ObjectStoreGraphDiff changes, CompoundDiff afterCommitDiff, List<DbRowOp> dbRowOps) {
        ObjectStore objectStore = context.getObjectStore();

        PostprocessVisitor postprocessor = new PostprocessVisitor(context);
        dbRowOps.forEach(row -> row.accept(postprocessor));

        DataDomainIndirectDiffBuilder indirectDiffBuilder = new DataDomainIndirectDiffBuilder(context.getEntityResolver());
        indirectDiffBuilder.processChanges(changes);

        objectStore.getDataRowCache()
                .processSnapshotChanges(
                        objectStore,
                        postprocessor.getUpdatedSnapshots(),
                        postprocessor.getDeletedIds(),
                        Collections.emptyList(),
                        indirectDiffBuilder.getIndirectModifications()
                );
        objectStore.postprocessAfterCommit(afterCommitDiff);
    }

    protected static class PhantomDbRowOpCleaner implements DbRowOpVisitor<Void> {

        protected static final DbRowOpVisitor<Void> INSTANCE = new PhantomDbRowOpCleaner();

        @Override
        public Void visitUpdate(UpdateDbRowOp dbRow) {
            //
            if(dbRow.getChangeId().isTemporary() && !dbRow.getChangeId().isReplacementIdAttached()) {
                dbRow.getValues().clear();
            }
            return null;
        }
    }
}
