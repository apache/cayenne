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

package org.objectstyle.cayenne.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.query.MockQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.MtTable1;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

public class EntityResolverTst extends CayenneTestCase {

    public void testObjEntityLookupDuplicates() {
        AccessStack stack = CayenneTestResources.getResources().getAccessStack(
                "GenericStack");

        DataMap generic = stack.getDataDomain().getMap("generic");
        EntityResolver resolver = new EntityResolver(Collections.singleton(generic));

        ObjEntity g1 = resolver.lookupObjEntity("Generic1");
        assertNotNull(g1);

        ObjEntity g2 = resolver.lookupObjEntity("Generic2");
        assertNotNull(g2);

        assertNotSame(g1, g2);
        assertNull(resolver.lookupObjEntity(Object.class));

        try {
            resolver.lookupObjEntity(CayenneDataObject.class);
            fail("two entities mapped to the same class... resolver must have thrown.");
        }
        catch (CayenneRuntimeException e) {
            // expected
        }
    }

    public void testDbEntityLookupDuplicates() {
        AccessStack stack = CayenneTestResources.getResources().getAccessStack(
                "GenericStack");

        DataMap generic = stack.getDataDomain().getMap("generic");
        EntityResolver resolver = new EntityResolver(Collections.singleton(generic));

        assertNull(resolver.lookupObjEntity(Object.class));

        try {
            resolver.lookupDbEntity(CayenneDataObject.class);
            fail("two entities mapped to the same class... resolver must have thrown.");
        }
        catch (CayenneRuntimeException e) {
            // expected
        }
    }

    public void testGetClientEntityResolver() {

        AccessStack stack = CayenneTestResources.getResources().getAccessStack(
                MULTI_TIER_ACCESS_STACK);

        EntityResolver resolver = new EntityResolver(stack.getDataDomain().getDataMaps());
        EntityResolver clientResolver = resolver.getClientEntityResolver();
        assertNotNull(clientResolver);

        // make sure that client entities got translated properly...

        try {
            assertNotNull(clientResolver.lookupObjEntity("MtTable1"));
        }
        catch (CayenneRuntimeException e) {
            fail("'MtTable1' entity is not mapped. All entities: "
                    + clientResolver.getObjEntities());
        }

        assertNotNull(clientResolver.lookupObjEntity(ClientMtTable1.class));
        assertNull(clientResolver.lookupObjEntity(MtTable1.class));
    }

    // //Test DbEntitylookups

    public void testLookupDbEntityByClass() {
        EntityResolver resolver = new EntityResolver(getDomain().getDataMaps());
        assertIsArtistDbEntity(resolver.lookupDbEntity(Artist.class));
    }

    public void testLookupDbEntityByDataobject() throws Exception {
        EntityResolver resolver = new EntityResolver(getDomain().getDataMaps());
        Artist artist = (Artist) this.createDataContext().createAndRegisterNewObject(
                "Artist");
        assertIsArtistDbEntity(resolver.lookupDbEntity(artist));
    }

    // //Test ObjEntity lookups

    public void testGetObjEntity() {
        EntityResolver resolver = new EntityResolver(getDomain().getDataMaps());
        assertIsArtistObjEntity(resolver.getObjEntity("Artist"));
    }

    public void testLookupObjEntityByClass() {
        EntityResolver resolver = new EntityResolver(getDomain().getDataMaps());
        assertIsArtistObjEntity(resolver.lookupObjEntity(Artist.class));
    }

    public void testLookupObjEntityByInstance() {
        EntityResolver resolver = new EntityResolver(getDomain().getDataMaps());
        assertIsArtistObjEntity(resolver.lookupObjEntity(new Artist()));
    }

    public void testLookupObjEntityByDataobject() {
        EntityResolver resolver = new EntityResolver(getDomain().getDataMaps());
        Artist artist = (Artist) this.createDataContext().createAndRegisterNewObject(
                "Artist");
        assertIsArtistObjEntity(resolver.lookupObjEntity(artist));
    }

    public void testGetDataMapList() {
        DataMap m1 = new DataMap();
        DataMap m2 = new DataMap();
        List list = new ArrayList();
        list.add(m1);
        list.add(m2);

        EntityResolver resolver = new EntityResolver(list);
        Collection maps = resolver.getDataMaps();
        assertNotNull(maps);
        assertEquals(2, maps.size());
        assertTrue(maps.containsAll(list));
    }

    public void testAddDataMap() {

        // create empty resolver
        EntityResolver resolver = new EntityResolver();
        assertEquals(0, resolver.getDataMaps().size());
        assertNull(resolver.lookupObjEntity(Object.class));

        DataMap m1 = new DataMap();
        ObjEntity oe1 = new ObjEntity("test");
        oe1.setClassName(Object.class.getName());
        m1.addObjEntity(oe1);

        resolver.addDataMap(m1);

        assertEquals(1, resolver.getDataMaps().size());
        assertSame(oe1, resolver.lookupObjEntity(Object.class));
        assertEquals(resolver, m1.getNamespace());
    }

    public void testRemoveDataMap() {
        // create a resolver with a single map
        DataMap m1 = new DataMap();
        ObjEntity oe1 = new ObjEntity("test");
        oe1.setClassName(Object.class.getName());
        m1.addObjEntity(oe1);
        List list = new ArrayList();
        list.add(m1);
        EntityResolver resolver = new EntityResolver(list);

        assertEquals(1, resolver.getDataMaps().size());
        assertSame(oe1, resolver.lookupObjEntity(Object.class));

        resolver.removeDataMap(m1);

        assertEquals(0, resolver.getDataMaps().size());
        assertNull(resolver.lookupObjEntity(Object.class));
    }

    public void testAddObjEntity() {
        // create a resolver with a single map
        DataMap m1 = new DataMap();
        ObjEntity oe1 = new ObjEntity("test1");
        oe1.setClassName(Object.class.getName());
        m1.addObjEntity(oe1);
        List list = new ArrayList();
        list.add(m1);
        EntityResolver resolver = new EntityResolver(list);

        assertSame(oe1, resolver.lookupObjEntity(Object.class));

        ObjEntity oe2 = new ObjEntity("test2");
        oe2.setClassName(String.class.getName());
        m1.addObjEntity(oe2);

        assertSame(oe2, resolver.lookupObjEntity(String.class));
    }

    public void testGetQuery() {
        // create a resolver with a single map
        DataMap m1 = new DataMap();
        Query q = new MockQuery("query1");
        m1.addQuery(q);

        EntityResolver resolver = new EntityResolver(Collections.singleton(m1));
        assertSame(q, resolver.getQuery("query1"));

        // check that the query added on-the-fly will be recognized
        assertNull(resolver.getQuery("query2"));

        Query q2 = new MockQuery("query2");
        m1.addQuery(q2);
        assertSame(q2, resolver.getQuery("query2"));
    }

    private void assertIsArtistDbEntity(DbEntity ae) {
        assertNotNull(ae);
        assertEquals(ae, getDbEntity("ARTIST"));
    }

    private void assertIsArtistObjEntity(ObjEntity ae) {
        assertNotNull(ae);
        assertEquals(ae, getObjEntity("Artist"));
    }

}
