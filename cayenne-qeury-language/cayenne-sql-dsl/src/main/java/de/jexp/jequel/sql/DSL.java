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

/**
 * @since 4.0
 */
public interface Dsl {

    /*
        <query specification>    ::=
             SELECT [ <set quantifier> ] <select list>
            FROM <table reference> [ { "," <table reference> }* ] // Note that <correlation specification> does not appear in the ISO/IEC grammar. The notation is written out longhand several times, instead.
            [ WHERE <search condition> ]
            [ GROUP BY <grouping column reference> [ { "," <grouping column reference> }* ] ]
            [ HAVING <search condition> ]
    */
    enum SetQuantifier { DISTINCT, ALL }

    interface Alias<T> {
        T as(String alias);
    }

    interface Select extends ToSql {
        From from(RowListExpression... tableReferences);
    }

    interface Selectable { }

    interface ValueExpression extends Selectable { }
    interface NumericValueExpression extends ValueExpression {
        NumericValueExpression plus(NumericValueExpression exp);
        NumericValueExpression minus(NumericValueExpression exp);
        NumericValueExpression prod(NumericValueExpression exp);
        NumericValueExpression div(NumericValueExpression exp);
    }
    interface NumericValue extends NumericValueExpression {
        /*
        <value expression primary>    ::=
               <unsigned numeric literal>
             | <general literal>
             |  ":" <identifier> [ [ INDICATOR ] <parameter name> ]
             | "?"
             | VALUE
             | <column reference>
             |  COUNT "(" "*" ")"
             | {AVG | MAX | MIN | SUM | COUNT} "(" [ <set quantifier> ] <value expression> ")"
             | "(" <query expression> ")"
             | <case expression>
             | "(" <value expression> ")"
             | <cast specification>

         */
    }
    interface NumericFunction extends NumericValueExpression {
        /*
        <numeric value function>    ::=
               POSITION "(" <character value expression> IN <character value expression> ")"
             | EXTRACT "(" <extract field> FROM <extract source> ")"
             | CHAR_LENGTH "(" <string value expression> ")"
             | CHARACTER_LENGTH "(" <string value expression> ")"
             | OCTET_LENGTH "(" <string value expression> ")"
             | BIT_LENGTH "(" <string value expression> ")"
         */
    }

    interface StringValueExpression extends ValueExpression {
        <T extends StringValueExpression> T concat(T exp);
    }
    interface DatetimeValueExpression extends ValueExpression { }
    interface IntervalValueExpression extends ValueExpression { }

    interface ToSql {
        public Sql toSql();

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

    interface SearchCondition {
        SearchCondition not(SearchCondition searchCondition);
        SearchCondition and(SearchCondition searchCondition);
        SearchCondition or(SearchCondition searchCondition);

        SearchCondition isTrue();
        SearchCondition isFalse();
        SearchCondition isUnknown();
        SearchCondition isNull();
        SearchCondition isNotTrue();
        SearchCondition isNotFalse();
        SearchCondition isNotUnknown();
        SearchCondition isNotNull();
    }

    interface BooleanPrimary extends SearchCondition {}

    interface TableReference {
    }
}
