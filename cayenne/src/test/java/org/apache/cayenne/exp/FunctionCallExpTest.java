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

import static org.junit.jupiter.api.Assertions.*;

public class FunctionCallExpTest {

    @Test
    public void equals() throws Exception {
        CountExp count1 = new CountExp();
        CountExp count2 = new CountExp();
        CountExp count3 = new CountExp(ExpressionFactory.dbPathExp("y"));
        SumExp sum1 = new SumExp(ExpressionFactory.dbPathExp("x"));
        SumExp sum2 = new SumExp(ExpressionFactory.dbPathExp("x"));
        SumExp sum3 = new SumExp(null);
        SumExp sum4 = new SumExp(ExpressionFactory.dbPathExp("y"));

        assertEquals(count1, count2);
        assertEquals(sum1, sum2);

        assertNotEquals(count1, count3);
        assertNotEquals(count1, sum1);
        assertNotEquals(count3, sum4);
        assertNotEquals(sum1, sum3);
        assertNotEquals(sum1, sum4);
        assertNotEquals(sum3, sum4);
    }

    @Test
    public void hashCodeValue() throws Exception {
        CountExp count1 = new CountExp();
        CountExp count2 = new CountExp();
        CountExp count3 = new CountExp(ExpressionFactory.dbPathExp("y"));
        SumExp sum1 = new SumExp(ExpressionFactory.dbPathExp("x"));
        SumExp sum2 = new SumExp(ExpressionFactory.dbPathExp("x"));
        SumExp sum3 = new SumExp(null);
        SumExp sum4 = new SumExp(ExpressionFactory.dbPathExp("y"));

        assertEquals(count1.hashCode(), count2.hashCode());
        assertEquals(sum1.hashCode(), sum2.hashCode());

        assertNotEquals(count1.hashCode(), count3.hashCode());
        assertNotEquals(count1.hashCode(), sum1.hashCode());
        assertNotEquals(count3.hashCode(), sum4.hashCode());
        assertNotEquals(sum1.hashCode(), sum3.hashCode());
        assertNotEquals(sum1.hashCode(), sum4.hashCode());
        assertNotEquals(sum3.hashCode(), sum4.hashCode());
    }

}