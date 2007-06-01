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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.util.BatchQueryUtils;
import org.objectstyle.cayenne.access.util.ContextCommitObserver;
import org.objectstyle.cayenne.access.util.DataNodeCommitHelper;
import org.objectstyle.cayenne.access.util.PrimaryKeyHelper;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.EntitySorter;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.DeleteBatchQuery;
import org.objectstyle.cayenne.query.InsertBatchQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.UpdateBatchQuery;

/**
 * ContextCommit implements DataContext commit logic. DataContext internally delegates
 * commit operations to an instance of ContextCommit. ContextCommit resolves primary key
 * dependencies, referential integrity dependencies (including multi-reflexive entities),
 * generates primary keys, creates batches for massive data modifications, assigns
 * operations to data nodes. It indirectly relies on graph algorithms provided by ASHWOOD
 * library.
 * 
 * @author Andriy Shapochka
 */

class ContextCommit {

    private static Logger logObj = Logger.getLogger(ContextCommit.class);

    private DataContext context;
    private Level logLevel;
    private Map newObjectsByObjEntity;
    private Map objectsToDeleteByObjEntity;
    private Map objectsToUpdateByObjEntity;
    private List objEntitiesToInsert;
    private List objEntitiesToDelete;
    private List objEntitiesToUpdate;
    private List nodeHelpers;
    private List insObjects; //event support
    private List delObjects; //event support
    private List updObjects; //event support

    ContextCommit(DataContext contextToCommit) {
        context = contextToCommit;
    }

    /**
     * Commits changes in the enclosed DataContext.
     */
    void commit(Level logLevel) throws CayenneException {
        if (logLevel == null) {
            logLevel = QueryLogger.DEFAULT_LOG_LEVEL;
        }

        this.logLevel = logLevel;

        // synchronize on both object store and underlying DataRowStore
        synchronized (context.getObjectStore()) {
            synchronized (context.getObjectStore().getDataRowCache()) {

                categorizeObjects();
                createPrimaryKeys();
                categorizeFlattenedInsertsAndCreateBatches();
                categorizeFlattenedDeletesAndCreateBatches();

                insObjects = new ArrayList();
                delObjects = new ArrayList();
                updObjects = new ArrayList();

                for (Iterator i = nodeHelpers.iterator(); i.hasNext();) {
                    DataNodeCommitHelper nodeHelper = (DataNodeCommitHelper) i.next();
                    prepareInsertQueries(nodeHelper);
                    prepareFlattenedQueries(nodeHelper, nodeHelper
                            .getFlattenedInsertQueries());

                    prepareUpdateQueries(nodeHelper);

                    prepareFlattenedQueries(nodeHelper, nodeHelper
                            .getFlattenedDeleteQueries());

                    prepareDeleteQueries(nodeHelper);
                }

                ContextCommitObserver observer = new ContextCommitObserver(
                        logLevel,
                        context,
                        insObjects,
                        updObjects,
                        delObjects);

                if (context.isTransactionEventsEnabled()) {
                    observer.registerForDataContextEvents();
                }

                try {
                    context.fireWillCommit();

                    Transaction transaction = context
                            .getParentDataDomain()
                            .createTransaction();
                    transaction.begin();

                    try {
                        Iterator i = nodeHelpers.iterator();
                        while (i.hasNext()) {
                            DataNodeCommitHelper nodeHelper = (DataNodeCommitHelper) i
                                    .next();
                            List queries = nodeHelper.getQueries();

                            if (queries.size() > 0) {
                                // note: observer throws on error
                                nodeHelper.getNode().performQueries(
                                        queries,
                                        observer,
                                        transaction);
                            }
                        }

                        // commit
                        transaction.commit();
                    }
                    catch (Throwable th) {
                        try {
                            // rollback
                            transaction.rollback();
                        }
                        catch (Throwable rollbackTh) {
                            // ignoring...
                        }

                        context.fireTransactionRolledback();
                        throw new CayenneException("Transaction was rolledback.", th);
                    }

                    context.getObjectStore().objectsCommitted();
                    context.fireTransactionCommitted();
                }
                finally {
                    if (context.isTransactionEventsEnabled()) {
                        observer.unregisterFromDataContextEvents();
                    }
                }
            }
        }
    }

