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
package de.jexp.jequel.expression;

import de.jexp.jequel.literals.Operator;

import static de.jexp.jequel.literals.Operator.*;

/**
* @since 4.0
*/
abstract class NumericAbstractExpression extends AbstractExpression implements NumericExpression {

    @Override
    public NumericExpression plus(NumericExpression expression) {
        return exp(PLUS, expression);
    }

    @Override
    public NumericExpression plus(Number expression) {
        return exp(PLUS, expression);
    }

    @Override
    public NumericExpression minus(NumericExpression expression) {
        return exp(MINUS, expression);
    }

    @Override
    public NumericExpression minus(Number expression) {
        return exp(MINUS, expression);
    }

    @Override
    public NumericExpression times(NumericExpression expression) {
        return exp(TIMES, expression);
    }

    @Override
    public NumericExpression times(Number expression) {
        return exp(TIMES, expression);
    }

    @Override
    public NumericExpression by(NumericExpression expression) {
        return exp(BY, expression);
    }

    @Override
    public NumericExpression by(Number expression) {
        return exp(BY, expression);
    }

    protected NumericExpression exp(Operator operator, NumericExpression expression) {
        return factory().createNumeric(this, operator, expression);
    }

    protected NumericExpression exp(Operator operator, Number number) {
        return exp(operator, factory().createNumeric(number));
    }
}
