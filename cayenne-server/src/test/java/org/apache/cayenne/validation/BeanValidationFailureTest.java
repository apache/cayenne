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

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BeanValidationFailureTest {

    @Test
    public void testValidateJavaClassName() {

        Object source = new Object();
        String attribute = "xyz";

        assertNull(BeanValidationFailure.validateJavaClassName(source, attribute, "a"));
        assertNull(BeanValidationFailure.validateJavaClassName(source, attribute, "a.b"));
        assertNull(BeanValidationFailure.validateJavaClassName(source, attribute, "XYZ._AA_A"));
        assertNull(BeanValidationFailure.validateJavaClassName(source, attribute, "abc[]"));
        
        assertNotNull(BeanValidationFailure.validateJavaClassName(source, attribute, ""));
        assertNotNull(BeanValidationFailure.validateJavaClassName(source, attribute, ".XYZ._AA_A"));
        assertNotNull(BeanValidationFailure.validateJavaClassName(source, attribute, "a..b"));
        assertNotNull(BeanValidationFailure.validateJavaClassName(source, attribute, "a?c"));
        assertNotNull(BeanValidationFailure.validateJavaClassName(source, attribute, "a."));
        assertNotNull(BeanValidationFailure.validateJavaClassName(source, attribute, "ab]c["));
        assertNotNull(BeanValidationFailure.validateJavaClassName(source, attribute, "abc[[]"));
    }
}
