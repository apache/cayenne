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

package org.apache.cayenne.exp.parser;

import java.io.IOException;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.translator.select.FluentSelectWrapper;
import org.apache.cayenne.access.translator.select.TranslatableQueryWrapper;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.Ordering;

/**
 * @since 4.2
 */
public class ASTSubquery extends SimpleNode {

    private static final TraversalHandler IN_MEMORY_VALIDATOR = new TraversalHandler() {
        @Override
        public void startNode(Expression node, Expression parentNode) {
            if (node.getType() == Expression.ENCLOSING_OBJECT) {
                throw new UnsupportedOperationException(
                        "Can't evaluate subquery expression with enclosing object expression."
                );
            }
        }
    };

    private final TranslatableQueryWrapper query;

    public ASTSubquery(FluentSelect<?, ?> query) {
        this(new FluentSelectWrapper(query));
    }

    public ASTSubquery(TranslatableQueryWrapper query) {
        super(0);
        this.query = query;
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
        return context.select(query.unwrap());
    }

    /**
     * Check that we can execute this subquery directly
     */
    private void validateForInmemory(TranslatableQueryWrapper query) {
        query.getQualifier().traverse(IN_MEMORY_VALIDATOR);
        query.getHavingQualifier().traverse(IN_MEMORY_VALIDATOR);
        for(Ordering ordering : query.getOrderings()) {
            ordering.getSortSpec().traverse(IN_MEMORY_VALIDATOR);
        }
    }

    @Override
    public Expression shallowCopy() {
        return new ASTSubquery(query);
    }

    @Override
    public int getType() {
        return Expression.SUBQUERY;
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        out.append("EXISTS");
    }

    public TranslatableQueryWrapper getQuery() {
        return query;
    }

}
