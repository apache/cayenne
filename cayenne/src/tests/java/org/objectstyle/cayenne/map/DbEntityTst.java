/*
 * ==================================================================== The ObjectStyle
 * Group Software License, version 1.1 ObjectStyle Group - http://objectstyle.org/
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors of the
 * software. All rights reserved. Redistribution and use in source and binary forms, with
 * or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must
 * reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. The end-user documentation included with the redistribution, if any, must include
 * the following acknowlegement: "This product includes software developed by independent
 * contributors and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 * Alternately, this acknowlegement may appear in the software itself, if and wherever
 * such third-party acknowlegements normally appear. 4. The names "ObjectStyle Group" and
 * "Cayenne" must not be used to endorse or promote products derived from this software
 * without prior written permission. For written permission, email "andrus at objectstyle
 * dot org". 5. Products derived from this software may not be called "ObjectStyle" or
 * "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their names without prior
 * written permission. THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE OBJECTSTYLE
 * GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ==================================================================== This software
 * consists of voluntary contributions made by many individuals and hosted on ObjectStyle
 * Group web site. For more information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.map;

import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class DbEntityTst extends CayenneTestCase {

    protected DbEntity ent;

    public void setUp() throws Exception {
        ent = new DbEntity();
    }

    public void testConstructor1() throws Exception {
        ent = new DbEntity();
        assertNull(ent.getName());
    }

    public void testConstructor2() throws Exception {
        ent = new DbEntity("abc");
        assertEquals("abc", ent.getName());
    }

    public void testCatalog() throws Exception {
        String tstName = "tst_name";
        ent.setCatalog(tstName);
        assertEquals(tstName, ent.getCatalog());
    }

    public void testSchema() throws Exception {
        String tstName = "tst_name";
        ent.setSchema(tstName);
        assertEquals(tstName, ent.getSchema());
    }

    public void testFullyQualifiedName() throws Exception {
        String tstName = "tst_name";
        String schemaName = "tst_schema_name";
        ent.setName(tstName);

        assertEquals(tstName, ent.getName());
        assertEquals(tstName, ent.getFullyQualifiedName());

        ent.setSchema(schemaName);

        assertEquals(tstName, ent.getName());
        assertEquals(schemaName + "." + tstName, ent.getFullyQualifiedName());
    }

    public void testGetPrimaryKey() throws Exception {
        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);
        ent.addAttribute(a1);

        DbAttribute a2 = new DbAttribute();
        a2.setName("a2");
        a2.setPrimaryKey(true);
        ent.addAttribute(a2);

        List pk = ent.getPrimaryKey();
        assertNotNull(pk);
        assertEquals(1, pk.size());
        assertSame(a2, pk.get(0));
    }

    public void testAddPKAttribute() {
        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);

        assertTrue(ent.getPrimaryKey().isEmpty());
        ent.addAttribute(a1);
        assertTrue(ent.getPrimaryKey().isEmpty());
    }

    public void testChangeAttributeToPK() {
        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);
        ent.addAttribute(a1);

        assertFalse(ent.getPrimaryKey().contains(a1));
        a1.setPrimaryKey(true);
        assertTrue(ent.getPrimaryKey().contains(a1));
    }

    public void testChangePKAttribute() {
        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(true);
        ent.addAttribute(a1);

        assertTrue(ent.getPrimaryKey().contains(a1));
        a1.setPrimaryKey(false);
        assertFalse(ent.getPrimaryKey().contains(a1));
    }

    public void testRemovAttribute() throws Exception {
        DataMap map = new DataMap("map");
        ent.setName("ent");
        map.addDbEntity(ent);

        DbAttribute a1 = new DbAttribute();
        a1.setName("a1");
        a1.setPrimaryKey(false);
        ent.addAttribute(a1);

        DbEntity otherEntity = new DbEntity("22ent1");
        assertNotNull(otherEntity.getName());
        map.addDbEntity(otherEntity);
        DbAttribute a11 = new DbAttribute();
        a11.setName("a11");
        a11.setPrimaryKey(false);
        otherEntity.addAttribute(a11);

        DbRelationship rel = new DbRelationship("relfrom");
        ent.addRelationship(rel);
        rel.setTargetEntity(otherEntity);
        rel.addJoin(new DbJoin(rel, "a1", "a11"));

        DbRelationship rel1 = new DbRelationship("relto");
        otherEntity.addRelationship(rel1);
        rel1.setTargetEntity(ent);
        rel1.addJoin(new DbJoin(rel1, "a11", "a1"));

        // check that the test case is working
        assertSame(a1, ent.getAttribute(a1.getName()));
        assertSame(rel, ent.getRelationship(rel.getName()));

        // test removal
        ent.removeAttribute(a1.getName());

        assertNull(ent.getAttribute(a1.getName()));
        assertEquals(0, rel1.getJoins().size());
        assertEquals(0, rel.getJoins().size());
    }

    public void testTranslateToRelatedEntityIndependentPath() throws Exception {
        DbEntity artistE = getDomain().getEntityResolver().lookupDbEntity(Artist.class);

        Expression e1 = Expression.fromString("db:paintingArray");
        Expression translated = artistE
                .translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals("failure: " + translated, Expression
                .fromString("db:toArtist.paintingArray"), translated);
    }

    public void testTranslateToRelatedEntityTrimmedPath() throws Exception {
        DbEntity artistE = getDomain().getEntityResolver().lookupDbEntity(Artist.class);

        Expression e1 = Expression.fromString("db:artistExhibitArray.toExhibit");
        Expression translated = artistE
                .translateToRelatedEntity(e1, "artistExhibitArray");
        assertEquals(
                "failure: " + translated,
                Expression.fromString("db:toExhibit"),
                translated);
    }

    public void testTranslateToRelatedEntitySplitHalfWay() throws Exception {
        DbEntity artistE = getDomain().getEntityResolver().lookupDbEntity(Artist.class);

        Expression e1 = Expression
                .fromString("db:paintingArray.toPaintingInfo.TEXT_REVIEW");
        Expression translated = artistE.translateToRelatedEntity(
                e1,
                "paintingArray.toGallery");
        assertEquals("failure: " + translated, Expression
                .fromString("db:paintingArray.toPaintingInfo.TEXT_REVIEW"), translated);
    }

    public void testTranslateToRelatedEntityMatchingPath() throws Exception {
        DbEntity artistE = getDomain().getEntityResolver().lookupDbEntity(Artist.class);

        Expression e1 = Expression.fromString("db:artistExhibitArray.toExhibit");
        Expression translated = artistE.translateToRelatedEntity(
                e1,
                "artistExhibitArray.toExhibit");

        assertEquals("failure: " + translated, Expression
                .fromString("db:artistExhibitArray.toExhibit"), translated);
    }
}