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
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @since 4.0
 */
public class ASTConcatTest {

    @Test
    public void testEvaluateConcat() throws Exception {

        ASTObjPath path = new ASTObjPath("artistName");
        ASTScalar scalar = new ASTScalar(" ");
        ASTScalar scalar1 = new ASTScalar("test");

        ASTConcat concat = new ASTConcat(path, scalar, scalar1);

        Artist a = new Artist();
        a.setArtistName("name");

        Object res = concat.evaluateNode(a);
        assertTrue(res instanceof String);
        assertEquals("name test", res);
    }

    @Test
    public void testParseConcat() throws Exception {
        Expression exp = ExpressionFactory.exp("concat(artistName, ' ', 'test')");
        assertEquals(ASTConcat.class, exp.getClass());
        assertEquals(3, exp.getOperandCount());

        Artist a = new Artist();
        a.setArtistName("name");

        Object res = exp.evaluate(a);
        assertTrue(res instanceof String);
        assertEquals("name test", res);
    }

    @Test
    public void parseTest() throws Exception {
        String expString = "concat(xyz , \" \" , abc)";
        Expression exp = ExpressionFactory.exp(expString);

        assertTrue(exp instanceof ASTConcat);
        String toString = exp.toString();
        assertEquals(expString, toString);
    }

}
