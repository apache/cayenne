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

package org.apache.cayenne.access.flush;

import java.util.Collection;
import java.util.HashSet;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.ObjectStoreGraphDiff;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * A processor of ObjectStore indirect changes, such as flattened relationships
 * and to-many relationships.
 *
 * @since 4.2
 */
final class DataDomainIndirectDiffBuilder implements GraphChangeHandler {

    private final EntityResolver resolver;
    private Collection<ObjectId> indirectModifications;

    DataDomainIndirectDiffBuilder(EntityResolver resolver) {
        this.resolver = resolver;
    }

    void processChanges(ObjectStoreGraphDiff allChanges) {
        // extract flattened and indirect changes and remove duplicate changes...
        allChanges.getChangesByObjectId()
                .forEach((obj, diff) -> diff.apply(this));
    }

    Collection<ObjectId> getIndirectModifications() {
        return indirectModifications;
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {
        processArcChange((ObjectId) nodeId, arcId);
    }

    @Override
    public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {
        processArcChange((ObjectId) nodeId, arcId);
    }

    private void processArcChange(ObjectId nodeId, ArcId arcId) {
        ObjEntity entity = resolver.getObjEntity(nodeId.getEntityName());
        ObjRelationship relationship = entity.getRelationship(arcId.getForwardArc());

        if (relationship != null && relationship.isSourceIndependentFromTargetChange()) {
            // do not record temporary id mods...
            if (!nodeId.isTemporary()) {
                if(indirectModifications == null) {
                    indirectModifications = new HashSet<>();
                }
                indirectModifications.add(nodeId);
            }

            if (relationship.isFlattened() && relationship.isReadOnly()) {
                throw new CayenneRuntimeException("Cannot change the read-only flattened relationship %s in ObjEntity '%s'."
                        , relationship.getName(), relationship.getSourceEntity().getName());
            }
        }
    }
}