    private void prepareInsertQueries(DataNodeCommitHelper commitHelper)
            throws CayenneException {

        List entities = commitHelper.getObjEntitiesForInsert();
        if (entities.isEmpty()) {
            return;
        }

        List dbEntities = new ArrayList(entities.size());
        Map objEntitiesByDbEntity = new HashMap(entities.size());
        groupObjEntitiesBySpannedDbEntities(dbEntities, objEntitiesByDbEntity, entities);

        EntitySorter sorter = commitHelper.getNode().getEntitySorter();
        sorter.sortDbEntities(dbEntities, false);

        for (Iterator i = dbEntities.iterator(); i.hasNext();) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            InsertBatchQuery batch = new InsertBatchQuery(dbEntity, 27);
            batch.setLoggingLevel(logLevel);
            if (logObj.isDebugEnabled()) {
                logObj.debug("Creating InsertBatchQuery for DbEntity "
                        + dbEntity.getName());
            }
            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ObjEntity entity = (ObjEntity) j.next();
                boolean isMasterDbEntity = (entity.getDbEntity() == dbEntity);
                DbRelationship masterDependentDbRel = (isMasterDbEntity
                        ? null
                        : findMasterToDependentDbRelationship(
                                entity.getDbEntity(),
                                dbEntity));

                List objects = (List) newObjectsByObjEntity.get(entity.getClassName());

                // throw an exception - an attempt to modify read-only entity
                if (entity.isReadOnly() && objects.size() > 0) {
                    throw attemptToCommitReadOnlyEntity(objects.get(0).getClass(), entity);
                }

                if (isMasterDbEntity)
                    sorter.sortObjectsForEntity(entity, objects, false);

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();
                    //                    batch.add(context.takeObjectSnapshot(o));
                    Map snapshot = BatchQueryUtils.buildSnapshotForInsert(
                            entity,
                            o,
                            masterDependentDbRel);
                    batch.add(snapshot);
                }

