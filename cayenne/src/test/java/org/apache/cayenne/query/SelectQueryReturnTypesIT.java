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

package org.apache.cayenne.query;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.BitwiseAndExp;
import org.apache.cayenne.exp.BitwiseNotExp;
import org.apache.cayenne.exp.BitwiseOrExp;
import org.apache.cayenne.exp.BitwiseXorExp;
import org.apache.cayenne.exp.EqualExp;
import org.apache.cayenne.exp.GreaterExp;
import org.apache.cayenne.exp.ObjPathExp;
import org.apache.cayenne.exp.ScalarExp;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.return_types.ReturnTypesMap1;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SelectQueryReturnTypesIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.RETURN_TYPES_PROJECT);

    protected void createNumericsDataSet() throws Exception {
        TableHelper tNumerics = env.table("TYPES_MAPPING_TEST1", "AAAID", "INTEGER_COLUMN");

        tNumerics.insert(1, 0);
        tNumerics.insert(2, 1);
        tNumerics.insert(3, 2);
        tNumerics.insert(4, 3);
        tNumerics.insert(5, 4);
    }

    @Test
    public void selectBitwiseNot() throws Exception {

        if (!env.testDbAdapter().supportsBitwiseOps()) {
            return;
        }

        createNumericsDataSet();

        // to simplify result checking, do double NOT
        Expression left = new BitwiseNotExp(new BitwiseNotExp(ExpressionFactory.pathExp(ReturnTypesMap1.INTEGER_COLUMN.getName())));
        Expression right = ExpressionFactory.wrapScalarValue(2);
        Expression greater = new GreaterExp(left);
        greater.setOperand(1, right);

        List<ReturnTypesMap1> objects = ObjectSelect.query(ReturnTypesMap1.class).where(greater).select(env.context());
        assertEquals(2, objects.size());
    }

    @Test
    public void selectBitwiseOr() throws Exception {

        if (!env.testDbAdapter().supportsBitwiseOps()) {
            return;
        }

        createNumericsDataSet();

        // to simplify result checking, do double NOT
        Expression left = new BitwiseOrExp(new Object[] { ExpressionFactory.pathExp(ReturnTypesMap1.INTEGER_COLUMN.getName()),
                ExpressionFactory.wrapScalarValue(1) });
        Expression right = ExpressionFactory.wrapScalarValue(1);
        Expression equal = new EqualExp(left);
        equal.setOperand(1, right);

        List<ReturnTypesMap1> objects = ObjectSelect.query(ReturnTypesMap1.class).where(equal).select(env.context());
        assertEquals(2, objects.size());
    }

    @Test
    public void selectBitwiseAnd() throws Exception {

        if (!env.testDbAdapter().supportsBitwiseOps()) {
            return;
        }

        createNumericsDataSet();

        // to simplify result checking, do double NOT
        Expression left = new BitwiseAndExp(new Object[] { ExpressionFactory.pathExp(ReturnTypesMap1.INTEGER_COLUMN.getName()),
                ExpressionFactory.wrapScalarValue(1) });
        Expression right = ExpressionFactory.wrapScalarValue(0);
        Expression equal = new EqualExp(left);
        equal.setOperand(1, right);

        List<ReturnTypesMap1> objects = ObjectSelect.query(ReturnTypesMap1.class).where(equal).select(env.context());
        assertEquals(3, objects.size());
    }

    @Test
    public void selectBitwiseXor() throws Exception {

        if (!env.testDbAdapter().supportsBitwiseOps()) {
            return;
        }

        createNumericsDataSet();

        // to simplify result checking, do double NOT
        Expression left = new BitwiseXorExp(new Object[] { ExpressionFactory.pathExp(ReturnTypesMap1.INTEGER_COLUMN.getName()),
                ExpressionFactory.wrapScalarValue(1) });
        Expression right = ExpressionFactory.wrapScalarValue(5);
        Expression equal = new EqualExp(left);
        equal.setOperand(1, right);

        List<ReturnTypesMap1> objects = ObjectSelect.query(ReturnTypesMap1.class).where(equal).select(env.context());
        assertEquals(1, objects.size());
        assertEquals(4, objects.get(0).getIntegerColumn().intValue());
    }
}
