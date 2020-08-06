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
package org.apache.cayenne.access.translator.ejbql;

import java.util.Collection;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;
import org.apache.cayenne.ejbql.parser.EJBQLAggregateColumn;
import org.apache.cayenne.ejbql.parser.EJBQLIntegerLiteral;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 3.0
 */
class EJBQLAggregateColumnTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private String attributeType;

    EJBQLAggregateColumnTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    @Override
    public boolean visitCount(EJBQLAggregateColumn expression) {
        visitAggregateColumn(expression, new CountColumnVisitor());
        return false;
    }

    @Override
    public boolean visitAverage(EJBQLAggregateColumn expression) {
        visitAggregateColumn(expression, new FieldPathTranslator());
        return false;
    }

    @Override
    public boolean visitMax(EJBQLAggregateColumn expression) {
        visitAggregateColumn(expression, new FieldPathTranslator());
        return false;
    }

    @Override
    public boolean visitMin(EJBQLAggregateColumn expression) {
        visitAggregateColumn(expression, new FieldPathTranslator());
        return false;
    }

    @Override
    public boolean visitSum(EJBQLAggregateColumn expression) {
        visitAggregateColumn(expression, new FieldPathTranslator());
        return false;
    }

    private void visitAggregateColumn(EJBQLAggregateColumn column, EJBQLExpressionVisitor pathVisitor) {

        if (context.isAppendingResultColumns()) {
            context.append(" #result('");
        } else {
            context.append(' ');
        }

        context.append(column.getFunction()).append('(');

        // path visitor must set attributeType ivar
        column.visit(pathVisitor);
        context.append(')');

        if (context.isAppendingResultColumns()) {
            context.append("' '").append(column.getJavaType(attributeType)).append("' '")
                    .append(context.nextColumnAlias()).append("')");
        }
    }

    /**
     * This method tries to find mandatory PK attribute, if no such exists returns first PK attribute.
     * Used to translate COUNT(entity) expressions into COUNT(alias.pk_column).
     */
    private DbAttribute getPk(DbEntity dbEntity) {
        for(DbAttribute attribute : dbEntity.getPrimaryKeys()) {
            if(attribute.isMandatory()) {
                return attribute;
            }
        }

        return dbEntity.getPrimaryKeys().iterator().next();
    }

    class FieldPathTranslator extends EJBQLPathTranslator {

        FieldPathTranslator() {
            super(EJBQLAggregateColumnTranslator.this.context);
        }

        @Override
        public boolean visitDistinct(EJBQLExpression expression) {
            context.append("DISTINCT ");
            return true;
        }

        @Override
        protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
            throw new EJBQLException("Can't use multi-column paths in column clause");
        }

        @Override
        protected void processTerminatingAttribute(ObjAttribute attribute) {

            EJBQLAggregateColumnTranslator.this.attributeType = attribute.getType();

            DbEntity table = currentEntity.getDbEntity();
            String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(idPath, context
                    .getQuotingStrategy().quotedFullyQualifiedName(table));
            context.append(alias).append('.')
                    .append(context.getQuotingStrategy().quotedName(attribute.getDbAttribute()));
        }

        @Override
        protected void processTerminatingRelationship(ObjRelationship relationship) {
            Collection<DbAttribute> dbAttr = relationship.getTargetEntity().getDbEntity().getAttributes();

            if (dbAttr.size() > 0) {
                resolveJoin();
            }

            DbAttribute pk = getPk(relationship.getTargetEntity().getDbEntity());
            context.append(lastAlias).append('.').append(context.getQuotingStrategy().quotedName(pk));
        }
    }

    class CountColumnVisitor extends EJBQLBaseVisitor {

        @Override
        public boolean visitDistinct(EJBQLExpression expression) {
            context.append("DISTINCT ");
            return true;
        }

        @Override
        public boolean visitIntegerLiteral(EJBQLIntegerLiteral expression) {
            // this allows to use COUNT(1) in EJBQL
            context.append(expression.getText());
            return false;
        }

        @Override
        public boolean visitIdentifier(EJBQLExpression expression) {
            ClassDescriptor classDescriptor = context.getCompiledExpression().getEntityDescriptor(expression.getText());
            if(classDescriptor == null) {
                throw new EJBQLException("Unmapped id variable: " + expression.getText());
            }

            DbEntity dbEntity = classDescriptor.getEntity().getDbEntity();
            String tableName = context.getQuotingStrategy().quotedFullyQualifiedName(dbEntity);
            context.append(context.getTableAlias(expression.getText(), tableName))
                    .append('.').append(context.getQuotingStrategy().quotedName(getPk(dbEntity)));
            return false;
        }

        @Override
        public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {
            expression.visit(new FieldPathTranslator());
            return false;
        }
    }
}
