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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.testdo.testmap.Artist;

public class ExpressionTest extends TestCase {

    public void testFromStringLong() {
        Expression e = Expression.fromString("216201000180L");
        assertEquals(new Long(216201000180L), e.evaluate(new Object()));
    }

    public void testFromStringPath() {
        Expression e1 = Expression.fromString("object.path");
        assertEquals(Expression.OBJ_PATH, e1.getType());

        Expression e2 = Expression.fromString("db:object.path");
        assertEquals(Expression.DB_PATH, e2.getType());

        Expression e3 = Expression.fromString("object+.path");
        assertEquals(Expression.OBJ_PATH, e3.getType());

        Expression e4 = Expression.fromString("db:object.path+");
        assertEquals(Expression.DB_PATH, e4.getType());
    }

    public void testFromStringScalar() {
        Expression e1 = Expression.fromString("a = 'abc'");
        assertEquals("abc", e1.getOperand(1));
    }

    public void testFromStringEnum() {
        Expression e1 = Expression
                .fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.ONE");
        assertEquals(ExpEnum1.ONE, e1.getOperand(1));

        Expression e2 = Expression
                .fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.TWO");
        assertEquals(ExpEnum1.TWO, e2.getOperand(1));

        Expression e3 = Expression
                .fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.THREE");
        assertEquals(ExpEnum1.THREE, e3.getOperand(1));

        try {
            Expression.fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.BOGUS");
            fail("Didn't throw on bad enum");
        }
        catch (ExpressionException e) {
            // expected
        }

        try {
            Expression.fromString("a = enum:BOGUS");
            fail("Didn't throw on bad enum");
        }
        catch (ExpressionException e) {
            // expected
        }
    }

    public void testExpWithParametersNullHandling_CAY847() {
        Expression e = Expression.fromString("X = $x");

        e = e.expWithParameters(Collections.singletonMap("x", null));
        assertEquals("X = null", e.toString());
    }

    public void testToEJBQL1() {

        Expression e = Expression.fromString("artistName = \"bla\"");

        // note single quotes - EJBQL does not support doublequotes...
        assertEquals("x.artistName = 'bla'", e.toEJBQL("x"));
    }

    public void testEncodeAsEJBQL1() {

        Expression e = Expression.fromString("artistName = 'bla'");

        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e.encodeAsEJBQL(pw, "x");
        pw.close();
        buffer.flush();
        String ejbql = buffer.toString();

        assertEquals("x.artistName = 'bla'", ejbql);
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

    public void testEncodeAsEJBQL3_EncodeListOfParameters() {

        Expression e = ExpressionFactory.inExp("artistName", "a", "b", "c");

        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e.encodeAsEJBQL(pw, "x");
        pw.close();
        buffer.flush();
        String ejbql = buffer.toString();

        assertEquals("x.artistName in ('a', 'b', 'c')", ejbql);
    }

    public void testEncodeAsEJBQL_PersistentParamater() {

        Artist a = new Artist();
        ObjectId aId = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 1);
        a.setObjectId(aId);

        Expression e = ExpressionFactory.matchExp("artist", a);

        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e.encodeAsEJBQL(pw, "x");
        pw.close();
        buffer.flush();
        String ejbql = buffer.toString();

        assertEquals("x.artist = 1", ejbql);
    }

    public void testEncodeAsEJBQLNotEquals() {

        Expression e = Expression.fromString("artistName != 'bla'");

        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e.encodeAsEJBQL(pw, "x");
        pw.close();
        buffer.flush();
        String ejbql = buffer.toString();

        assertEquals("x.artistName <> 'bla'", ejbql);
    }

    public void testEncodeAsEJBQL_Enum() {

        Expression e = Expression
                .fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.THREE");

        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e.encodeAsEJBQL(pw, "x");
        pw.close();
        buffer.flush();
        String ejbql = buffer.toString();

        assertEquals("x.a = enum:org.apache.cayenne.exp.ExpEnum1.THREE", ejbql);
    }

    public void testEncodeAsString_StringLiteral() {
        Expression e1 = Expression.fromString("a = 'abc'");

        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e1.encodeAsString(pw);
        pw.close();
        buffer.flush();

        assertEquals("a = \"abc\"", buffer.toString());
    }

    public void testEncodeAsString_Enum() {
        Expression e1 = Expression
                .fromString("a = enum:org.apache.cayenne.exp.ExpEnum1.TWO");

        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        e1.encodeAsString(pw);
        pw.close();
        buffer.flush();

        assertEquals("a = enum:org.apache.cayenne.exp.ExpEnum1.TWO", buffer.toString());
    }
}
