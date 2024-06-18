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

import java.io.IOException;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class ASTDbIdPathTest {

    @Test
    public void testShallowCopy() {
        ASTDbIdPath path = new ASTDbIdPath("test");

        Expression exp = path.shallowCopy();
        assertEquals(exp.getType(), Expression.DBID_PATH);
        assertThat(exp, instanceOf(ASTDbIdPath.class));

        ASTDbIdPath clone = (ASTDbIdPath)exp;
        assertEquals("test", clone.getPath().value());
    }

    @Test
    public void testAppendAsString() throws IOException {
        ASTDbIdPath path = new ASTDbIdPath("test");
        StringBuilder sb = new StringBuilder();
        path.appendAsString(sb);

        assertEquals("dbid:test", sb.toString());
    }

    @Test
    public void testSimpleParse() {
        Expression exp = ExpressionFactory.exp("dbid:test");
        assertThat(exp, instanceOf(ASTDbIdPath.class));
        ASTDbIdPath path = (ASTDbIdPath)exp;
        assertEquals("test", path.getPath().value());
    }

    @Test
    public void testExpParse() {
        Expression exp = ExpressionFactory.exp("dbid:test = 1");
        assertThat(exp, instanceOf(ASTEqual.class));
        ASTEqual equal = (ASTEqual)exp;

        Node child0 = equal.jjtGetChild(0);
        assertThat(child0, instanceOf(ASTDbIdPath.class));
        ASTDbIdPath path = (ASTDbIdPath)child0;
        assertEquals("test", path.getPath().value());
    }

}