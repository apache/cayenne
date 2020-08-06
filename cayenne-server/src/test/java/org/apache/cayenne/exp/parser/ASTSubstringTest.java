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
public class ASTSubstringTest {

    @Test
    public void testEvaluateSubstring() throws Exception {
        ASTObjPath path = new ASTObjPath("artistName");
        ASTScalar offset = new ASTScalar((Integer)2);
        ASTScalar length = new ASTScalar((Integer)8);
        ASTSubstring exp = new ASTSubstring(path, offset, length);

        Artist a = new Artist();
        a.setArtistName("1234567890xyz");

        Object res = exp.evaluateNode(a);
        assertTrue(res instanceof String);
        assertEquals("23456789", res);
    }

    @Test
    public void parseTest() throws Exception {
        String expString = "substring(xyz , 2 , 3)";
        Expression exp = ExpressionFactory.exp(expString);

        assertTrue(exp instanceof ASTSubstring);
        String toString = exp.toString();
        assertEquals(expString, toString);
    }
}
