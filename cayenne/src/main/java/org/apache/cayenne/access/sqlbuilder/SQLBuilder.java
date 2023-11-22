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

import org.apache.cayenne.access.sqlbuilder.sqltree.AliasedNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.SimpleNodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public final class SQLBuilder {

    public static SelectBuilder select(NodeBuilder... params) {
        return new SelectBuilder(params);
    }

    public static InsertBuilder insert(String table) {
        return new InsertBuilder(table);
    }

    public static InsertBuilder insert(DbEntity table) {
        return new InsertBuilder(table);
    }

    public static UpdateBuilder update(String table) {
        return new UpdateBuilder(table);
    }

    public static UpdateBuilder update(DbEntity table) {
        return new UpdateBuilder(table);
    }

    public static DeleteBuilder delete(String table) {
        return new DeleteBuilder(table);
    }

    public static DeleteBuilder delete(DbEntity table) {
        return new DeleteBuilder(table);
    }

    public static TableNodeBuilder table(String table) {
        return new TableNodeBuilder(table);
    }

    public static TableNodeBuilder table(DbEntity table) {
        return new TableNodeBuilder(table);
    }

    public static ColumnNodeBuilder column(String column) {
        return new ColumnNodeBuilder(null, column);
    }

    public static WhenBuilder caseWhen(NodeBuilder param) {
        return new CaseWhenBuilder().when(param);
    }

    public static JoinNodeBuilder join(NodeBuilder table) {
        return new JoinNodeBuilder(JoinType.INNER, table);
    }

    public static JoinNodeBuilder leftJoin(NodeBuilder table) {
        return new JoinNodeBuilder(JoinType.LEFT, table);
    }

    public static JoinNodeBuilder rightJoin(NodeBuilder table) {
        return new JoinNodeBuilder(JoinType.RIGHT, table);
    }

    public static JoinNodeBuilder innerJoin(NodeBuilder table) {
        return new JoinNodeBuilder(JoinType.INNER, table);
    }

    public static JoinNodeBuilder outerJoin(NodeBuilder table) {
        return new JoinNodeBuilder(JoinType.OUTER, table);
    }

    public static ExpressionNodeBuilder exists(NodeBuilder builder) {
        return new ExpressionNodeBuilder(new ExistsNodeBuilder(builder));
    }

    public static ValueNodeBuilder value(Object value) {
        return new ValueNodeBuilder(value);
    }

    public static ExpressionNodeBuilder exp(NodeBuilder builder) {
        return new ExpressionNodeBuilder(builder);
    }

    public static NodeBuilder node(Node node) {
        return () -> node;
    }

    public static NodeBuilder aliased(NodeBuilder nodeBuilder, String alias) {
        return new AliasedNodeBuilder(nodeBuilder, alias);
    }

    public static NodeBuilder aliased(Node node, String alias) {
        if(suppressAlias(node)) {
            return node(node);
        }

        if(node instanceof FunctionNode) {
            ((FunctionNode) node).setAlias(alias);
            return node(node);
        }

        if(node instanceof ColumnNode) {
            ((ColumnNode) node).setAlias(alias);
            return node(node);
        }

        return new AliasedNodeBuilder(node(node), alias);
    }

    public static NodeBuilder text(String text) {
        return () -> new TextNode(text);
    }

    public static NodeBuilder all() {
        return text(" *");
    }

    public static ExpressionNodeBuilder not(NodeBuilder value) {
        return new ExpressionNodeBuilder(value).not();
    }

    public static FunctionNodeBuilder count(NodeBuilder value) {
        return function("COUNT", value);
    }

    public static FunctionNodeBuilder count() {
        return function("COUNT", column("*"));
    }

    public static FunctionNodeBuilder avg(NodeBuilder value) {
        return function("AVG", value);
    }

    public static FunctionNodeBuilder min(NodeBuilder value) {
        return function("MIN", value);
    }

    public static FunctionNodeBuilder max(NodeBuilder value) {
        return function("MAX", value);
    }

    public static FunctionNodeBuilder function(String function, NodeBuilder... values) {
        return new FunctionNodeBuilder(function, values);
    }

    public static OrderingNodeBuilder order(NodeBuilder expression) {
        return new OrderingNodeBuilder(expression);
    }

    private SQLBuilder() {
    }

    private static boolean suppressAlias(Node node) {
        return new SuppressAliasChecker().shouldSuppressForNode(node);
    }

    private static class SuppressAliasChecker extends SimpleNodeTreeVisitor {

        private boolean suppressAlias;

        public boolean shouldSuppressForNode(Node node) {
            node.visit(this);
            return suppressAlias;
        }

        @Override
        public boolean onNodeStart(Node node) {
            if(node.getType() == NodeType.COLUMN && ((ColumnNode) node).getAlias() != null) {
                suppressAlias = true;
                return false;
            } else if(node.getType() == NodeType.FUNCTION && ((FunctionNode) node).getAlias() != null) {
                suppressAlias = true;
                return false;
            } else if(node instanceof AliasedNode) {
                suppressAlias = true;
                return false;
            }
            return true;
        }
    }
}
