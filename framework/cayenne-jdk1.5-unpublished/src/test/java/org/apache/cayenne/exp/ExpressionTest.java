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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class ExpressionTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private ServerRuntime runtime;

    @Override
    protected void setUpAfterInjection() throws Exception {

        SelectQuery query = new SelectQuery(Artist.class);
        Expression qual = ExpressionFactory.likeExp("artistName", "Equals");
        query.setQualifier(qual);
        List<?> objects = context.performQuery(query);

        if (objects.size() > 0) {
            SelectQuery query1 = new SelectQuery(Painting.class);
            Expression e = ExpressionFactory.matchExp(
                    Painting.TO_ARTIST_PROPERTY,
                    objects.get(0));
            query.setQualifier(e);
            objects.addAll(context.performQuery(query1));
        }

        context.deleteObjects(objects);
    }

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

    public void testEqualsObjects() {

        assertTrue(context instanceof DataContext);

        DataContext context2 = (DataContext) runtime.getContext();

        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("Equals");
        Painting p1 = context.newObject(Painting.class);
        p1.setToArtist(a1);
        p1.setPaintingTitle("painting1");

        SelectQuery query = new SelectQuery(Painting.class);
        Expression e = ExpressionFactory.matchExp(Painting.TO_ARTIST_PROPERTY, a1);
        query.setQualifier(e);

        context.commitChanges();

        assertNotSame(context2, context);

        List<Artist> objects = context2.performQuery(query);
        assertEquals(1, objects.size());

        // 2 same objects in different contexts
        assertTrue(e.match(objects.get(0)));

        // we change one object - so the objects are different now (PersistenceState
        // different)
        a1.setArtistName("newName");

        SelectQuery q2 = new SelectQuery(Painting.class);
        Expression ex2 = ExpressionFactory.matchExp(Painting.TO_ARTIST_PROPERTY, a1);
        q2.setQualifier(ex2);

        assertFalse(ex2.match(objects.get(0)));

        Artist a2 = context.newObject(Artist.class);
        a1.setArtistName("Equals");

        SelectQuery q = new SelectQuery(Painting.class);
        Expression ex = ExpressionFactory.matchExp(Painting.TO_ARTIST_PROPERTY, a2);
        q.setQualifier(ex);

        // 2 different objects in different contexts
        assertFalse(ex.match(objects.get(0)));
    }

    public void testFirst() {
        List<Painting> paintingList = new ArrayList<Painting>();
        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("x1");
        paintingList.add(p1);

        Painting p2 = context.newObject(Painting.class);
        p2.setPaintingTitle("x2");
        paintingList.add(p2);

        Painting p3 = context.newObject(Painting.class);
        p3.setPaintingTitle("x3");
        paintingList.add(p3);

        Expression e1 = ExpressionFactory.likeExp("paintingTitle", "x%");
        assertSame(p1, e1.first(paintingList));

        Expression e3 = ExpressionFactory.matchExp("paintingTitle", "x3");
        assertSame(p3, e3.first(paintingList));

        Expression e4 = ExpressionFactory.matchExp("paintingTitle", "x4");
        assertNull(e4.first(paintingList));
    }

}
