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
import java.util.List;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.graph.ArcCreateOperation;
import org.apache.cayenne.graph.ArcDeleteOperation;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.NodeCreateOperation;
import org.apache.cayenne.graph.NodeDeleteOperation;
import org.apache.cayenne.graph.NodeIdChangeOperation;
import org.apache.cayenne.graph.NodePropertyChangeOperation;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * Filters diffs before returning them to the client. Ensures that no server-only data
 * leaks to the client and also that all diff objects returned to the client are public
 * classes available on the client.
 * 
 * @since 3.0
 */
// TODO: andrus, 2008/03/01 - integrate with GraphDiffCompressor.
class ClientReturnDiffFilter implements GraphChangeHandler {

    private List<GraphDiff> diffs;
    private EntityResolver resolver;

    ClientReturnDiffFilter(EntityResolver resolver) {
        this.resolver = resolver;
    }

    GraphDiff filter(GraphDiff in) {
        diffs = new ArrayList<GraphDiff>();
        in.apply(this);
        return new CompoundDiff(diffs);
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        if (isClientArc(nodeId, targetNodeId, arcId)) {
            diffs.add(new ArcCreateOperation(nodeId, targetNodeId, arcId));
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        if (isClientArc(nodeId, targetNodeId, arcId)) {
            diffs.add(new ArcDeleteOperation(nodeId, targetNodeId, arcId));
        }
    }

    public void nodeCreated(Object nodeId) {
        if (isClientNode(nodeId)) {
            diffs.add(new NodeCreateOperation(nodeId));
        }
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        if (isClientNode(nodeId)) {
            diffs.add(new NodeIdChangeOperation(nodeId, newId));
        }
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        if (isClientNode(nodeId)) {
            diffs.add(new NodePropertyChangeOperation(
                    nodeId,
                    property,
                    oldValue,
                    newValue));
        }
    }

    public void nodeRemoved(Object nodeId) {
        if (isClientNode(nodeId)) {
            diffs.add(new NodeDeleteOperation(nodeId));
        }
    }

    private boolean isClientNode(Object id) {
        ObjectId oid = (ObjectId) id;
        return resolver.getObjEntity(oid.getEntityName()).isClientAllowed();
    }

    private boolean isClientArc(Object id, Object targetId, Object arcId) {

        ObjectId oid = (ObjectId) id;
        ObjEntity entity = resolver.getObjEntity(oid.getEntityName());

        if(!entity.isClientAllowed()) {
            return false;
        }
        
        if(entity.getRelationship(arcId.toString()).isRuntime()) {
            return false;
        }
        
        ObjectId targetOid = (ObjectId) targetId;
        ObjEntity targetEntity = resolver.getObjEntity(targetOid.getEntityName());
        if(!targetEntity.isClientAllowed()) {
            return false;
        }

        return true;
    }
}
