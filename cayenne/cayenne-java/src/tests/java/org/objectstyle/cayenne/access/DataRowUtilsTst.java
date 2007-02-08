/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataRowUtilsTst extends CayenneTestCase {

    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = createDataContext();
    }

    public void testMerge() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists", null);

        String n1 = "changed";
        String n2 = "changed again";

        SelectQuery artistQ = new SelectQuery(Artist.class, Expression
                .fromString("artistName = 'artist1'"));
        Artist a1 = (Artist) context.performQuery(artistQ).get(0);
        a1.setArtistName(n1);

        DataRow s2 = new DataRow(2);
        s2.put("ARTIST_NAME", n2);
        s2.put("DATE_OF_BIRTH", new java.util.Date());
        ObjEntity e = context.getEntityResolver().lookupObjEntity(a1);
        DataRowUtils.mergeObjectWithSnapshot(e, a1, s2);

        // name was modified, so it should not change during merge
        assertEquals(n1, a1.getArtistName());

        // date of birth came from database, it should be updated during merge
        assertEquals(s2.get("DATE_OF_BIRTH"), a1.getDateOfBirth());
    }

    public void testIsToOneTargetModified() throws Exception {
        getAccessStack().createTestData(DataContextTestBase.class, "testArtists", null);

        ObjEntity paintingEntity = context.getEntityResolver().lookupObjEntity(
                Painting.class);
        ObjRelationship toArtist = (ObjRelationship) paintingEntity
                .getRelationship("toArtist");

        SelectQuery artistQ = new SelectQuery(Artist.class, Expression
                .fromString("artistName = 'artist2'"));
        Artist anotherArtist = (Artist) context.performQuery(artistQ).get(0);
        Painting painting = (Painting) context.createAndRegisterNewObject(Painting.class);
        painting.setPaintingTitle("PX");
        painting.setToArtist(anotherArtist);

        context.commitChanges();

        artistQ = new SelectQuery(Artist.class, Expression
                .fromString("artistName = 'artist1'"));
        Artist artist = (Artist) context.performQuery(artistQ).get(0);
        assertNotSame(artist, painting.getToArtist());

        ObjectDiff diff = context.getObjectStore().registerDiff(painting, null);

        assertFalse(DataRowUtils.isToOneTargetModified(toArtist, painting, diff));

        painting.setToArtist(artist);
        assertTrue(DataRowUtils.isToOneTargetModified(toArtist, painting, diff));
    }

    public void testIsToOneTargetModifiedWithNewTarget() throws Exception {
        createTestData("testIsToOneTargetModifiedWithNewTarget");

        // add NEW gallery to painting
        List paintings = context.performQuery(new SelectQuery(Painting.class));
        assertEquals(1, paintings.size());
        Painting p1 = (Painting) paintings.get(0);

        ObjEntity paintingEntity = context.getEntityResolver().lookupObjEntity(
                Painting.class);
        ObjRelationship toGallery = (ObjRelationship) paintingEntity
                .getRelationship("toGallery");

        ObjectDiff diff = context.getObjectStore().registerDiff(p1, null);
        assertFalse(DataRowUtils.isToOneTargetModified(toGallery, p1, diff));

        Gallery g1 = (Gallery) context.createAndRegisterNewObject("Gallery");
        g1.addToPaintingArray(p1);
        assertTrue(DataRowUtils.isToOneTargetModified(toGallery, p1, diff));
    }
}
