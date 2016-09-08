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

package org.apache.cayenne.exp.parser;

import org.apache.cayenne.exp.Expression;

/**
 * @since 4.0
 */
public class AggregationFunction {

    public interface SqlAggregationFunction {
        String sql();
    }

    public enum Function implements SqlAggregationFunction {
        COUNT, MAX, MIN, AVG, SUM;

        public String sql() {
            return name().toLowerCase();
        }
    }

    private final SqlAggregationFunction function;
    private final String name;
    private final Expression expression;

    public AggregationFunction(SqlAggregationFunction function) {
        this(function, null, null);
    }

    public AggregationFunction(SqlAggregationFunction function, String name) {
        this(function, null, name);
    }

    public AggregationFunction(SqlAggregationFunction function, Expression exp) {
        this(function, exp, null);
    }

    public AggregationFunction(SqlAggregationFunction function, Expression exp, String name) {
        if (function == null) {
            throw new IllegalArgumentException("function can't be null");
        }

        this.function = function;
        this.name = name;
        this.expression = exp;
    }

    public String getName() {
        return name;
    }

    public Expression getExpression() {
        return expression;
    }

    public SqlAggregationFunction getFunction() {
        return function;
    }

}
