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

package org.apache.cayenne.access;

import java.util.Collection;
import java.util.HashSet;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * A processor of ObjectStore indirect changes, such as flattened relationships
 * and to-many relationships.
 * 
 * @since 1.2
 * @deprecated since 4.2 as part of deprecated {@link LegacyDataDomainFlushAction}
 */
@Deprecated
final class DataDomainIndirectDiffBuilder implements GraphChangeHandler {

    private final LegacyDataDomainFlushAction parent;
    private final EntityResolver resolver;
    private final Collection<ObjectId> indirectModifications;
    private final Collection<FlattenedArcKey> flattenedInserts;
    private final Collection<FlattenedArcKey> flattenedDeletes;

    DataDomainIndirectDiffBuilder(LegacyDataDomainFlushAction parent) {
        this.parent = parent;
        this.indirectModifications = parent.getResultIndirectlyModifiedIds();
        this.resolver = parent.getDomain().getEntityResolver();
        this.flattenedInserts = new HashSet<>();
        this.flattenedDeletes = new HashSet<>();
    }

    void processIndirectChanges(GraphDiff allChanges) {
        // extract flattened and indirect changes and remove duplicate changes...
        allChanges.apply(this);

        if (!flattenedInserts.isEmpty()) {
            for (final FlattenedArcKey key : flattenedInserts) {
                DbEntity entity = key.getJoinEntity();
                parent.addFlattenedInsert(entity, key);
            }
        }

        if (!flattenedDeletes.isEmpty()) {
            for (final FlattenedArcKey key : flattenedDeletes) {
                DbEntity entity = key.getJoinEntity();
                parent.addFlattenedDelete(entity, key);
            }
        }
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
        ObjEntity entity = resolver.getObjEntity(((ObjectId) nodeId).getEntityName());
        ObjRelationship relationship = entity.getRelationship(arcId.toString());

        if (relationship.isSourceIndependentFromTargetChange()) {

            ObjectId nodeObjectId = (ObjectId) nodeId;
            if (!nodeObjectId.isTemporary()) {
                indirectModifications.add(nodeObjectId);
            }

            if (relationship.isFlattened()) {
                if (relationship.isReadOnly()) {
                    throw new CayenneRuntimeException("Cannot set the read-only flattened relationship '%s' in ObjEntity '%s'."
                            , relationship.getName(), relationship.getSourceEntity().getName());
                }

                // build path without last segment
                StringBuilder path = new StringBuilder();
                for(int i=0; i<relationship.getDbRelationships().size() - 1; i++) {
                    if(path.length() > 0) {
                        path.append('.');
                    }
                    path.append(relationship.getDbRelationships().get(i).getName());
                }

                if(!parent.getContext().getObjectStore().hasFlattenedPath(nodeObjectId, path.toString())) {
                    // Register this combination (so we can remove it later if an insert occurs before commit)
                    FlattenedArcKey key = new FlattenedArcKey(nodeObjectId, (ObjectId) targetNodeId, relationship);

                    // If this combination has already been deleted, simply undelete it.
                    if (!flattenedDeletes.remove(key)) {
                        flattenedInserts.add(key);
                    }
                }
            }
        }
    }

    @Override
    public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {

        ObjEntity entity = resolver.getObjEntity(((ObjectId) nodeId).getEntityName());
        ObjRelationship relationship = entity.getRelationship(arcId.toString());

        if (relationship.isSourceIndependentFromTargetChange()) {
            // do not record temporary id mods...
            ObjectId nodeObjectId = (ObjectId) nodeId;
            if (!nodeObjectId.isTemporary()) {
                indirectModifications.add(nodeObjectId);
            }

            if (relationship.isFlattened()) {
                if (relationship.isReadOnly()) {
                    throw new CayenneRuntimeException("Cannot unset the read-only flattened relationship %s"
                            , relationship.getName());
                }

                // build path without last segment
                StringBuilder path = new StringBuilder();
                for(int i=0; i<relationship.getDbRelationships().size() - 1; i++) {
                    if(path.length() > 0) {
                        path.append('.');
                    }
                    path.append(relationship.getDbRelationships().get(i).getName());
                }

                if(!parent.getContext().getObjectStore().hasFlattenedPath(nodeObjectId, path.toString())) {
                    // Register this combination (so we can remove it later if an insert occurs before commit)
                    FlattenedArcKey key = new FlattenedArcKey(nodeObjectId, (ObjectId) targetNodeId, relationship);

                    // If this combination has already been inserted, simply "uninsert" it also do not delete it twice
                    if (!flattenedInserts.remove(key)) {
                        flattenedDeletes.add(key);
                    }
                }
            }
        }
    }
}
