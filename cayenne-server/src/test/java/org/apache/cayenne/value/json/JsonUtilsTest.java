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
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * @since 4.2
 */
@RunWith(Enclosed.class)
public class JsonUtilsTest {

    @RunWith(Parameterized.class)
    public static class CompareTest {

        @Parameterized.Parameters(name = " {0} eq {1} ")
        public static Object[][] data() {
            return new Object[][]{
                    {"[]", "[]", true},
                    {"{}", "{}", true},
                    {"[]", "{}", false},

                    {"123", "123", true},
                    {"123", "124", false},

                    {"null", "null", true},
                    {"true", "true", true},
                    {"true", "false", false},

                    {"\"123\"", "\"123\"", true},
                    {"123", "\"123\"", false},

                    {"[1,2,3]", "[1, 2, 3]", true},
                    {"[1,2,3]", "[1,2,3,4]", false},
                    {"[1,2,3]", "[1,2]", false},
                    {"[1,2,3]", "[1,2,4]", false},

                    {"{\"abc\":123,\"def\":321}", " {\"def\" :  321 , \n\t\"abc\" :\t123 }", true},
                    {"{\"abc\":123}", " {\"abc\" :  124 }", false}
            };
        }

        @Parameterized.Parameter
        public String jsonStringA;
        @Parameterized.Parameter(1)
        public String jsonStringB;
        @Parameterized.Parameter(2)
        public boolean areEquals;

        @Test
        public void compare() {
            assertEquals(areEquals, JsonUtils.compare(jsonStringA, jsonStringB));
        }
    }

    @RunWith(Parameterized.class)
    public static class NormalizeTest {

        @Parameterized.Parameters(name = " {0} ")
        public static Object[][] data() {
            return new Object[][]{
                    {"[]", "[]", null},
                    {"{}", "{}", null},
                    {"true", "true", null},
                    {"null", "null", null},
                    {"false", "false", null},
                    {"123", "123", null},
                    {"-10.24e3", "-10.24e3", null},
                    {"\"abc\\\"def\"", "\"abc\\\"def\"", null},

                    {
                            "[1, 2.0, -0.3e3, false, null, true]",
                            "[1 ,  2.0  ,-0.3e3, false,\nnull,\ttrue]",
                            null
                    },
                    {
                            "{\"abc\": 321, \"def\": true, \"ghi\": \"jkl\"}",
                            "{\"abc\":321,\n\"def\":true,\n\t\"ghi\":\"jkl\"}",
                            null
                    },
                    {
                            "{\"tags\": [\"ad\", \"irure\", \"anim\"], \"age\": 20}",
                            "{\"tags\": [\"ad\",\n\"irure\", \"anim\"],\n\"age\": 20}",
                            null
                    },

                    {"", "", MalformedJsonException.class},
            };
        }

        @Parameterized.Parameter
        public String expected;
        @Parameterized.Parameter(1)
        public String jsonString;
        @Parameterized.Parameter(2)
        public Class<? extends Throwable> throwable;

        @Test
        public void normalize() {
            if (throwable == null) {
                assertEquals(expected, JsonUtils.normalize(jsonString));
            } else {
                assertThrows(throwable, () -> JsonUtils.normalize(jsonString));
            }
        }
    }
}