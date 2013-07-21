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

import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.parallel.ParallelTestContainer;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextDelegateSharedCacheTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DataContext context1;

    private Artist artist;

    @Override
    protected void setUpAfterInjection() throws Exception {

        // prepare a single artist record
        artist = (Artist) context.newObject("Artist");
        artist.setArtistName("version1");
        artist.setDateOfBirth(new Date());
        context.commitChanges();
    }

    /**
     * Test case to prove that delegate method is invoked on external change of object in
     * the store.
     */
    public void testShouldMergeChanges() throws Exception {

        final boolean[] methodInvoked = new boolean[1];
        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public boolean shouldMergeChanges(DataObject object, DataRow snapshotInStore) {
                methodInvoked[0] = true;
                return true;
            }
        };

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = context1.localObject(artist);
        assertNotNull(altArtist);
        assertNotSame(altArtist, artist);
        assertEquals(artist.getArtistName(), altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        context1.setDelegate(delegate);

        // Update and save artist in peer context
        artist.setArtistName("version2");
        context.commitChanges();

        // assert that delegate was consulted when an object store
        // was refreshed
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertTrue("Delegate was not consulted", methodInvoked[0]);
            }
        };
        helper.runTest(3000);
    }

    /**
     * Test case to prove that delegate method can block changes made by ObjectStore.
     * 
     * @throws Exception
     */
    public void testBlockedShouldMergeChanges() throws Exception {
        String oldName = artist.getArtistName();

        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public boolean shouldMergeChanges(DataObject object, DataRow snapshotInStore) {
                return false;
            }
        };
        context1.setDelegate(delegate);

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = context1.localObject(artist);
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

        final boolean[] methodInvoked = new boolean[1];
        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public boolean shouldProcessDelete(DataObject object) {
                methodInvoked[0] = true;
                return true;
            }
        };
        context1.setDelegate(delegate);

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = context1.localObject(artist);
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(artist.getArtistName(), altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update and save artist in peer context
        context.deleteObjects(artist);
        context.commitChanges();

        // assert that delegate was consulted when an object store
        // was refreshed
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertTrue("Delegate was not consulted", methodInvoked[0]);
            }
        };
        helper.runTest(3000);
    }

    /**
     * Test case to prove that delegate method is invoked on external change of object in
     * the store, and is able to block further object processing.
     * 
     * @throws Exception
     */
    public void testBlockShouldProcessDeleteOnExternalChange() throws Exception {

        final boolean[] methodInvoked = new boolean[1];
        DataContextDelegate delegate = new MockDataContextDelegate() {

            @Override
            public boolean shouldProcessDelete(DataObject object) {
                methodInvoked[0] = true;
                return false;
            }
        };
        context1.setDelegate(delegate);

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = context1.localObject(artist);
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(artist.getArtistName(), altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update and save artist in peer context
        context.deleteObjects(artist);
        context.commitChanges();

        // assert that delegate was consulted when an object store
        // was refreshed, and actually blocked object expulsion
        ParallelTestContainer helper = new ParallelTestContainer() {

            @Override
            protected void assertResult() throws Exception {
                assertTrue("Delegate was not consulted", methodInvoked[0]);
            }
        };
        helper.runTest(3000);
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());
        assertNotNull(altArtist.getObjectContext());
    }
}
