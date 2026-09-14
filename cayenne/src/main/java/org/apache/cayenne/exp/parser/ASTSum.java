/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.exp.parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.exp.Expression;

/**
 * @since 4.0
 */
public class ASTSum extends ASTAggregateFunctionCall {

    ASTSum(int id) {
        super(id, "SUM");
    }

    public ASTSum(Expression expression) {
        super(ExpressionParserTreeConstants.JJTSUM, "SUM", expression);
    }

    @Override
    public Expression shallowCopy() {
        return new ASTSum(id);
    }

    /**
     * Sums numeric values following SQL semantics: nulls are skipped and an empty input produces null. The result is
     * a BigDecimal if any value is a BigDecimal, a Long if all values are integral, and a Double otherwise.
     */
    @Override
    protected Object evaluateCollection(Collection<?> values) {
        List<Number> numbers = new ArrayList<>(values.size());
        boolean decimal = false;
        boolean integral = true;
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (!(value instanceof Number number)) {
                throw new UnsupportedOperationException("Can't calculate sum for non-numeric type.");
            }
            numbers.add(number);
            decimal |= number instanceof BigDecimal;
            integral &= number instanceof Byte || number instanceof Short
                    || number instanceof Integer || number instanceof Long;
        }

        if (numbers.isEmpty()) {
            return null;
        }
        if (decimal) {
            return numbers.stream().map(ASTSum::toBigDecimal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (integral) {
            return numbers.stream().mapToLong(Number::longValue).reduce(0L, Math::addExact);
        }
        return numbers.stream().mapToDouble(Number::doubleValue).sum();
    }

    private static BigDecimal toBigDecimal(Number number) {
        return switch (number) {
            case BigDecimal bd -> bd;
            case BigInteger bi -> new BigDecimal(bi);
            case Double d -> BigDecimal.valueOf(d);
            case Float f -> new BigDecimal(f.toString());
            default -> BigDecimal.valueOf(number.longValue());
        };
    }
}
