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

import java.util.Date;

import org.apache.art.Artist;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.unit.MultiContextCase;
import org.apache.cayenne.unit.util.ThreadedTestHelper;

/**
 */
public class DataContextDelegateSharedCacheTest extends MultiContextCase {

    protected Artist artist;
    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        context = createDataContextWithSharedCache();

        // prepare a single artist record
        artist = (Artist) context.newObject("Artist");
        artist.setArtistName("version1");
        artist.setDateOfBirth(new Date());
        context.commitChanges();
    }

    /**
     * Test case to prove that delegate method is invoked on external change of object in
     * the store.
     * 
     * @throws Exception
     */
    public void testShouldMergeChanges() throws Exception {

        DataContext altContext = mirrorDataContext(context);

        final boolean[] methodInvoked = new boolean[1];
        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public boolean shouldMergeChanges(DataObject object, DataRow snapshotInStore) {
                methodInvoked[0] = true;
                return true;
            }
        };
        altContext.setDelegate(delegate);

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = (Artist) altContext.getGraphManager().getNode(
                artist.getObjectId());
        assertNotNull(altArtist);
        assertNotSame(altArtist, artist);
        assertEquals(artist.getArtistName(), altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update and save artist in peer context
        artist.setArtistName("version2");
        context.commitChanges();

        // assert that delegate was consulted when an object store
        // was refreshed
        ThreadedTestHelper helper = new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                assertTrue("Delegate was not consulted", methodInvoked[0]);
            }
        };
        helper.assertWithTimeout(3000);
    }

    /**
     * Test case to prove that delegate method can block changes made by ObjectStore.
     * 
     * @throws Exception
     */
    public void testBlockedShouldMergeChanges() throws Exception {
        String oldName = artist.getArtistName();

        DataContext altContext = mirrorDataContext(context);

        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public boolean shouldMergeChanges(DataObject object, DataRow snapshotInStore) {
                return false;
            }
        };
        altContext.setDelegate(delegate);

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = (Artist) altContext.getObjectStore().getNode(
                artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(oldName, altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update and save artist in peer context
        artist.setArtistName("version2");
        context.commitChanges();

        // assert that delegate was able to block the merge
        assertEquals(oldName, altArtist.getArtistName());
    }

    /**
     * Test case to prove that delegate method is invoked on external change of object in
     * the store.
     * 
     * @throws Exception
     */
    public void testShouldProcessDeleteOnExternalChange() throws Exception {

        DataContext altContext = mirrorDataContext(context);

        final boolean[] methodInvoked = new boolean[1];
        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public boolean shouldProcessDelete(DataObject object) {
                methodInvoked[0] = true;
                return true;
            }
        };
        altContext.setDelegate(delegate);

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = (Artist) altContext.getGraphManager().getNode(
                artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(artist.getArtistName(), altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update and save artist in peer context
        context.deleteObject(artist);
        context.commitChanges();

        // assert that delegate was consulted when an object store
        // was refreshed
        ThreadedTestHelper helper = new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                assertTrue("Delegate was not consulted", methodInvoked[0]);
            }
        };
        helper.assertWithTimeout(3000);
    }

    /**
     * Test case to prove that delegate method is invoked on external change of object in
     * the store, and is able to block further object processing.
     * 
     * @throws Exception
     */
    public void testBlockShouldProcessDeleteOnExternalChange() throws Exception {

        DataContext altContext = mirrorDataContext(context);

        final boolean[] methodInvoked = new boolean[1];
        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public boolean shouldProcessDelete(DataObject object) {
                methodInvoked[0] = true;
                return false;
            }
        };
        altContext.setDelegate(delegate);

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = (Artist) altContext.getObjectStore().getNode(
                artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(artist.getArtistName(), altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update and save artist in peer context
        context.deleteObject(artist);
        context.commitChanges();

        // assert that delegate was consulted when an object store
        // was refreshed, and actually blocked object expulsion
        ThreadedTestHelper helper = new ThreadedTestHelper() {

            @Override
            protected void assertResult() throws Exception {
                assertTrue("Delegate was not consulted", methodInvoked[0]);
            }
        };
        helper.assertWithTimeout(3000);
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());
        assertNotNull(altArtist.getObjectContext());
    }
}
