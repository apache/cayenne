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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.DistinctNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.InNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LikeNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.SimpleNodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;


/**
 * @since 4.2
 */
public class BaseSQLTreeProcessor extends SimpleNodeTreeVisitor implements SQLTreeProcessor {

    @Override
    public Node process(Node node) {
        node.visit(this);
        return node;
    }

    protected void onValueNode(Node parent, ValueNode child, int index) {
    }

    protected void onFunctionNode(Node parent, FunctionNode child, int index) {
    }

    protected void onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
    }

    protected void onColumnNode(Node parent, ColumnNode child, int index) {
    }

    protected void onInNode(Node parent, InNode child, int index) {
    }

    protected void onLikeNode(Node parent, LikeNode child, int index) {
    }

    protected void onResultNode(Node parent, Node child, int index) {
    }

    protected void onDistinctNode(Node parent, DistinctNode child, int index) {
    }

    protected void onUndefinedNode(Node parent, Node child, int index) {
    }

    protected static void replaceChild(Node parent, int index, Node newChild) {
        replaceChild(parent, index, newChild, true);
    }

    protected static void replaceChild(Node parent, int index, Node newChild, boolean transferChildren) {
        if (transferChildren) {
            Node oldChild = parent.getChild(index);
            for (int i = 0; i < oldChild.getChildrenCount(); i++) {
                newChild.addChild(oldChild.getChild(i));
            }
        }
        parent.replaceChild(index, newChild);
    }

    protected static Node wrapInFunction(Node node, String function) {
        FunctionNode functionNode = new FunctionNode(function, null);
        functionNode.addChild(node);
        return functionNode;
    }

    @Override
    public boolean onChildNodeStart(Node parent, Node child, int index, boolean hasMore) {
        switch (child.getType()) {
            case VALUE:
                onValueNode(parent, (ValueNode) child, index);
                break;

            case FUNCTION:
                onFunctionNode(parent, (FunctionNode) child, index);
                break;

            case LIMIT_OFFSET:
                onLimitOffsetNode(parent, (LimitOffsetNode) child, index);
                break;

            case COLUMN:
                onColumnNode(parent, (ColumnNode) child, index);
                break;

            case IN:
                onInNode(parent, (InNode) child, index);
                break;

            case LIKE:
                onLikeNode(parent, (LikeNode) child, index);
                break;

            case RESULT:
                onResultNode(parent, child, index);
                break;

            case DISTINCT:
                onDistinctNode(parent, (DistinctNode) child, index);
                break;

            default:
                onUndefinedNode(parent, child, index);
                break;
        }
        return true;
    }
}
