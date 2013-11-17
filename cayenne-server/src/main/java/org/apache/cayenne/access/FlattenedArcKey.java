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
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataDomainSyncBucket.PropagatedValueFactory;
import org.apache.cayenne.access.util.DefaultOperationObserver;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.HashCodeBuilder;
import org.apache.cayenne.util.Util;

/**
 * A holder of flattened relationship modification data.
 * 
 * @since 1.2
 */
final class FlattenedArcKey {

    ObjRelationship relationship;

    DbArcId id1;
    DbArcId id2;

    FlattenedArcKey(ObjectId sourceId, ObjectId destinationId,
            ObjRelationship relationship) {

        this.relationship = relationship;

        List<DbRelationship> dbRelationships = relationship
                .getDbRelationships();
        if (dbRelationships.size() != 2) {
            throw new CayenneRuntimeException(
                    "Only single-step flattened relationships are supported in this operation, whereas the relationship '%s' has %s",
                    relationship, dbRelationships.size());
        }

        DbRelationship r1 = dbRelationships.get(0);
        DbRelationship r2 = dbRelationships.get(1).getReverseRelationship();

        if (r2 == null) {
            throw new IllegalStateException(
                    "No reverse relationship for DbRelationship "
                            + dbRelationships.get(1));
        }

        id1 = new DbArcId(sourceId, r1);
        id2 = new DbArcId(destinationId, r2);
    }

    /**
     * Returns a join DbEntity for the single-step flattened relationship.
     */
    DbEntity getJoinEntity() {
        return id1.getEntity();
    }

    /**
     * Returns a snapshot for join record for the single-step flattened
     * relationship, generating value for the primary key column if it is not
     * propagated via the relationships.
     */
    Map<String, Object> buildJoinSnapshotForInsert(DataNode node) {
        Map<String, Object> snapshot = lazyJoinSnapshot();

        boolean autoPkDone = false;
        DbEntity joinEntity = getJoinEntity();

        for (DbAttribute dbAttr : joinEntity.getPrimaryKeys()) {
            String dbAttrName = dbAttr.getName();
            if (snapshot.containsKey(dbAttrName)) {
                continue;
            }

            DbAdapter adapter = node.getAdapter();

            // skip db-generated... looks like we don't care about the actual PK
            // value
            // here, so no need to retrieve db-generated pk back to Java.
            if (adapter.supportsGeneratedKeys() && dbAttr.isGenerated()) {
                continue;
            }

            if (autoPkDone) {
                throw new CayenneRuntimeException(
                        "Primary Key autogeneration only works for a single attribute.");
            }

            // finally, use database generation mechanism
            try {
                Object pkValue = adapter.getPkGenerator().generatePk(node,
                        dbAttr);
                snapshot.put(dbAttrName, pkValue);
                autoPkDone = true;
            } catch (Exception ex) {
                throw new CayenneRuntimeException("Error generating PK: "
                        + ex.getMessage(), ex);
            }
        }

        return snapshot;
    }

