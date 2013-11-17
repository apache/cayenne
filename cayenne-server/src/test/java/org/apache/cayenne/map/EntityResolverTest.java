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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.MockQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class EntityResolverTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private DataContext context;

    public void testGetObjEntity() {
        EntityResolver resolver = new EntityResolver(runtime.getDataDomain().getDataMaps());
        assertIsArtistObjEntity(resolver.getObjEntity("Artist"));
    }

    public void testLookupObjEntityByClass() {
        EntityResolver resolver = new EntityResolver(runtime.getDataDomain().getDataMaps());
        assertIsArtistObjEntity(resolver.getObjEntity(Artist.class));
    }

    public void testLookupObjEntityByInstance() {
        EntityResolver resolver = new EntityResolver(runtime.getDataDomain().getDataMaps());
        assertIsArtistObjEntity(resolver.getObjEntity(new Artist()));
    }

    public void testLookupObjEntityByDataobject() {
        EntityResolver resolver = new EntityResolver(runtime.getDataDomain().getDataMaps());
        Artist artist = (Artist) context.newObject("Artist");
        assertIsArtistObjEntity(resolver.getObjEntity(artist));
    }

    public void testGetDataMapList() {
        DataMap m1 = new DataMap();
        DataMap m2 = new DataMap();
        List list = new ArrayList();
        list.add(m1);
        list.add(m2);

        EntityResolver resolver = new EntityResolver(list);
        Collection<?> maps = resolver.getDataMaps();
        assertNotNull(maps);
        assertEquals(2, maps.size());
        assertTrue(maps.containsAll(list));
    }

    public void testAddDataMap() {

        // create empty resolver
        EntityResolver resolver = new EntityResolver();
        assertEquals(0, resolver.getDataMaps().size());
        assertNull(resolver.getObjEntity(Object.class));

        DataMap m1 = new DataMap();
        ObjEntity oe1 = new ObjEntity("test");
        oe1.setClassName(Object.class.getName());
        m1.addObjEntity(oe1);

        resolver.addDataMap(m1);

        assertEquals(1, resolver.getDataMaps().size());
        assertSame(oe1, resolver.getObjEntity(Object.class));
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
        assertSame(oe1, resolver.getObjEntity(Object.class));

        resolver.removeDataMap(m1);

        assertEquals(0, resolver.getDataMaps().size());
        assertNull(resolver.getObjEntity(Object.class));
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

        assertSame(oe1, resolver.getObjEntity(Object.class));

        ObjEntity oe2 = new ObjEntity("test2");
        oe2.setClassName(String.class.getName());
        m1.addObjEntity(oe2);

        assertSame(oe2, resolver.getObjEntity(String.class));
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

    private void assertIsArtistObjEntity(ObjEntity ae) {
        assertNotNull(ae);
        assertEquals(ae, getObjEntity("Artist"));
    }

    private ObjEntity getObjEntity(String objEntityName) {
        for (DataMap map : runtime.getDataDomain().getDataMaps()) {
            for (ObjEntity e : map.getObjEntities()) {
                if (objEntityName.equals(e.getName())) {
                    return e;
                }
            }
        }

        throw new CayenneRuntimeException("No ObjEntity found: " + objEntityName);
    }
}
