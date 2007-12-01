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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataDomainSyncBucket.PropagatedValueFactory;
import org.apache.cayenne.access.util.DefaultOperationObserver;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;

/**
 * A holder of flattened relationship modification data.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
final class FlattenedArcKey {

    ObjectId sourceId;
    ObjectId destinationId;
    ObjRelationship relationship;
    ObjRelationship reverseRelationship;
    String compareToken;

    FlattenedArcKey(ObjectId sourceId, ObjectId destinationId,
            ObjRelationship relationship) {

        this.sourceId = sourceId;
        this.destinationId = destinationId;
        this.relationship = relationship;
        this.reverseRelationship = relationship.getReverseRelationship();

        // build a string token to make comparison (or at least hashcode) indepent from
        // direction
        String relName1 = relationship.getName();
        if (reverseRelationship != null) {
            String relName2 = reverseRelationship.getName();

            // Find the lexically lesser name and use it as the name of the source, then
            // use the second.
            // If equal (the same name), it doesn't matter which order...
            if (relName1.compareTo(relName2) <= 0) {
                this.compareToken = relName1 + "." + relName2;
            }
            else {
                this.compareToken = relName2 + "." + relName1;
            }
        }
        else {
            this.compareToken = relName1;
        }
    }

    /**
     * Returns a join DbEntity for the single-step flattened relationship.
     */
    DbEntity getJoinEntity() {
        List relList = relationship.getDbRelationships();
        if (relList.size() != 2) {
            throw new CayenneRuntimeException(
                    "Only single-step flattened relationships are supported in this operation: "
                            + relationship);
        }

        DbRelationship firstDbRel = (DbRelationship) relList.get(0);
        return (DbEntity) firstDbRel.getTargetEntity();
    }

    /**
     * Returns a snapshot for join record for the single-step flattened relationship,
     * generating value for the primary key column if it is not propagated via the
     * relationships.
     */
    Map buildJoinSnapshotForInsert(DataNode node) {
        Map snapshot = lazyJoinSnapshot();

        boolean autoPkDone = false;
        DbEntity joinEntity = getJoinEntity();

        for (DbAttribute dbAttr : joinEntity.getPrimaryKeys()) {
            String dbAttrName = dbAttr.getName();
            if (snapshot.containsKey(dbAttrName)) {
                continue;
            }

            if (autoPkDone) {
                throw new CayenneRuntimeException(
                        "Primary Key autogeneration only works for a single attribute.");
            }

            // finally, use database generation mechanism
            try {
                PkGenerator pkGenerator = node.getAdapter().getPkGenerator();
                Object pkValue = pkGenerator.generatePkForDbEntity(node, joinEntity);
                snapshot.put(dbAttrName, pkValue);
                autoPkDone = true;
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException("Error generating PK: "
                        + ex.getMessage(), ex);
            }
        }

        return snapshot;
    }

    /**
     * Returns pk snapshots for join records for the single-stp flattened relationship.
     * Multiple joins between the same pair of objects are theoretically possible, so the
     * return value is a list.
     */
    List buildJoinSnapshotsForDelete(DataNode node) {
        Map snapshot = eagerJoinSnapshot();

        DbEntity joinEntity = getJoinEntity();

        boolean fetchKey = false;
        for (DbAttribute dbAttr : joinEntity.getPrimaryKeys()) {
            String dbAttrName = dbAttr.getName();
            if (!snapshot.containsKey(dbAttrName)) {
                fetchKey = true;
                break;
            }
        }

        if (!fetchKey) {
            return Collections.singletonList(snapshot);
        }

        // ok, the key is not included in snapshot, must do the fetch...
        // TODO: this should be optimized in the future, but now DeleteBatchQuery
        // expects a PK snapshot, so we must provide it.

        SelectQuery query = new SelectQuery(joinEntity, ExpressionFactory.matchAllDbExp(
                snapshot,
                Expression.EQUAL_TO));
        query.setFetchingDataRows(true);

        for (DbAttribute dbAttr : joinEntity.getPrimaryKeys()) {
            query.addCustomDbAttribute(dbAttr.getName());
        }

        final List[] result = new List[1];

        node.performQueries(Collections.singleton((Query) query), new DefaultOperationObserver() {

            public void nextDataRows(Query query, List dataRows) {
                result[0] = dataRows;
            }
        });

        return result[0];
    }

    boolean isBidirectional() {
        return reverseRelationship != null;
    }

    public int hashCode() {
        // TODO: use hashcode builder to make a better hashcode.
        return sourceId.hashCode() + destinationId.hashCode() + compareToken.hashCode();
    }

    /**
     * Defines equal based on whether the relationship is bidirectional.
     */
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof FlattenedArcKey)) {
            return false;
        }

        FlattenedArcKey update = (FlattenedArcKey) object;

        if (!this.compareToken.equals(update.compareToken)) {
            return false;
        }

        boolean bidi = isBidirectional();
        if (bidi != update.isBidirectional()) {
            return false;
        }

        return (bidi) ? bidiEquals(update) : uniEquals(update);
    }

    private boolean bidiEquals(FlattenedArcKey update) {
        return (sourceId.equals(update.sourceId) && destinationId
                .equals(update.destinationId))
                || (this.sourceId.equals(update.destinationId) && this.destinationId
                        .equals(update.sourceId));
    }

    private boolean uniEquals(FlattenedArcKey update) {
        return (this.sourceId.equals(update.sourceId) && this.destinationId
                .equals(update.destinationId));
    }

    private Map eagerJoinSnapshot() {

        List relList = relationship.getDbRelationships();
        if (relList.size() != 2) {
            throw new CayenneRuntimeException(
                    "Only single-step flattened relationships are supported in this operation: "
                            + relationship);
        }

        DbRelationship firstDbRel = (DbRelationship) relList.get(0);
        DbRelationship secondDbRel = (DbRelationship) relList.get(1);

        Map<String, ?> sourceId = this.sourceId.getIdSnapshot();
        Map<String, ?> destinationId = this.destinationId.getIdSnapshot();

        Map<String, Object> snapshot = new HashMap<String, Object>(sourceId.size() + destinationId.size(), 1);
        for (DbJoin join : firstDbRel.getJoins()) {
            snapshot.put(join.getTargetName(), sourceId.get(join.getSourceName()));
        }

        for (DbJoin join : secondDbRel.getJoins()) {
            snapshot.put(join.getSourceName(), destinationId.get(join.getTargetName()));
        }

        return snapshot;
    }

    private Map lazyJoinSnapshot() {

        List relList = relationship.getDbRelationships();
        if (relList.size() != 2) {
            throw new CayenneRuntimeException(
                    "Only single-step flattened relationships are supported in this operation: "
                            + relationship);
        }

        DbRelationship firstDbRel = (DbRelationship) relList.get(0);
        DbRelationship secondDbRel = (DbRelationship) relList.get(1);

        List fromSourceJoins = firstDbRel.getJoins();
        List toTargetJoins = secondDbRel.getJoins();

        Map snapshot = new HashMap(fromSourceJoins.size() + toTargetJoins.size(), 1);

        for (int i = 0, numJoins = fromSourceJoins.size(); i < numJoins; i++) {
            DbJoin join = (DbJoin) fromSourceJoins.get(i);

            Object value = new PropagatedValueFactory(sourceId, join.getSourceName());
            snapshot.put(join.getTargetName(), value);
        }

        for (int i = 0, numJoins = toTargetJoins.size(); i < numJoins; i++) {
            DbJoin join = (DbJoin) toTargetJoins.get(i);
            Object value = new PropagatedValueFactory(destinationId, join.getTargetName());
            snapshot.put(join.getSourceName(), value);
        }

        return snapshot;
    }
}
