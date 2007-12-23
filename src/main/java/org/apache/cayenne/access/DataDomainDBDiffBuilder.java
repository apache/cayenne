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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataDomainSyncBucket.PropagatedValueFactory;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * Processes object diffs, generating DB diffs. Can be used for both UPDATE and INSERT.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class DataDomainDBDiffBuilder implements GraphChangeHandler {

    private ObjEntity objEntity;
    private DbEntity dbEntity;

    // diff snapshot expressed in terms of object properties.
    private Map<Object, Object> currentPropertyDiff;
    private Map<Object, Object> currentArcDiff;
    private Object currentId;

    /**
     * Resets the builder to process a new combination of objEntity/dbEntity.
     */
    void reset(ObjEntity objEntity, DbEntity dbEntity) {
        this.objEntity = objEntity;
        this.dbEntity = dbEntity;
    }

    /**
     * Resets the builder to process a new object for the previously set combination of
     * objEntity/dbEntity.
     */
    private void reset() {
        currentPropertyDiff = null;
        currentArcDiff = null;
        currentId = null;
    }

    /**
     * Processes GraphDiffs of a single object, converting them to DB diff.
     */
    Map<Object, Object> buildDBDiff(GraphDiff singleObjectDiff) {

        reset();
        singleObjectDiff.apply(this);

        if (currentPropertyDiff == null && currentArcDiff == null && currentId == null) {
            return null;
        }

        Map<Object, Object> dbDiff = new HashMap<Object, Object>();

        appendSimpleProperties(dbDiff);
        appendForeignKeys(dbDiff);
        appendPrimaryKeys(dbDiff);

        return dbDiff.isEmpty() ? null : dbDiff;
    }

    private void appendSimpleProperties(Map<Object, Object> dbDiff) {
        // populate changed columns
        if (currentPropertyDiff != null) {
            Iterator<?> it = currentPropertyDiff.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) it.next();
                ObjAttribute attribute = (ObjAttribute) objEntity.getAttribute(entry
                        .getKey()
                        .toString());

                // this takes care of the flattened attributes, as 'getDbAttributeName'
                // returns the last path component...
                Attribute dbAttribute = dbEntity.getAttribute(attribute
                        .getDbAttributeName());
                dbDiff.put(dbAttribute.getName(), entry.getValue());
            }
        }
    }

    private void appendForeignKeys(Map<Object, Object> dbDiff) {
        // populate changed FKs
        if (currentArcDiff != null) {
            Iterator<?> it = currentArcDiff.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) it.next();
                ObjRelationship relation = (ObjRelationship) objEntity
                        .getRelationship(entry.getKey().toString());

                DbRelationship dbRelation = relation.getDbRelationships().get(0);

                ObjectId targetId = (ObjectId) entry.getValue();
                for (DbJoin join : dbRelation.getJoins()) {
                    Object value = (targetId != null) ? new PropagatedValueFactory(
                            targetId,
                            join.getTargetName()) : null;

                    dbDiff.put(join.getSourceName(), value);
                }
            }
        }
    }

    private void appendPrimaryKeys(Map<Object, Object> dbDiff) {

        // populate changed PKs; note that we might end up overriding some values taken
        // from the object (e.g. zero PK's).
        if (currentId != null) {
            dbDiff.putAll(((ObjectId) currentId).getIdSnapshot());
        }
    }

    // ==================================================
    // GraphChangeHandler methods.
    // ==================================================

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        // note - no checking for phantom mod... assuming there is no phantom diffs

        if (currentPropertyDiff == null) {
            currentPropertyDiff = new HashMap<Object, Object>();
        }

        currentPropertyDiff.put(property, newValue);
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {

        ObjRelationship relationship = (ObjRelationship) objEntity.getRelationship(arcId
                .toString());
        if (!relationship.isSourceIndependentFromTargetChange()) {
            if (currentArcDiff == null) {
                currentArcDiff = new HashMap<Object, Object>();
            }
            currentArcDiff.put(arcId, targetNodeId);
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {

        ObjRelationship relationship = (ObjRelationship) objEntity.getRelationship(arcId
                .toString());
        if (!relationship.isSourceIndependentFromTargetChange()) {

            if (currentArcDiff == null) {
                currentArcDiff = new HashMap<Object, Object>();
                currentArcDiff.put(arcId, null);
            }
            else {
                // skip deletion record if a substitute arc was created prior to deleting
                // the old arc...
                Object existingTargetId = currentArcDiff.get(arcId);
                if (existingTargetId == null || targetNodeId.equals(existingTargetId)) {
                    currentArcDiff.put(arcId, null);
                }
            }
        }
    }

    public void nodeCreated(Object nodeId) {
        // need to append PK columns
        this.currentId = nodeId;
    }

    public void nodeRemoved(Object nodeId) {
        // noop
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        // noop
    }
}
