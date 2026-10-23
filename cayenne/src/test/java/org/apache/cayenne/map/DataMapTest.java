/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.map;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataMap unit tests.
 */
public class DataMapTest {

    @Test
    public void defaultSchema() {
        DataMap map = new DataMap();
        String tstSchema = "tst_schema";
        assertNull(map.getDefaultSchema());
        map.setDefaultSchema(tstSchema);
        assertEquals(tstSchema, map.getDefaultSchema());

        map.setDefaultSchema(null);
        assertNull(map.getDefaultSchema());
    }

    @Test
    public void renameObjEntityUpdatesSelectQueries() {
        DataMap map = new DataMap("map");
        ObjEntity artist = new ObjEntity("Artist");
        map.addObjEntity(artist);
        map.addObjEntity(new ObjEntity("Painting"));

        SelectQueryDescriptor rooted = selectQuery("rooted", "from Artist\nwhere name = $name");
        SelectQueryDescriptor subquery = selectQuery("subquery",
                "from Painting where exists (from Artist where artist = enclosing(self))");
        SelectQueryDescriptor unrelated = selectQuery("unrelated", "from Painting where artist.name = 'Artist'");
        SelectQueryDescriptor invalid = selectQuery("invalid", "from Artist where");
        map.addQueryDescriptor(rooted);
        map.addQueryDescriptor(subquery);
        map.addQueryDescriptor(unrelated);
        map.addQueryDescriptor(invalid);

        map.renameObjEntity(artist, "Painter");

        assertEquals("from Painter\nwhere name = $name", rooted.getQl());
        assertSame(artist, rooted.getRoot());
        assertEquals("from Painting where exists (from Painter where artist = enclosing(self))", subquery.getQl());
        assertEquals("from Painting where artist.name = 'Artist'", unrelated.getQl());

        // a String that can't be parsed is renamed too, as it is kept to be fixed
        assertEquals("from Painter where", invalid.getQl());
    }

    @Test
    public void renameDbEntityUpdatesSelectQueries() {
        DataMap map = new DataMap("map");
        DbEntity table = new DbEntity("ARTIST");
        map.addDbEntity(table);
        map.addObjEntity(new ObjEntity("ARTIST"));

        SelectQueryDescriptor db = selectQuery("db", "from db:ARTIST where NAME = 'a'");
        SelectQueryDescriptor obj = selectQuery("obj", "from ARTIST where NAME = 'a'");
        map.addQueryDescriptor(db);
        map.addQueryDescriptor(obj);

        map.renameDbEntity(table, "PAINTER");

        assertEquals("from db:PAINTER where NAME = 'a'", db.getQl());
        assertSame(table, db.getRoot());
        assertEquals("from ARTIST where NAME = 'a'", obj.getQl());
    }

    @Test
    public void renameObjEntityUpdatesSelectQueriesInOtherMaps() {
        DataMap mapA = new DataMap("a");
        DataMap mapB = new DataMap("b");
        EntityResolver resolver = new EntityResolver();
        resolver.addDataMap(mapA);
        resolver.addDataMap(mapB);

        // the cache built here is stale once the entity is added
        assertNull(resolver.getObjEntity("Artist"));
        ObjEntity artist = new ObjEntity("Artist");
        mapA.addObjEntity(artist);
        mapB.addObjEntity(new ObjEntity("Painting"));

        SelectQueryDescriptor inA = selectQuery("inA", "from Artist");
        SelectQueryDescriptor inB = selectQuery("inB",
                "from Artist where exists (from Painting where artist = enclosing(self))");
        SelectQueryDescriptor subqueryInB = selectQuery("subqueryInB",
                "from Painting where exists (from Artist where artist = enclosing(self))");
        mapA.addQueryDescriptor(inA);
        mapB.addQueryDescriptor(inB);
        mapB.addQueryDescriptor(subqueryInB);

        mapA.renameObjEntity(artist, "Painter");

        assertEquals("from Painter", inA.getQl());
        assertEquals("from Painter where exists (from Painting where artist = enclosing(self))", inB.getQl());
        assertSame(artist, inB.getRoot());
        assertEquals("from Painting where exists (from Painter where artist = enclosing(self))", subqueryInB.getQl());
    }

    @Test
    public void renameDbEntityUpdatesSelectQueriesInOtherMaps() {
        DataMap mapA = new DataMap("a");
        DataMap mapB = new DataMap("b");
        EntityResolver resolver = new EntityResolver();
        resolver.addDataMap(mapA);
        resolver.addDataMap(mapB);

        DbEntity table = new DbEntity("ARTIST");
        mapA.addDbEntity(table);
        mapB.addObjEntity(new ObjEntity("ARTIST"));

        SelectQueryDescriptor db = selectQuery("db", "from db:ARTIST where NAME = 'a'");
        SelectQueryDescriptor obj = selectQuery("obj", "from ARTIST where NAME = 'a'");
        mapB.addQueryDescriptor(db);
        mapB.addQueryDescriptor(obj);

        mapA.renameDbEntity(table, "PAINTER");

        assertEquals("from db:PAINTER where NAME = 'a'", db.getQl());
        assertSame(table, db.getRoot());
        assertEquals("from ARTIST where NAME = 'a'", obj.getQl());
    }

