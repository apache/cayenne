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

import java.math.BigDecimal;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.table_primitives.TablePrimitives;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ASTFunctionCallMathIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TABLE_PRIMITIVES_PROJECT);

    private TablePrimitives createPrimitives(int value) {
        TablePrimitives primitives = env.context().newObject(TablePrimitives.class);
        primitives.setIntColumn(value);
        env.context().commitChanges();
        return primitives;
    }

    @Test
    public void aSTAbs() throws Exception {
        TablePrimitives p1 = createPrimitives(-10);

        TablePrimitives p2 = ObjectSelect.query(TablePrimitives.class)
                .where(TablePrimitives.INT_COLUMN.abs().eq(10)).selectOne(env.context());
        assertEquals(p1, p2);
    }

    @Test
    public void aSTSqrt() throws Exception {
        TablePrimitives p1 = createPrimitives(9);

        TablePrimitives p2 = ObjectSelect.query(TablePrimitives.class)
                .where(TablePrimitives.INT_COLUMN.sqrt().eq(3)).selectOne(env.context());
        assertEquals(p1, p2);
    }

    @Test
    public void aSTMod() throws Exception {
        TablePrimitives p1 = createPrimitives(10);

        TablePrimitives p2 = ObjectSelect.query(TablePrimitives.class)
                .where(TablePrimitives.INT_COLUMN.mod(3).eq(1)).selectOne(env.context());
        assertEquals(p1, p2);
    }

    @Test
    public void aSTAbsParse() {
        Expression exp = ExpressionFactory.exp("abs(-3)");
        assertEquals(3.0, exp.evaluate(new Object()));
    }

    @Test
    public void aSTSqrtParse() {
        Expression exp = ExpressionFactory.exp("sqrt(16)");
        assertEquals(4.0, exp.evaluate(new Object()));
    }

    @Test
    public void aSTModParse() {
        Expression exp = ExpressionFactory.exp("mod(11,2)");
        assertEquals(1.0, exp.evaluate(new Object()));
    }

    @Test
    public void complexParse() {
        Expression exp = ExpressionFactory.exp("10 - mod(sqrt(abs(-9)), 2)");
        assertEquals(BigDecimal.valueOf(9L), exp.evaluate(new Object()));
    }

    @Test
    public void aSTNegate() throws Exception {
        TablePrimitives p1 = createPrimitives(-7);

        // unary minus applied to a column translates to "-INT_COLUMN = ?"; verify it round-trips
        TablePrimitives p2 = ObjectSelect.query(TablePrimitives.class)
                .where(ExpressionFactory.exp("-intColumn = 7")).selectOne(env.context());
        assertEquals(p1, p2);
    }
}
