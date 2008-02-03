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
package org.apache.cayenne.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class that removes redundant graph changes from the graph diff.
 * 
 * @author Andrus Adamchik
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

        private List<GraphDiff> compressed = new ArrayList<GraphDiff>();
        private Map<Object, List<NodeDiff>> diffsByNode = new HashMap<Object, List<NodeDiff>>();

        GraphDiff getCompressedDiff() {
            return new CompoundDiff(compressed);
        }

        public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
            registerDiff(new ArcCreateOperation(nodeId, targetNodeId, arcId));
        }

        public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
            registerDiff(new ArcDeleteOperation(nodeId, targetNodeId, arcId));
        }

        public void nodeCreated(Object nodeId) {
            registerDiff(new NodeCreateOperation(nodeId));
        }

        public void nodeIdChanged(Object nodeId, Object newId) {
            registerDiff(new NodeIdChangeOperation(nodeId, newId));
        }

        public void nodeRemoved(Object nodeId) {
            registerDiff(new NodeDeleteOperation(nodeId));
        }

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
            List<NodeDiff> diffs = diffsByNode.get(diff.getNodeId());
            if (diffs == null) {
                diffs = new ArrayList<NodeDiff>();
                diffsByNode.put(diff.getNodeId(), diffs);
            }

            diffs.add(diff);
        }
    }
}
