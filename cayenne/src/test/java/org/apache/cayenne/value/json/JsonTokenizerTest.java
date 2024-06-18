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

/**
 * @since 4.2
 */
public class JsonTokenizerTest {

    @Test
    public void testEmpty() {
        JsonTokenizer tokenizer;
        JsonTokenizer.JsonToken token;

        tokenizer = new JsonTokenizer("");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NONE, token.type);
    }

    @Test
    public void testNull() {
        JsonTokenizer tokenizer;
        JsonTokenizer.JsonToken token;

        tokenizer = new JsonTokenizer("null");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NULL, token.type);

        tokenizer = new JsonTokenizer("NULL");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NULL, token.type);

        tokenizer = new JsonTokenizer("  nUlL  ");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NULL, token.type);
    }

    @Test
    public void testTrue() {
        JsonTokenizer tokenizer;
        JsonTokenizer.JsonToken token;

        tokenizer = new JsonTokenizer("true");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.TRUE, token.type);

        tokenizer = new JsonTokenizer("TRUE");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.TRUE, token.type);

        tokenizer = new JsonTokenizer("  tRuE  ");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.TRUE, token.type);
    }

    @Test
    public void testFalse() {
        JsonTokenizer tokenizer;
        JsonTokenizer.JsonToken token;

        tokenizer = new JsonTokenizer("false");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.FALSE, token.type);

        tokenizer = new JsonTokenizer("FALSE");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.FALSE, token.type);

        tokenizer = new JsonTokenizer("  fAlSe  ");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.FALSE, token.type);
    }

    @Test(expected = MalformedJsonException.class)
    public void testNotNull() {
        JsonTokenizer tokenizer = new JsonTokenizer("  nUlLs  ");
        tokenizer.nextToken();
    }

    @Test(expected = MalformedJsonException.class)
    public void testUnknown() {
        JsonTokenizer tokenizer = new JsonTokenizer("  abc  ");
        tokenizer.nextToken();
    }

    @Test
    public void testNumber() {
        JsonTokenizer tokenizer;
        JsonTokenizer.JsonToken token;

        tokenizer = new JsonTokenizer("0");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("0", token.toString());

        tokenizer = new JsonTokenizer("123");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("123", token.toString());

        tokenizer = new JsonTokenizer("-0");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("-0", token.toString());

        tokenizer = new JsonTokenizer("-123");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("-123", token.toString());

        tokenizer = new JsonTokenizer("0.0123");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("0.0123", token.toString());

        tokenizer = new JsonTokenizer("0.");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("0.", token.toString());

        tokenizer = new JsonTokenizer("123.123");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("123.123", token.toString());

        tokenizer = new JsonTokenizer("-0.0123");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("-0.0123", token.toString());

        tokenizer = new JsonTokenizer("-123.123");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("-123.123", token.toString());

        tokenizer = new JsonTokenizer("123e13");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("123e13", token.toString());

        tokenizer = new JsonTokenizer("-123e13");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("-123e13", token.toString());

        tokenizer = new JsonTokenizer("-123e-13");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("-123e-13", token.toString());

        tokenizer = new JsonTokenizer("-123e+13");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("-123e+13", token.toString());

        tokenizer = new JsonTokenizer("  -0.001e+001  ");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NUMBER, token.type);
        assertEquals("-0.001e+001", token.toString());
    }

    @Test
    public void testString() {
        JsonTokenizer tokenizer;
        JsonTokenizer.JsonToken token;

        tokenizer = new JsonTokenizer("\"123\"");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.STRING, token.type);
        assertEquals("123", token.toString());

        tokenizer = new JsonTokenizer("\"test\\ttest\\ntest\"");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.STRING, token.type);
        assertEquals("test\\ttest\\ntest", token.toString());

        tokenizer = new JsonTokenizer("\"test\\\"test\\\"test\"");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.STRING, token.type);
        assertEquals("test\\\"test\\\"test", token.toString());

        tokenizer = new JsonTokenizer("\"test\\\\test\\\\test\"");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.STRING, token.type);
        assertEquals("test\\\\test\\\\test", token.toString());

        tokenizer = new JsonTokenizer("\"\\uaaaa \\uffff \\uAAAA \\uFFFF \\u0000 \\u9999\"");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.STRING, token.type);
        assertEquals("\\uaaaa \\uffff \\uAAAA \\uFFFF \\u0000 \\u9999", token.toString());
    }

    @Test(expected = MalformedJsonException.class)
    public void testUnclosedString() {
        JsonTokenizer tokenizer = new JsonTokenizer("\"1234567");
        tokenizer.nextToken();
    }

    @Test
    public void testEmptyObject() {
        JsonTokenizer tokenizer = new JsonTokenizer("{}");
        JsonTokenizer.JsonToken token1 = tokenizer.nextToken();
        JsonTokenizer.JsonToken token2 = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.OBJECT_START, token1.type);
        assertEquals(JsonTokenizer.TokenType.OBJECT_END, token2.type);
    }

    @Test(expected = MalformedJsonException.class)
    public void testUnclosedObject() {
        JsonTokenizer tokenizer = new JsonTokenizer("{");
        tokenizer.nextToken();
        tokenizer.nextToken();
    }

    @Test(expected = MalformedJsonException.class)
    public void testUnclosedObject2() {
        JsonTokenizer tokenizer = new JsonTokenizer("{\"abc\"");
        JsonTokenizer.JsonToken token;
        while((token = tokenizer.nextToken()).getType() != JsonTokenizer.TokenType.NONE) {
            // do nothing
        }
    }
    @Test(expected = MalformedJsonException.class)
    public void testUnclosedObject3() {
        JsonTokenizer tokenizer = new JsonTokenizer("{\"abc\":");
        JsonTokenizer.JsonToken token;
        while((token = tokenizer.nextToken()).getType() != JsonTokenizer.TokenType.NONE) {
            // do nothing
        }
    }

    @Test(expected = MalformedJsonException.class)
    public void testUnclosedObject4() {
        JsonTokenizer tokenizer = new JsonTokenizer("{\"abc\":123");
        JsonTokenizer.JsonToken token;
        while((token = tokenizer.nextToken()).getType() != JsonTokenizer.TokenType.NONE) {
            // do nothing
        }
    }

    @Test(expected = MalformedJsonException.class)
    public void testUnclosedObject5() {
        JsonTokenizer tokenizer = new JsonTokenizer("{\"abc\":123,");
        JsonTokenizer.JsonToken token;
        while((token = tokenizer.nextToken()).getType() != JsonTokenizer.TokenType.NONE) {
            // do nothing
        }
    }

    @Test
    public void testObject() {
        JsonTokenizer tokenizer = new JsonTokenizer("  {\"test\": 123,\n\t\"2\": \"abc\"  } ");
        JsonTokenizer.JsonToken token1 = tokenizer.nextToken();
        JsonTokenizer.JsonToken token2 = tokenizer.nextToken();
        JsonTokenizer.JsonToken token3 = tokenizer.nextToken();
        JsonTokenizer.JsonToken token4 = tokenizer.nextToken();
        JsonTokenizer.JsonToken token5 = tokenizer.nextToken();

        assertEquals(JsonTokenizer.TokenType.OBJECT_START, token1.type);
        assertEquals(JsonTokenizer.TokenType.STRING, token2.type);
        assertEquals("test", token2.toString());
        assertEquals(JsonTokenizer.TokenType.NUMBER, token3.type);
        assertEquals("123", token3.toString());
        assertEquals(JsonTokenizer.TokenType.STRING, token4.type);
        assertEquals("2", token4.toString());
        assertEquals(JsonTokenizer.TokenType.STRING, token5.type);
        assertEquals("abc", token5.toString());
    }

    @Test
    public void testTrailingSpaces() {
        JsonTokenizer tokenizer;
        JsonTokenizer.JsonToken token;

        tokenizer = new JsonTokenizer("{\"test\": \"some value\"} \n");
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.OBJECT_START, token.type);
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.STRING, token.type);
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.STRING, token.type);
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.OBJECT_END, token.type);
        token = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.NONE, token.type);
    }

    @Test
    public void testEmptyArray() {
        JsonTokenizer tokenizer = new JsonTokenizer("[]");
        JsonTokenizer.JsonToken token1 = tokenizer.nextToken();
        JsonTokenizer.JsonToken token2 = tokenizer.nextToken();
        assertEquals(JsonTokenizer.TokenType.ARRAY_START, token1.type);
        assertEquals(JsonTokenizer.TokenType.ARRAY_END, token2.type);
    }

    @Test(expected = MalformedJsonException.class)
    public void testUnclosedArray() {
        JsonTokenizer tokenizer = new JsonTokenizer("[");
        JsonTokenizer.JsonToken token;
        while((token = tokenizer.nextToken()).getType() != JsonTokenizer.TokenType.NONE) {
            // do nothing
        }
    }

    @Test(expected = MalformedJsonException.class)
    public void testUnclosedArray2() {
        JsonTokenizer tokenizer = new JsonTokenizer("[1");
        JsonTokenizer.JsonToken token;
        while((token = tokenizer.nextToken()).getType() != JsonTokenizer.TokenType.NONE) {
            // do nothing
        }
    }

    @Test(expected = MalformedJsonException.class)
    public void testUnclosedArray3() {
        JsonTokenizer tokenizer = new JsonTokenizer("[1,");
        JsonTokenizer.JsonToken token;
        while((token = tokenizer.nextToken()).getType() != JsonTokenizer.TokenType.NONE) {
            // do nothing
        }
    }

    @Test
    public void testArray() {
        JsonTokenizer tokenizer = new JsonTokenizer("[0,2]");
        JsonTokenizer.JsonToken token1 = tokenizer.nextToken();
        JsonTokenizer.JsonToken token2 = tokenizer.nextToken();
        JsonTokenizer.JsonToken token3 = tokenizer.nextToken();
        JsonTokenizer.JsonToken token4 = tokenizer.nextToken();

        assertEquals(JsonTokenizer.TokenType.ARRAY_START, token1.type);
        assertEquals(JsonTokenizer.TokenType.NUMBER, token2.type);
        assertEquals("0", token2.toString());
        assertEquals(JsonTokenizer.TokenType.NUMBER, token3.type);
        assertEquals("2", token3.toString());
        assertEquals(JsonTokenizer.TokenType.ARRAY_END, token4.type);
    }

    @Test
    public void testFullJson() {
        JsonTokenizer tokenizer = new JsonTokenizer(JSON);
        JsonTokenizer.JsonToken token;
        while((token = tokenizer.nextToken()).getType() != JsonTokenizer.TokenType.NONE) {
            // do nothing, just trying to read everything
        }

        assertEquals(JsonTokenizer.TokenType.NONE, token.type);
    }


    private static final String JSON = "[\n" +
            "  {\n" +
            "    \"_id\": \"5fc4d1ffde690418e483588a\",\n" +
            "    \"index\": 0,\n" +
            "    \"guid\": \"e7cb7511-5b58-482b-9662-bbabc17c7999\",\n" +
            "    \"isActive\": false,\n" +
            "    \"balance\": \"$2,019.14\",\n" +
            "    \"picture\": \"http://placehold.it/32x32\",\n" +
            "    \"age\": 40,\n" +
            "    \"eyeColor\": \"green\",\n" +
            "    \"name\": \"Briana Jimenez\",\n" +
            "    \"gender\": \"female\",\n" +
            "    \"company\": \"VERBUS\",\n" +
            "    \"email\": \"brianajimenez@verbus.com\",\n" +
            "    \"phone\": \"+1 (911) 471-2705\",\n" +
            "    \"address\": \"178 Ashland Place, Cetronia, Alaska, 7446\",\n" +
            "    \"about\": \"Do ullamco et nulla incididunt dolore culpa voluptate et cupidatat excepteur labore proident. Nisi exercitation tempor duis est reprehenderit exercitation aliquip velit veniam. Fugiat mollit pariatur enim qui excepteur minim officia sunt mollit sint do.\\r\\n\",\n" +
            "    \"registered\": \"2016-12-21T04:56:36 -03:00\",\n" +
            "    \"latitude\": -68.436891,\n" +
            "    \"longitude\": -40.276385,\n" +
            "    \"tags\": [\n" +
            "      \"incididunt\",\n" +
            "      \"voluptate\",\n" +
            "      \"irure\",\n" +
            "      \"eu\",\n" +
            "      \"voluptate\",\n" +
            "      \"do\",\n" +
            "      \"mollit\"\n" +
            "    ],\n" +
            "    \"friends\": [\n" +
            "      {\n" +
            "        \"id\": 0,\n" +
            "        \"name\": \"Hyde Thompson\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 1,\n" +
            "        \"name\": \"Cathleen Mercer\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 2,\n" +
            "        \"name\": \"Emilia Mckenzie\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"greeting\": \"Hello, Briana Jimenez! You have 3 unread messages.\",\n" +
            "    \"favoriteFruit\": \"apple\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"_id\": \"5fc4d1ffad699bf86a1de6f1\",\n" +
            "    \"index\": 1,\n" +
            "    \"guid\": \"7a25ff47-980f-4163-b679-5b82e6bc0693\",\n" +
            "    \"isActive\": true,\n" +
            "    \"balance\": \"$3,888.34\",\n" +
            "    \"picture\": \"http://placehold.it/32x32\",\n" +
            "    \"age\": 20,\n" +
            "    \"eyeColor\": \"green\",\n" +
            "    \"name\": \"Finley Hawkins\",\n" +
            "    \"gender\": \"male\",\n" +
            "    \"company\": \"OMATOM\",\n" +
            "    \"email\": \"finleyhawkins@omatom.com\",\n" +
            "    \"phone\": \"+1 (904) 545-2548\",\n" +
            "    \"address\": \"552 Pilling Street, Roosevelt, American Samoa, 3424\",\n" +
            "    \"about\": \"Aliquip ad cillum minim exercitation officia proident laborum excepteur est laborum irure laboris. Nisi pariatur labore Lorem et ad exercitation. Occaecat ullamco exercitation ut in anim eiusmod sint pariatur dolor Lorem elit incididunt nulla.\\r\\n\",\n" +
            "    \"registered\": \"2019-03-22T09:16:38 -03:00\",\n" +
            "    \"latitude\": 8.588498,\n" +
            "    \"longitude\": 140.490892,\n" +
            "    \"tags\": [\n" +
            "      \"elit\",\n" +
            "      \"ex\",\n" +
            "      \"dolore\",\n" +
            "      \"elit\",\n" +
            "      \"minim\",\n" +
            "      \"excepteur\",\n" +
            "      \"minim\"\n" +
            "    ],\n" +
            "    \"friends\": [\n" +
            "      {\n" +
            "        \"id\": 0,\n" +
            "        \"name\": \"Iris Fletcher\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 1,\n" +
            "        \"name\": \"Moss Whitfield\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 2,\n" +
            "        \"name\": \"Esmeralda Christensen\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"greeting\": \"Hello, Finley Hawkins! You have 5 unread messages.\",\n" +
            "    \"favoriteFruit\": \"banana\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"_id\": \"5fc4d1ffb2a31b910a2159f1\",\n" +
            "    \"index\": 2,\n" +
            "    \"guid\": \"ab53f6a7-25e2-41e9-9744-7fbe01b16f6b\",\n" +
            "    \"isActive\": true,\n" +
            "    \"balance\": \"$1,083.29\",\n" +
            "    \"picture\": \"http://placehold.it/32x32\",\n" +
            "    \"age\": 34,\n" +
            "    \"eyeColor\": \"green\",\n" +
            "    \"name\": \"Wendi Bowen\",\n" +
            "    \"gender\": \"female\",\n" +
            "    \"company\": \"ZILLANET\",\n" +
            "    \"email\": \"wendibowen@zillanet.com\",\n" +
            "    \"phone\": \"+1 (874) 458-3093\",\n" +
            "    \"address\": \"601 Fountain Avenue, Boonville, Maine, 6733\",\n" +
            "    \"about\": \"Eu exercitation est duis occaecat excepteur tempor sint culpa. Dolore ullamco irure pariatur reprehenderit esse qui. Exercitation tempor non duis elit exercitation cupidatat sunt ad adipisicing id. Mollit mollit reprehenderit voluptate sunt dolor nulla id. Tempor officia elit ut officia Lorem in veniam.\\r\\n\",\n" +
            "    \"registered\": \"2017-08-26T10:33:44 -03:00\",\n" +
            "    \"latitude\": -85.532155,\n" +
            "    \"longitude\": -127.824759,\n" +
            "    \"tags\": [\n" +
            "      \"est\",\n" +
            "      \"exercitation\",\n" +
            "      \"reprehenderit\",\n" +
            "      \"aliqua\",\n" +
            "      \"irure\",\n" +
            "      \"in\",\n" +
            "      \"non\"\n" +
            "    ],\n" +
            "    \"friends\": [\n" +
            "      {\n" +
            "        \"id\": 0,\n" +
            "        \"name\": \"Bridget Todd\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 1,\n" +
            "        \"name\": \"Mccall Dennis\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 2,\n" +
            "        \"name\": \"Willis Cohen\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"greeting\": \"Hello, Wendi Bowen! You have 3 unread messages.\",\n" +
            "    \"favoriteFruit\": \"strawberry\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"_id\": \"5fc4d1ffb828546ee5d53781\",\n" +
            "    \"index\": 3,\n" +
            "    \"guid\": \"63d5c010-6b97-4fdd-9733-52824dfdab01\",\n" +
            "    \"isActive\": false,\n" +
            "    \"balance\": \"$3,395.34\",\n" +
            "    \"picture\": \"http://placehold.it/32x32\",\n" +
            "    \"age\": 38,\n" +
            "    \"eyeColor\": \"blue\",\n" +
            "    \"name\": \"Harrell Zamora\",\n" +
            "    \"gender\": \"male\",\n" +
            "    \"company\": \"LUDAK\",\n" +
            "    \"email\": \"harrellzamora@ludak.com\",\n" +
            "    \"phone\": \"+1 (839) 494-3495\",\n" +
            "    \"address\": \"698 Kenilworth Place, Whitmer, Hawaii, 963\",\n" +
            "    \"about\": \"Ex nisi minim adipisicing et amet sint sunt minim deserunt dolore. Incididunt tempor dolore tempor ipsum officia mollit non. Officia aute aute consequat amet mollit sit officia. Nostrud in laborum do duis.\\r\\n\",\n" +
            "    \"registered\": \"2014-09-08T08:52:48 -03:00\",\n" +
            "    \"latitude\": 48.622813,\n" +
            "    \"longitude\": -52.26753,\n" +
            "    \"tags\": [\n" +
            "      \"proident\",\n" +
            "      \"nulla\",\n" +
            "      \"enim\",\n" +
            "      \"nostrud\",\n" +
            "      \"fugiat\",\n" +
            "      \"qui\",\n" +
            "      \"dolore\"\n" +
            "    ],\n" +
            "    \"friends\": [\n" +
            "      {\n" +
            "        \"id\": 0,\n" +
            "        \"name\": \"Burris Mercado\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 1,\n" +
            "        \"name\": \"Bertie Schroeder\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 2,\n" +
            "        \"name\": \"Estrada Hampton\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"greeting\": \"Hello, Harrell Zamora! You have 1 unread messages.\",\n" +
            "    \"favoriteFruit\": \"apple\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"_id\": \"5fc4d1ff8f09e316ea28bbaf\",\n" +
            "    \"index\": 4,\n" +
            "    \"guid\": \"0fc17d32-5a40-4d30-85ac-f4f3f6d146b1\",\n" +
            "    \"isActive\": false,\n" +
            "    \"balance\": \"$3,671.08\",\n" +
            "    \"picture\": \"http://placehold.it/32x32\",\n" +
            "    \"age\": 33,\n" +
            "    \"eyeColor\": \"green\",\n" +
            "    \"name\": \"Bernice Allison\",\n" +
            "    \"gender\": \"female\",\n" +
            "    \"company\": \"INSOURCE\",\n" +
            "    \"email\": \"berniceallison@insource.com\",\n" +
            "    \"phone\": \"+1 (816) 459-3811\",\n" +
            "    \"address\": \"417 Gerry Street, Lavalette, South Dakota, 7943\",\n" +
            "    \"about\": \"Id amet magna dolor occaecat aute dolor ullamco voluptate irure Lorem sunt. Elit aliqua ad Lorem irure aute eu. Incididunt ullamco elit dolore consectetur ipsum anim non incididunt dolor sit in consequat. Eiusmod ea ut fugiat voluptate cupidatat ullamco esse in. Ea voluptate excepteur duis labore excepteur occaecat. Tempor est ad anim eu ea. Eu irure do reprehenderit veniam velit ex eu incididunt officia eiusmod aliquip excepteur nisi.\\r\\n\",\n" +
            "    \"registered\": \"2015-03-27T10:29:54 -03:00\",\n" +
            "    \"latitude\": 24.183295,\n" +
            "    \"longitude\": 82.144996,\n" +
            "    \"tags\": [\n" +
            "      \"enim\",\n" +
            "      \"occaecat\",\n" +
            "      \"laborum\",\n" +
            "      \"velit\",\n" +
            "      \"fugiat\",\n" +
            "      \"anim\",\n" +
            "      \"sint\"\n" +
            "    ],\n" +
            "    \"friends\": [\n" +
            "      {\n" +
            "        \"id\": 0,\n" +
            "        \"name\": \"Christine Horton\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 1,\n" +
            "        \"name\": \"Fowler Good\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 2,\n" +
            "        \"name\": \"Antoinette Cooper\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"greeting\": \"Hello, Bernice Allison! You have 9 unread messages.\",\n" +
            "    \"favoriteFruit\": \"banana\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"_id\": \"5fc4d1ff88489df59c89193c\",\n" +
            "    \"index\": 5,\n" +
            "    \"guid\": \"1b3b06d6-88d2-4b89-ade9-252de03c11b2\",\n" +
            "    \"isActive\": true,\n" +
            "    \"balance\": \"$3,814.72\",\n" +
            "    \"picture\": \"http://placehold.it/32x32\",\n" +
            "    \"age\": 26,\n" +
            "    \"eyeColor\": \"green\",\n" +
            "    \"name\": \"Thornton Alexander\",\n" +
            "    \"gender\": \"male\",\n" +
            "    \"company\": \"ASSITIA\",\n" +
            "    \"email\": \"thorntonalexander@assitia.com\",\n" +
            "    \"phone\": \"+1 (862) 524-2047\",\n" +
            "    \"address\": \"247 Beach Place, Barronett, Guam, 272\",\n" +
            "    \"about\": \"Consequat nulla occaecat aliquip fugiat fugiat ipsum. Veniam incididunt ad est enim sit aliquip exercitation et do sint voluptate. Nostrud culpa velit cillum Lorem labore laborum id voluptate ad et.\\r\\n\",\n" +
            "    \"registered\": \"2018-01-17T05:36:12 -03:00\",\n" +
            "    \"latitude\": -55.650877,\n" +
            "    \"longitude\": 42.279245,\n" +
            "    \"tags\": [\n" +
            "      \"do\",\n" +
            "      \"qui\",\n" +
            "      \"id\",\n" +
            "      \"eiusmod\",\n" +
            "      \"labore\",\n" +
            "      \"consequat\",\n" +
            "      \"ullamco\"\n" +
            "    ],\n" +
            "    \"friends\": [\n" +
            "      {\n" +
            "        \"id\": 0,\n" +
            "        \"name\": \"Helen Copeland\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 1,\n" +
            "        \"name\": \"Hall Joseph\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 2,\n" +
            "        \"name\": \"Ursula Mckee\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"greeting\": \"Hello, Thornton Alexander! You have 7 unread messages.\",\n" +
            "    \"favoriteFruit\": \"strawberry\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"_id\": \"5fc4d1ff9e6d40f38aceb25f\",\n" +
            "    \"index\": 6,\n" +
            "    \"guid\": \"cd160d1b-7d66-4746-866c-2390d236e6db\",\n" +
            "    \"isActive\": false,\n" +
            "    \"balance\": \"$1,385.92\",\n" +
            "    \"picture\": \"http://placehold.it/32x32\",\n" +
            "    \"age\": 38,\n" +
            "    \"eyeColor\": \"blue\",\n" +
            "    \"name\": \"Ester Cooke\",\n" +
            "    \"gender\": \"female\",\n" +
            "    \"company\": \"DAYCORE\",\n" +
            "    \"email\": \"estercooke@daycore.com\",\n" +
            "    \"phone\": \"+1 (814) 450-3865\",\n" +
            "    \"address\": \"922 Coleman Street, Johnsonburg, Georgia, 8863\",\n" +
            "    \"about\": \"Commodo nisi officia deserunt pariatur cillum adipisicing incididunt. Duis pariatur duis consectetur dolor magna aute sunt. Enim occaecat mollit veniam qui voluptate. Ea id fugiat laborum eu aute esse mollit id consequat deserunt. Amet incididunt cupidatat fugiat do Lorem veniam dolor aliquip aliquip magna anim velit. Sint do commodo tempor tempor tempor irure cillum velit consequat sunt ut est.\\r\\n\",\n" +
            "    \"registered\": \"2014-08-23T08:53:19 -03:00\",\n" +
            "    \"latitude\": 26.474005,\n" +
            "    \"longitude\": -122.921901,\n" +
            "    \"tags\": [\n" +
            "      \"esse\",\n" +
            "      \"elit\",\n" +
            "      \"adipisicing\",\n" +
            "      \"sunt\",\n" +
            "      \"incididunt\",\n" +
            "      \"esse\",\n" +
            "      \"quis\"\n" +
            "    ],\n" +
            "    \"friends\": [\n" +
            "      {\n" +
            "        \"id\": 0,\n" +
            "        \"name\": \"Harding Sampson\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 1,\n" +
            "        \"name\": \"Rosario Hansen\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"id\": 2,\n" +
            "        \"name\": \"Larsen Black\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"greeting\": \"Hello, Ester Cooke! You have 8 unread messages.\",\n" +
            "    \"favoriteFruit\": \"banana\"\n" +
            "  }\n" +
            "]";
}