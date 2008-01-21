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

package org.apache.cayenne.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.art.Artist;
import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.query.MockQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class EntityResolverTest extends CayenneCase {

    public void testObjEntityLookupDuplicates() {
        AccessStack stack = CayenneResources
                .getResources()
                .getAccessStack("GenericStack");

        DataMap generic = stack.getDataDomain().getMap("generic");
        EntityResolver resolver = new EntityResolver(Collections.singleton(generic));

        ObjEntity g1 = resolver.getObjEntity("Generic1");
        assertNotNull(g1);

        ObjEntity g2 = resolver.getObjEntity("Generic2");
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

    /**
     * @deprecated since 3.0
     */
    public void testDbEntityLookupDuplicates() {
        AccessStack stack = CayenneResources
                .getResources()
                .getAccessStack("GenericStack");

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

        AccessStack stack = CayenneResources.getResources().getAccessStack(
                MULTI_TIER_ACCESS_STACK);

        EntityResolver resolver = new EntityResolver(stack.getDataDomain().getDataMaps());
        EntityResolver clientResolver = resolver.getClientEntityResolver();
        assertNotNull(clientResolver);

        // make sure that client entities got translated properly...

        try {
            assertNotNull(clientResolver.getObjEntity("MtTable1"));
        }
        catch (CayenneRuntimeException e) {
            fail("'MtTable1' entity is not mapped. All entities: "
                    + clientResolver.getObjEntities());
        }

        assertNotNull(clientResolver.lookupObjEntity(ClientMtTable1.class));
        assertNull(clientResolver.lookupObjEntity(MtTable1.class));
    }

    /**
     * @deprecated since 3.0
     */
    public void testLookupDbEntityByClass() {
        EntityResolver resolver = new EntityResolver(getDomain().getDataMaps());
        assertIsArtistDbEntity(resolver.lookupDbEntity(Artist.class));
    }

    /**
     * @deprecated since 3.0
     */
    public void testLookupDbEntityByDataobject() throws Exception {
        EntityResolver resolver = new EntityResolver(getDomain().getDataMaps());
        Artist artist = (Artist) this.createDataContext().newObject("Artist");
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
        Artist artist = (Artist) this.createDataContext().newObject("Artist");
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
