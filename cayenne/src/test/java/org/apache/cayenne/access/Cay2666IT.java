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
package org.apache.cayenne.access;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTEqual;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.cay_2666.CAY2666;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class Cay2666IT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.CAY_2666);

    @Test
    public void exp_Path() {
        Expression exp1 = ExpressionFactory.exp("object$.path");
        assertEquals(Expression.OBJ_PATH, exp1.getType());

        Expression exp2 = ExpressionFactory.exp("db:object.path$");
        assertEquals(Expression.DB_PATH, exp2.getType());
    }

    @Test
    public void pathExp() {
        assertEquals("abc$.xyz$", ExpressionFactory.pathExp("abc$.xyz$").toString());
    }

    @Test
    public void dbPathExp() {
        assertEquals("db:abc.xyz$", ExpressionFactory.dbPathExp("abc.xyz$").toString());
    }

    @Test
    public void expWithAlias() {
        Expression expression = ExpressionFactory.exp("paintings#p1.galleries$#p2.name = 'Test'");
        assertEquals("p1.p2.name", expression.getOperand(0).toString());
        assertEquals("galleries$", ((ASTObjPath)expression.getOperand(0)).getPathAliases().get("p2"));
    }

    @Test
    public void expWithAliasAndOuterJoin() {
        Expression expression = ExpressionFactory.exp("paintings$#p1+.name = 'Test'");
        assertEquals("p1.name", expression.getOperand(0).toString());
        assertEquals("paintings$+", ((ASTObjPath)expression.getOperand(0)).getPathAliases().get("p1"));
    }

    @Test
    public void dbPathWithDollarSign() throws IOException {
        StringBuilder buffer = new StringBuilder();
        new ASTDbPath("x$").appendAsString(buffer);
        assertEquals("db:x$", buffer.toString());
    }

    @Test
    public void expDbPathWithDollarSign() throws IOException {
        Expression exp = ExpressionFactory.exp("db:x$ = 'A'");
        Expression expression = new ASTEqual(new ASTDbPath("x$"), "A");
        assertEquals(exp, expression);

        exp = ExpressionFactory.exp("x$ = 'A'");
        expression = new ASTEqual(new ASTDbPath("x$"), "A");
        assertNotEquals(exp, expression);

        exp = ExpressionFactory.exp("db:x$ = $name", "A");
        expression = new ASTEqual(new ASTDbPath("x$"), "A");
        assertEquals(exp, expression);
    }

    @Test
    public void objPathWithDollarSign() throws IOException {
        StringBuilder buffer = new StringBuilder();

        new ASTObjPath("obj:x$").appendAsString(buffer);
        assertEquals("obj:x$", buffer.toString());

        assertEquals("y$", new ASTObjPath("y$").toString());
    }

    @Test
    public void expObjPathWithDollarSign() throws IOException {
        Expression exp = ExpressionFactory.exp("obj:x$ = 'A'");
        Expression expression = new ASTEqual(new ASTObjPath("x$"), "A");
        assertEquals(exp, expression);

        exp = ExpressionFactory.exp("x$ = 'A'");
        expression = new ASTEqual(new ASTObjPath("x$"), "A");
        assertEquals(exp, expression);

        exp = ExpressionFactory.exp("obj:x$ = $name", "A");
        expression = new ASTEqual(new ASTObjPath("x$"), "A");
        assertEquals(exp, expression);
    }

    @Test
    public void expressionWithDollarSign() throws Exception {
        TableHelper tTest = env.table("Cay2666", "ID", "NAME$");
        tTest.insert(1, "st.One");

        Expression expression = ExpressionFactory.exp("name$ = 'st.One'");
        List<CAY2666> cay2666List = ObjectSelect.query(CAY2666.class).where(expression).select(env.context());
        assertEquals(1, cay2666List.size());

        expression = ExpressionFactory.exp("obj:name$ = 'st.Two'");
        cay2666List = ObjectSelect.query(CAY2666.class).where(expression).select(env.context());
        assertEquals(0, cay2666List.size());

        tTest.insert(2, "st.Two");

        expression = ExpressionFactory.exp("db:NAME$ = 'st.Two'");
        cay2666List = ObjectSelect.query(CAY2666.class).where(expression).select(env.context());
        assertEquals(1, cay2666List.size());
    }

}
