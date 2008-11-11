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

package org.apache.cayenne.project;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.DataNode;

/**
 * FlatProjectView converts a project tree into a list of nodes,
 * thus flattening the tree. Normally used as a singleton.
 * 
 */
public class FlatProjectView {

    protected static FlatProjectView instance = new FlatProjectView();

    /** 
     * Returns a FlatProjectView singleton.
     */
    public static FlatProjectView getInstance() {
        return instance;
    }

    /**
     * Returns flat tree view.
     */
    public List<ProjectPath> flattenProjectTree(Object rootNode) {
        List<ProjectPath> nodes = new ArrayList<ProjectPath>();
        TraversalHelper helper = new TraversalHelper(nodes);
        new ProjectTraversal(helper).traverse(rootNode);
        return nodes;
    }

    /**
     * Helper class that serves as project traversal helper.
     */
    class TraversalHelper implements ProjectTraversalHandler {
        protected List<ProjectPath> nodes;

        public TraversalHelper(List<ProjectPath> nodes) {
            this.nodes = nodes;
        }

        public void projectNode(ProjectPath path) {
            nodes.add(path);
        }

        /**
         * Returns true unless an object is a DataNode.
         */
        public boolean shouldReadChildren(
            Object node,
            ProjectPath parentPath) {
            // don't read linked maps
            return !(node instanceof DataNode);
        }
    }
}