    @Test
    public void removeQueryDescriptorDetachesIt() {
        DataMap map = new DataMap("map");
        SelectQueryDescriptor query = selectQuery("q", "from Artist");
        map.addQueryDescriptor(query);
        assertSame(map, query.getDataMap());

        map.removeQueryDescriptor("q");
        assertNull(query.getDataMap());
        assertNull(map.getQueryDescriptor("q"));
    }

    @Test
    public void defaultPackage() {
        DataMap map = new DataMap();
        String tstPackage = "tst.pkg";
        assertNull(map.getDefaultPackage());
        map.setDefaultPackage(tstPackage);
        assertEquals(tstPackage, map.getDefaultPackage());

        map.setDefaultPackage(null);
        assertNull(map.getDefaultPackage());
    }

    @Test
    public void defaultSuperclass() {
        DataMap map = new DataMap();
        String tstSuperclass = "tst_superclass";
        assertNull(map.getDefaultSuperclass());
        map.setDefaultSuperclass(tstSuperclass);
        assertEquals(tstSuperclass, map.getDefaultSuperclass());

        map.setDefaultSuperclass(null);
        assertNull(map.getDefaultSuperclass());
    }

    @Test
    public void defaultLockType() {
        DataMap map = new DataMap();
        assertEquals(ObjEntity.LOCK_TYPE_NONE, map.getDefaultLockType());
        map.setDefaultLockType(ObjEntity.LOCK_TYPE_OPTIMISTIC);
        assertEquals(ObjEntity.LOCK_TYPE_OPTIMISTIC, map.getDefaultLockType());

        map.setDefaultLockType(ObjEntity.LOCK_TYPE_NONE);
        assertEquals(ObjEntity.LOCK_TYPE_NONE, map.getDefaultLockType());
    }

    @Test
    public void name() {
        DataMap map = new DataMap();
        String tstName = "tst_name";
        map.setName(tstName);
        assertEquals(tstName, map.getName());
    }

    @Test
    public void location() {
        DataMap map = new DataMap();
        String tstName = "tst_name";
        assertNull(map.getLocation());
        map.setLocation(tstName);
        assertEquals(tstName, map.getLocation());
    }

    @Test
    public void addObjEntity() {
        DataMap map = new DataMap();
        ObjEntity e = new ObjEntity("b");
        e.setClassName("b");
        map.addObjEntity(e);
        assertSame(e, map.getObjEntity(e.getName()));
        assertSame(map, e.getDataMap());
    }

    @Test
    public void addEntityWithSameName() {
        DataMap map = new DataMap();

        // Give them different class-names... we are only testing for the same
        // entity name
        // being a problem
        ObjEntity e1 = new ObjEntity("c");
        e1.setClassName("c1");
        ObjEntity e2 = new ObjEntity("c");
        e2.setClassName("c2");
        map.addObjEntity(e1);
        assertThrows(Exception.class, () -> map.addObjEntity(e2));
    }

    @Test
    public void removeThenAddNullClassName() {
        DataMap map = new DataMap();
        // It should be possible to cleanly remove and then add the same entity
        // again.
        // Uncovered the need for this while testing modeller manually.

        ObjEntity e = new ObjEntity("f");
        map.addObjEntity(e);

        map.removeObjEntity(e.getName(), false);
        map.addObjEntity(e);
    }

    @Test
    public void removeObjEntity() {
        // make sure deleting an ObjEntity & other entity's relationships to it
        // works & does not cause a ConcurrentModificationException

        ObjEntity e1 = new ObjEntity("1");
        ObjEntity e2 = new ObjEntity("2");

        ObjRelationship r1 = new ObjRelationship("r1");
        r1.setTargetEntityName("2");

        ObjRelationship r2 = new ObjRelationship("r2");
        r2.setTargetEntityName("1");

        ObjRelationship r3 = new ObjRelationship("r3");
        r1.setTargetEntityName("2");

        ObjRelationship r4 = new ObjRelationship("r4");
        r4.setTargetEntityName("1");

        e1.addRelationship(r1);
        e1.addRelationship(r2);
        e2.addRelationship(r3);
        e2.addRelationship(r4);

        DataMap map = new DataMap();
        map.addObjEntity(e1);
        map.addObjEntity(e2);

        map.removeObjEntity("1", true);
        assertNull(map.getObjEntity("1"));
        assertEquals(1, e2.getRelationships().size());

        map.removeObjEntity("2", true);
        assertNull(map.getObjEntity("2"));
    }

    @Test
    public void multipleNullClassNames() {
        // Now possible to have more than one objEntity with a null class name.
        // This test proves it

        ObjEntity e1 = new ObjEntity("g");
        ObjEntity e2 = new ObjEntity("h");

        DataMap map = new DataMap();
        map.addObjEntity(e1);
        map.addObjEntity(e2);
    }

