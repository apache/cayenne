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

import java.util.Date;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.unit.MultiContextTestCase;
import org.objectstyle.cayenne.unit.util.ThreadedTestHelper;

/**
 * @author Andrei Adamchik
 */
public class DataContextDelegateSharedCacheTst extends MultiContextTestCase {

    protected Artist artist;

    protected void setUp() throws Exception {
        super.setUp();

        DataContext context = createDataContextWithSharedCache();

        // prepare a single artist record
        artist = (Artist) context.createAndRegisterNewObject("Artist");
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

        // two contexts being tested
        DataContext context = artist.getDataContext();
        DataContext altContext = mirrorDataContext(context);

        final boolean[] methodInvoked = new boolean[1];
        DataContextDelegate delegate = new DefaultDataContextDelegate() {

            public boolean shouldMergeChanges(DataObject object, DataRow snapshotInStore) {
                methodInvoked[0] = true;
                return true;
            }
        };
        altContext.setDelegate(delegate);

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = (Artist) altContext.getObjectStore().getObject(
                artist.getObjectId());
        assertNotNull(altArtist);
        assertFalse(altArtist == artist);
        assertEquals(artist.getArtistName(), altArtist.getArtistName());
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());

        // Update and save artist in peer context
        artist.setArtistName("version2");
        context.commitChanges();

        // assert that delegate was consulted when an object store
        // was refreshed
        ThreadedTestHelper helper = new ThreadedTestHelper() {

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

        // two contexts being tested
        DataContext context = artist.getDataContext();
        DataContext altContext = mirrorDataContext(context);

        DataContextDelegate delegate = new DefaultDataContextDelegate() {

            public boolean shouldMergeChanges(DataObject object, DataRow snapshotInStore) {
                return false;
            }
        };
        altContext.setDelegate(delegate);

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = (Artist) altContext.getObjectStore().getObject(
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
        // two contexts being tested
        DataContext context = artist.getDataContext();
        DataContext altContext = mirrorDataContext(context);

        final boolean[] methodInvoked = new boolean[1];
        DataContextDelegate delegate = new DefaultDataContextDelegate() {

            public boolean shouldProcessDelete(DataObject object) {
                methodInvoked[0] = true;
                return true;
            }
        };
        altContext.setDelegate(delegate);

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = (Artist) altContext.getObjectStore().getObject(
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
        // two contexts being tested
        DataContext context = artist.getDataContext();
        DataContext altContext = mirrorDataContext(context);

        final boolean[] methodInvoked = new boolean[1];
        DataContextDelegate delegate = new DefaultDataContextDelegate() {

            public boolean shouldProcessDelete(DataObject object) {
                methodInvoked[0] = true;
                return false;
            }
        };
        altContext.setDelegate(delegate);

        // make sure we have a fully resolved copy of an artist object
        // in the second context
        Artist altArtist = (Artist) altContext.getObjectStore().getObject(
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

            protected void assertResult() throws Exception {
                assertTrue("Delegate was not consulted", methodInvoked[0]);
            }
        };
        helper.assertWithTimeout(3000);
        assertEquals(PersistenceState.COMMITTED, altArtist.getPersistenceState());
        assertNotNull(altArtist.getDataContext());
    }

    /**
     * Test case to prove that delegate method is invoked on an unsuccessful fault
     * resolution.
     * 
     * @throws Exception
     */
    public void testShouldProcessDeleteOnResolveFault() throws Exception {
        // two contexts being tested
        DataContext context = artist.getDataContext();

        final boolean[] methodInvoked = new boolean[1];
        DataContextDelegate delegate = new DefaultDataContextDelegate() {

            public boolean shouldProcessDelete(DataObject object) {
                methodInvoked[0] = true;
                return true;
            }
        };
        context.setDelegate(delegate);

        // create a fault for artist with a non-existing id
        ObjectId fakeID = new ObjectId(Artist.class, Artist.ARTIST_ID_PK_COLUMN, -10);
        Artist noSuchArtist = (Artist) context.registeredObject(fakeID);
        assertEquals(PersistenceState.HOLLOW, noSuchArtist.getPersistenceState());

        // attempt to resolve

        try {
            noSuchArtist.resolveFault();
        }
        catch (CayenneRuntimeException ex) {
            // expected, as fault resolving failed...
        }

        assertTrue("Delegate was not consulted", methodInvoked[0]);
        assertEquals(PersistenceState.TRANSIENT, noSuchArtist.getPersistenceState());
    }
}