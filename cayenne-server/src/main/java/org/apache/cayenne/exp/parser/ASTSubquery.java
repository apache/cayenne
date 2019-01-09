/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.access.translator.select.ColumnSelectWrapper;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.access.translator.select.ObjectSelectWrapper;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.access.translator.select.SelectQueryWrapper;
import org.apache.cayenne.access.translator.select.TranslatableQueryWrapper;

/**
 * @since 4.2
 */
public class ASTSubquery extends SimpleNode {

    private final TranslatableQueryWrapper query;

    public ASTSubquery(SelectQuery<?> query) {
        this(new SelectQueryWrapper(query));
    }

    public ASTSubquery(ObjectSelect<?> query) {
        this(new ObjectSelectWrapper(query));
    }

    public ASTSubquery(ColumnSelect<?> query) {
        this(new ColumnSelectWrapper(query));
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

        return context.select(query.unwrap());
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
