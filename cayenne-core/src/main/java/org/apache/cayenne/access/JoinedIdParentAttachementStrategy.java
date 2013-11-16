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

import java.util.Collection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A ParentAttachmentStrategy that extracts parent ObjectId from the joined columns in the
 * child snapshot.
 */
class JoinedIdParentAttachementStrategy implements ParentAttachmentStrategy {

    private String relatedIdPrefix;
    private Collection<ObjEntity> sourceEntities;
    private PrefetchProcessorNode node;
    private GraphManager graphManager;

    JoinedIdParentAttachementStrategy(GraphManager graphManager,
            PrefetchProcessorNode node) {

        ClassDescriptor parentDescriptor = ((PrefetchProcessorNode) node.getParent())
                .getResolver()
                .getDescriptor();

        relatedIdPrefix = node
                .getIncoming()
                .getRelationship()
                .getReverseDbRelationshipPath()
                + ".";

        sourceEntities = parentDescriptor.getEntityInheritanceTree().allSubEntities();

        this.node = node;
        this.graphManager = graphManager;
    }

    public void linkToParent(DataRow row, Persistent object) {
        Persistent parentObject = null;

        for (ObjEntity entity : sourceEntities) {
            if (entity.isAbstract()) {
                continue;
            }

            ObjectId id = node.getResolver().createObjectId(row, entity, relatedIdPrefix);
            if (id == null) {
                throw new CayenneRuntimeException("Can't build ObjectId from row: "
                        + row
                        + ", entity: "
                        + entity.getName()
                        + ", prefix: "
                        + relatedIdPrefix);
            }

            parentObject = (Persistent) graphManager.getNode(id);

            if (parentObject != null) {
                break;
            }
        }

        node.linkToParent(object, parentObject);
    }
}
