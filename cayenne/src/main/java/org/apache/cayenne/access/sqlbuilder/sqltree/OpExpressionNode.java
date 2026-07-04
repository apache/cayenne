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

import java.util.Objects;

/**
 * @since 4.2
 */
public class OpExpressionNode extends ExpressionNode {

    private final String op;

    public OpExpressionNode(String op) {
        this.op = op;
    }

    @Override
    public void appendChildrenSeparator(SQLAppendable buffer, int childInd) {
        buffer.appendTokenSeparator().append(op);
    }

    @Override
    protected String logicalOperator() {
        return "AND".equals(op) || "OR".equals(op) ? op : null;
    }

    @Override
    protected boolean isComparison() {
        return switch (op) {
            case "<", "<=", ">", ">=" -> true;
            default -> false;
        };
    }

    @Override
    public Node copy() {
        return new OpExpressionNode(op);
    }

    public String getOp() {
        return op;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OpExpressionNode that = (OpExpressionNode) o;
        return Objects.equals(op, that.op);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), op);
    }
}
