/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.jexp.jequel.sql;

import de.jexp.jequel.SqlString;
import de.jexp.jequel.expression.BooleanExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.expression.visitor.Format;

/**
 * @since 4.0
 */
public interface SqlDsl {

    interface ToSql extends SqlVisitable {
        Sql toSql();
    }

    interface Select extends ToSql {
        From from(RowListExpression... tableReferences);
    }

    interface From extends SqlString, ToSql {
        Where where(BooleanExpression expression);
        OrderBy orderBy(Expression... groupBy);
        GroupBy groupBy(Expression... groupBy);
    }

    interface Where extends SqlString, ToSql {
        OrderBy orderBy(Expression... orderBy);
        GroupBy groupBy(Expression... groupBy);
        Having having(BooleanExpression searchCondition);
    }

    interface OrderBy extends SqlString, ToSql {
        GroupBy groupBy(Expression... groupBy);
        Having having(BooleanExpression searchCondition);
    }

    interface GroupBy extends SqlString, ToSql {
        Having having(BooleanExpression searchCondition);
    }

    interface Having extends SqlString, ToSql {

    }

    interface SqlFormat extends SqlVisitor<String>, Format {
    }

    interface SqlVisitor<R> {
        R visit(SqlModel.SelectPartColumnListExpression sqlPartColumnTupleExpression);

        R visit(SqlModel.Select select);

        R visit(SqlModel.Where where);

        R visit(SqlModel.Having having);

        R visit(SqlModel.From from);

        R visit(Sql sql);
    }

    interface SqlVisitable {
        <R> R accept(SqlVisitor<R> sqlVisitor);
    }
}
