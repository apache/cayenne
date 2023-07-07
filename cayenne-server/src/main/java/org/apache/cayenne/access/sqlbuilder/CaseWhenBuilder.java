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

import org.apache.cayenne.access.sqlbuilder.sqltree.CaseNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ElseNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.WhenNode;

import java.util.ArrayList;
import java.util.List;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.aliased;

class CaseWhenBuilder implements NodeBuilder {

    private final Node root;
    private final List<NodeBuilder> nodeBuilders;
    private NodeBuilder elseBuilder;

    public CaseWhenBuilder() {
        this.root = new CaseNode();
        this.nodeBuilders = new ArrayList<>();
    }

    public WhenBuilder when(NodeBuilder param) {
        nodeBuilders.add(() -> {
            WhenNode whenNode = new WhenNode();
            whenNode.addChild(param.build());
            return whenNode;
        });
        return new WhenBuilder(this);
    }

    public CaseWhenBuilder elseResult(NodeBuilder result) {
        elseBuilder = () -> {
            ElseNode elseNode = new ElseNode();
            elseNode.addChild(result.build());
            return elseNode;
        };
        return this;
    }

    public NodeBuilder as(String alias) {
        return aliased(this, alias);
    }

    @Override
    public Node build() {
        for (NodeBuilder builder : nodeBuilders) {
            root.addChild(builder.build());
        }
        if (elseBuilder != null){
            root.addChild(elseBuilder.build());
        }
        return root;
    }

    List<NodeBuilder> getNodeBuilders() {
        return nodeBuilders;
    }
}