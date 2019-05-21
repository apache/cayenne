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

package org.apache.cayenne.validation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class ValidationResultTest {

    private ValidationResult res;

    private Object obj1;
    private Object obj2;

    @Before
    public void setUp() throws Exception {
        obj1 = new Object();
        obj2 = new Object();
    }

    @Test
    public void testHasFailures() {
        res = new ValidationResult();
        res.addFailure(new BeanValidationFailure(obj1, "obj1 1", "mes obj1 1"));
        assertTrue(res.hasFailures());
        assertTrue(res.hasFailures(obj1));
        assertFalse(res.hasFailures(obj2));
    }

    @Test
    public void testGetFailures() {
        res = new ValidationResult();
        res.addFailure(new BeanValidationFailure(obj1, "obj1 1", "mes obj1 1"));
        res.addFailure(new BeanValidationFailure(obj1, "obj1 1", "mes obj1 1"));
        res.addFailure(new BeanValidationFailure(obj2, "obj1 1", "mes obj1 1"));

        assertEquals(2, res.getFailures(obj1).size());
        assertEquals(1, res.getFailures(obj2).size());
        assertEquals(3, res.getFailures().size());
    }

    @Test
    public void testEmpty() {
        res = new ValidationResult();
        assertFalse(res.hasFailures());

        assertFalse(res.hasFailures(obj1));
        assertFalse(res.hasFailures(null));
    }
}
