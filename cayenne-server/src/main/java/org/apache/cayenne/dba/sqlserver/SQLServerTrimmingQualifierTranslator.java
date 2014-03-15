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
package org.apache.cayenne.dba.sqlserver;

import java.io.IOException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.access.translator.select.TrimmingQualifierTranslator;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 3.0
 */
class SQLServerTrimmingQualifierTranslator extends TrimmingQualifierTranslator {

    // since LIKE IGNORE CASE requires more contextual information than the super
    // translator can provide, we are using an internal element stack to trace translation
    // context.. Maybe it is a good idea to introduce it in the superclass?
    private List<Expression> expressionStack;

    SQLServerTrimmingQualifierTranslator(QueryAssembler queryAssembler,
            String trimFunction) {
        super(queryAssembler, trimFunction);
        expressionStack = new ArrayList<Expression>();
    }

    @Override
    public void startNode(Expression node, Expression parentNode) {
        push(node);
        super.startNode(node, parentNode);
    }

    @Override
    protected void processColumn(DbAttribute dbAttr) throws IOException {

        Expression node = peek(1);

        boolean likeCI = node != null
                && dbAttr.getType() == Types.CLOB
                && (node.getType() == Expression.LIKE_IGNORE_CASE || node.getType() == Expression.NOT_LIKE_IGNORE_CASE);

        if (likeCI) {
            out.append("CAST(");
        }

        super.processColumn(dbAttr);

        if (likeCI) {
            out.append(" AS NVARCHAR(MAX))");
        }
    }

    @Override
    protected void processColumnWithQuoteSqlIdentifiers(
            DbAttribute dbAttr,
            Expression pathExp) throws IOException {
        Expression node = peek(1);

        boolean likeCI = node != null
                && dbAttr.getType() == Types.CLOB
                && (node.getType() == Expression.LIKE_IGNORE_CASE || node.getType() == Expression.NOT_LIKE_IGNORE_CASE);

        if (likeCI) {
            out.append("CAST(");
        }

        super.processColumnWithQuoteSqlIdentifiers(dbAttr, node);

        if (likeCI) {
            out.append(" AS NVARCHAR(MAX))");
        }
    }

    @Override
    public void endNode(Expression node, Expression parentNode) {
        super.endNode(node, parentNode);
        pop();
    }

    private void push(Expression node) {
        expressionStack.add(node);
    }

    private void pop() {
        int len = expressionStack.size();
        if (len > 0) {
            expressionStack.remove(len - 1);
        }
    }

    private Expression peek(int tailIndex) {
        int index = expressionStack.size() - tailIndex - 1;
        if (index < 0) {
            return null;
        }

        return expressionStack.get(index);
    }
}
