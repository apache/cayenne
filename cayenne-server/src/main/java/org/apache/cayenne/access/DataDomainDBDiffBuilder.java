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
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataDomainSyncBucket.PropagatedValueFactory;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * Processes object diffs, generating DB diffs. Can be used for both UPDATE and
 * INSERT.
 * 
 * @since 1.2
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
    void reset(DbEntityClassDescriptor descriptor) {
        this.objEntity = descriptor.getEntity();
        this.dbEntity = descriptor.getDbEntity();
    }

    /**
     * Resets the builder to process a new object for the previously set
     * combination of objEntity/dbEntity.
     */
    private void reset() {
        currentPropertyDiff = null;
        currentArcDiff = null;
        currentId = null;
    }

    /**
     * Processes GraphDiffs of a single object, converting them to DB diff.
     */
    Map<String, Object> buildDBDiff(GraphDiff singleObjectDiff) {

        reset();
        singleObjectDiff.apply(this);

        if (currentPropertyDiff == null && currentArcDiff == null && currentId == null) {
            return null;
        }

        Map<String, Object> dbDiff = new HashMap<String, Object>();

        appendSimpleProperties(dbDiff);
        appendForeignKeys(dbDiff);
        appendPrimaryKeys(dbDiff);

        return dbDiff.isEmpty() ? null : dbDiff;
    }

    private void appendSimpleProperties(Map<String, Object> dbDiff) {
        // populate changed columns
        if (currentPropertyDiff != null) {
            for (final Map.Entry<Object, Object> entry : currentPropertyDiff.entrySet()) {
                ObjAttribute attribute = objEntity.getAttribute(entry.getKey().toString());

                // in case of a flattened attribute, ensure that it belongs to
                // this
                // bucket...
                DbAttribute dbAttribute = attribute.getDbAttribute();
                if (dbAttribute.getEntity() == dbEntity) {
                    dbDiff.put(dbAttribute.getName(), entry.getValue());
                }
            }
        }
    }

    private void appendForeignKeys(Map<String, Object> dbDiff) {
        // populate changed FKs
        if (currentArcDiff != null) {
            for (Entry<Object, Object> entry : currentArcDiff.entrySet()) {

                DbRelationship dbRelation;

                String arcIdString = entry.getKey().toString();
                ObjRelationship relation = objEntity.getRelationship(arcIdString);
                if (relation == null) {
                    dbRelation = dbEntity.getRelationship(arcIdString.substring(ASTDbPath.DB_PREFIX.length()));
                } else {
                    dbRelation = relation.getDbRelationships().get(0);
                }

                ObjectId targetId = (ObjectId) entry.getValue();
                for (DbJoin join : dbRelation.getJoins()) {
                    Object value = (targetId != null) ? new PropagatedValueFactory(targetId, join.getTargetName())
                            : null;

                    dbDiff.put(join.getSourceName(), value);
                }
            }
        }
    }

    private void appendPrimaryKeys(Map<String, Object> dbDiff) {

        // populate changed PKs; note that we might end up overriding some
        // values taken
        // from the object (e.g. zero PK's).
        if (currentId != null) {
            dbDiff.putAll(((ObjectId) currentId).getIdSnapshot());
        }
    }

    // ==================================================
    // GraphChangeHandler methods.
    // ==================================================

    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {
        // note - no checking for phantom mod... assuming there is no phantom
        // diffs

        if (currentPropertyDiff == null) {
            currentPropertyDiff = new HashMap<Object, Object>();
        }

        currentPropertyDiff.put(property, newValue);
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        String arcIdString = arcId.toString();
        ObjRelationship relationship = objEntity.getRelationship(arcIdString);

        if (relationship == null) {
            // phantom FK
            if (arcIdString.startsWith(ASTDbPath.DB_PREFIX)) {

                DbRelationship dbRelationship = dbEntity.getRelationship(arcIdString.substring(ASTDbPath.DB_PREFIX
                        .length()));
                if (!dbRelationship.isSourceIndependentFromTargetChange()) {
                    doArcCreated(targetNodeId, arcId);
                }
            } else {
                throw new IllegalArgumentException("Bad arcId: " + arcId);
            }

        } else if (!relationship.isSourceIndependentFromTargetChange()) {
            doArcCreated(targetNodeId, arcId);
        }
    }

    private void doArcCreated(Object targetNodeId, Object arcId) {
        if (currentArcDiff == null) {
            currentArcDiff = new HashMap<Object, Object>();
        }
        currentArcDiff.put(arcId, targetNodeId);
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {

        String arcIdString = arcId.toString();
        ObjRelationship relationship = objEntity.getRelationship(arcIdString);

        if (relationship == null) {
            // phantom FK
            if (arcIdString.startsWith(ASTDbPath.DB_PREFIX)) {

                DbRelationship dbRelationship = dbEntity.getRelationship(arcIdString.substring(ASTDbPath.DB_PREFIX
                        .length()));
                if (!dbRelationship.isSourceIndependentFromTargetChange()) {
                    doArcDeleted(targetNodeId, arcId);
                }
            } else {
                throw new IllegalArgumentException("Bad arcId: " + arcId);
            }

        } else if (!relationship.isSourceIndependentFromTargetChange()) {
            doArcDeleted(targetNodeId, arcId);
        }
    }

    private void doArcDeleted(Object targetNodeId, Object arcId) {
        if (currentArcDiff == null) {
            currentArcDiff = new HashMap<Object, Object>();
            currentArcDiff.put(arcId, null);
        } else {
            // skip deletion record if a substitute arc was created prior to
            // deleting the old arc...
            Object existingTargetId = currentArcDiff.get(arcId);
            if (existingTargetId == null || targetNodeId.equals(existingTargetId)) {
                currentArcDiff.put(arcId, null);
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
