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
import java.util.Iterator;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 3.0
 */
class EJBQLGroupByTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private int itemCount;

    EJBQLGroupByTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    @Override
    public boolean visitIdentifier(EJBQLExpression expression) {
        if (itemCount++ > 0) {
            context.append(',');
        }

        expression.visit(context.getTranslatorFactory().getIdentifierColumnsTranslator(context));
        return false;
    }

    @Override
    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {

        if (itemCount++ > 0) {
            context.append(',');
        }

        EJBQLExpressionVisitor childVisitor = new EJBQLPathTranslator(context) {

            @Override
            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                throw new EJBQLException("Can't GROUP BY on multi-column paths or objects");
            }

            @Override
            public boolean visitIdentificationVariable(EJBQLExpression expression) {

                String idVariableAbsolutePath = fullPath + "." + expression.getText();
                ClassDescriptor descriptor = context.getEntityDescriptor(idVariableAbsolutePath);
                if (descriptor != null) {
                    this.lastAlias = context.getTableAlias(idVariableAbsolutePath, context.getQuotingStrategy()
                            .quotedFullyQualifiedName(descriptor.getEntity().getDbEntity()));
                }

                resolveLastPathComponent(expression.getText());
                this.fullPath = fullPath + '.' + lastPathComponent;

                return true;
            }

            @Override
            protected void processTerminatingRelationship(ObjRelationship relationship) {

                Collection<DbAttribute> dbAttr = ((ObjEntity) relationship.getTargetEntity()).getDbEntity()
                        .getAttributes();

                DbRelationship dbRelationship = relationship.getDbRelationships().get(0);
                DbEntity table = (DbEntity) dbRelationship.getTargetEntity();

                Iterator<DbAttribute> it = dbAttr.iterator();

                String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(idPath, context
                        .getQuotingStrategy().quotedFullyQualifiedName(table));

                boolean first = true;
                while (it.hasNext()) {

                    context.append(!first ? ", " : " ");

                    DbAttribute dbAttribute = it.next();
                    context.append(alias).append('.').append(context.getQuotingStrategy().quotedName(dbAttribute));

                    first = false;
                }

            }

        };

        expression.visit(childVisitor);

        return false;
    }
}
