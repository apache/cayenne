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

package org.apache.cayenne.exp;

import java.io.IOException;

import org.apache.cayenne.query.ObjectSelect;

/**
 * @since 5.0
 */
public final class SubqueryExp extends Expression {
    private final ObjectSelect<?> query;

    /**
     * Creates a subquery node wrapping the given select, or an empty one without arguments.
     */
    public SubqueryExp(Object... operands) {
        if (operands != null && operands.length > 0) {
            if (operands.length > 1 || !(operands[0] instanceof ObjectSelect<?> select)) {
                throw new IllegalArgumentException("A subquery takes a single select");
            }
            this.query = select;
        } else {
            this.query = null;
        }
    }

    @Override
    protected String getExpressionOperator(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * In-memory evaluation of a subquery is not supported yet, as it ranges over all the objects of an entity, which
     * are only available in the database. Until it is, an expression with a subquery can only be used as a query
     * qualifier.
     */
    @Override
    protected Object evaluateNode(Object o) {
        throw new UnsupportedOperationException(
                "In-memory evaluation of a subquery is not supported, use it in a query: " + this);
    }

    @Override
    public Expression shallowCopy() {
        return new SubqueryExp(query);
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        out.append('(');
        query.appendQL(out);
        out.append(')');
    }

    public ObjectSelect<?> getQuery() {
        return query;
    }

    @Override
    protected boolean parenthesizeAsOperand() {
        return false;
    }
}
