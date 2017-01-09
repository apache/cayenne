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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.0
 */
public class ASTAggregateTest {

    @Test
    public void testAvgConstruct() throws Exception {
        ASTAvg avg = new ASTAvg(null);
        assertEquals("AVG", avg.getFunctionName());
    }

    @Test
    public void testCountConstruct() throws Exception {
        ASTCount count = new ASTCount(null);
        assertEquals("COUNT", count.getFunctionName());
    }

    @Test
    public void testMinConstruct() throws Exception {
        ASTMin min = new ASTMin(null);
        assertEquals("MIN", min.getFunctionName());
    }

    @Test
    public void testMaxConstruct() throws Exception {
        ASTMax max = new ASTMax(null);
        assertEquals("MAX", max.getFunctionName());
    }

    @Test
    public void testSumConstruct() throws Exception {
        ASTSum sum = new ASTSum(null);
        assertEquals("SUM", sum.getFunctionName());
    }

}