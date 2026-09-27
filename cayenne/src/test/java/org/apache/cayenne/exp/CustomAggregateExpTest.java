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

package org.apache.cayenne.exp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomAggregateExpTest {

    @Test
    public void parse() {
        Expression exp = ExpressionFactory.exp("agg('quantile', value, 0.3)");

        assertTrue(exp instanceof CustomAggregateExp);
        assertEquals("agg(\"quantile\", value, 0.3)", exp.toString());
    }

    @Test
    public void customAggregateAsFunctionArg() {
        Expression exp = ExpressionFactory.exp("fn('format_quantile', agg('quantile', value, 0.3))");

        assertTrue(exp instanceof CustomFunctionExp);
        assertEquals("fn(\"format_quantile\", agg(\"quantile\", value, 0.3))", exp.toString());
    }

    @Test
    public void evaluate() {
        CustomAggregateExp aggregate = new CustomAggregateExp();
        aggregate.setFunctionName("test");
        assertThrows(ExpressionException.class, () -> aggregate.evaluate(new Object()));
        assertThrows(UnsupportedOperationException.class, () -> aggregate.evaluateCollection(List.of()));
    }
}