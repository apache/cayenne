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

package org.apache.cayenne.exp.parser;

import java.math.BigDecimal;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.table_primitives.TablePrimitives;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * @since 4.0
 */
@UseServerRuntime(CayenneProjects.TABLE_PRIMITIVES_PROJECT)
public class ASTFunctionCallMathIT extends ServerCase {

    @Inject
    private ObjectContext context;

    private TablePrimitives createPrimitives(int value) {
        TablePrimitives primitives = context.newObject(TablePrimitives.class);
        primitives.setIntColumn(value);
        context.commitChanges();
        return primitives;
    }

    @Test
    public void testASTAbs() throws Exception {
        TablePrimitives p1 = createPrimitives(-10);

        ASTAbs exp = new ASTAbs(TablePrimitives.INT_COLUMN.path());
        Property<Integer> intColumn = Property.create("intColumn", exp, Integer.class);

        TablePrimitives p2 = ObjectSelect.query(TablePrimitives.class).where(intColumn.eq(10)).selectOne(context);
        assertEquals(p1, p2);
    }

    @Test
    public void testASTSqrt() throws Exception {
        TablePrimitives p1 = createPrimitives(9);

        ASTSqrt exp = new ASTSqrt(TablePrimitives.INT_COLUMN.path());
        Property<Integer> intColumn = Property.create("intColumn", exp, Integer.class);

        TablePrimitives p2 = ObjectSelect.query(TablePrimitives.class).where(intColumn.eq(3)).selectOne(context);
        assertEquals(p1, p2);
    }

    @Test
    public void testASTMod() throws Exception {
        TablePrimitives p1 = createPrimitives(10);

        ASTMod exp = new ASTMod(TablePrimitives.INT_COLUMN.path(), new ASTScalar((Integer)3));
        Property<Integer> intColumn = Property.create("intColumn", exp, Integer.class);

        TablePrimitives p2 = ObjectSelect.query(TablePrimitives.class).where(intColumn.eq(1)).selectOne(context);
        assertEquals(p1, p2);
    }

    @Test
    public void testASTAbsParse() {
        Expression exp = ExpressionFactory.exp("ABS(-3)");
        assertEquals(3.0, exp.evaluate(new Object()));
    }

    @Test
    public void testASTSqrtParse() {
        Expression exp = ExpressionFactory.exp("SQRT(16)");
        assertEquals(4.0, exp.evaluate(new Object()));
    }

    @Test
    public void testASTModParse() {
        Expression exp = ExpressionFactory.exp("MOD(11,2)");
        assertEquals(1.0, exp.evaluate(new Object()));
    }

    @Test
    public void testComplexParse() {
        Expression exp = ExpressionFactory.exp("10 - MOD(SQRT(ABS(-9)), 2)");
        assertEquals(BigDecimal.valueOf(9L), exp.evaluate(new Object()));
    }
}