    @Test
    public void removeThenAddRealClassName() {
        ObjEntity e = new ObjEntity("f");
        e.setClassName("f");

        DataMap map = new DataMap();
        map.addObjEntity(e);

        map.removeObjEntity(e.getName(), false);
        map.addObjEntity(e);
    }

    @Test
    public void addEmbeddable() {
        Embeddable e = new Embeddable("XYZ");

        DataMap map = new DataMap();
        assertEquals(0, map.getEmbeddables().size());
        map.addEmbeddable(e);
        assertEquals(1, map.getEmbeddables().size());
        assertTrue(map.getEmbeddables().contains(e));
    }

    @Test
    public void removeEmbeddable() {
        Embeddable e = new Embeddable("XYZ");

        DataMap map = new DataMap();
        map.addEmbeddable(e);
        assertTrue(map.getEmbeddables().contains(e));

        map.removeEmbeddable("123");
        assertTrue(map.getEmbeddables().contains(e));
        map.removeEmbeddable("XYZ");
        assertFalse(map.getEmbeddables().contains(e));
    }

    @Test
    public void addDbEntity() {
        DbEntity e = new DbEntity("b");

        DataMap map = new DataMap();
        map.addDbEntity(e);
        assertSame(e, map.getDbEntity(e.getName()));
        assertSame(map, e.getDataMap());
    }

    @Test
    public void addQueryDescriptor() {
        QueryDescriptor q = new SelectQueryDescriptor();
        q.setName("a");
        DataMap map = new DataMap();
        map.addQueryDescriptor(q);
        assertSame(q, map.getQueryDescriptor("a"));
    }

    @Test
    public void removeQueryDescriptor() {
        QueryDescriptor q = new SelectQueryDescriptor();
        q.setName("a");

        DataMap map = new DataMap();
        map.addQueryDescriptor(q);
        assertSame(q, map.getQueryDescriptor("a"));
        map.removeQueryDescriptor("a");
        assertNull(map.getQueryDescriptor("a"));
    }

    @Test
    public void getQueryMap() {
        QueryDescriptor q = new SelectQueryDescriptor();
        q.setName("a");
        DataMap map = new DataMap();
        map.addQueryDescriptor(q);
        Map<String, QueryDescriptor> queries = map.getQueryDescriptorMap();
        assertEquals(1, queries.size());
        assertSame(q, queries.get("a"));
    }

    // make sure deleting a DbEntity & other entity's relationships to it
    // works & does not cause a ConcurrentModificationException
    @Test
    public void removeDbEntity() {

        DataMap map = new DataMap();

        // create a twisty maze of intermingled relationships.
        DbEntity e1 = new DbEntity();
        e1.setName("e1");

        DbEntity e2 = new DbEntity();
        e2.setName("e2");

        DbRelationship r1 = new DbRelationship();
        r1.setName("r1");
        r1.setTargetEntityName(e2);

        DbRelationship r2 = new DbRelationship();
        r2.setName("r2");
        r2.setTargetEntityName(e1);

        DbRelationship r3 = new DbRelationship();
        r3.setName("r3");
        r3.setTargetEntityName(e2);

        e1.addRelationship(r1);
        e1.addRelationship(r2);
        e1.addRelationship(r3);

        e2.addRelationship(r1);
        e2.addRelationship(r2);
        e2.addRelationship(r3);

        map.addDbEntity(e1);
        map.addDbEntity(e2);

        // now actually test something
        map.removeDbEntity(e1.getName(), true);
        assertNull(map.getDbEntity(e1.getName()));
        map.removeDbEntity(e2.getName(), true);
        assertNull(map.getDbEntity(e2.getName()));
    }

    @Test
    public void childProcedures() throws Exception {
        DataMap map = new DataMap();
        checkProcedures(map, new String[0]);

        map.addProcedure(new Procedure("proc1"));
        checkProcedures(map, new String[] { "proc1" });

        map.addProcedure(new Procedure("proc2"));
        checkProcedures(map, new String[] { "proc1", "proc2" });

        map.removeProcedure("proc2");
        checkProcedures(map, new String[] { "proc1" });
    }

    protected void checkProcedures(DataMap map, String[] expectedNames) throws Exception {
        int len = expectedNames.length;
        Map<String, Procedure> proceduresMap = map.getProcedureMap();
        Collection<Procedure> proceduresCollection = map.getProcedures();

        assertNotNull(proceduresMap);
        assertEquals(len, proceduresMap.size());
        assertNotNull(proceduresCollection);
        assertEquals(len, proceduresCollection.size());

        for (String expectedName : expectedNames) {
            Procedure proc = map.getProcedure(expectedName);
            assertNotNull(proc);
            assertEquals(expectedName, proc.getName());
        }
    }

    private static SelectQueryDescriptor selectQuery(String name, String ql) {
        SelectQueryDescriptor query = new SelectQueryDescriptor();
        query.setName(name);
        query.setQl(ql);
        return query;
    }
}
