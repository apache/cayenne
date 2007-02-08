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

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class NestedDataContextPeerEventsTst extends CayenneTestCase {

    public void testPeerObjectUpdatedTempOID() {
        DataContext context = createDataContext();

        DataContext peer1 = context.createChildDataContext();
        Artist a1 = (Artist) peer1.newObject(Artist.class);
        a1.setArtistName("Y");
        ObjectId a1TempId = a1.getObjectId();

        DataContext peer2 = context.createChildDataContext();
        Artist a2 = (Artist) peer2.localObject(a1TempId, a1);

        assertEquals(a1TempId, a2.getObjectId());

        peer1.commitChanges();
        assertFalse(a1.getObjectId().isTemporary());
        assertFalse(a2.getObjectId().isTemporary());
        assertEquals(a2.getObjectId(), a1.getObjectId());
    }

    public void testPeerObjectUpdatedSimpleProperty() {
        DataContext context = createDataContext();

        Artist a = (Artist) context.newObject(Artist.class);
        a.setArtistName("X");
        context.commitChanges();

        DataContext peer1 = context.createChildDataContext();
        Artist a1 = (Artist) peer1.localObject(a.getObjectId(), a);

        DataContext peer2 = context.createChildDataContext();
        Artist a2 = (Artist) peer2.localObject(a.getObjectId(), a);

        a1.setArtistName("Y");
        assertEquals("X", a2.getArtistName());
        peer1.commitChangesToParent();
        assertEquals("Y", a2.getArtistName());

        assertFalse("Peer data context became dirty on event processing", peer2
                .hasChanges());
    }

    public void testPeerObjectUpdatedToOneRelationship() {

        DataContext context = createDataContext();

        Artist a = (Artist) context.newObject(Artist.class);
        Artist altA = (Artist) context.newObject(Artist.class);

        Painting p = (Painting) context.newObject(Painting.class);
        p.setToArtist(a);
        p.setPaintingTitle("PPP");
        a.setArtistName("X");
        altA.setArtistName("Y");
        context.commitChanges();

        DataContext peer1 = context.createChildDataContext();
        Painting p1 = (Painting) peer1.localObject(p.getObjectId(), p);
        Artist altA1 = (Artist) peer1.localObject(altA.getObjectId(), altA);

        DataContext peer2 = context.createChildDataContext();
        Painting p2 = (Painting) peer2.localObject(p.getObjectId(), p);
        Artist altA2 = (Artist) peer2.localObject(altA.getObjectId(), altA);
        Artist a2 = (Artist) peer2.localObject(a.getObjectId(), a);

        p1.setToArtist(altA1);
        assertSame(a2, p2.getToArtist());
        peer1.commitChangesToParent();
        assertEquals(altA2, p2.getToArtist());

        assertFalse("Peer data context became dirty on event processing", peer2
                .hasChanges());
    }

    public void testPeerObjectUpdatedToManyRelationship() {

        DataContext context = createDataContext();

        Artist a = (Artist) context.newObject(Artist.class);
        a.setArtistName("X");

        Painting px = (Painting) context.newObject(Painting.class);
        px.setToArtist(a);
        px.setPaintingTitle("PX");

        Painting py = (Painting) context.newObject(Painting.class);
        py.setPaintingTitle("PY");

        context.commitChanges();

        DataContext peer1 = context.createChildDataContext();
        Painting py1 = (Painting) peer1.localObject(py.getObjectId(), py);
        Artist a1 = (Artist) peer1.localObject(a.getObjectId(), a);

        DataContext peer2 = context.createChildDataContext();
        Painting py2 = (Painting) peer2.localObject(py.getObjectId(), py);
        Artist a2 = (Artist) peer2.localObject(a.getObjectId(), a);

        a1.addToPaintingArray(py1);
        assertEquals(1, a2.getPaintingArray().size());
        assertFalse(a2.getPaintingArray().contains(py2));
        peer1.commitChangesToParent();
        assertEquals(2, a2.getPaintingArray().size());
        assertTrue(a2.getPaintingArray().contains(py2));

        assertFalse("Peer data context became dirty on event processing", peer2
                .hasChanges());
    }
}