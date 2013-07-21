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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A stateful commit handler used by DataContext to perform commit operation.
 * DataContextCommitAction resolves primary key dependencies, referential integrity
 * dependencies (including multi-reflexive entities), generates primary keys, creates
 * batches for massive data modifications, assigns operations to data nodes.
 * 
 * @since 1.2
 */
class DataDomainFlushAction {

    private final DataDomain domain;
    private DataContext context;
    private Map<Object, ObjectDiff> changesByObjectId;

    private CompoundDiff resultDiff;
    private Collection<ObjectId> resultDeletedIds;
    private Map<ObjectId, DataRow> resultModifiedSnapshots;
    private Collection<ObjectId> resultIndirectlyModifiedIds;

    private DataDomainInsertBucket insertBucket;
    private DataDomainUpdateBucket updateBucket;
    private DataDomainDeleteBucket deleteBucket;
    private DataDomainFlattenedBucket flattenedBucket;

    private List<Query> queries;

    private JdbcEventLogger logger;

    DataDomainFlushAction(DataDomain domain) {
        this.domain = domain;
    }

    DataDomain getDomain() {
        return domain;
    }

    DataContext getContext() {
        return context;
    }

    Collection<ObjectId> getResultDeletedIds() {
        return resultDeletedIds;
    }

    CompoundDiff getResultDiff() {
        return resultDiff;
    }

    Collection<ObjectId> getResultIndirectlyModifiedIds() {
        return resultIndirectlyModifiedIds;
    }

    Map<ObjectId, DataRow> getResultModifiedSnapshots() {
        return resultModifiedSnapshots;
    }

    public void setJdbcEventLogger(JdbcEventLogger logger) {
        this.logger = logger;
    }

    public JdbcEventLogger getJdbcEventLogger() {
        return this.logger;
    }

    ObjectDiff objectDiff(Object objectId) {
        return changesByObjectId.get(objectId);
    }

    void addFlattenedInsert(DbEntity flattenedEntity, FlattenedArcKey flattenedInsertInfo) {
        flattenedBucket.addFlattenedInsert(flattenedEntity, flattenedInsertInfo);
    }

    void addFlattenedDelete(DbEntity flattenedEntity, FlattenedArcKey flattenedDeleteInfo) {
        flattenedBucket.addFlattenedDelete(flattenedEntity, flattenedDeleteInfo);
    }

    GraphDiff flush(DataContext context, GraphDiff changes) {

        if (changes == null) {
            return new CompoundDiff();
        }

        // TODO: Andrus, 3/13/2006 - support categorizing an arbitrary diff
        if (!(changes instanceof ObjectStoreGraphDiff)) {
            throw new IllegalArgumentException("Expected 'ObjectStoreGraphDiff', got: "
                    + changes.getClass().getName());
        }

        this.context = context;

        // ObjectStoreGraphDiff contains changes already categorized by objectId...
        this.changesByObjectId = ((ObjectStoreGraphDiff) changes).getChangesByObjectId();
        this.insertBucket = new DataDomainInsertBucket(this);
        this.deleteBucket = new DataDomainDeleteBucket(this);
        this.updateBucket = new DataDomainUpdateBucket(this);
        this.flattenedBucket = new DataDomainFlattenedBucket(this);

        this.queries = new ArrayList<Query>();
        this.resultIndirectlyModifiedIds = new HashSet<ObjectId>();

        preprocess(context, changes);

        if (queries.isEmpty()) {
            return new CompoundDiff();
        }

        this.resultDiff = new CompoundDiff();
        this.resultDeletedIds = new ArrayList<ObjectId>();
        this.resultModifiedSnapshots = new HashMap<ObjectId, DataRow>();

        runQueries();

        postprocess(context);
        return resultDiff;
    }

    private void preprocess(DataContext context, GraphDiff changes) {

        // categorize dirty objects by state

        ObjectStore objectStore = context.getObjectStore();

        Iterator<?> it = changesByObjectId.keySet().iterator();
        while (it.hasNext()) {
            ObjectId id = (ObjectId) it.next();
            Persistent object = (Persistent) objectStore.getNode(id);
            ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                    id.getEntityName());

            switch (object.getPersistenceState()) {
                case PersistenceState.NEW:
                    insertBucket.addDirtyObject(object, descriptor);
                    break;
                case PersistenceState.MODIFIED:
                    updateBucket.addDirtyObject(object, descriptor);
                    break;
                case PersistenceState.DELETED:
                    deleteBucket.addDirtyObject(object, descriptor);
                    break;
            }
        }

        new DataDomainIndirectDiffBuilder(this).processIndirectChanges(changes);

        insertBucket.appendQueries(queries);
        flattenedBucket.appendInserts(queries);
        updateBucket.appendQueries(queries);
        flattenedBucket.appendDeletes(queries);
        deleteBucket.appendQueries(queries);
    }

    private void runQueries() {
        DataDomainFlushObserver observer = new DataDomainFlushObserver(
                domain.getJdbcEventLogger());

        // split query list by spanned nodes and run each single node range individually.
        // Since connections are reused per node within an open transaction, there should
        // not be much overhead in accessing the same node multiple times (may happen due
        // to imperfect sorting)

        try {

            DataNode lastNode = null;
            DbEntity lastEntity = null;
            int rangeStart = 0;
            int len = queries.size();

            for (int i = 0; i < len; i++) {

                BatchQuery query = (BatchQuery) queries.get(i);
                if (query.getDbEntity() != lastEntity) {
                    lastEntity = query.getDbEntity();

                    DataNode node = domain.lookupDataNode(lastEntity.getDataMap());
                    if (node != lastNode) {

                        if (i - rangeStart > 0) {
                            lastNode.performQueries(
                                    queries.subList(rangeStart, i),
                                    observer);
                        }

                        rangeStart = i;
                        lastNode = node;
                    }
                }
            }

            // process last segment of the query list...
            lastNode.performQueries(queries.subList(rangeStart, len), observer);
        }
        catch (Throwable th) {
            Transaction.getThreadTransaction().setRollbackOnly();
            throw new CayenneRuntimeException("Transaction was rolledback.", th);
        }
    }

    /*
     * Sends notification of changes to the DataRowStore, returns GraphDiff with replaced
     * ObjectIds.
     */
    private void postprocess(DataContext context) {

        deleteBucket.postprocess();
        updateBucket.postprocess();
        insertBucket.postprocess();

        // notify cache...
        if (!resultDeletedIds.isEmpty()
                || !resultModifiedSnapshots.isEmpty()
                || !resultIndirectlyModifiedIds.isEmpty()) {

            context
                    .getObjectStore()
                    .getDataRowCache()
                    .processSnapshotChanges(
                            context.getObjectStore(),
                            resultModifiedSnapshots,
                            resultDeletedIds,
                            Collections.EMPTY_LIST,
                            resultIndirectlyModifiedIds);
        }

        context.getObjectStore().postprocessAfterCommit(resultDiff);
    }
}
