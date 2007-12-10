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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A processor of ObjectStore indirect changes, such as flattened relationships and
 * to-many relationships.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
final class DataDomainIndirectDiffBuilder implements GraphChangeHandler {

    private final DataDomainFlushAction parent;
    private final EntityResolver resolver;
    private final Collection indirectModifications;
    private final Collection flattenedInserts;
    private final Collection flattenedDeletes;

    DataDomainIndirectDiffBuilder(DataDomainFlushAction parent) {
        this.parent = parent;
        this.indirectModifications = parent.getResultIndirectlyModifiedIds();
        this.resolver = parent.getDomain().getEntityResolver();
        this.flattenedInserts = new HashSet();
        this.flattenedDeletes = new HashSet();
    }

    void processIndirectChanges(GraphDiff allChanges) {
        // extract flattened and indirect changes and remove duplicate changes...
        allChanges.apply(this);

        if (!flattenedInserts.isEmpty()) {
            Iterator it = flattenedInserts.iterator();
            while (it.hasNext()) {
                FlattenedArcKey key = (FlattenedArcKey) it.next();
                DbEntity entity = key.getJoinEntity();
                parent.addFlattenedInsert(entity, key);
            }
        }

        if (!flattenedDeletes.isEmpty()) {
            Iterator it = flattenedDeletes.iterator();
            while (it.hasNext()) {
                FlattenedArcKey key = (FlattenedArcKey) it.next();
                DbEntity entity = key.getJoinEntity();
                parent.addFlattenedDelete(entity, key);
            }
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        ObjEntity entity = resolver.getObjEntity(((ObjectId) nodeId).getEntityName());
        ObjRelationship relationship = (ObjRelationship) entity.getRelationship(arcId
                .toString());

        if (relationship.isSourceIndependentFromTargetChange()) {

            if (!((ObjectId) nodeId).isTemporary()) {
                indirectModifications.add(nodeId);
            }

            if (relationship.isFlattened()) {
                if (relationship.isReadOnly()) {
                    throw new CayenneRuntimeException(
                            "Cannot set the read-only flattened relationship '"
                                + relationship.getName() + "' in ObjEntity '" + relationship.getSourceEntity().getName() + "'.");
                }

                // Register this combination (so we can remove it later if an insert
                // occurs before commit)
                FlattenedArcKey key = new FlattenedArcKey(
                        (ObjectId) nodeId,
                        (ObjectId) targetNodeId,
                        relationship);

                // If this combination has already been deleted, simply undelete it.
                if (!flattenedDeletes.remove(key)) {
                    flattenedInserts.add(key);
                }
            }
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {

        ObjEntity entity = resolver.getObjEntity(((ObjectId) nodeId).getEntityName());
        ObjRelationship relationship = (ObjRelationship) entity.getRelationship(arcId
                .toString());

        if (relationship.isSourceIndependentFromTargetChange()) {
            // do not record temporary id mods...
            if (!((ObjectId) nodeId).isTemporary()) {
                indirectModifications.add(nodeId);
            }

            if (relationship.isFlattened()) {
                if (relationship.isReadOnly()) {
                    throw new CayenneRuntimeException(
                            "Cannot unset the read-only flattened relationship "
                                    + relationship.getName());
                }

                // Register this combination (so we can remove it later if an insert
                // occurs before commit)
                FlattenedArcKey key = new FlattenedArcKey(
                        (ObjectId) nodeId,
                        (ObjectId) targetNodeId,
                        relationship);

                // If this combination has already been inserted, simply "uninsert" it
                // also do not delete it twice
                if (!flattenedInserts.remove(key)) {
                    flattenedDeletes.add(key);
                }
            }
        }
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        // noop
    }

    public void nodeCreated(Object nodeId) {
        // noop
    }

    public void nodeRemoved(Object nodeId) {
        // noop
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        // noop
    }
}
