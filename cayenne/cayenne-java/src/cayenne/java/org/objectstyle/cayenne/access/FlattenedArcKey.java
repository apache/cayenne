/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.DataDomainSyncBucket.PropagatedValueFactory;
import org.objectstyle.cayenne.access.util.DefaultOperationObserver;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;

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
        List pkAttributes = joinEntity.getPrimaryKey();
        Iterator it = pkAttributes.iterator();

        while (it.hasNext()) {
            DbAttribute dbAttr = (DbAttribute) it.next();
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
        List pkAttributes = joinEntity.getPrimaryKey();

        boolean fetchKey = false;
        Iterator it = pkAttributes.iterator();
        while (it.hasNext()) {
            DbAttribute dbAttr = (DbAttribute) it.next();
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

        it = pkAttributes.iterator();
        while (it.hasNext()) {
            DbAttribute dbAttr = (DbAttribute) it.next();
            query.addCustomDbAttribute(dbAttr.getName());
        }

        final List[] result = new List[1];

        node.performQueries(Collections.singleton(query), new DefaultOperationObserver() {

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

        Map sourceId = this.sourceId.getIdSnapshot();
        Map destinationId = this.destinationId.getIdSnapshot();

        Map snapshot = new HashMap(sourceId.size() + destinationId.size(), 1);
        List joins = firstDbRel.getJoins();
        for (int i = 0, numJoins = joins.size(); i < numJoins; i++) {
            DbJoin join = (DbJoin) joins.get(i);
            snapshot.put(join.getTargetName(), sourceId.get(join.getSourceName()));
        }

        joins = secondDbRel.getJoins();
        for (int i = 0, numJoins = joins.size(); i < numJoins; i++) {
            DbJoin join = (DbJoin) joins.get(i);
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