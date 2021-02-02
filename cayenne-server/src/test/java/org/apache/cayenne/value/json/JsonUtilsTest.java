/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.value.json;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @since 4.2
 */
public class JsonUtilsTest {

    @Test
    public void compare() {
        assertTrue(JsonUtils.compare("[]", "[]"));
        assertTrue(JsonUtils.compare("{}", "{}"));
        assertFalse(JsonUtils.compare("[]", "{}"));

        assertTrue(JsonUtils.compare("123", "123"));
        assertFalse(JsonUtils.compare("123", "124"));

        assertTrue(JsonUtils.compare("null", "null"));
        assertTrue(JsonUtils.compare("true", "true"));
        assertFalse(JsonUtils.compare("true", "false"));

        assertTrue(JsonUtils.compare("\"123\"", "\"123\""));
        assertFalse(JsonUtils.compare("123", "\"123\""));

        assertTrue(JsonUtils.compare("[1,2,3]", "[1, 2, 3]"));
        assertFalse(JsonUtils.compare("[1,2,3]", "[1,2,3,4]"));
        assertFalse(JsonUtils.compare("[1,2,3]", "[1,2]"));
        assertFalse(JsonUtils.compare("[1,2,3]", "[1,2,4]"));

        assertTrue(JsonUtils.compare("{\"abc\":123,\"def\":321}", " {\"def\" :  321 , \n\t\"abc\" :\t123 }"));
        assertFalse(JsonUtils.compare("{\"abc\":123}", " {\"abc\" :  124 }"));
    }

    @Test
    public void normalize() {
        assertEquals("[]", JsonUtils.normalize("[]"));
        assertEquals("{}", JsonUtils.normalize("{}"));
        assertEquals("true", JsonUtils.normalize("true"));
        assertEquals("null", JsonUtils.normalize("null"));
        assertEquals("false", JsonUtils.normalize("false"));
        assertEquals("123", JsonUtils.normalize("123"));
        assertEquals("-10.24e3", JsonUtils.normalize("-10.24e3"));
        assertEquals("\"abc\\\"def\"", JsonUtils.normalize("\"abc\\\"def\""));

        assertEquals("[1, 2.0, -0.3e3, false, null, true]",
                JsonUtils.normalize("[1 ,  2.0  ,-0.3e3, false,\nnull,\ttrue]"));
        assertEquals("{\"abc\": 321, \"def\": true, \"ghi\": \"jkl\"}",
                JsonUtils.normalize("{\"abc\":321,\n\"def\":true,\n\t\"ghi\":\"jkl\"}"));
    }
}