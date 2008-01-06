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

package org.apache.cayenne.exp;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import junit.framework.TestCase;

/**
 * @author Andrus Adamchik
 */
public class ExpressionTest extends TestCase {

    public void testFromStringLong() {
        Expression e = Expression.fromString("216201000180L");
        assertEquals(new Long(216201000180L), e.evaluate(new Object()));
    }

    public void testExpWithParametersNullHandling_CAY847() {
        Expression e = Expression.fromString("X = $x");

        e = e.expWithParameters(Collections.singletonMap("x", null));
        assertEquals("X = null", e.toString());
    }

    public void testToEJBQL1() {

        Expression e = Expression.fromString("artistName = 'bla'");
        assertEquals("x.artistName = \"bla\"", e.toEJBQL("x"));
    }

    public void testEncodeAsEJBQL1() {

        Expression e = Expression.fromString("artistName = 'bla'");

        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e.encodeAsEJBQL(pw, "x");
        pw.close();
        buffer.flush();
        String ejbql = buffer.toString();

        assertEquals("x.artistName = \"bla\"", ejbql);
    }

    public void testEncodeAsEJBQL2() {

        Expression e = Expression.fromString("artistName.stuff = $name");

        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e.encodeAsEJBQL(pw, "x");
        pw.close();
        buffer.flush();
        String ejbql = buffer.toString();

        assertEquals("x.artistName.stuff = :name", ejbql);
    }
}
