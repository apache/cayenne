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

import de.jexp.jequel.Sql92Format;
import de.jexp.jequel.expression.AppendableExpression;
import de.jexp.jequel.expression.DelegatingFormat;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.SearchCondition;
import de.jexp.jequel.expression.SimpleListExpression;
import de.jexp.jequel.expression.visitor.Format;
import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.literals.SelectKeyword;

public interface SqlModel {

    interface Yahoo {
        interface Functions {}

        class Select {
            public Selectable[] selectable;
            public From from;
            public BooleanExp where;
            public Order[] orderBy;
            public Column[] groupBy;
            public BooleanExp having;
        }

        interface Selectable {}

        class Column implements Selectable {}
        interface Expression extends Selectable {}
        interface StringExp extends Expression {
            StringExp concat(StringExp exp);
        }
        interface NumericExp extends Expression {}
        interface DateTimeExp extends Expression {}
        interface IntervalExp extends Expression {}
        interface BooleanExp extends Expression {
            enum Literal { TRUE, FALSE }

            BooleanExp not(BooleanExp exp);
            BooleanExp and(BooleanExp exp);
            BooleanExp or(BooleanExp exp);

            BooleanExp is(Literal exp);
            BooleanExp isNot(Literal exp);
        }


        interface From {}

        class Table implements From {}
        class Join implements From {
            public Table primaryKeyTable;
            public Table foreignKeyTable;
            public BooleanExp joinCondition;
        }
        class SubSelect extends Select implements From {}

        enum OrderType { ASC, DESC}
        class Order {
            public Column column;
            public OrderType type;
        }
    }

    class From { }

    class OrderBy {}

    class GroupBy {}

    class SelectPartColumnListExpression extends SimpleListExpression implements SelectPartExpression<Expression> {
        private final SelectKeyword selectKeyword;

        public SelectPartColumnListExpression(SelectKeyword selectKeyword) {
            this(selectKeyword, Delimeter.COMMA);
        }

        public SelectPartColumnListExpression(SelectKeyword selectKeyword, Delimeter delimeter, Expression... expressions) {
            super(delimeter, expressions);

            this.selectKeyword = selectKeyword;
        }

        public String toString() {
            return accept(SQL_FORMAT);
        }

        public <R> R accept(SqlVisitor<R> sqlVisitor) {
            return sqlVisitor.visit(this);
        }

        public SelectKeyword getSelectKeyword() {
            return selectKeyword;
        }
    }

    class Select extends SimpleListExpression implements SqlVisitable {
        public Select(Expression... expressions) {
            super(Delimeter.COMMA, expressions);
        }

        public String toString() {
            return accept(SelectPartExpression.SQL_FORMAT);
        }

        public <R> R accept(SqlVisitor<R> sqlVisitor) {
            return sqlVisitor.visit(this);
        }
    }

    interface SelectPartExpression<T extends Expression> extends AppendableExpression<T>, SqlVisitable {
        SqlExpressionFormat SQL_FORMAT = new SqlExpressionFormat(new Sql92Format());

        SelectKeyword getSelectKeyword();
    }

    class Where extends SearchCondition { }

    class Having extends SearchCondition { }


    class SqlExpressionFormat extends DelegatingFormat<SqlFormat> implements SqlFormat {

        public SqlExpressionFormat(SqlFormat format) {
            super(format);
        }

        @Override
        public String visit(SelectPartColumnListExpression sqlPartColumnTupleExpression) {
            return formatAround(getFormat().visit(sqlPartColumnTupleExpression), sqlPartColumnTupleExpression);
        }

        @Override
        public String visit(Select select) {
            return formatAround(getFormat().visit(select), select);
        }

        @Override
        public String visit(Where where) {
            return formatAround(getFormat().visit(where), where);
        }

        @Override
        public String visit(Having having) {
            return formatAround(getFormat().visit(having), having);
        }
    }

    interface SqlFormat extends SqlVisitor<String>, Format {
    }

    interface SqlVisitor<R> {
        R visit(SelectPartColumnListExpression sqlPartColumnTupleExpression);

        R visit(Select select);

        R visit(Where where);

        R visit(Having having);
    }

    interface SqlVisitable {
        <R> R accept(SqlVisitor<R> sqlVisitor);
    }
}
