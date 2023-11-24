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

package org.apache.cayenne.value.json;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.testdo.json.JsonOther;
import org.apache.cayenne.testdo.json.JsonVarchar;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.value.Json;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.JSON_PROJECT)
public class JsonTypeIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private UnitDbAdapter unitDbAdapter;

    @Test
    public void testJsonBasic() {
        testJson("{\"id\": 1, \"property\": \"value\"}");
    }

    @Test
    public void testJsonOver4k() {
        testJson("[\n" +
                         "  {\n" +
                         "    \"_id\": \"63f151cd2df4280fe4258ef3\",\n" +
                         "    \"index\": 0,\n" +
                         "    \"guid\": \"2e05e933-7468-4573-aa76-e29a80810c57\",\n" +
                         "    \"isActive\": true,\n" +
                         "    \"balance\": \"$3,282.00\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 20,\n" +
                         "    \"eyeColor\": \"blue\",\n" +
                         "    \"name\": \"Mathews Rutledge\",\n" +
                         "    \"gender\": \"male\",\n" +
                         "    \"company\": \"FREAKIN\",\n" +
                         "    \"email\": \"mathewsrutledge@freakin.com\",\n" +
                         "    \"phone\": \"+1 (873) 411-3555\",\n" +
                         "    \"address\": \"896 Lee Avenue, Fairhaven, New Jersey, 1031\",\n" +
                         "    \"about\": \"nostrud cupidatat proident eu anim sint eu\",\n" +
                         "    \"registered\": \"2021-10-10T11:59:50 -03:00\",\n" +
                         "    \"latitude\": -18.854904,\n" +
                         "    \"longitude\": -138.392089,\n" +
                         "    \"tags\": [\n" +
                         "      \"ad\",\n" +
                         "      \"irure\",\n" +
                         "      \"anim\",\n" +
                         "      \"aliqua\",\n" +
                         "      \"do\",\n" +
                         "      \"eu\",\n" +
                         "      \"excepteur\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Guzman K\\u0450mp\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Delgado Beasley\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Noelle Owen\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Mathews Rutledge! You have 4 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"banana\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"_id\": \"63f151cd00a687261825c3d3\",\n" +
                         "    \"index\": 1,\n" +
                         "    \"guid\": \"7c6b8859-5a81-4654-980f-e31ae7c3a04d\",\n" +
                         "    \"isActive\": false,\n" +
                         "    \"balance\": \"$2,805.03\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 32,\n" +
                         "    \"eyeColor\": \"brown\",\n" +
                         "    \"name\": \"Nieves Gallegos\",\n" +
                         "    \"gender\": \"male\",\n" +
                         "    \"company\": \"SPACEWAX\",\n" +
                         "    \"email\": \"nievesgallegos@spacewax.com\",\n" +
                         "    \"phone\": \"+1 (839) 414-3310\",\n" +
                         "    \"address\": \"446 Mill Avenue, Nicholson, Minnesota, 8852\",\n" +
                         "    \"about\": \"sunt magna quis officia exercitation laboris officia\",\n" +
                         "    \"registered\": \"2020-01-17T08:49:38 -03:00\",\n" +
                         "    \"latitude\": 11.681513,\n" +
                         "    \"longitude\": -129.960233,\n" +
                         "    \"tags\": [\n" +
                         "      \"labore\",\n" +
                         "      \"fugiat\",\n" +
                         "      \"cillum\",\n" +
                         "      \"incididunt\",\n" +
                         "      \"nostrud\",\n" +
                         "      \"non\",\n" +
                         "      \"et\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Perry Hunter\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Angelina Cooper\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Kendra Bonner\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Nieves Gallegos! You have 2 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"strawberry\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"_id\": \"63f151cd9d0e1b90e4150a10\",\n" +
                         "    \"index\": 2,\n" +
                         "    \"guid\": \"dfea2156-c940-43d3-a500-19b5b32719b3\",\n" +
                         "    \"isActive\": true,\n" +
                         "    \"balance\": \"$2,684.11\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 38,\n" +
                         "    \"eyeColor\": \"brown\",\n" +
                         "    \"name\": \"Virginia Watts\",\n" +
                         "    \"gender\": \"female\",\n" +
                         "    \"company\": \"OPTIQUE\",\n" +
                         "    \"email\": \"virginiawatts@optique.com\",\n" +
                         "    \"phone\": \"+1 (864) 547-3451\",\n" +
                         "    \"address\": \"518 Crooke Avenue, Advance, Arkansas, 7719\",\n" +
                         "    \"about\": \"ullamco exercitation excepteur mollit ad labore do\",\n" +
                         "    \"registered\": \"2018-11-30T07:01:17 -03:00\",\n" +
                         "    \"latitude\": -77.530698,\n" +
                         "    \"longitude\": 174.424542,\n" +
                         "    \"tags\": [\n" +
                         "      \"ex\",\n" +
                         "      \"ad\",\n" +
                         "      \"exercitation\",\n" +
                         "      \"dolor\",\n" +
                         "      \"aute\",\n" +
                         "      \"ex\",\n" +
                         "      \"Lorem\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Molly Blake\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Pearlie Dodson\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Montoya Watkins\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Virginia Watts! You have 4 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"apple\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"_id\": \"63f151cde376bf473f79cc97\",\n" +
                         "    \"index\": 3,\n" +
                         "    \"guid\": \"4f77bba3-531c-4450-b441-589bd19f2a57\",\n" +
                         "    \"isActive\": false,\n" +
                         "    \"balance\": \"$3,381.01\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 22,\n" +
                         "    \"eyeColor\": \"green\",\n" +
                         "    \"name\": \"Walter Patrick\",\n" +
                         "    \"gender\": \"male\",\n" +
                         "    \"company\": \"PEARLESEX\",\n" +
                         "    \"email\": \"walterpatrick@pearlesex.com\",\n" +
                         "    \"phone\": \"+1 (954) 448-3420\",\n" +
                         "    \"address\": \"387 Church Avenue, Geyserville, New Hampshire, 4849\",\n" +
                         "    \"about\": \"cupidatat officia qui dolor veniam eu minim\",\n" +
                         "    \"registered\": \"2016-12-04T05:12:01 -03:00\",\n" +
                         "    \"latitude\": 83.816972,\n" +
                         "    \"longitude\": -30.59895,\n" +
                         "    \"tags\": [\n" +
                         "      \"eu\",\n" +
                         "      \"Lorem\",\n" +
                         "      \"ad\",\n" +
                         "      \"ea\",\n" +
                         "      \"adipisicing\",\n" +
                         "      \"velit\",\n" +
                         "      \"ex\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Pate Sweet\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Stein Burns\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Candy Swanson\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Walter Patrick! You have 5 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"banana\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"_id\": \"63f151cd64d59419599bd15f\",\n" +
                         "    \"index\": 4,\n" +
                         "    \"guid\": \"2da48623-9b34-47ec-962e-400d45c8620a\",\n" +
                         "    \"isActive\": false,\n" +
                         "    \"balance\": \"$2,891.52\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 37,\n" +
                         "    \"eyeColor\": \"brown\",\n" +
                         "    \"name\": \"Ella Carey\",\n" +
                         "    \"gender\": \"female\",\n" +
                         "    \"company\": \"PORTICO\",\n" +
                         "    \"email\": \"ellacarey@portico.com\",\n" +
                         "    \"phone\": \"+1 (906) 400-3097\",\n" +
                         "    \"address\": \"381 Bowne Street, Rose, Palau, 7582\",\n" +
                         "    \"about\": \"voluptate pariatur magna occaecat elit magna excepteur\",\n" +
                         "    \"registered\": \"2015-08-07T10:22:10 -03:00\",\n" +
                         "    \"latitude\": 80.548898,\n" +
                         "    \"longitude\": 67.575077,\n" +
                         "    \"tags\": [\n" +
                         "      \"duis\",\n" +
                         "      \"occaecat\",\n" +
                         "      \"excepteur\",\n" +
                         "      \"tempor\",\n" +
                         "      \"excepteur\",\n" +
                         "      \"Lorem\",\n" +
                         "      \"proident\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Melendez Martin\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Haley Colon\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Emilia Schmidt\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Ella Carey! You have 8 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"strawberry\"\n" +
                         "  }\n" +
                         "]");
    }

    @Test
    public void testJsonOver16k() {
        testJson("[\n" +
                         "  {\n" +
                         "    \"_id\": \"63f153352d42c0451f6f18a1\",\n" +
                         "    \"index\": 0,\n" +
                         "    \"guid\": \"4db9bc53-e652-47bd-8162-aad6b4280eb9\",\n" +
                         "    \"isActive\": true,\n" +
                         "    \"balance\": \"$3,604.21\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 26,\n" +
                         "    \"eyeColor\": \"brown\",\n" +
                         "    \"name\": \"Landry Malone\",\n" +
                         "    \"gender\": \"male\",\n" +
                         "    \"company\": \"FROLIX\",\n" +
                         "    \"email\": \"landrymalone@frolix.com\",\n" +
                         "    \"phone\": \"+1 (898) 454-3351\",\n" +
                         "    \"address\": \"881 Strauss Street, Troy, Utah, 4374\",\n" +
                         "    \"about\": \"ea et consequat fugiat est laboris sint\",\n" +
                         "    \"registered\": \"2016-06-14T05:08:16 -03:00\",\n" +
                         "    \"latitude\": 49.131275,\n" +
                         "    \"longitude\": -171.114829,\n" +
                         "    \"tags\": [\n" +
                         "      \"dolore\",\n" +
                         "      \"elit\",\n" +
                         "      \"excepteur\",\n" +
                         "      \"in\",\n" +
                         "      \"sint\",\n" +
                         "      \"exercitation\",\n" +
                         "      \"cillum\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Palmer Pittman\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Clarice Wolfe\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Clements Battle\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Landry Malone! You have 9 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"banana\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"_id\": \"63f153355ae6023dbfb7c2f8\",\n" +
                         "    \"index\": 1,\n" +
                         "    \"guid\": \"b4fdfc88-e367-42ba-ae1d-2f9f6a538986\",\n" +
                         "    \"isActive\": true,\n" +
                         "    \"balance\": \"$1,909.36\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 24,\n" +
                         "    \"eyeColor\": \"brown\",\n" +
                         "    \"name\": \"Nicholson Dodson\",\n" +
                         "    \"gender\": \"male\",\n" +
                         "    \"company\": \"BUNGA\",\n" +
                         "    \"email\": \"nicholsondodson@bunga.com\",\n" +
                         "    \"phone\": \"+1 (857) 550-2984\",\n" +
                         "    \"address\": \"425 Thornton Street, Roosevelt, South Carolina, 4979\",\n" +
                         "    \"about\": \"voluptate consequat consequat pariatur reprehenderit et exercitation\",\n" +
                         "    \"registered\": \"2021-08-16T06:28:03 -03:00\",\n" +
                         "    \"latitude\": -12.667348,\n" +
                         "    \"longitude\": 84.401994,\n" +
                         "    \"tags\": [\n" +
                         "      \"est\",\n" +
                         "      \"in\",\n" +
                         "      \"mollit\",\n" +
                         "      \"id\",\n" +
                         "      \"proident\",\n" +
                         "      \"incididunt\",\n" +
                         "      \"qui\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Nelson Wolf\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Corina Fry\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Carlene Bean\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Nicholson Dodson! You have 8 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"strawberry\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"_id\": \"63f15335f03974b701b22feb\",\n" +
                         "    \"index\": 2,\n" +
                         "    \"guid\": \"bf249929-ce37-4702-9671-1935278a2ff8\",\n" +
                         "    \"isActive\": false,\n" +
                         "    \"balance\": \"$2,455.81\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 38,\n" +
                         "    \"eyeColor\": \"blue\",\n" +
                         "    \"name\": \"Strickland Hodges\",\n" +
                         "    \"gender\": \"male\",\n" +
                         "    \"company\": \"MACRONAUT\",\n" +
                         "    \"email\": \"stricklandhodges@macronaut.com\",\n" +
                         "    \"phone\": \"+1 (806) 573-3642\",\n" +
                         "    \"address\": \"929 Sutton Street, Caroleen, Delaware, 2273\",\n" +
                         "    \"about\": \"culpa eiusmod commodo et officia aute exercitation\",\n" +
                         "    \"registered\": \"2017-10-26T06:25:14 -03:00\",\n" +
                         "    \"latitude\": 24.973892,\n" +
                         "    \"longitude\": 50.218781,\n" +
                         "    \"tags\": [\n" +
                         "      \"sit\",\n" +
                         "      \"exercitation\",\n" +
                         "      \"Lorem\",\n" +
                         "      \"qui\",\n" +
                         "      \"reprehenderit\",\n" +
                         "      \"incididunt\",\n" +
                         "      \"cupidatat\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Trevino Howard\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Carol Frye\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Kara Parks\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Strickland Hodges! You have 7 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"apple\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"_id\": \"63f15335c8e74b04cc2cf3de\",\n" +
                         "    \"index\": 3,\n" +
                         "    \"guid\": \"450adca9-503b-41a2-b0a5-3ae7c9d68f81\",\n" +
                         "    \"isActive\": true,\n" +
                         "    \"balance\": \"$3,895.80\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 30,\n" +
                         "    \"eyeColor\": \"brown\",\n" +
                         "    \"name\": \"Judy Scott\",\n" +
                         "    \"gender\": \"female\",\n" +
                         "    \"company\": \"ZENSOR\",\n" +
                         "    \"email\": \"judyscott@zensor.com\",\n" +
                         "    \"phone\": \"+1 (961) 568-2876\",\n" +
                         "    \"address\": \"359 Williamsburg Street, Venice, Ohio, 6487\",\n" +
                         "    \"about\": \"in nulla adipisicing non culpa quis do\",\n" +
                         "    \"registered\": \"2014-01-08T07:06:47 -04:00\",\n" +
                         "    \"latitude\": -66.571697,\n" +
                         "    \"longitude\": 92.502775,\n" +
                         "    \"tags\": [\n" +
                         "      \"labore\",\n" +
                         "      \"minim\",\n" +
                         "      \"adipisicing\",\n" +
                         "      \"nostrud\",\n" +
                         "      \"elit\",\n" +
                         "      \"deserunt\",\n" +
                         "      \"cupidatat\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Lorna Hines\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Farrell Ryan\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Georgette Elliott\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Judy Scott! You have 8 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"strawberry\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"_id\": \"63f1533590ac45e8b03b433e\",\n" +
                         "    \"index\": 4,\n" +
                         "    \"guid\": \"88b4e813-d6dc-4902-b4f4-c28fca5d8b32\",\n" +
                         "    \"isActive\": true,\n" +
                         "    \"balance\": \"$2,997.00\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 38,\n" +
                         "    \"eyeColor\": \"blue\",\n" +
                         "    \"name\": \"Briggs Shields\",\n" +
                         "    \"gender\": \"male\",\n" +
                         "    \"company\": \"XLEEN\",\n" +
                         "    \"email\": \"briggsshields@xleen.com\",\n" +
                         "    \"phone\": \"+1 (987) 435-3420\",\n" +
                         "    \"address\": \"807 Vine Street, Callaghan, New Mexico, 7939\",\n" +
                         "    \"about\": \"consectetur cupidatat anim pariatur adipisicing adipisicing irure\",\n" +
                         "    \"registered\": \"2022-02-28T09:49:04 -03:00\",\n" +
                         "    \"latitude\": 5.401627,\n" +
                         "    \"longitude\": 64.076763,\n" +
                         "    \"tags\": [\n" +
                         "      \"irure\",\n" +
                         "      \"sint\",\n" +
                         "      \"aliqua\",\n" +
                         "      \"officia\",\n" +
                         "      \"consectetur\",\n" +
                         "      \"qui\",\n" +
                         "      \"eiusmod\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Roberts Weeks\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Justice Bullock\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Simone Jacobson\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Briggs Shields! You have 8 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"strawberry\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"_id\": \"63f153359ee33fe36670766f\",\n" +
                         "    \"index\": 5,\n" +
                         "    \"guid\": \"f87bfc03-46d5-4e8f-9f55-dafa25e124cb\",\n" +
                         "    \"isActive\": false,\n" +
                         "    \"balance\": \"$2,933.39\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 27,\n" +
                         "    \"eyeColor\": \"green\",\n" +
                         "    \"name\": \"Kendra Peterson\",\n" +
                         "    \"gender\": \"female\",\n" +
                         "    \"company\": \"FUELWORKS\",\n" +
                         "    \"email\": \"kendrapeterson@fuelworks.com\",\n" +
                         "    \"phone\": \"+1 (951) 518-3222\",\n" +
                         "    \"address\": \"155 Dewey Place, Westwood, Georgia, 1647\",\n" +
                         "    \"about\": \"occaecat in ipsum non cillum proident officia\",\n" +
                         "    \"registered\": \"2017-04-26T06:50:40 -03:00\",\n" +
                         "    \"latitude\": -14.371127,\n" +
                         "    \"longitude\": -0.400474,\n" +
                         "    \"tags\": [\n" +
                         "      \"ad\",\n" +
                         "      \"ipsum\",\n" +
                         "      \"eiusmod\",\n" +
                         "      \"cillum\",\n" +
                         "      \"et\",\n" +
                         "      \"et\",\n" +
                         "      \"ipsum\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Orr Stone\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Mavis Mccullough\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Lea Whitfield\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Kendra Peterson! You have 6 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"banana\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"_id\": \"63f15335cb2b3032b85383bb\",\n" +
                         "    \"index\": 6,\n" +
                         "    \"guid\": \"f1df8765-ebcb-40b9-957e-a04d0b16f2c6\",\n" +
                         "    \"isActive\": true,\n" +
                         "    \"balance\": \"$2,219.05\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 38,\n" +
                         "    \"eyeColor\": \"brown\",\n" +
                         "    \"name\": \"Carissa Hogan\",\n" +
                         "    \"gender\": \"female\",\n" +
                         "    \"company\": \"SOPRANO\",\n" +
                         "    \"email\": \"carissahogan@soprano.com\",\n" +
                         "    \"phone\": \"+1 (919) 432-2299\",\n" +
                         "    \"address\": \"555 Myrtle Avenue, Yonah, Arkansas, 4416\",\n" +
                         "    \"about\": \"commodo exercitation ex adipisicing reprehenderit amet ut\",\n" +
                         "    \"registered\": \"2019-04-02T12:22:13 -03:00\",\n" +
                         "    \"latitude\": 26.396021,\n" +
                         "    \"longitude\": -51.909653,\n" +
                         "    \"tags\": [\n" +
                         "      \"deserunt\",\n" +
                         "      \"veniam\",\n" +
                         "      \"ut\",\n" +
                         "      \"velit\",\n" +
                         "      \"elit\",\n" +
                         "      \"proident\",\n" +
                         "      \"reprehenderit\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Mcknight Walsh\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Dalton Mclean\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Crystal Poole\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Carissa Hogan! You have 7 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"banana\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"_id\": \"63f15335754d0e8d9275a344\",\n" +
                         "    \"index\": 7,\n" +
                         "    \"guid\": \"4f7f213b-29b1-48e6-b586-c76b609340f0\",\n" +
                         "    \"isActive\": true,\n" +
                         "    \"balance\": \"$1,114.53\",\n" +
                         "    \"picture\": \"http://placehold.it/32x32\",\n" +
                         "    \"age\": 34,\n" +
                         "    \"eyeColor\": \"green\",\n" +
                         "    \"name\": \"Vonda Whitley\",\n" +
                         "    \"gender\": \"female\",\n" +
                         "    \"company\": \"APEXTRI\",\n" +
                         "    \"email\": \"vondawhitley@apextri.com\",\n" +
                         "    \"phone\": \"+1 (852) 464-2850\",\n" +
                         "    \"address\": \"115 Rogers Avenue, Mahtowa, Northern Mariana Islands, 8529\",\n" +
                         "    \"about\": \"cupidatat consequat excepteur consequat incididunt officia esse\",\n" +
                         "    \"registered\": \"2014-08-15T07:32:32 -04:00\",\n" +
                         "    \"latitude\": -11.275146,\n" +
                         "    \"longitude\": 114.522759,\n" +
                         "    \"tags\": [\n" +
                         "      \"occaecat\",\n" +
                         "      \"occaecat\",\n" +
                         "      \"incididunt\",\n" +
                         "      \"ea\",\n" +
                         "      \"et\",\n" +
                         "      \"id\",\n" +
                         "      \"eiusmod\"\n" +
                         "    ],\n" +
                         "    \"friends\": [\n" +
                         "      {\n" +
                         "        \"id\": 0,\n" +
                         "        \"name\": \"Tania Cunningham\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 1,\n" +
                         "        \"name\": \"Boone Best\"\n" +
                         "      },\n" +
                         "      {\n" +
                         "        \"id\": 2,\n" +
                         "        \"name\": \"Nanette Yates\"\n" +
                         "      }\n" +
                         "    ],\n" +
                         "    \"greeting\": \"Hello, Vonda Whitley! You have 1 unread messages.\",\n" +
                         "    \"favoriteFruit\": \"banana\"\n" +
                         "  }\n" +
                         "]");
    }

    @Test
    public void testJsonEmptyString() {
        assertThrows(MalformedJsonException.class, () -> testJson(""));
    }

    @Test
    public void testJsonBlankString() {
        assertThrows(MalformedJsonException.class, () -> testJson("  "));
    }

    private void testJson(String jsonString) {
        testJsonVarchar(jsonString);
        if (unitDbAdapter.supportsJsonType()) {
            testJsonOther(jsonString);
        }
    }

    private void testJsonOther(String jsonString) {
        JsonOther jsonInsert = context.newObject(JsonOther.class);
        jsonInsert.setData(new Json(jsonString));
        context.commitChanges();

        JsonOther jsonSelect = context.selectOne(SelectById.query(JsonOther.class, jsonInsert.getObjectId()));
        assertEquals(jsonInsert.getData(), jsonSelect.getData());
    }

    private void testJsonVarchar(String jsonString) {
        JsonVarchar jsonInsert = context.newObject(JsonVarchar.class);
        jsonInsert.setData(new Json(jsonString));
        context.commitChanges();

        JsonVarchar jsonSelect = context.selectOne(SelectById.query(JsonVarchar.class, jsonInsert.getObjectId()));
        assertEquals(jsonInsert.getData(), jsonSelect.getData());
    }
}
