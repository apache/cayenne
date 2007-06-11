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
import java.util.Set;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.ejbql.EJBQLDelegatingVisitor;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * A translator of EJBQL select statements into SQL.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLSelectTranslator extends EJBQLDelegatingVisitor {

    private EJBQLTranslationContext parent;
    private Set columns;

    EJBQLSelectTranslator(EJBQLTranslationContext parent) {
        this.parent = parent;
    }

    void appendColumn(String identifier, DbAttribute column) {
        appendColumn(identifier, column, TypesMapping.getJavaBySqlType(column.getType()));
    }

    void appendColumn(String identifier, DbAttribute column, String javaType) {
        DbEntity table = (DbEntity) column.getEntity();
        String alias = parent.createAlias(identifier, table.getFullyQualifiedName());
        String columnName = alias + "." + column.getName();

        if (columns == null) {
            columns = new HashSet();
        }

        if (columns.add(columnName)) {
            // using #result directive:
            // 1. to ensure that DB default captalization rules won't lead to changing
            // result columns capitalization, as #result() gives SQLTemplate a hint as to
            // what name is expected by the caller.
            // 2. to ensure proper type conversion
            parent
                    .getBuffer()
                    .append(columns.size() > 1 ? ", " : " ")
                    .append("#result('")
                    .append(columnName)
                    .append("' '")
                    .append(javaType)
                    .append("' '")
                    .append(column.getName())
                    .append("')");
        }
    }

    EJBQLTranslationContext getParent() {
        return parent;
    }

    public boolean visitDistinct(EJBQLExpression expression) {
        parent.getBuffer().append(" DISTINCT");
        return true;
    }

    public boolean visitFrom(EJBQLExpression expression) {
        parent.getBuffer().append(" FROM");
        setDelegate(new EJBQLFromTranslator(this));
        return true;
    }

    public boolean visitOrderBy(EJBQLExpression expression) {
        parent.getBuffer().append(" ORDER BY");
        setDelegate(new EJBQLSelectOrderByTranslator());
        return true;
    }

    public boolean visitSelect(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            parent.getBuffer().append("SELECT");
            setDelegate(new EJBQLSelectColumnsTranslator(this));
        }

        return true;
    }

    public boolean visitWhere(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            parent.getBuffer().append(" WHERE");
            setDelegate(new EJBQLConditionTranslator(this));
        }
        return true;
    }
}
