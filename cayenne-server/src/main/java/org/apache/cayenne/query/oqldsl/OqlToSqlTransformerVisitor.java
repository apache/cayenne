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
package org.apache.cayenne.query.oqldsl;

import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.StringPathExpression;
import de.jexp.jequel.sql.Sql;
import de.jexp.jequel.sql.SqlModel;
import de.jexp.jequel.sql.SqlModel.FromSource;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.oqldsl.model.From;
import org.apache.cayenne.query.oqldsl.model.From.Entity;
import org.apache.cayenne.query.oqldsl.model.From.Relation;
import org.apache.cayenne.query.oqldsl.model.Select;
import org.apache.cayenne.query.oqldsl.model.SelectResult;
import org.apache.cayenne.query.oqldsl.model.SelectResult.SelectAttr;
import org.apache.cayenne.query.oqldsl.model.SelectResult.SelectFrom;
import org.apache.cayenne.query.oqldsl.model.visitor.ObjectQueryVisitor;

import java.util.LinkedList;
import java.util.List;

/**
 * @since 4.0
 */
public class OqlToSqlTransformerVisitor implements ObjectQueryVisitor<Expression> {
    private final DataMap dataMap;

    public OqlToSqlTransformerVisitor(DataMap dataMap) {
        this.dataMap = dataMap;
    }

    @Override
    public Expression visit(Select select) {
        List<SelectResult> selectResults = select.getSelectResults();

        Expression[] sqlResults = new Expression[selectResults.size()];
        for (int i = 0; i < selectResults.size(); i++) {
            SelectResult selectResult = selectResults.get(i);
            sqlResults[i] = selectResult.accept(this);
        }

        FromSource[] fromSources = new FromSource[select.getFrom().size()];
        List<From> from1 = select.getFrom();
        for (int i = 0; i < from1.size(); i++) {
            From from = from1.get(i);
            fromSources[i] = (FromSource) from.accept(this);
        }

        return Sql.Select(sqlResults)
                  .from(fromSources)
                  .toSql();
    }

    @Override
    public Expression visit(final SelectFrom selectEntity) {
        return new StringPathExpression(selectEntity.from.name()) {
            @Override
            public String getValue() {
                return selectEntity.from.name() + ".*";
            }
        };
    }

    @Override
    public Expression visit(SelectAttr selectAttr) {
        return null;
    }

    @Override
    public Expression visit(Entity from) {
        return new DbEntityFromSource(from.entity().getDbEntity());
    }

    @Override
    public Expression visit(Relation from) {
        return null;
    }
}
