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
public class ASTScalarTest {

    @Test
    public void testEquals() throws Exception {
        ASTScalar strScalar1 = new ASTScalar("test");
        ASTScalar strScalar2 = new ASTScalar("test");
        assertEquals(strScalar1, strScalar2);

        ASTScalar nullScalar1 = new ASTScalar(null);
        ASTScalar nullScalar2 = new ASTScalar(null);
        assertEquals(nullScalar1, nullScalar2);

        assertNotEquals(strScalar1, nullScalar1);
    }

    @Test
    public void testHashCode() throws Exception {
        ASTScalar strScalar1 = new ASTScalar("test");
        ASTScalar strScalar2 = new ASTScalar("test");
        assertEquals(strScalar1.hashCode(), strScalar2.hashCode());

        ASTScalar nullScalar1 = new ASTScalar(null);
        ASTScalar nullScalar2 = new ASTScalar(null);
        assertEquals(nullScalar1.hashCode(), nullScalar2.hashCode());

        assertNotEquals(strScalar1.hashCode(), nullScalar1.hashCode());
    }

}