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

import static org.junit.Assert.assertEquals;

public class ExpressionUtilsTest {

    @Test
    public void testParsePath() throws ParseException {
        ASTPath path = new ASTObjPath();
        ExpressionUtils.parsePath(path, "a.b.c.d");

        assertEquals("a.b.c.d", path.getPath().value());
        assertEquals(0, path.getPathAliases().size());
    }

    @Test
    public void testParsePathOuterJoin() throws ParseException {
        ASTPath path = new ASTObjPath();
        ExpressionUtils.parsePath(path, "a.b+.c+.d");

        assertEquals("a.b+.c+.d", path.getPath().value());
        assertEquals(0, path.getPathAliases().size());
    }

    @Test
    public void testParsePathWithAlias() throws ParseException {
        ASTPath path = new ASTObjPath();
        ExpressionUtils.parsePath(path, "a.b.c#p1.d#p2");

        assertEquals("a.b.p1.p2", path.getPath().value());
        assertEquals(2, path.getPathAliases().size());
        assertEquals("c", path.getPathAliases().get("p1"));
        assertEquals("d", path.getPathAliases().get("p2"));
    }

    @Test
    public void testParsePathWithAliasAndOuterJoin() throws ParseException {
        ASTPath path = new ASTObjPath();
        ExpressionUtils.parsePath(path, "a.b+.c#p1+.d#p2");

        assertEquals("a.b+.p1.p2", path.getPath().value());
        assertEquals(2, path.getPathAliases().size());
        assertEquals("c+", path.getPathAliases().get("p1"));
        assertEquals("d", path.getPathAliases().get("p2"));
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidPath() throws ParseException {
        ASTPath path = new ASTObjPath();
        ExpressionUtils.parsePath(path, "a.b.c#p1.d#p1");
    }
}