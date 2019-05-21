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

import java.util.Objects;

import org.apache.cayenne.access.sqlbuilder.sqltree.JoinNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;

/**
 * @since 4.2
 */
public class JoinNodeBuilder implements NodeBuilder {

    private final JoinType joinType;
    private final NodeBuilder table;

    private NodeBuilder joinExp;

    JoinNodeBuilder(JoinType joinType, NodeBuilder table) {
        this.joinType = Objects.requireNonNull(joinType);
        this.table = Objects.requireNonNull(table);
    }

    public JoinNodeBuilder on(NodeBuilder joinExp) {
        this.joinExp = Objects.requireNonNull(joinExp);
        return this;
    }

    @Override
    public Node build() {
        Node node = new JoinNode(joinType);
        node.addChild(table.build());
        node.addChild(joinExp.build());
        return node;
    }
}
