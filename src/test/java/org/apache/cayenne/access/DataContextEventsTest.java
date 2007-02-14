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

package org.apache.cayenne.access;

import java.sql.Date;

import org.apache.art.oneway.Artist;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.LifecycleListener;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.unit.OneWayMappingCase;

/**
 * @author Holger Hoffstaette
 * @author Andrus Adamchik
 * @deprecated since 3.0M1 in favor of {@link LifecycleListener}. Will be removed in
 *             later 3.0 milestones.
 */
public class DataContextEventsTest extends OneWayMappingCase {

    protected DataContext context;
    protected Artist artist;

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = getDomain().createDataContext();

        context.setTransactionEventsEnabled(true);
        artist = (Artist) context.newObject("Artist");
        artist.setArtistName("artist1");
        context.commitChanges();
        artist.resetEvents();
    }

    public void testDataContext() throws Exception {
        assertTrue(context.isTransactionEventsEnabled());

        assertFalse(context.hasChanges());
        assertFalse(artist.receivedWillCommit());
        assertFalse(artist.receivedDidCommit());

        // modify artist
        artist.setDateOfBirth(new Date(System.currentTimeMillis()));

        // commit the pending changes
        context.commitChanges();

        assertTrue(artist.receivedWillCommit());
        assertTrue(artist.receivedDidCommit());
    }

    public void testDataContextRolledBackTransaction() throws Exception {
        // This test will not work on MySQL, since transaction support
        // is either non-existent or dubious (depending on your view).
        // See: http://www.mysql.com/doc/en/Design_Limitations.html
        if (((DataNode) getDomain().getDataNodes().iterator().next())
                .getAdapter()
                .getClass() == MySQLAdapter.class) {
            return;
        }

        // turn off cayenne validation
        context.setValidatingObjectsOnCommit(false);

        assertFalse(context.hasChanges());
        assertFalse(artist.receivedWillCommit());
        assertFalse(artist.receivedDidCommit());

        // modify artist so that it cannot be saved correctly anymore
        artist.setArtistName(null); // name is mandatory

        try {
            // commit the pending changes
            context.commitChanges();
            fail("No exception on saving invalid data.");
        }
        catch (CayenneRuntimeException ex) {
            // expected
        }

        assertTrue(artist.receivedWillCommit());
        assertFalse(artist.receivedDidCommit());
    }

    // tests that no notifications are sent to objects that won't be updated/inserted into
    // database
    public void testDataContextNoModifications() {
        assertFalse(context.hasChanges());
        assertFalse(artist.receivedWillCommit());
        assertFalse(artist.receivedDidCommit());

        // commit without any pending changes
        context.commitChanges();

        assertFalse(artist.receivedDidCommit());
        assertFalse(artist.receivedWillCommit());
    }
}
