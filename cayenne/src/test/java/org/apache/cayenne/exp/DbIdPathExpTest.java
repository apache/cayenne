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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DbIdPathExpTest {

    @Test
    public void shallowCopy() {
        DbIdPathExp path = new DbIdPathExp("test");

        Expression exp = path.shallowCopy();
        assertInstanceOf(DbIdPathExp.class, exp);
        assertInstanceOf(DbIdPathExp.class, exp);

        DbIdPathExp clone = (DbIdPathExp)exp;
        assertEquals("test", clone.getPath().value());
    }

    @Test
    public void appendAsString() throws IOException {
        DbIdPathExp path = new DbIdPathExp("test");
        StringBuilder sb = new StringBuilder();
        path.appendAsString(sb);

        assertEquals("dbid:test", sb.toString());
    }

    @Test
    public void simpleParse() {
        Expression exp = ExpressionFactory.exp("dbid:test");
        assertInstanceOf(DbIdPathExp.class, exp);
        DbIdPathExp path = (DbIdPathExp)exp;
        assertEquals("test", path.getPath().value());
    }

    @Test
    public void expParse() {
        Expression exp = ExpressionFactory.exp("dbid:test = 1");
        assertInstanceOf(EqualExp.class, exp);
        EqualExp equal = (EqualExp)exp;

        Object child0 = equal.getOperand(0);
        assertInstanceOf(DbIdPathExp.class, child0);
        DbIdPathExp path = (DbIdPathExp)child0;
        assertEquals("test", path.getPath().value());
    }

}