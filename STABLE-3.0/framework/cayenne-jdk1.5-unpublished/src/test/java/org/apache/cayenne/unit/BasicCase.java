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

package org.apache.cayenne.unit;

import java.io.File;
import java.math.BigDecimal;

import junit.framework.TestCase;

/**
 * A test case that requires no DB access.
 * 
 * @since 1.1
 */
public abstract class BasicCase extends TestCase {

    public static void assertEquals(BigDecimal d1, Object d2, double delta) {
        assertNotNull(d2);
        assertTrue("d2: " + d2.getClass().getName(), d2 instanceof BigDecimal);
        BigDecimal d3 = d1.subtract((BigDecimal) d2);
        assertTrue(Math.abs(d3.doubleValue()) < delta);
    }

    /**
     * Returns directory that should be used by all test cases that perform file
     * operations.
     */
    protected File getTestDir() {
        return CayenneResources.getResources().getTestDir();
    }
}
