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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * Translator of the EJBQL select clause.
 * 
 * @author Andrus Adamchik
 * @since 3.0
 */
class EJBQLSelectColumnsTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;

    EJBQLSelectColumnsTranslator(EJBQLTranslationContext context) {
        super(false);
        this.context = context;
    }

    public boolean visitSelectExpression(EJBQLExpression expression) {
        return true;
    }

    public boolean visitIdentifier(EJBQLExpression expression) {

        final String idVar = expression.getText();

        // append all table columns
        ClassDescriptor descriptor = context.getCompiledExpression().getEntityDescriptor(
                idVar);

        PropertyVisitor visitor = new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                ObjAttribute oa = property.getAttribute();
                Iterator dbPathIterator = oa.getDbPathIterator();
                while (dbPathIterator.hasNext()) {
                    Object pathPart = dbPathIterator.next();
                    if (pathPart instanceof DbRelationship) {
                        // DbRelationship rel = (DbRelationship) pathPart;
                        // dbRelationshipAdded(rel);
                    }
                    else if (pathPart instanceof DbAttribute) {
                        DbAttribute dbAttr = (DbAttribute) pathPart;
                        if (dbAttr == null) {
                            throw new CayenneRuntimeException(
                                    "ObjAttribute has no DbAttribute: " + oa.getName());
                        }

                        appendColumn(idVar, dbAttr, oa.getType());
                    }
                }
                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                visitRelationship(property);
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                visitRelationship(property);
                return true;
            }

            private void visitRelationship(ArcProperty property) {
                ObjRelationship rel = property.getRelationship();
                DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);

                List joins = dbRel.getJoins();
                int len = joins.size();
                for (int i = 0; i < len; i++) {
                    DbJoin join = (DbJoin) joins.get(i);
                    DbAttribute src = join.getSource();

                    appendColumn(idVar, src);
                }
            }
        };

        // EJBQL queries are polimorphic by definition - there is no distinction between
        // inheritance/no-inheritance fetch
        descriptor.visitAllProperties(visitor);

        return true;
    }

    private void appendColumn(String identifier, DbAttribute column) {
        appendColumn(identifier, column, TypesMapping.getJavaBySqlType(column.getType()));
    }

    private void appendColumn(String identifier, DbAttribute column, String javaType) {
        DbEntity table = (DbEntity) column.getEntity();
        String alias = context.getAlias(identifier, table.getFullyQualifiedName());
        String columnName = alias + "." + column.getName();

        Set columns = getColumns();

        if (columns.add(columnName)) {
            // using #result directive:
            // 1. to ensure that DB default captalization rules won't lead to changing
            // result columns capitalization, as #result() gives SQLTemplate a hint as to
            // what name is expected by the caller.
            // 2. to ensure proper type conversion
            context.append(columns.size() > 1 ? ", " : " ").append("#result('").append(
                    columnName).append("' '").append(javaType).append("' '").append(
                    column.getName()).append("')");
        }
    }

    private Set getColumns() {

        String columnsKey = getClass().getName() + ":columns";
        Set columns = (Set) context.getAttribute(columnsKey);

        if (columns == null) {
            columns = new HashSet();
            context.setAttribute(columnsKey, columns);
        }

        return columns;
    }
}
