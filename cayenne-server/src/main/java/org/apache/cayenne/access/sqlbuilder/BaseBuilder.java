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

package org.apache.cayenne.access.sqlbuilder;

import java.util.function.Supplier;

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;

/**
 * @since 4.2
 */
public abstract class BaseBuilder implements NodeBuilder {
    /**
     * Main root of this query
     */
    protected final Node root;

    /*
     * Following nodes are all children of root,
     * but we keep them here for quick access.
     */
    protected final Node[] nodes;

    public BaseBuilder(Node root, int size) {
        this.root = root;
        this.nodes = new Node[size];
    }

    protected Node node(int idx, Supplier<Node> nodeSupplier) {
        if(nodes[idx] == null) {
            nodes[idx] = nodeSupplier.get();
        }
        return nodes[idx];
    }

    @Override
    public Node build() {
        for (Node next : nodes) {
            if (next != null) {
                root.addChild(next);
            }
        }
        return root;
    }

    public Node getRoot() {
        return root;
    }
}
