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
package org.objectstyle.cayenne;

import java.util.List;

import org.objectstyle.art.oneway.Artist;
import org.objectstyle.art.oneway.Painting;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.OneWayMappingTestCase;

/**
 * @author Andrei Adamchik
 */
public class OneWayOneToManyTst extends OneWayMappingTestCase {
    protected DataContext ctxt;

    protected void setUp() throws Exception {
        deleteTestData();
        ctxt = getDomain().createDataContext();
    }

    public void testReadList() throws Exception {
        createTestData("testReadList");

        Artist a2 = fetchArtist();
        assertNotNull(a2);
        assertEquals(2, a2.getPaintingArray().size());
    }

    // ALL THE COMMENTED OUT TESTS CURRENTLY FAIL

    /*    public void testAddNew() throws Exception {
            // create a painting that will be saved 
            // without ARTIST attached to it
            newPainting("p12");
    
            Artist a1 = newArtist();
            ctxt.commitChanges();
    
            Painting p11 = newPainting("p11");
    
            // **** TESTING THIS *****
            a1.addToPaintingArray(p11);
    
            assertEquals(1, a1.getPaintingArray().size());
            assertEquals(1, ctxt.newObjects().size());
            assertEquals(1, ctxt.modifiedObjects().size());
            ctxt.commitChanges();
    
            // reset context and do a refetch
            ctxt = createDataContext();
            Artist a2 = fetchArtist();
            assertNotNull(a2);
            assertEquals(1, a2.getPaintingArray().size());
        }
    
        public void testAddExisting() throws Exception {
            // prepare and save a gallery
            Painting p11 = newPainting("p11");
            newPainting("p12");
            Artist a1 = newArtist();
            ctxt.commitChanges();
    
            // **** TESTING THIS *****
            a1.addToPaintingArray(p11);
    
            assertEquals(1, a1.getPaintingArray().size());
            assertEquals(
                "Both artist and painting should be modified.",
                2,
                ctxt.modifiedObjects().size());
            ctxt.commitChanges();
        }
    
        public void testRevertModification() throws Exception {
            // prepare and save a gallery
            Painting p11 = newPainting("p11");
            Painting p12 = newPainting("p12");
            ctxt.commitChanges();
    
            Artist a1 = newArtist();
            a1.addToPaintingArray(p11);
    
            // test before save
            assertEquals(1, a1.getPaintingArray().size());
            assertEquals(1, ctxt.newObjects().size());
            assertEquals(1, ctxt.modifiedObjects().size());
            ctxt.commitChanges();
    
            a1.addToPaintingArray(p12);
            assertEquals(2, a1.getPaintingArray().size());
            ctxt.rollbackChanges();
    
            assertEquals(1, a1.getPaintingArray().size());
            assertEquals(p11, a1.getPaintingArray().get(0));
            assertFalse(ctxt.hasChanges());
        } */

    protected Painting newPainting(String name) {
        Painting p1 = (Painting) ctxt.createAndRegisterNewObject("Painting");
        p1.setPaintingTitle(name);
        return p1;
    }

    protected Artist newArtist() {
        Artist a1 = (Artist) ctxt.createAndRegisterNewObject("Artist");
        a1.setArtistName(CayenneDOTestBase.artistName);
        return a1;
    }

    protected Artist fetchArtist() {
        SelectQuery q =
            new SelectQuery(
                "Artist",
                ExpressionFactory.matchExp("artistName", CayenneDOTestBase.artistName));
        List ats = ctxt.performQuery(q);
        return (ats.size() > 0) ? (Artist) ats.get(0) : null;
    }
}
