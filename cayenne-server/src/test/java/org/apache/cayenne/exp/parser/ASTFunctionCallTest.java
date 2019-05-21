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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.0
 */
public class ASTFunctionCallTest {

    @Test
    public void testEquals() throws Exception {
        ASTCount count1 = new ASTCount();
        ASTCount count2 = new ASTCount();
        ASTCount count3 = new ASTCount(new ASTDbPath("y"));
        ASTSum sum1 = new ASTSum(new ASTDbPath("x"));
        ASTSum sum2 = new ASTSum(new ASTDbPath("x"));
        ASTSum sum3 = new ASTSum(null);
        ASTSum sum4 = new ASTSum(new ASTDbPath("y"));

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
    public void testHashCode() throws Exception {
        ASTCount count1 = new ASTCount();
        ASTCount count2 = new ASTCount();
        ASTCount count3 = new ASTCount(new ASTDbPath("y"));
        ASTSum sum1 = new ASTSum(new ASTDbPath("x"));
        ASTSum sum2 = new ASTSum(new ASTDbPath("x"));
        ASTSum sum3 = new ASTSum(null);
        ASTSum sum4 = new ASTSum(new ASTDbPath("y"));

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