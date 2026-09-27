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

package org.apache.cayenne.exp;

import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcatExpTest {

    @Test
    public void evaluateConcat() throws Exception {

        ObjPathExp path = new ObjPathExp("artistName");
        ScalarExp scalar = new ScalarExp(" ");
        ScalarExp scalar1 = new ScalarExp("test");

        ConcatExp concat = new ConcatExp(path, scalar, scalar1);

        Artist a = new Artist();
        a.setArtistName("name");

        Object res = concat.evaluateNode(a);
        assertTrue(res instanceof String);
        assertEquals("name test", res);
    }

    @Test
    public void parseConcat() throws Exception {
        Expression exp = ExpressionFactory.exp("concat(artistName, ' ', 'test')");
        assertEquals(ConcatExp.class, exp.getClass());
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

        assertTrue(exp instanceof ConcatExp);
        String toString = exp.toString();
        assertEquals(expString, toString);
    }

}
