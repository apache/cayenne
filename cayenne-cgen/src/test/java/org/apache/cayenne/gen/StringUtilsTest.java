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

package org.apache.cayenne.gen;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringUtilsTest {

    protected StringUtils stringUtils;

    @BeforeEach
    public void setUp() throws Exception {
        stringUtils = new StringUtils();
    }

    @AfterEach
    public void tearDown() throws Exception {
        stringUtils = null;
    }

    @Test
    public void pluralize() throws Exception {
        assertEquals("Words", stringUtils.pluralize("Word"));
        assertEquals("Statuses", stringUtils.pluralize("Status"));
        assertEquals("Indexes", stringUtils.pluralize("Index"));
        assertEquals("Factories", stringUtils.pluralize("Factory"));
        assertEquals("", stringUtils.pluralize(""));
        assertEquals(null, stringUtils.pluralize(null));
    }


    @Test
    public void capitalizedAsConstant1() throws Exception {
        String expected = "LAST_NAME";
        assertEquals(expected, stringUtils.capitalizedAsConstant("LastName"));
    }

    @Test
    public void capitalizedAsConstant2() throws Exception {
        String expected = "A_CLASS";
        assertEquals(expected, stringUtils.capitalizedAsConstant("aClass"));
    }

    @Test
    public void capitalizedAsConstant3() throws Exception {
        String expected = "VAR_A";
        assertEquals(expected, stringUtils.capitalizedAsConstant("varA"));
    }

    @Test
    public void capitalizedAsConstant4() throws Exception {
        String expected = "LAST_NAME";
        assertEquals(expected, stringUtils.capitalizedAsConstant("LAST_NAME"));
    }

    @Test
    public void capitalizedAsConstant5() throws Exception {
        String expected = "ABC_A";
        assertEquals(expected, stringUtils.capitalizedAsConstant("abc_A"));
    }

    @Test
    public void capitalizedAsConstant6() throws Exception {
        String expected = "A123";
        assertEquals(expected, stringUtils.capitalizedAsConstant("a123"));
    }

    @Test
    public void capitalizedAsConstant7() throws Exception {
        String expected = "AB_CDEF";
        assertEquals(expected, stringUtils.capitalizedAsConstant("abCDEF"));
    }

    @Test
    public void capitalizedAsConstant8() throws Exception {
        String expected = "AB_CE";
        assertEquals(expected, stringUtils.capitalizedAsConstant("abCe"));
    }

    @Test
    public void stripGeneric() throws Exception {
        assertEquals("List", stringUtils.stripGeneric("List"));
        assertEquals("List", stringUtils.stripGeneric("List<Integer>"));
        assertEquals("List", stringUtils.stripGeneric("List<List<Map<Integer,List<String>>>>"));
        assertEquals("List123", stringUtils.stripGeneric("List<List<Map<Integer,List<String>>>>123"));
        assertEquals("List<Integer", stringUtils.stripGeneric("List<Integer"));
    }

    /**
     * Test pattern expansion.
     */
    @Test
    public void replaceWildcardInStringWithString() throws Exception {
        assertEquals(null, stringUtils.replaceWildcardInStringWithString("*", null, "Entity"));
        assertEquals("*.java", stringUtils.replaceWildcardInStringWithString(null, "*.java", "Entity"));
        assertEquals("Entity.java", stringUtils.replaceWildcardInStringWithString("*", "*.java", "Entity"));
        assertEquals("java.Entity", stringUtils.replaceWildcardInStringWithString("*", "java.*", "Entity"));
        assertEquals("Entity.Entity", stringUtils.replaceWildcardInStringWithString("*", "*.*", "Entity"));
        assertEquals("EntityEntity", stringUtils.replaceWildcardInStringWithString("*", "**", "Entity"));
        assertEquals("EditEntityReport.vm", stringUtils.replaceWildcardInStringWithString("*", "Edit*Report.vm", "Entity"));
        assertEquals("Entity", stringUtils.replaceWildcardInStringWithString("*", "*", "Entity"));
    }
}
