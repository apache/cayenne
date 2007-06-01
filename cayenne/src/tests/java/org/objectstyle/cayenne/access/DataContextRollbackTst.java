/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * @author Andrei Adamchik
 */
public class DataContextRollbackTst extends DataContextTestBase {

    public void testRollbackNew() {
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName("a");

        Painting p1 = (Painting) context.createAndRegisterNewObject("Painting");
        p1.setPaintingTitle("p1");
        p1.setToArtist(artist);

        Painting p2 = (Painting) context.createAndRegisterNewObject("Painting");
        p2.setPaintingTitle("p2");
        p2.setToArtist(artist);

        Painting p3 = (Painting) context.createAndRegisterNewObject("Painting");
        p3.setPaintingTitle("p3");
        p3.setToArtist(artist);

        // before:
        assertEquals(artist, p1.getToArtist());
        assertEquals(3, artist.getPaintingArray().size());

        context.rollbackChanges();

        // after: 
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());

        // TODO: should we expect relationships to be unset?
        // assertNull(p1.getToArtist());
        // assertEquals(0, artist.getPaintingArray().size());
    }

    public void testRollbackNewObject() {
        String artistName = "revertTestArtist";
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName(artistName);

        context.rollbackChanges();

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        context.commitChanges();
        //The commit should have made no changes, so
        //perform a fetch to ensure that this artist hasn't been persisted to the db

        DataContext freshContext = createDataContext();
        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(ExpressionFactory.matchExp("artistName", artistName));
        List queryResults = freshContext.performQuery(query);

        assertEquals(0, queryResults.size());
    }

    //Catches a bug where new objects were unregistered within an object iterator, thus modifying the
    // collection the iterator was iterating over (ConcurrentModificationException)
    public void testRollbackWithMultipleNewObjects() {
        String artistName = "rollbackTestArtist";
        String paintingTitle = "rollbackTestPainting";
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName(artistName);

        Painting painting = (Painting) context.createAndRegisterNewObject("Painting");
        painting.setPaintingTitle(paintingTitle);
        painting.setToArtist(artist);

        try {
            context.rollbackChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(
                "rollbackChanges should not have caused the exception " + e.getMessage());
        }

        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        context.commitChanges();
        //The commit should have made no changes, so
        //perform a fetch to ensure that this artist hasn't been persisted to the db

        DataContext freshContext = createDataContext();
        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(ExpressionFactory.matchExp("artistName", artistName));
        List queryResults = freshContext.performQuery(query);

        assertEquals(0, queryResults.size());
    }

    public void testRollbackRelationshipModification() {
        String artistName = "relationshipModArtist";
        String paintingTitle = "relationshipTestPainting";
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName(artistName);
        Painting painting = (Painting) context.createAndRegisterNewObject("Painting");
        painting.setPaintingTitle(paintingTitle);
        painting.setToArtist(artist);
        context.commitChanges();

        painting.setToArtist(null);
        assertEquals(0, artist.getPaintingArray().size());
        context.rollbackChanges();

        assertTrue(((ToManyList) artist.getPaintingArray()).needsFetch());
        assertEquals(1, artist.getPaintingArray().size());
        assertEquals(artist, painting.getToArtist());

        //Check that the reverse relationship was handled
        assertEquals(1, artist.getPaintingArray().size());
        context.commitChanges();

        DataContext freshContext = createDataContext();
        SelectQuery query = new SelectQuery(Painting.class);
        query.setQualifier(ExpressionFactory.matchExp("paintingTitle", paintingTitle));
        List queryResults = freshContext.performQuery(query);

        assertEquals(1, queryResults.size());
        Painting queriedPainting = (Painting) queryResults.get(0);

        //NB:  This is an easier comparison than manually fetching artist
        assertEquals(artistName, queriedPainting.getToArtist().getArtistName());
    }

    public void testRollbackDeletedObject() {
        String artistName = "deleteTestArtist";
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName(artistName);
        context.commitChanges();
        //Save... cayenne doesn't yet handle deleting objects that are uncommitted
        context.deleteObject(artist);
        context.rollbackChanges();

        // Now check everything is as it should be
        assertEquals(PersistenceState.HOLLOW, artist.getPersistenceState());

        context.commitChanges();
        //The commit should have made no changes, so
        //perform a fetch to ensure that this artist hasn't been deleted from the db

        DataContext freshContext = createDataContext();
        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(ExpressionFactory.matchExp("artistName", artistName));
        List queryResults = freshContext.performQuery(query);

        assertEquals(1, queryResults.size());
    }

    public void testRollbackModifiedObject() {
        String artistName = "initialTestArtist";
        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName(artistName);
        context.commitChanges();

        artist.setArtistName("a new value");

        context.rollbackChanges();

        //Make sure the inmemory changes have been rolled back
        assertEquals(artistName, artist.getArtistName());

        //Commit what's in memory...
        context.commitChanges();

        //.. and ensure that the correct data is in the db
        DataContext freshContext = createDataContext();
        SelectQuery query = new SelectQuery(Artist.class);
        query.setQualifier(ExpressionFactory.matchExp("artistName", artistName));
        List queryResults = freshContext.performQuery(query);

        assertEquals(1, queryResults.size());
    }
}
