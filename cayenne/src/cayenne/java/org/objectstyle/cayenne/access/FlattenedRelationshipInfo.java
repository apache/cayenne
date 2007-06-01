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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * A holder of flattened relationship modification data.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
final class FlattenedRelationshipInfo {

    DataObject source;
    DataObject destination;
    ObjRelationship baseRelationship;
    String canonicalRelationshipName;

    FlattenedRelationshipInfo(DataObject aSource, DataObject aDestination,
            ObjRelationship relationship) {

        this.source = aSource;
        this.destination = aDestination;
        this.baseRelationship = relationship;

        // Calculate canonical relationship name
        String relName1 = relationship.getName();
        ObjRelationship reverseRel = relationship.getReverseRelationship();
        if (reverseRel != null) {
            String relName2 = reverseRel.getName();
            //Find the lexically lesser name and use it first, then use the second.
            //If equal (the same name), it doesn't matter which order.. be arbitrary
            if (relName1.compareTo(relName2) <= 0) {
                this.canonicalRelationshipName = relName1 + "." + relName2;
            }
            else {
                this.canonicalRelationshipName = relName2 + "." + relName1;
            }
        }
        else {
            this.canonicalRelationshipName = relName1;
        }
    }

    /**
     * Does not care about the order of source/destination, only that the pair and the
     * canonical relationship name match
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof FlattenedRelationshipInfo)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        FlattenedRelationshipInfo otherObj = (FlattenedRelationshipInfo) obj;

        if (!this.canonicalRelationshipName.equals(otherObj.canonicalRelationshipName)) {
            return false;
        }
        // Check that either direct mapping matches (src=>src, dest=>dest), or that
        // cross mapping matches (src=>dest, dest=>src).
        if (((this.source.equals(otherObj.source)) && (this.destination
                .equals(otherObj.destination)))
                || ((this.source.equals(otherObj.destination)) && (this.destination
                        .equals(otherObj.source)))) {
            return true;
        }
        return false;
    }

    /**
     * Because equals effectively ignores the order of dataObject1/2, summing the
     * hashcodes is sufficient to fulfill the equals/hashcode contract
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return source.hashCode()
                + destination.hashCode()
                + canonicalRelationshipName.hashCode();
    }

    /**
     * Returns the baseRelationship.
     * 
     * @return ObjRelationship
     */
    ObjRelationship getBaseRelationship() {
        return baseRelationship;
    }

    /**
     * Returns the destination.
     * 
     * @return DataObject
     */
    DataObject getDestination() {
        return destination;
    }

    /**
     * Returns the source.
     * 
     * @return DataObject
     */
    DataObject getSource() {
        return source;
    }

    /**
     * Returns a join DbEntity for the single-step flattened relationship.
     */
    DbEntity getJoinEntity() {
        List relList = baseRelationship.getDbRelationships();
        if (relList.size() != 2) {
            throw new CayenneRuntimeException(
                    "Only single-step flattened relationships are supported in this operation: "
                            + baseRelationship);
        }

        DbRelationship firstDbRel = (DbRelationship) relList.get(0);
        return (DbEntity) firstDbRel.getTargetEntity();
    }

    /**
     * Returns a snapshot for the join record for the single-step flattened relationship.
     */
    Map buildJoinSnapshot() {

        List relList = baseRelationship.getDbRelationships();
        if (relList.size() != 2) {
            throw new CayenneRuntimeException(
                    "Only single-step flattened relationships are supported in this operation: "
                            + baseRelationship);
        }

        DbRelationship firstDbRel = (DbRelationship) relList.get(0);
        DbRelationship secondDbRel = (DbRelationship) relList.get(1);

        Map sourceId = source.getObjectId().getIdSnapshot();
        Map destinationId = destination.getObjectId().getIdSnapshot();

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

    /**
     * Returns a snapshot for join record for the single-step flattened relationship,
     * generating value for the primary key column if it is not propagated via the
     * relationships.
     */
    Map buildJoinSnapshotForInsert() {
        Map snapshot = buildJoinSnapshot();

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
                DataNode node = source.getDataContext().lookupDataNode(
                        joinEntity.getDataMap());
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
    List buildJoinSnapshotsForDelete() {
        Map snapshot = buildJoinSnapshot();

        DbEntity joinEntity = getJoinEntity();
        List pkAttributes = joinEntity.getPrimaryKey();
        Iterator it = pkAttributes.iterator();

        boolean fetchKey = false;
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

        return source.getDataContext().performQuery(query);
    }
}