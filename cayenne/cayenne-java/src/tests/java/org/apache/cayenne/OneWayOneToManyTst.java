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

package org.apache.cayenne;

import java.util.List;

import org.apache.art.oneway.Artist;
import org.apache.art.oneway.Painting;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.OneWayMappingTestCase;

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

    /*
    public void testAddNew() throws Exception {
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
        assertEquals("Both artist and painting should be modified.", 2, ctxt
                .modifiedObjects()
                .size());
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
    }*/

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
        SelectQuery q = new SelectQuery("Artist", ExpressionFactory.matchExp(
                "artistName",
                CayenneDOTestBase.artistName));
        List ats = ctxt.performQuery(q);
        return (ats.size() > 0) ? (Artist) ats.get(0) : null;
    }
}
