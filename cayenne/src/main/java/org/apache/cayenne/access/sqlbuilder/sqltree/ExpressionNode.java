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

package org.apache.cayenne.access.sqlbuilder.sqltree;


import org.apache.cayenne.access.sqlbuilder.SQLAppendable;

/**
 * @since 4.2
 */
public class ExpressionNode extends Node {

    public ExpressionNode() {
    }

    public ExpressionNode(NodeType nodeType) {
        super(nodeType);
    }

    @Override
    public SQLAppendable append(SQLAppendable buffer) {
        return buffer;
    }

    @Override
    public void appendChildrenStart(SQLAppendable buffer) {
        if (needsParentheses()) {
            buffer.appendTokenSeparator().append('(').suppressNextTokenSeparator();
        }
    }

    @Override
    public void appendChildrenEnd(SQLAppendable buffer) {
        if (needsParentheses()) {
            buffer.append(")");
        }
    }

    protected boolean needsParentheses() {
        if (parent == null
                || parent.type == NodeType.WHERE
                || parent.type == NodeType.JOIN
                || parent.type == NodeType.UPDATE_SET
                || parent.type == NodeType.WHEN
                || parent.type == NodeType.THEN
                || parent.type == NodeType.ELSE) {
            return false;
        }

        if (parent instanceof ExpressionNode parentExpr) {
            String parentOp = parentExpr.logicalOperator();
            if (parentOp != null) {
                if (isComparison()) {
                    return false;
                }

                return !parentOp.equals(logicalOperator());
            }
        }
        return true;
    }

    /**
     * @return {@code "AND"} or {@code "OR"} if this node is a logical connective, otherwise {@code null}.
     */
    protected String logicalOperator() {
        return null;
    }

    /**
     * @return true for comparison operators that bind tighter than {@code AND}/{@code OR} and therefore don't need
     * parentheses when nested directly under them.
     */
    protected boolean isComparison() {
        return type == NodeType.EQUALITY || type == NodeType.LIKE;
    }

    @Override
    public String toString() {
        return "{ExpressionNode}";
    }

    @Override
    public Node copy() {
        return new ExpressionNode();
    }
}
