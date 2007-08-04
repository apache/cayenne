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
package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;
import org.apache.cayenne.ejbql.parser.EJBQLAggregateColumn;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLAggregateColumnTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private String attributeType;
    private boolean resultColumns;

    EJBQLAggregateColumnTranslator(EJBQLTranslationContext context, boolean resultColumns) {
        this.context = context;
        this.resultColumns = resultColumns;
    }

    public boolean visitCount(EJBQLAggregateColumn expression) {
        visitAggregateColumn(expression, new CountColumnVisitor());
        return false;
    }

    public boolean visitAverage(EJBQLAggregateColumn expression) {
        visitAggregateColumn(expression, new FieldPathTranslator());
        return false;
    }

    public boolean visitMax(EJBQLAggregateColumn expression) {
        visitAggregateColumn(expression, new FieldPathTranslator());
        return false;
    }

    public boolean visitMin(EJBQLAggregateColumn expression) {
        visitAggregateColumn(expression, new FieldPathTranslator());
        return false;
    }

    public boolean visitSum(EJBQLAggregateColumn expression) {
        visitAggregateColumn(expression, new FieldPathTranslator());
        return false;
    }

    private void visitAggregateColumn(
            EJBQLAggregateColumn column,
            EJBQLExpressionVisitor pathVisitor) {

        if (resultColumns) {
            context.append(" #result('");
        }
        else {
            context.append(' ');
        }

        context.append(column.getFunction()).append('(');

        // path visitor must set attributeType ivar
        column.visit(pathVisitor);
        context.append(')');

        if (resultColumns) {
            context
                    .append("' '")
                    .append(column.getJavaType(attributeType))
                    .append("' '")
                    .append(context.nextColumnAlias())
                    .append("')");
        }
    }

    class FieldPathTranslator extends EJBQLPathTranslator {

        FieldPathTranslator() {
            super(EJBQLAggregateColumnTranslator.this.context);
        }

        public boolean visitDistinct(EJBQLExpression expression) {
            context.append("DISTINCT ");
            return true;
        }

        protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
            throw new EJBQLException("Can't use multi-column paths in column clause");
        }

        protected void processTerminatingAttribute(ObjAttribute attribute) {

            EJBQLAggregateColumnTranslator.this.attributeType = attribute.getType();

            DbEntity table = currentEntity.getDbEntity();
            String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(
                    idPath,
                    table.getFullyQualifiedName());
            context.append(alias).append('.').append(attribute.getDbAttributeName());
        }
    }

    class CountColumnVisitor extends EJBQLBaseVisitor {

        public boolean visitDistinct(EJBQLExpression expression) {
            context.append("DISTINCT ");
            return true;
        }

        public boolean visitIdentifier(EJBQLExpression expression) {
            context.append('*');
            return false;
        }

        public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
            expression.visit(new FieldPathTranslator());
            return false;
        }
    }
}
