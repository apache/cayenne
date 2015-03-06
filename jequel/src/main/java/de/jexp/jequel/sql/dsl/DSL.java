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
package de.jexp.jequel.sql.dsl;

import de.jexp.jequel.SqlString;
import de.jexp.jequel.expression.BooleanExpression;
import de.jexp.jequel.expression.Expression;
import de.jexp.jequel.expression.RowListExpression;
import de.jexp.jequel.sql.Where;

import java.util.List;

/**
 * @since 4.0
 */
public interface DSL {

    /*
        <query specification>    ::=
             SELECT [ <set quantifier> ] <select list>
            FROM <table reference> [ { "," <table reference> }* ] // Note that <correlation specification> does not appear in the ISO/IEC grammar. The notation is written out longhand several times, instead.
            [ WHERE <search condition> ]
            [ GROUP BY <grouping column reference> [ { "," <grouping column reference> }* ] ]
            [ HAVING <search condition> ]
    */

    interface Select {
        From from(TableReference ... tableReferences);
        From from(List<TableReference> tableReferences);


    }

    interface From extends SqlString {
        Where from(RowListExpression... tables);
    }

    interface GroupBy extends SqlString {
        Having groupBy(Expression... groupBy);
    }

    interface Having extends SqlString {
        Expression having(BooleanExpression having);
    }

    interface OrderBy extends GroupBy {
        GroupBy orderBy(Expression... orderBy);
    }

    interface TableReference {
    }


}
