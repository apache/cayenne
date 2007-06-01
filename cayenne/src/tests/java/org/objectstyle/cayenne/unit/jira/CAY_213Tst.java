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
package org.objectstyle.cayenne.unit.jira;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.access.ObjectStore;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class CAY_213Tst extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testSnapshotRetainedOnPropertyModification() throws Exception {
        createTestData("test");

        Artist a = (Artist) DataObjectUtils.objectForPK(
                createDataContext(),
                Artist.class,
                2000);
        ObjectStore objectStore = a.getDataContext().getObjectStore();

        assertNull(objectStore.getRetainedSnapshot(a.getObjectId()));

        a.setArtistName("some other name");
        assertNotNull(objectStore.getRetainedSnapshot(a.getObjectId()));
    }

    public void testSnapshotRetainedOnRelAndPropertyModification() throws Exception {
        createTestData("test");

        Artist a = (Artist) DataObjectUtils.objectForPK(
                createDataContext(),
                Artist.class,
                2000);
        ObjectStore objectStore = a.getDataContext().getObjectStore();

        assertNull(objectStore.getRetainedSnapshot(a.getObjectId()));

        // we are trying to reproduce the bug CAY-213 - relationship modification puts
        // object in a modified state, so later when object is really modified, its
        // snapshot is not retained... in testing this I am leaving some flexibility for
        // the framework to retain a snapshot when it deems appropriate...
        
        a.addToPaintingArray((Painting) a.getDataContext().createAndRegisterNewObject(
                Painting.class));
        a.setArtistName("some other name");
        assertNotNull("Snapshot wasn't retained - CAY-213", objectStore
                .getRetainedSnapshot(a.getObjectId()));
    }
}