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
import org.apache.cayenne.testdo.table_primitives.TablePrimitives;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.0
 */
public class ASTModTest {

    @Test
    public void evaluateNode() throws Exception {
        ASTObjPath path = new ASTObjPath("intColumn");
        ASTMod mod = new ASTMod(path, new ASTScalar(3.0));

        TablePrimitives a = new TablePrimitives();
        a.setIntColumn(10);

        Object res = mod.evaluateNode(a);
        assertTrue(res instanceof Double);
        assertEquals(1.0, res);
    }

    @Test
    public void parseTest() throws Exception {
        String expString = "mod(xyz , 3)";
        Expression exp = ExpressionFactory.exp(expString);

        assertTrue(exp instanceof ASTMod);
        String toString = exp.toString();
        assertEquals(expString, toString);
    }

}