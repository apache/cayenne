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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.ArtistExhibit;
import org.objectstyle.art.Exhibit;
import org.objectstyle.art.Gallery;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * Testing chained prefetches...
 * 
 * @author Andrei Adamchik
 */
public class DataContextPrefetchMultistepTst extends DataContextTestBase {

    protected void setUp() throws Exception {
        super.setUp();

        createTestData("testGalleries");
        populateExhibits();
        createTestData("testArtistExhibits");
    }

    public void testToManyToManyFirstStepUnresolved() throws Exception {

        // Check the target ArtistExhibit objects do not exist yet

        Map id1 = new HashMap();
        id1.put("ARTIST_ID", new Integer(33001));
        id1.put("EXHIBIT_ID", new Integer(2));
        ObjectId oid1 = new ObjectId(ArtistExhibit.class, id1);

        Map id2 = new HashMap();
        id2.put("ARTIST_ID", new Integer(33003));
        id2.put("EXHIBIT_ID", new Integer(2));
        ObjectId oid2 = new ObjectId(ArtistExhibit.class, id2);

        assertNull(context.getObjectStore().getObject(oid1));
        assertNull(context.getObjectStore().getObject(oid2));

        Expression e = Expression.fromString("galleryName = $name");
        SelectQuery q =
            new SelectQuery(
                Gallery.class,
                e.expWithParameters(Collections.singletonMap("name", "gallery2")));
        q.addPrefetch("exhibitArray.artistExhibitArray");

        List galleries = context.performQuery(q);
        assertEquals(1, galleries.size());

        Gallery g2 = (Gallery) galleries.get(0);

        // this relationship wasn't explicitly prefetched....
        assertTrue(g2.readPropertyDirectly("exhibitArray") instanceof Fault);

        // however the target objects must be resolved
        ArtistExhibit ae1 = (ArtistExhibit) context.getObjectStore().getObject(oid1);
        ArtistExhibit ae2 = (ArtistExhibit) context.getObjectStore().getObject(oid2);
        assertNotNull(ae1);
        assertNotNull(ae2);
        assertEquals(PersistenceState.COMMITTED, ae1.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, ae2.getPersistenceState());
    }

    public void testToManyToManyFirstStepResolved() throws Exception {

        Expression e = Expression.fromString("galleryName = $name");
        SelectQuery q =
            new SelectQuery(
                Gallery.class,
                e.expWithParameters(Collections.singletonMap("name", "gallery2")));
        q.addPrefetch("exhibitArray");
        q.addPrefetch("exhibitArray.artistExhibitArray");

        List galleries = context.performQuery(q);
        assertEquals(1, galleries.size());

        Gallery g2 = (Gallery) galleries.get(0);

        // this relationship should be resolved
        assertTrue(g2.readPropertyDirectly("exhibitArray") instanceof ToManyList);
        ToManyList exhibits = (ToManyList) g2.readPropertyDirectly("exhibitArray");
        assertFalse(exhibits.needsFetch());
        assertEquals(1, exhibits.size());

        Exhibit e1 = (Exhibit) exhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, e1.getPersistenceState());

        // this to-many must also be resolved
        assertTrue(e1.readPropertyDirectly("artistExhibitArray") instanceof ToManyList);
        ToManyList aexhibits = (ToManyList) e1.readPropertyDirectly("artistExhibitArray");
        assertFalse(aexhibits.needsFetch());
        assertEquals(1, exhibits.size());

        ArtistExhibit ae1 = (ArtistExhibit) aexhibits.get(0);
        assertEquals(PersistenceState.COMMITTED, ae1.getPersistenceState());
    }
}
