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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonUtilsTest {

    @Nested
    public class CompareTest {

        public static Stream<Object[]> data() {
            return Stream.of(
                    new Object[]{"[]", "[]", true},
                    new Object[]{"{}", "{}", true},
                    new Object[]{"[]", "{}", false},

                    new Object[]{"123", "123", true},
                    new Object[]{"123", "124", false},

                    new Object[]{"null", "null", true},
                    new Object[]{"true", "true", true},
                    new Object[]{"true", "false", false},

                    new Object[]{"\"123\"", "\"123\"", true},
                    new Object[]{"123", "\"123\"", false},

                    new Object[]{"[1,2,3]", "[1, 2, 3]", true},
                    new Object[]{"[1,2,3]", "[1,2,3,4]", false},
                    new Object[]{"[1,2,3]", "[1,2]", false},
                    new Object[]{"[1,2,3]", "[1,2,4]", false},

                    new Object[]{"{\"abc\":123,\"def\":321}", " {\"def\" :  321 , \n\t\"abc\" :\t123 }", true},
                    new Object[]{"{\"abc\":123}", " {\"abc\" :  124 }", false}
            );
        }

        @ParameterizedTest(name = " {0} eq {1} ")
        @MethodSource("data")
        public void compare(String jsonStringA, String jsonStringB, boolean areEquals) {
            assertEquals(areEquals, JsonUtils.compare(jsonStringA, jsonStringB));
        }
    }

    @Nested
    public class NormalizeTest {

        public static Stream<Object[]> data() {
            return Stream.of(
                    new Object[]{"[]", "[]", null},
                    new Object[]{"{}", "{}", null},
                    new Object[]{"true", "true", null},
                    new Object[]{"null", "null", null},
                    new Object[]{"false", "false", null},
                    new Object[]{"123", "123", null},
                    new Object[]{"-10.24e3", "-10.24e3", null},
                    new Object[]{"\"abc\\\"def\"", "\"abc\\\"def\"", null},

                    new Object[]{
                            "[1, 2.0, -0.3e3, false, null, true]",
                            "[1 ,  2.0  ,-0.3e3, false,\nnull,\ttrue]",
                            null
                    },
                    new Object[]{
                            "{\"abc\": 321, \"def\": true, \"ghi\": \"jkl\"}",
                            "{\"abc\":321,\n\"def\":true,\n\t\"ghi\":\"jkl\"}",
                            null
                    },
                    new Object[]{
                            "{\"tags\": [\"ad\", \"irure\", \"anim\"], \"age\": 20}",
                            "{\"tags\": [\"ad\",\n\"irure\", \"anim\"],\n\"age\": 20}",
                            null
                    },
                    new Object[]{
                            "{\"objects\": [{\"id\": 1}, {\"id\": 2}]}",
                            "{\"objects\":\n[\n{\n\"id\": 1\n},\n{\n\"id\": 2\n}\n]}",
                            null
                    },
                    new Object[]{
                            "["
                            + "{"
                            + "\"_id\": \"63f218c8ae709e45c7b32c5f\", "
                            + "\"index\": 0, "
                            + "\"guid\": \"b3c2b147-9031-40ee-b2a9-fabbd7f5da81\", "
                            + "\"isActive\": false, "
                            + "\"balance\": \"$2,836.15\", "
                            + "\"picture\": \"http://placehold.it/32x32\", "
                            + "\"age\": 21, "
                            + "\"eyeColor\": \"green\", "
                            + "\"name\": \"Ratliff Martin\", "
                            + "\"gender\": \"male\", "
                            + "\"company\": \"PLASMOSIS\", "
                            + "\"email\": \"ratliffmartin@plasmosis.com\", "
                            + "\"phone\": \"+1 (897) 415-2945\", "
                            + "\"address\": \"241 Foster Avenue, Outlook, New Jersey, 1479\", "
                            + "\"about\": \"pariatur irure qui consequat excepteur laborum nulla\", "
                            + "\"registered\": \"2018-05-18T08:04:15 -03:00\", "
                            + "\"latitude\": -51.195497, "
                            + "\"longitude\": 163.317807, "
                            + "\"tags\": ["
                            + "\"exercitation\", "
                            + "\"nulla\", "
                            + "\"labore\", "
                            + "\"enim\", "
                            + "\"ad\", "
                            + "\"anim\", "
                            + "\"excepteur\""
                            + "], "
                            + "\"friends\": ["
                            + "{"
                            + "\"id\": 0, "
                            + "\"name\": \"Rowena Benson\""
                            + "}, "
                            + "{"
                            + "\"id\": 1, "
                            + "\"name\": \"Bird Mclaughlin\""
                            + "}, "
                            + "{"
                            + "\"id\": 2, "
                            + "\"name\": \"Mabel James\""
                            + "}"
                            + "], "
                            + "\"greeting\": \"Hello, Ratliff Martin! You have 2 unread messages.\", "
                            + "\"favoriteFruit\": \"strawberry\""
                            + "}"
                            + "]",

                            "[\n"
                            + "  {\n"
                            + "    \"_id\": \"63f218c8ae709e45c7b32c5f\",\n"
                            + "    \"index\": 0,\n"
                            + "    \"guid\": \"b3c2b147-9031-40ee-b2a9-fabbd7f5da81\",\n"
                            + "    \"isActive\": false,\n"
                            + "    \"balance\": \"$2,836.15\",\n"
                            + "    \"picture\": \"http://placehold.it/32x32\",\n"
                            + "    \"age\": 21,\n"
                            + "    \"eyeColor\": \"green\",\n"
                            + "    \"name\": \"Ratliff Martin\",\n"
                            + "    \"gender\": \"male\",\n"
                            + "    \"company\": \"PLASMOSIS\",\n"
                            + "    \"email\": \"ratliffmartin@plasmosis.com\",\n"
                            + "    \"phone\": \"+1 (897) 415-2945\",\n"
                            + "    \"address\": \"241 Foster Avenue, Outlook, New Jersey, 1479\",\n"
                            + "    \"about\": \"pariatur irure qui consequat excepteur laborum nulla\",\n"
                            + "    \"registered\": \"2018-05-18T08:04:15 -03:00\",\n"
                            + "    \"latitude\": -51.195497,\n"
                            + "    \"longitude\": 163.317807,\n"
                            + "    \"tags\": [\n"
                            + "      \"exercitation\",\n"
                            + "      \"nulla\",\n"
                            + "      \"labore\",\n"
                            + "      \"enim\",\n"
                            + "      \"ad\",\n"
                            + "      \"anim\",\n"
                            + "      \"excepteur\"\n"
                            + "    ],\n"
                            + "    \"friends\": [\n"
                            + "      {\n"
                            + "        \"id\": 0,\n"
                            + "        \"name\": \"Rowena Benson\"\n"
                            + "      },\n"
                            + "      {\n"
                            + "        \"id\": 1,\n"
                            + "        \"name\": \"Bird Mclaughlin\"\n"
                            + "      },\n"
                            + "      {\n"
                            + "        \"id\": 2,\n"
                            + "        \"name\": \"Mabel James\"\n"
                            + "      }\n"
                            + "    ],\n"
                            + "    \"greeting\": \"Hello, Ratliff Martin! You have 2 unread messages.\",\n"
                            + "    \"favoriteFruit\": \"strawberry\"\n"
                            + "  }\n"
                            + "]",

                            null
                    },

                    new Object[]{"", "", MalformedJsonException.class}
            );
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("data")
        public void normalize(String expected, String jsonString, Class<? extends Throwable> throwable) {
            if (throwable == null) {
                assertEquals(expected, JsonUtils.normalize(jsonString));
            } else {
                assertThrows(throwable, () -> JsonUtils.normalize(jsonString));
            }
        }
    }
}