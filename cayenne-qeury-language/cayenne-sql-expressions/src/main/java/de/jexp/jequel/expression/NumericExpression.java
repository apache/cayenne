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

/**
 * TODO all operation should take Numeric.Expression or Number
 * */
public interface NumericExpression extends Expression {

    NumericExpression plus(NumericExpression expression);

    NumericExpression plus(Number expression);

    NumericExpression minus(NumericExpression expression);

    NumericExpression minus(Number expression);

    NumericExpression times(NumericExpression expression);

    NumericExpression times(Number expression);

    NumericExpression by(NumericExpression expression);

    NumericExpression by(Number expression);
}
