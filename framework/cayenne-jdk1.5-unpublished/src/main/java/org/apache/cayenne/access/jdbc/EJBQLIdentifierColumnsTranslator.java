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
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLIdentifierColumnsTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private Set columns;

    EJBQLIdentifierColumnsTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    public boolean visitIdentifier(EJBQLExpression expression) {

        final String idVar = expression.getText();

        // append all table columns
        ClassDescriptor descriptor = context.getEntityDescriptor(idVar);

        PropertyVisitor visitor = new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                ObjAttribute oa = property.getAttribute();
                Iterator dbPathIterator = oa.getDbPathIterator();
                while (dbPathIterator.hasNext()) {
                    Object pathPart = dbPathIterator.next();

                    if (pathPart == null) {
                        throw new CayenneRuntimeException(
                                "ObjAttribute has no component: " + oa.getName());
                    }

                    else if (pathPart instanceof DbRelationship) {
                        // DbRelationship rel = (DbRelationship) pathPart;
                        // dbRelationshipAdded(rel);
                    }
                    else if (pathPart instanceof DbAttribute) {
                        DbAttribute dbAttr = (DbAttribute) pathPart;

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

        // append id columns ... (some may have been appended already via relationships)

        DbEntity table = descriptor.getEntity().getDbEntity();
        Iterator it = table.getPrimaryKeys().iterator();
        while (it.hasNext()) {
            DbAttribute pk = (DbAttribute) it.next();
            appendColumn(idVar, pk);
        }

        return false;
    }

    private void appendColumn(String identifier, DbAttribute column) {
        appendColumn(identifier, column, null);
    }

    private void appendColumn(String identifier, DbAttribute column, String javaType) {
        DbEntity table = (DbEntity) column.getEntity();
        String alias = context.getTableAlias(identifier, table.getFullyQualifiedName());
        String columnName = alias + "." + column.getName();

        Set columns = getColumns();

        if (columns.add(columnName)) {

            context.append(columns.size() > 1 ? ", " : " ");

            if (context.isAppendingResultColumns()) {
                context.append("#result('");
            }

            context.append(columnName);

            if (context.isAppendingResultColumns()) {
                if (javaType == null) {
                    javaType = TypesMapping.getJavaBySqlType(column.getType());
                }

                // TODO: andrus 6/27/2007 - the last parameter is an unofficial "jdbcType"
                // pending CAY-813 implementation, switch to #column directive
                context.append("' '").append(javaType).append("' '").append(
                        column.getName()).append("' '").append(column.getName()).append(
                        "' " + column.getType()).append(")");
            }
        }
    }

    private Set getColumns() {

        if (columns == null) {
            columns = new HashSet();
        }

        return columns;
    }
}
