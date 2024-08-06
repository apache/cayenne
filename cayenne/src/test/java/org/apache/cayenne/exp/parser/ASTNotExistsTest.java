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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ASTNotExistsTest {

    @Test
    public void parseSinglePath() {
        Expression exp = ExpressionFactory.exp("not exists a");
        assertTrue(exp instanceof ASTNotExists);
        SimpleNode node = (SimpleNode) exp;
        assertEquals("a", node.jjtGetChild(0).toString());
    }

    @Test
    public void parseLongPath() {
        Expression exp = ExpressionFactory.exp("not exists a.b.c");
        assertTrue(exp instanceof ASTNotExists);
        SimpleNode node = (SimpleNode) exp;
        assertEquals("a.b.c", node.jjtGetChild(0).toString());
    }

    @Test
    public void parseLongDbPath() {
        Expression exp = ExpressionFactory.exp("not exists db:a.b.c");
        assertTrue(exp instanceof ASTNotExists);
        SimpleNode node = (SimpleNode) exp;
        assertEquals("db:a.b.c", node.jjtGetChild(0).toString());
    }

    @Test
    public void parseCondition() {
        Expression exp = ExpressionFactory.exp("not exists a > 5");
        assertTrue(exp instanceof ASTNotExists);
        SimpleNode node = (SimpleNode) exp;
        assertEquals("(a > 5)", node.jjtGetChild(0).toString());
    }

    @Test
    public void parseFunction() {
        Expression exp = ExpressionFactory.exp("not exists length(a) <= 5");
        assertTrue(exp instanceof ASTNotExists);
        SimpleNode node = (SimpleNode) exp;
        assertEquals("(length(a) <= 5)", node.jjtGetChild(0).toString());
    }

}