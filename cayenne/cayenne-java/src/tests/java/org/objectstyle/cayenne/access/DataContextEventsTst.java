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

import java.sql.Date;

import org.objectstyle.art.oneway.Artist;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.dba.mysql.MySQLAdapter;
import org.objectstyle.cayenne.unit.OneWayMappingTestCase;

/**
 * @author Holger Hoffstaette
 * @author Andrei Adamchik
 */
public class DataContextEventsTst extends OneWayMappingTestCase {
    protected DataContext context;
    protected Artist artist;

    protected void setUp() throws Exception {
        super.setUp();
        
        deleteTestData();
        context = getDomain().createDataContext();
        
        context.setTransactionEventsEnabled(true);
        artist = (Artist) context.createAndRegisterNewObject("Artist");
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
            .getClass()
            == MySQLAdapter.class) {
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

    // tests that no notifications are sent to objects that won't be updated/inserted into database
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
