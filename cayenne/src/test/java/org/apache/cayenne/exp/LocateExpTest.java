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

public class LocateExpTest {

    @Test
    public void evaluateLocate() throws Exception {
        ObjPathExp path = new ObjPathExp("artistName");
        ScalarExp substr = new ScalarExp("678");
        ScalarExp offset = new ScalarExp((Integer)5);
        LocateExp exp = new LocateExp(substr, path, offset);

        Artist a = new Artist();
        a.setArtistName("1267834567890abc");

        Object res = exp.evaluateNode(a);
        assertTrue(res instanceof Integer);
        assertEquals(9, res);

        a.setArtistName("abcdefgh");
        res = exp.evaluateNode(a);
        assertTrue(res instanceof Integer);
        assertEquals(0, res);
    }

    @Test
    public void parseTest() throws Exception {
        String expString = "locate(\"xyz\" , abc , 4)";
        Expression exp = ExpressionFactory.exp(expString);

        assertTrue(exp instanceof LocateExp);
        String toString = exp.toString();
        assertEquals(expString, toString);
    }

}