    /**
     * Returns pk snapshots for join records for the single-step flattened
     * relationship. Multiple joins between the same pair of objects are
     * theoretically possible, so the return value is a list.
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
        // TODO: this should be optimized in the future, but now
        // DeleteBatchQuery
        // expects a PK snapshot, so we must provide it.

        QuotingStrategy quoter = node.getAdapter().getQuotingStrategy();

        StringBuilder sql = new StringBuilder("SELECT ");
        Collection<DbAttribute> pk = joinEntity.getPrimaryKeys();
        final List<DbAttribute> pkList = pk instanceof List ? (List<DbAttribute>) pk
                : new ArrayList<DbAttribute>(pk);

        for (int i = 0; i < pkList.size(); i++) {

            if (i > 0) {
                sql.append(", ");
            }

            DbAttribute attribute = pkList.get(i);

            sql.append("#result('");
            sql.append(quoter.quotedName(attribute));

            // since the name of the column can potentially be quoted and
            // use reserved keywords as name, let's specify generated column
            // name parameters to ensure the query doesn't explode
            sql.append("' '").append(TypesMapping.getJavaBySqlType(attribute.getType()));
            sql.append("' '").append("pk").append(i);
            sql.append("')");
        }

        sql.append(" FROM ").append(quoter.quotedFullyQualifiedName(joinEntity))
                .append(" WHERE ");
        int i = snapshot.size();
        for (Object key : snapshot.keySet()) {
            sql.append(quoter.quotedIdentifier(joinEntity, String.valueOf(key)))
                    .append(" #bindEqual($").append(key).append(")");

            if (--i > 0) {
                sql.append(" AND ");
            }
        }

        SQLTemplate query = new SQLTemplate(joinEntity.getDataMap(),
                sql.toString(), true);
        query.setParameters(snapshot);

        final List[] result = new List[1];

        node.performQueries(Collections.singleton((Query) query),
                new DefaultOperationObserver() {

                    @Override
                    public void nextRows(Query query, List dataRows) {

                        if (!dataRows.isEmpty()) {
                            // decode results...

                            List<DataRow> fixedRows = new ArrayList<DataRow>(
                                    dataRows.size());
                            for (Object o : dataRows) {
                                DataRow row = (DataRow) o;

                                DataRow fixedRow = new DataRow(2);

                                for (int i = 0; i < pkList.size(); i++) {
                                    DbAttribute attribute = pkList.get(i);
                                    fixedRow.put(attribute.getName(),
                                            row.get("pk" + i));
                                }

                                fixedRows.add(fixedRow);
                            }

                            dataRows = fixedRows;
                        }

                        result[0] = dataRows;
                    }

                    @Override
                    public void nextQueryException(Query query, Exception ex) {
                        throw new CayenneRuntimeException(
                                "Raising from query exception.", Util
                                        .unwindException(ex));
                    }

                    @Override
                    public void nextGlobalException(Exception ex) {
                        throw new CayenneRuntimeException(
                                "Raising from underlyingQueryEngine exception.",
                                Util.unwindException(ex));
                    }
                });

        return result[0];
    }

    @Override
    public int hashCode() {
        // order ids in array for hashcode consistency purposes. The actual
        // order direction is not important, as long as it
        // is consistent across invocations

        int compare = id1.getSourceId().getEntityName()
                .compareTo(id2.getSourceId().getEntityName());

        if (compare == 0) {
            compare = id1.getIncominArc().getName()
                    .compareTo(id2.getIncominArc().getName());

            if (compare == 0) {
                // since ordering is mostly important for detecting equivalent
                // FlattenedArc keys coming from 2 opposite directions, the name
                // of ObjRelationship can be a good criteria

                ObjRelationship or2 = relationship.getReverseRelationship();
                compare = or2 != null ? relationship.getName().compareTo(
                        or2.getName()) : 1;

                // TODO: if(compare == 0) ??
            }
        }

        DbArcId[] ordered;
        if (compare < 0) {
            ordered = new DbArcId[] { id1, id2 };
        } else {
            ordered = new DbArcId[] { id2, id1 };
        }

        return new HashCodeBuilder().append(ordered).toHashCode();
    }

    /**
     * Defines equal based on whether the relationship is bidirectional.
     */
    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof FlattenedArcKey)) {
            return false;
        }

        FlattenedArcKey key = (FlattenedArcKey) object;

        // ignore id order in comparison
        if (id1.equals(key.id1)) {
            return id2.equals(key.id2);
        } else if (id1.equals(key.id2)) {
            return id2.equals(key.id1);
        }

        return false;
    }

    private Map eagerJoinSnapshot() {

        List<DbRelationship> relList = relationship.getDbRelationships();
        if (relList.size() != 2) {
            throw new CayenneRuntimeException(
                    "Only single-step flattened relationships are supported in this operation: "
                            + relationship);
        }

        DbRelationship firstDbRel = relList.get(0);
        DbRelationship secondDbRel = relList.get(1);

        // here ordering of ids is determined by 'relationship', so use id1, id2
        // instead of orderedIds
        Map<String, ?> sourceId = id1.getSourceId().getIdSnapshot();
        Map<String, ?> destinationId = id2.getSourceId().getIdSnapshot();

        Map<String, Object> snapshot = new HashMap<String, Object>(
                sourceId.size() + destinationId.size(), 1);
        for (DbJoin join : firstDbRel.getJoins()) {
            snapshot.put(join.getTargetName(),
                    sourceId.get(join.getSourceName()));
        }

        for (DbJoin join : secondDbRel.getJoins()) {
            snapshot.put(join.getSourceName(),
                    destinationId.get(join.getTargetName()));
        }

        return snapshot;
    }

    private Map<String, Object> lazyJoinSnapshot() {

        List<DbRelationship> relList = relationship.getDbRelationships();
        if (relList.size() != 2) {
            throw new CayenneRuntimeException(
                    "Only single-step flattened relationships are supported in this operation: "
                            + relationship);
        }

        DbRelationship firstDbRel = relList.get(0);
        DbRelationship secondDbRel = relList.get(1);

        List<DbJoin> fromSourceJoins = firstDbRel.getJoins();
        List<DbJoin> toTargetJoins = secondDbRel.getJoins();

        Map<String, Object> snapshot = new HashMap<String, Object>(
                fromSourceJoins.size() + toTargetJoins.size(), 1);

        // here ordering of ids is determined by 'relationship', so use id1, id2
        // instead of orderedIds

        for (int i = 0, numJoins = fromSourceJoins.size(); i < numJoins; i++) {
            DbJoin join = fromSourceJoins.get(i);

            Object value = new PropagatedValueFactory(id1.getSourceId(),
                    join.getSourceName());
            snapshot.put(join.getTargetName(), value);
        }

        for (int i = 0, numJoins = toTargetJoins.size(); i < numJoins; i++) {
            DbJoin join = toTargetJoins.get(i);
            Object value = new PropagatedValueFactory(id2.getSourceId(),
                    join.getTargetName());
            snapshot.put(join.getSourceName(), value);
        }

        return snapshot;
    }
}
