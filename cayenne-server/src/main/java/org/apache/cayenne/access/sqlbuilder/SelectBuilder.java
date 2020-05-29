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

import org.apache.cayenne.access.sqlbuilder.sqltree.DistinctNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FromNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.GroupByNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.HavingNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.OrderByNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SelectNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SelectResultNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TopNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.WhereNode;

/**
 * @since 4.2
 */
public class SelectBuilder extends BaseBuilder {

    private static final int SELECT_NODE    = 0;
    private static final int FROM_NODE      = 1;
    private static final int WHERE_NODE     = 2;
    private static final int GROUPBY_NODE   = 3;
    private static final int HAVING_NODE    = 4;
    private static final int UNION_NODE     = 5;
    private static final int ORDERBY_NODE   = 6;
    private static final int LIMIT_NODE     = 7;

    SelectBuilder(NodeBuilder... selectExpressions) {
        super(new SelectNode(), LIMIT_NODE + 1);
        for(NodeBuilder exp : selectExpressions) {
            node(SELECT_NODE, SelectResultNode::new).addChild(exp.build());
        }
    }

    public SelectBuilder distinct() {
        root.addChild(new DistinctNode());
        return this;
    }

    public SelectBuilder top(int count) {
        root.addChild(new TopNode(count));
        return this;
    }

    public SelectBuilder result(NodeBuilder selectExpression) {
        node(SELECT_NODE, SelectResultNode::new).addChild(selectExpression.build());
        return this;
    }

    public SelectBuilder from(NodeBuilder table) {
        node(FROM_NODE, FromNode::new).addChild(table.build());
        return this;
    }

    public SelectBuilder from(NodeBuilder... tables) {
        for(NodeBuilder next : tables) {
            node(FROM_NODE, FromNode::new).addChild(next.build());
        }
        return this;
    }

    public SelectBuilder where(NodeBuilder... params) {
        for(NodeBuilder next : params) {
            node(WHERE_NODE, WhereNode::new).addChild(next.build());
        }
        return this;
    }

    public SelectBuilder where(Node node) {
        node(WHERE_NODE, WhereNode::new).addChild(node);
        return this;
    }

    public SelectBuilder orderBy(NodeBuilder... params) {
        for(NodeBuilder next : params) {
            node(ORDERBY_NODE, OrderByNode::new).addChild(next.build());
        }
        return this;
    }

    public SelectBuilder orderBy(NodeBuilder param) {
        node(ORDERBY_NODE, OrderByNode::new).addChild(param.build());
        return this;
    }

    public SelectBuilder groupBy(NodeBuilder... params) {
        for(NodeBuilder next : params) {
            node(GROUPBY_NODE, GroupByNode::new).addChild(next.build());
        }
        return this;
    }

    public SelectBuilder groupBy(Node node) {
        node(GROUPBY_NODE, GroupByNode::new).addChild(node);
        return this;
    }

    public SelectBuilder having(NodeBuilder... params) {
        for(NodeBuilder next : params) {
            node(HAVING_NODE, HavingNode::new).addChild(next.build());
        }
        return this;
    }

    public SelectBuilder having(Node node) {
        node(HAVING_NODE, HavingNode::new).addChild(node);
        return this;
    }

    public SelectBuilder limitOffset(int limit, int offset) {
        nodes[LIMIT_NODE] = new LimitOffsetNode(limit, offset);
        return this;
    }

}
