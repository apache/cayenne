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

package org.apache.cayenne.dataview;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

public class BasicQueryBuilder {

    private ObjEntityView queryTarget;
    private List conditions = new ArrayList();

    public BasicQueryBuilder(ObjEntityView queryTarget) {
        this.queryTarget = queryTarget;
    }

    public void addEqual(String fieldName, Object value) {
        ObjEntityViewField field = queryTarget.getField(fieldName);
        String path = null;
        if (field.getCalcType().getValue() == CalcTypeEnum.NO_CALC_TYPE_VALUE) {
            path = field.getObjAttribute().getName();
        }
        else if (field.isLookup()) {
            path = field.getObjRelationship().getName();
        }
        Object rawValue = field.toRawValue(value);
        conditions.add(ExpressionFactory.matchExp(path, rawValue));
    }

    public void addRange(String fieldName, Object start, Object end) {
        ObjEntityViewField field = queryTarget.getField(fieldName);
        String path = null;
        if (field.getCalcType().getValue() == CalcTypeEnum.NO_CALC_TYPE_VALUE) {
            path = field.getObjAttribute().getName();
        }
        else if (field.isLookup()) {
            path = field.getObjRelationship().getName();
        }
        Object rawStart = field.toRawValue(start);
        Object rawEnd = field.toRawValue(end);
        Expression expr = null;
        if (rawStart != null && rawEnd != null)
            expr = ExpressionFactory.betweenExp(path, rawStart, rawEnd);
        else if (rawStart != null)
            expr = ExpressionFactory.greaterOrEqualExp(path, rawStart);
        else if (rawEnd != null)
            expr = ExpressionFactory.lessOrEqualExp(path, rawEnd);

        if (expr != null)
            conditions.add(expr);
    }

    public void addLike(String fieldName, Object value, boolean caseSensetive) {
        ObjEntityViewField field = queryTarget.getField(fieldName);
        String path = null;
        if (field.getCalcType().getValue() == CalcTypeEnum.NO_CALC_TYPE_VALUE) {
            path = field.getObjAttribute().getName();
        }
        else if (field.isLookup()) {
            path = field.getObjRelationship().getName();
        }
        Object rawValue = field.toRawValue(value);
        String pattern = (rawValue != null ? rawValue.toString() : "");
        Expression expr = (caseSensetive
                ? ExpressionFactory.likeExp(path, pattern)
                : ExpressionFactory.likeIgnoreCaseExp(path, pattern));
        conditions.add(expr);
    }

    public SelectQuery getSelectQuery() {
        SelectQuery query = new SelectQuery(queryTarget.getObjEntity());
        if (!conditions.isEmpty()) {
            Expression qualifier = ExpressionFactory.joinExp(Expression.AND, conditions);
            query.setQualifier(qualifier);
        }
        return query;
    }
}
