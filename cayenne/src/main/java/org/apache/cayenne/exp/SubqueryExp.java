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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.ql.QLSelectPrinter;

/**
 * @since 5.0
 */
public final class SubqueryExp extends BaseExp {
    private static final TraversalHandler IN_MEMORY_VALIDATOR = new TraversalHandler() {
        @Override
        public void startNode(Expression node, Expression parentNode) {
            if (node instanceof EnclosingObjectExp) {
                throw new UnsupportedOperationException(
                        "Can't evaluate subquery expression with enclosing object expression."
                );
            }
        }
    };

    private final FluentSelect<?, ?> query;

    /**
     * Creates a subquery node wrapping the given select, or an empty one without arguments.
     */
    public SubqueryExp(Object... operands) {
        if (operands != null && operands.length > 0) {
            if (operands.length > 1 || !(operands[0] instanceof FluentSelect<?, ?> select)) {
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

    @Override
    protected Object evaluateNode(Object o) {
        ObjectContext context;
        if(o instanceof Persistent) {
            context = ((Persistent) o).getObjectContext();
        } else {
            throw new UnsupportedOperationException("Can't evaluate subquery expression against non-persistent object");
        }
        validateForInmemory(query);
        return context.select(query);
    }

    /**
     * Check that we can execute this subquery directly
     */
    private void validateForInmemory(FluentSelect<?, ?> query) {
        query.getWhere().traverse(IN_MEMORY_VALIDATOR);
        query.getHaving().traverse(IN_MEMORY_VALIDATOR);
        for(Ordering ordering : query.getOrderings()) {
            ordering.getSortSpec().traverse(IN_MEMORY_VALIDATOR);
        }
    }

    @Override
    public Expression shallowCopy() {
        return new SubqueryExp(query);
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        out.append('(');
        QLSelectPrinter.append(query, out);
        out.append(')');
    }

    public FluentSelect<?, ?> getQuery() {
        return query;
    }

    @Override
    protected boolean parenthesizeAsOperand() {
        return false;
    }
}
