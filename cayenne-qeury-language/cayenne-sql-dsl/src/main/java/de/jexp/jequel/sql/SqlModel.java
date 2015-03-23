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

import de.jexp.jequel.expression.Alias;
import de.jexp.jequel.expression.Aliased;
import de.jexp.jequel.expression.AppendableExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.PathExpression;
import de.jexp.jequel.expression.SearchCondition;
import de.jexp.jequel.expression.SimpleListExpression;
import de.jexp.jequel.literals.Delimeter;
import de.jexp.jequel.literals.SelectKeyword;

import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;

public interface SqlModel {

    /**
     * From what we can do selection:
     *      Table
     *      View
     *      SubSelect
     *
     * */
    interface FromSource<T extends FromSource> extends Expression, Alias<T>, SqlDsl.SqlVisitable {

        List<PathExpression> columns();

    }

    class From implements SqlDsl.SqlVisitable {

        private final List<FromSource> sources;

        protected From(FromSource ... sources) {
            this.sources = new LinkedList<FromSource>(asList(sources));
        }

        @Override
        public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
            return sqlVisitor.visit(this);
        }

        public List<FromSource> getSources() {
            return sources;
        }

        public void append(List<FromSource> sources) {
            this.sources.addAll(sources);
        }
    }

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

        public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
            return sqlVisitor.visit(this);
        }

        public SelectKeyword getSelectKeyword() {
            return selectKeyword;
        }
    }

    class Select extends SimpleListExpression implements SqlDsl.SqlVisitable {
        public Select(Expression... expressions) {
            super(Delimeter.COMMA, expressions);
        }

        public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
            return sqlVisitor.visit(this);
        }
    }

    interface SelectPartExpression<T extends Expression> extends AppendableExpression<T>, SqlDsl.SqlVisitable {
        SelectKeyword getSelectKeyword();
    }

    class Where extends SearchCondition {
        @Override
        public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
            return sqlVisitor.visit(this);
        }
    }

    class Having extends SearchCondition {
        @Override
        public <R> R accept(SqlDsl.SqlVisitor<R> sqlVisitor) {
            return sqlVisitor.visit(this);
        }
    }

}