                if (isMasterDbEntity)
                    insObjects.addAll(objects);
            }
            commitHelper.getQueries().add(batch);
        }
    }

    private void prepareDeleteQueries(DataNodeCommitHelper commitHelper)
            throws CayenneException {

        List entities = commitHelper.getObjEntitiesForDelete();
        if (entities.isEmpty()) {
            return;
        }

        List dbEntities = new ArrayList(entities.size());
        Map objEntitiesByDbEntity = new HashMap(entities.size());
        groupObjEntitiesBySpannedDbEntities(dbEntities, objEntitiesByDbEntity, entities);

        EntitySorter sorter = commitHelper.getNode().getEntitySorter();
        //        sorter.sortObjEntities(entities, true);
        sorter.sortDbEntities(dbEntities, true);

        for (Iterator i = dbEntities.iterator(); i.hasNext();) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            DeleteBatchQuery batch = new DeleteBatchQuery(dbEntity, 27);
            batch.setLoggingLevel(logLevel);

            if (logObj.isDebugEnabled())
                logObj.debug("Creating DeleteBatchQuery for DbEntity "
                        + dbEntity.getName());

            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ObjEntity entity = (ObjEntity) j.next();

                boolean isMasterDbEntity = (entity.getDbEntity() == dbEntity);
                DbRelationship masterDependentDbRel = (isMasterDbEntity
                        ? null
                        : findMasterToDependentDbRelationship(
                                entity.getDbEntity(),
                                dbEntity));

                List objects = (List) objectsToDeleteByObjEntity.get(entity
                        .getClassName());

                // throw an exception - an attempt to delete read-only entity
                if (entity.isReadOnly() && objects.size() > 0) {
                    throw attemptToCommitReadOnlyEntity(objects.get(0).getClass(), entity);
                }

                if (isMasterDbEntity) {
                    sorter.sortObjectsForEntity(entity, objects, true);
                }

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();

                    // check if object was modified from underneath and consult the
                    // delegate
                    // if this is the case...
                    // checkConcurrentModifications(o);

                    Map id = o.getObjectId().getIdSnapshot();
                    if (id != null && !id.isEmpty()) {
                        if (!isMasterDbEntity && masterDependentDbRel != null)
                            id = masterDependentDbRel.targetPkSnapshotWithSrcSnapshot(id);
                        batch.add(id);
                    }
                }

                if (isMasterDbEntity)
                    delObjects.addAll(objects);
            }
            commitHelper.getQueries().add(batch);
        }
    }

    private void prepareUpdateQueries(DataNodeCommitHelper commitHelper)
            throws CayenneException {
        List entities = commitHelper.getObjEntitiesForUpdate();
        if (entities.isEmpty()) {
            return;
        }

        List dbEntities = new ArrayList(entities.size());
        Map objEntitiesByDbEntity = new HashMap(entities.size());
        groupObjEntitiesBySpannedDbEntities(dbEntities, objEntitiesByDbEntity, entities);

        for (Iterator i = dbEntities.iterator(); i.hasNext();) {
            DbEntity dbEntity = (DbEntity) i.next();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            Map batches = new LinkedMap();

            for (Iterator j = objEntitiesForDbEntity.iterator(); j.hasNext();) {
                ObjEntity entity = (ObjEntity) j.next();

                // Per-objEntity optimistic locking conditional
                boolean optimisticLocking = (ObjEntity.LOCK_TYPE_OPTIMISTIC == entity
                        .getLockType());

                List qualifierAttributes = qualifierAttributes(entity, optimisticLocking);

                boolean isMasterDbEntity = (entity.getDbEntity() == dbEntity);

                DbRelationship masterDependentDbRel = (isMasterDbEntity)
                        ? null
                        : findMasterToDependentDbRelationship(
                                entity.getDbEntity(),
                                dbEntity);
                List objects = (List) objectsToUpdateByObjEntity.get(entity
                        .getClassName());

                for (Iterator k = objects.iterator(); k.hasNext();) {
                    DataObject o = (DataObject) k.next();

                    Map snapshot = BatchQueryUtils.buildSnapshotForUpdate(
                            entity,
                            o,
                            masterDependentDbRel);

                    // check whether MODIFIED object has real db-level
                    // modifications
                    if (snapshot.isEmpty()) {
                        o.setPersistenceState(PersistenceState.COMMITTED);
                        continue;
                    }

                    // after we filtered out "fake" modifications, check if an
                    // attempt is made to modify a read only entity
                    if (entity.isReadOnly()) {
                        throw attemptToCommitReadOnlyEntity(o.getClass(), entity);
                    }

                    // build qualifier snapshot
                    Map idSnapshot = o.getObjectId().getIdSnapshot();

                    if (!isMasterDbEntity && masterDependentDbRel != null) {
                        idSnapshot = masterDependentDbRel
                                .targetPkSnapshotWithSrcSnapshot(idSnapshot);
                    }

                    Map qualifierSnapshot = idSnapshot;
                    if (optimisticLocking) {
                        // clone snapshot and add extra keys...
                        qualifierSnapshot = new HashMap(qualifierSnapshot);
                        appendOptimisticLockingAttributes(
                                qualifierSnapshot,
                                o,
                                qualifierAttributes);
                    }

                    // organize batches by the updated columns + nulls in qualifier
                    Set snapshotSet = snapshot.keySet();
                    Set nullQualifierNames = new HashSet();
                    Iterator it = qualifierSnapshot.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        if (entry.getValue() == null) {
                            nullQualifierNames.add(entry.getKey());
                        }
                    }

                    List batchKey = Arrays.asList(new Object[] {
                            snapshotSet, nullQualifierNames
                    });

                    UpdateBatchQuery batch = (UpdateBatchQuery) batches.get(batchKey);
                    if (batch == null) {
                        batch = new UpdateBatchQuery(
                                dbEntity,
                                qualifierAttributes,
                                updatedAttributes(dbEntity, snapshot),
                                nullQualifierNames,
                                10);
                        batch.setLoggingLevel(logLevel);
                        batch.setUsingOptimisticLocking(optimisticLocking);
                        batches.put(batchKey, batch);
                    }

                    batch.add(qualifierSnapshot, snapshot);

                    if (isMasterDbEntity) {
                        ObjectId updId = updatedId(
                                o.getObjectId().getObjClass(),
                                idSnapshot,
                                snapshot);
                        if (updId != null) {
                            o.getObjectId().setReplacementId(updId);
                        }

                        updObjects.add(o);
                    }
                }
            }
            commitHelper.getQueries().addAll(batches.values());
        }
    }

    /**
     * Creates a list of DbAttributes that should be used in update WHERE clause.
     */
    private List qualifierAttributes(ObjEntity entity, boolean optimisticLocking) {
        if (!optimisticLocking) {
            return entity.getDbEntity().getPrimaryKey();
        }

        List attributes = new ArrayList(entity.getDbEntity().getPrimaryKey());

        Iterator attributeIt = entity.getAttributes().iterator();
        while (attributeIt.hasNext()) {
            ObjAttribute attribute = (ObjAttribute) attributeIt.next();

            if (attribute.isUsedForLocking()) {
                // only care about first step in a flattened attribute
                DbAttribute dbAttribute = (DbAttribute) attribute
                        .getDbPathIterator()
                        .next();

                if (!attributes.contains(dbAttribute)) {
                    attributes.add(dbAttribute);
                }
            }
        }

        Iterator relationshipIt = entity.getRelationships().iterator();
        while (relationshipIt.hasNext()) {
            ObjRelationship relationship = (ObjRelationship) relationshipIt.next();

            if (relationship.isUsedForLocking()) {
                // only care about the first DbRelationship
                DbRelationship dbRelationship = (DbRelationship) relationship
                        .getDbRelationships()
                        .get(0);

                Iterator joinsIterator = dbRelationship.getJoins().iterator();
                while (joinsIterator.hasNext()) {
                    DbJoin dbAttrPair = (DbJoin) joinsIterator.next();
                    DbAttribute dbAttribute = dbAttrPair.getSource();
                    if (!attributes.contains(dbAttribute)) {
                        attributes.add(dbAttribute);
                    }
                }
            }
        }

        return attributes;
    }

    /**
     * Creates a list of DbAttributes that are updated in a snapshot
     * 
     * @param entity
     * @return
     */
    private List updatedAttributes(DbEntity entity, Map updatedSnapshot) {
        List attributes = new ArrayList(updatedSnapshot.size());
        Map entityAttributes = entity.getAttributeMap();

        Iterator it = updatedSnapshot.keySet().iterator();
        while (it.hasNext()) {
            Object name = it.next();
            attributes.add(entityAttributes.get(name));
        }

        return attributes;
    }

    /**
     * Appends values used for optimistic locking to a given snapshot.
     */
    private void appendOptimisticLockingAttributes(
            Map qualifierSnapshot,
            DataObject dataObject,
            List qualifierAttributes) throws CayenneException {

        Map snapshot = dataObject.getDataContext().getObjectStore().getRetainedSnapshot(
                dataObject.getObjectId());

        Iterator it = qualifierAttributes.iterator();
        while (it.hasNext()) {
            DbAttribute attribute = (DbAttribute) it.next();
            String name = attribute.getName();
            if (!qualifierSnapshot.containsKey(name)) {
                qualifierSnapshot.put(name, snapshot.get(name));
            }
        }
    }

    private void createPrimaryKeys() throws CayenneException {

        DataDomain domain = context.getParentDataDomain();
        PrimaryKeyHelper pkHelper = domain.getPrimaryKeyHelper();

        Collections.sort(objEntitiesToInsert, pkHelper.getObjEntityComparator());
        Iterator i = objEntitiesToInsert.iterator();
        while (i.hasNext()) {
            ObjEntity currentEntity = (ObjEntity) i.next();
            List dataObjects = (List) newObjectsByObjEntity.get(currentEntity
                    .getClassName());
            pkHelper.createPermIdsForObjEntity(currentEntity, dataObjects);
        }
    }

    /**
     * Organizes committed objects by node, performs sorting operations.
     */
    private void categorizeObjects() throws CayenneException {
        this.nodeHelpers = new ArrayList();

        Iterator it = context.getObjectStore().getObjectIterator();
        newObjectsByObjEntity = new HashMap();
        objectsToDeleteByObjEntity = new HashMap();
        objectsToUpdateByObjEntity = new HashMap();
        objEntitiesToInsert = new ArrayList();
        objEntitiesToDelete = new ArrayList();
        objEntitiesToUpdate = new ArrayList();

        while (it.hasNext()) {
            DataObject nextObject = (DataObject) it.next();
            int objectState = nextObject.getPersistenceState();
            switch (objectState) {
                case PersistenceState.NEW:
                    objectToInsert(nextObject);
                    break;
                case PersistenceState.DELETED:
                    objectToDelete(nextObject);
                    break;
                case PersistenceState.MODIFIED:
                    objectToUpdate(nextObject);
                    break;
            }
        }
    }

    private void objectToInsert(DataObject o) throws CayenneException {
        classifyByEntityAndNode(
                o,
                newObjectsByObjEntity,
                objEntitiesToInsert,
                DataNodeCommitHelper.INSERT);
    }

    private void objectToDelete(DataObject o) throws CayenneException {
        classifyByEntityAndNode(
                o,
                objectsToDeleteByObjEntity,
                objEntitiesToDelete,
                DataNodeCommitHelper.DELETE);
    }

    private void objectToUpdate(DataObject o) throws CayenneException {
        classifyByEntityAndNode(
                o,
                objectsToUpdateByObjEntity,
                objEntitiesToUpdate,
                DataNodeCommitHelper.UPDATE);
    }

    private RuntimeException attemptToCommitReadOnlyEntity(
            Class objectClass,
            ObjEntity entity) {
        String className = (objectClass != null) ? objectClass.getName() : "<null>";
        StringBuffer message = new StringBuffer();
        message
                .append("Class '")
                .append(className)
                .append("' maps to a read-only entity");

        if (entity != null) {
            message.append(" '").append(entity.getName()).append("'");
        }

        message.append(". Can't commit changes.");
        return new CayenneRuntimeException(message.toString());
    }

    /**
     * Performs classification of a DataObject for the DML operation. Throws
     * CayenneRuntimeException if an object can't be classified.
     */
    private void classifyByEntityAndNode(
            DataObject o,
            Map objectsByObjEntity,
            List objEntities,
            int operationType) {

        Class objEntityClass = o.getObjectId().getObjClass();
        ObjEntity entity = context.getEntityResolver().lookupObjEntity(objEntityClass);
        Collection objectsForObjEntity = (Collection) objectsByObjEntity
                .get(objEntityClass.getName());
        if (objectsForObjEntity == null) {
            objEntities.add(entity);
            DataNode responsibleNode = context.lookupDataNode(entity.getDataMap());

            DataNodeCommitHelper commitHelper = DataNodeCommitHelper.getHelperForNode(
                    nodeHelpers,
                    responsibleNode);

            commitHelper.addToEntityList(entity, operationType);
            objectsForObjEntity = new ArrayList();
            objectsByObjEntity.put(objEntityClass.getName(), objectsForObjEntity);
        }
        objectsForObjEntity.add(o);
    }

    private void categorizeFlattenedInsertsAndCreateBatches() {
        Iterator i = context.getObjectStore().getFlattenedInserts().iterator();

        while (i.hasNext()) {
            FlattenedRelationshipInfo info = (FlattenedRelationshipInfo) i.next();

            DataObject source = info.getSource();

            // TODO: does it ever happen? How?
            if (source.getPersistenceState() == PersistenceState.DELETED) {
                continue;
            }

            if (info.getDestination().getPersistenceState() == PersistenceState.DELETED) {
                continue;
            }

            DbEntity flattenedEntity = info.getJoinEntity();
            DataNode responsibleNode = context.lookupDataNode(flattenedEntity
                    .getDataMap());
            DataNodeCommitHelper commitHelper = DataNodeCommitHelper.getHelperForNode(
                    nodeHelpers,
                    responsibleNode);
            Map batchesByDbEntity = commitHelper.getFlattenedInsertQueries();

            InsertBatchQuery relationInsertQuery = (InsertBatchQuery) batchesByDbEntity
                    .get(flattenedEntity);

            if (relationInsertQuery == null) {
                relationInsertQuery = new InsertBatchQuery(flattenedEntity, 50);
                relationInsertQuery.setLoggingLevel(logLevel);
                batchesByDbEntity.put(flattenedEntity, relationInsertQuery);
            }

            Map flattenedSnapshot = info.buildJoinSnapshotForInsert();
            relationInsertQuery.add(flattenedSnapshot);
        }
    }

    private void categorizeFlattenedDeletesAndCreateBatches() {
        Iterator i = context.getObjectStore().getFlattenedDeletes().iterator();

        while (i.hasNext()) {
            FlattenedRelationshipInfo info = (FlattenedRelationshipInfo) i.next();

            // TODO: does it ever happen?
            Map sourceId = info.getSource().getObjectId().getIdSnapshot();

            if (sourceId == null)
                continue;

            Map dstId = info.getDestination().getObjectId().getIdSnapshot();
            if (dstId == null)
                continue;

            DbEntity flattenedEntity = info.getJoinEntity();

            DataNode responsibleNode = context.lookupDataNode(flattenedEntity
                    .getDataMap());
            DataNodeCommitHelper commitHelper = DataNodeCommitHelper.getHelperForNode(
                    nodeHelpers,
                    responsibleNode);
            Map batchesByDbEntity = commitHelper.getFlattenedDeleteQueries();

            DeleteBatchQuery relationDeleteQuery = (DeleteBatchQuery) batchesByDbEntity
                    .get(flattenedEntity);
            if (relationDeleteQuery == null) {
                relationDeleteQuery = new DeleteBatchQuery(flattenedEntity, 50);
                relationDeleteQuery.setLoggingLevel(logLevel);
                batchesByDbEntity.put(flattenedEntity, relationDeleteQuery);
            }

            List flattenedSnapshots = info.buildJoinSnapshotsForDelete();
            if (!flattenedSnapshots.isEmpty()) {
                Iterator snapsIt = flattenedSnapshots.iterator();
                while(snapsIt.hasNext()) {
                    relationDeleteQuery.add((Map) snapsIt.next());
                }
            }
        }
    }

    private void prepareFlattenedQueries(
            DataNodeCommitHelper commitHelper,
            Map flattenedBatches) {

        for (Iterator i = flattenedBatches.values().iterator(); i.hasNext();) {
            commitHelper.addToQueries((Query) i.next());
        }
    }

    private ObjectId updatedId(Class objEntityClass, Map idMap, Map updAttrs) {
        Iterator it = updAttrs.keySet().iterator();
        HashMap newIdMap = null;
        while (it.hasNext()) {
            Object key = it.next();
            if (!idMap.containsKey(key))
                continue;
            if (newIdMap == null)
                newIdMap = new HashMap(idMap);
            newIdMap.put(key, updAttrs.get(key));
        }
        return (newIdMap != null) ? new ObjectId(objEntityClass, newIdMap) : null;
    }

    private void groupObjEntitiesBySpannedDbEntities(
            List dbEntities,
            Map objEntitiesByDbEntity,
            List objEntities) {
        for (Iterator i = objEntities.iterator(); i.hasNext();) {
            ObjEntity objEntity = (ObjEntity) i.next();
            DbEntity dbEntity = objEntity.getDbEntity();
            List objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
            if (objEntitiesForDbEntity == null) {
                objEntitiesForDbEntity = new ArrayList(1);
                dbEntities.add(dbEntity);
                objEntitiesByDbEntity.put(dbEntity, objEntitiesForDbEntity);
            }
            if (!objEntitiesForDbEntity.contains(objEntity))
                objEntitiesForDbEntity.add(objEntity);
            for (Iterator j = objEntity.getAttributeMap().values().iterator(); j
                    .hasNext();) {
                ObjAttribute objAttribute = (ObjAttribute) j.next();
                if (!objAttribute.isCompound())
                    continue;
                dbEntity = (DbEntity) objAttribute.getDbAttribute().getEntity();
                objEntitiesForDbEntity = (List) objEntitiesByDbEntity.get(dbEntity);
                if (objEntitiesForDbEntity == null) {
                    objEntitiesForDbEntity = new ArrayList(1);
                    dbEntities.add(dbEntity);
                    objEntitiesByDbEntity.put(dbEntity, objEntitiesForDbEntity);
                }
                if (!objEntitiesForDbEntity.contains(objEntity))
                    objEntitiesForDbEntity.add(objEntity);
            }
        }
    }

    private DbRelationship findMasterToDependentDbRelationship(
            DbEntity masterDbEntity,
            DbEntity dependentDbEntity) {
        if (masterDbEntity.equals(dependentDbEntity))
            return null;
        for (Iterator i = masterDbEntity.getRelationshipMap().values().iterator(); i
                .hasNext();) {
            DbRelationship rel = (DbRelationship) i.next();
            if (dependentDbEntity.equals(rel.getTargetEntity()) && rel.isToDependentPK())
                return rel;
        }
        return null;
    }
}