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
package org.apache.cayenne.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A utility class that removes redundant and mutually exclusive graph changes from the
 * graph diff.
 * 
 * @since 3.0
 */
public class GraphDiffCompressor {

    public GraphDiff compress(GraphDiff diff) {

        if (diff.isNoop()) {
            return diff;
        }

        CompressAction action = new CompressAction();
        diff.apply(action);
        return action.getCompressedDiff();
    }

    final class CompressAction implements GraphChangeHandler {

        private List<GraphDiff> compressed = new ArrayList<>();
        private Map<Object, List<NodeDiff>> diffsByNode = new HashMap<>();
        private Set<Object> deletedNodes;
        private Set<Object> createdNodes;

        GraphDiff getCompressedDiff() {

            // remove deleted nodes...
            if (deletedNodes != null) {

                for (Object nodeId : deletedNodes) {

                    Iterator<GraphDiff> it = compressed.iterator();

                    // if the node was inserted in the same transaction and later deleted,
                    // remove all its ops. Otherwise preserve arc ops (since delete rules
                    // depend on them), and delete operation itself.

                    // TODO: andrus 2008/02/04 - this doesn't take into account a
                    // possibility that a deleted node was re-inserted... Although I don't
                    // see how this could possibly happen with the present Cayenne API.
                    if (createdNodes != null && createdNodes.contains(nodeId)) {
                        while (it.hasNext()) {
                            NodeDiff diff = (NodeDiff) it.next();
                            if (nodeId.equals(diff.getNodeId())) {
                                it.remove();
                            }
                        }
                    }
                    else {
                        while (it.hasNext()) {
                            NodeDiff diff = (NodeDiff) it.next();
                            if (nodeId.equals(diff.getNodeId())) {
                                if (diff instanceof NodePropertyChangeOperation) {
                                    it.remove();
                                }
                            }
                        }
                    }

                }
            }

            return new CompoundDiff(compressed);
        }

        @Override
        public void arcCreated(Object nodeId, Object targetNodeId, ArcId arcId) {

            if (targetNodeId != null) {
                List<NodeDiff> diffs = diffsByNode.get(nodeId);
                if (diffs != null) {
                    for (int i = diffs.size() - 1; i >= 0; i--) {
                        NodeDiff diff = diffs.get(i);
                        if (diff instanceof ArcDeleteOperation) {
                            ArcDeleteOperation arcDiff = (ArcDeleteOperation) diff;
                            if (arcId.equals(arcDiff.getArcId())
                                    && targetNodeId.equals(arcDiff.targetNodeId)) {
                                diffs.remove(i);
                                compressed.remove(arcDiff);
                                return;
                            }
                        }
                    }
                }
            }

            registerDiff(new ArcCreateOperation(nodeId, targetNodeId, arcId));
        }

        @Override
        public void arcDeleted(Object nodeId, Object targetNodeId, ArcId arcId) {

            if (targetNodeId != null) {
                List<NodeDiff> diffs = diffsByNode.get(nodeId);
                if (diffs != null) {
                    for (int i = diffs.size() - 1; i >= 0; i--) {
                        NodeDiff diff = diffs.get(i);
                        if (diff instanceof ArcCreateOperation) {
                            ArcCreateOperation arcDiff = (ArcCreateOperation) diff;
                            if (arcId.equals(arcDiff.getArcId())
                                    && targetNodeId.equals(arcDiff.targetNodeId)) {
                                diffs.remove(i);
                                compressed.remove(arcDiff);
                                return;
                            }
                        }
                    }
                }
            }

            registerDiff(new ArcDeleteOperation(nodeId, targetNodeId, arcId));
        }

        @Override
        public void nodeCreated(Object nodeId) {
            registerDiff(new NodeCreateOperation(nodeId));

            if (createdNodes == null) {
                createdNodes = new HashSet<>();
            }

            createdNodes.add(nodeId);
        }

        @Override
        public void nodeIdChanged(Object nodeId, Object newId) {
            registerDiff(new NodeIdChangeOperation(nodeId, newId));
        }

        @Override
        public void nodeRemoved(Object nodeId) {

            registerDiff(new NodeDeleteOperation(nodeId));

            if (deletedNodes == null) {
                deletedNodes = new HashSet<>();
            }

            deletedNodes.add(nodeId);
        }

        @Override
        public void nodePropertyChanged(
                Object nodeId,
                String property,
                Object oldValue,
                Object newValue) {

            List<NodeDiff> diffs = diffsByNode.get(nodeId);
            if (diffs != null) {

                for (int i = diffs.size() - 1; i >= 0; i--) {
                    NodeDiff diff = diffs.get(i);
                    if (diff instanceof NodePropertyChangeOperation) {
                        NodePropertyChangeOperation propertyDiff = (NodePropertyChangeOperation) diff;
                        if (property.equals(propertyDiff.getProperty())) {
                            propertyDiff.setNewValue(newValue);
                            return;
                        }
                    }
                }
            }

            registerDiff(new NodePropertyChangeOperation(
                    nodeId,
                    property,
                    oldValue,
                    newValue));
        }

        private void registerDiff(NodeDiff diff) {

            compressed.add(diff);
            diffsByNode
                    .computeIfAbsent(diff.getNodeId(), k -> new ArrayList<>())
                    .add(diff);
        }
    }
}
