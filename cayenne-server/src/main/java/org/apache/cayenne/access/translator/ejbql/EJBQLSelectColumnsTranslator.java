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
package org.apache.cayenne.access.translator.ejbql;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * Translator of the EJBQL select clause.
 * 
 * @since 3.0
 */
public class EJBQLSelectColumnsTranslator extends EJBQLBaseVisitor {

    protected EJBQLTranslationContext context;
    private int expressionsCount;

    protected EJBQLSelectColumnsTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    @Override
    public boolean visitSelectExpression(EJBQLExpression expression) {
        if (expressionsCount++ > 0) {
            context.append(",");
        }

        return true;
    }

    @Override
    public boolean visitAggregate(EJBQLExpression expression) {
        expression.visit(context.getTranslatorFactory().getAggregateColumnTranslator(context));
        return false;
    }

    @Override
    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {

        EJBQLPathTranslator pathTranslator = new EJBQLPathTranslator(context) {

            @Override
            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                throw new EJBQLException("Can't use multi-column paths in column clause");
            }

            @Override
            protected void processTerminatingRelationship(ObjRelationship relationship) {

                Map<String, String> xfields = null;
                if (context.isAppendingResultColumns()) {
                    xfields = context.nextEntityResult().getFields();
                }

                final Map<String, String> fields = xfields;

                Collection<DbAttribute> dbAttr = ((ObjEntity) relationship.getTargetEntity()).getDbEntity()
                        .getAttributes();

                DbRelationship dbRelationship = relationship.getDbRelationships().get(0);
                DbEntity table = (DbEntity) dbRelationship.getTargetEntity();

                Iterator<DbAttribute> it = dbAttr.iterator();
                if (dbAttr.size() > 0) {
                    this.resolveJoin(true);
                }

                String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(idPath, context
                        .getQuotingStrategy().quotedFullyQualifiedName(table));

                boolean first = true;
                while (it.hasNext()) {

                    context.append(!first ? ", " : " ");

                    DbAttribute dbAttribute = it.next();
                    appendColumn(TypesMapping.getJavaBySqlType(dbAttribute.getType()), alias, dbAttribute,
                            fields != null ? fields.get(dbAttribute.getName()) : "");

                    first = false;
                }

            }

            @Override
            protected void processTerminatingAttribute(ObjAttribute attribute) {
                DbEntity table = currentEntity.getDbEntity();
                String alias = this.lastAlias != null ? lastAlias : context.getTableAlias(idPath, context
                        .getQuotingStrategy().quotedFullyQualifiedName(table));
                if (attribute.isFlattened()) {
                    Iterator<?> dbPathIterator = attribute.getDbPathIterator();
                    EJBQLTableId lhsId = new EJBQLTableId(idPath);

                    while (dbPathIterator.hasNext()) {
                        Object pathPart = dbPathIterator.next();
                        // DbRelationships not processed, because they will be
                        // processed
                        // later when appending table
                        if (pathPart == null) {
                            throw new CayenneRuntimeException("ObjAttribute has no component: " + attribute.getName());
                        } else if (pathPart instanceof DbAttribute) {
                            DbAttribute dbAttribute = (DbAttribute) pathPart;
                            appendColumn(attribute.getType(),
                                    context.getTableAlias(lhsId.getEntityId(), context.getQuotingStrategy()
                                            .quotedFullyQualifiedName((DbEntity) dbAttribute.getEntity())),
                                    dbAttribute, context.isAppendingResultColumns() ? context.nextColumnAlias() : "");

                        }

                    }

                } else {

                    DbAttribute dbAttribute = attribute.getDbAttribute();

                    appendColumn(attribute.getType(), alias, dbAttribute,
                            context.isAppendingResultColumns() ? context.nextColumnAlias() : "");
                }
            }

        };
        expression.visit(pathTranslator);
        return false;
    }

    @Override
    public boolean visitIdentifier(EJBQLExpression expression) {
        expression.visit(context.getTranslatorFactory().getIdentifierColumnsTranslator(context));
        return false;
    }

    public void appendColumn(String javaType, String alias, DbAttribute dbAttribute, String columnAlias) {
        if (context.isAppendingResultColumns()) {
            context.append(" #result('");
        } else {
            context.append(' ');
        }

        context.append(alias).append('.').append(context.getQuotingStrategy().quotedName(dbAttribute));

        if (context.isAppendingResultColumns()) {
            // String columnAlias = context.nextColumnAlias();

            // TODO: andrus 6/27/2007 - the last parameter is an unofficial
            // "jdbcType"
            // pending CAY-813 implementation, switch to #column directive
            context.append("' '").append(javaType).append("' '").append(columnAlias).append("' '").append(columnAlias)
                    .append("' " + dbAttribute.getType()).append(")");
        }

    }

}
