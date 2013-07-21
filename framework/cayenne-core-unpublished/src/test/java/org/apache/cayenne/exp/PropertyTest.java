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
package org.apache.cayenne.exp;

import java.util.Arrays;

import junit.framework.TestCase;

public class PropertyTest extends TestCase {

    public void testIn() {
        Property<String> p = new Property<String>("x.y");

        Expression e1 = p.in("a");
        assertEquals("x.y in (\"a\")", e1.toString());

        Expression e2 = p.in("a", "b");
        assertEquals("x.y in (\"a\", \"b\")", e2.toString());

        Expression e3 = p.in(Arrays.asList("a", "b"));
        assertEquals("x.y in (\"a\", \"b\")", e3.toString());
    }
}
