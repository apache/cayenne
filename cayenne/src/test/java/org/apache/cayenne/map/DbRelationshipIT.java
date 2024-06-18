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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DbRelationshipIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    protected DbEntity artistEnt;
    protected DbEntity paintingEnt;
    protected DbEntity galleryEnt;

    @Before
    public void setUp() throws Exception {
        artistEnt = runtime.getDataDomain().getEntityResolver().getDbEntity("ARTIST");
        paintingEnt = runtime.getDataDomain().getEntityResolver().getDbEntity("PAINTING");
        galleryEnt = runtime.getDataDomain().getEntityResolver().getDbEntity("GALLERY");
    }

    @Test
    public void testSrcFkSnapshotWithTargetSnapshot() throws Exception {
        Map<String, Object> map = new HashMap<>();
        Integer id = 44;
        map.put("GALLERY_ID", id);

        DbRelationship dbRel = galleryEnt.getRelationship("paintingArray");
        Map<String, Object> targetMap = dbRel.getReverseRelationship().srcFkSnapshotWithTargetSnapshot(map);
        assertEquals(id, targetMap.get("GALLERY_ID"));
    }

    @Test
    public void testGetReverseRelationship1() throws Exception {
        // start with "to many"
        DbRelationship r1 = artistEnt.getRelationship("paintingArray");
        DbRelationship r2 = r1.getReverseRelationship();

        assertNotNull(r2);
        assertSame(paintingEnt.getRelationship("toArtist"), r2);
    }

    @Test
    public void testGetReverseRelationship2() throws Exception {
        // start with "to one"
        DbRelationship r1 = paintingEnt.getRelationship("toArtist");
        DbRelationship r2 = r1.getReverseRelationship();

        assertNotNull(r2);
        assertSame(artistEnt.getRelationship("paintingArray"), r2);
    }

    @Test
    public void testGetReverseRelationshipToSelf() {

        // assemble mockup entity
        DataMap namespace = new DataMap();
        DbEntity e = new DbEntity("test");
        namespace.addDbEntity(e);
        DbRelationship rforward = new DbRelationship("rforward");
        e.addRelationship(rforward);
        rforward.setSourceEntity(e);
        rforward.setTargetEntityName(e);

        assertNull(rforward.getReverseRelationship());

        // add a joins
        e.addAttribute(new DbAttribute("a1"));
        e.addAttribute(new DbAttribute("a2"));
        rforward.addJoin(new DbJoin(rforward, "a1", "a2"));

        assertNull(rforward.getReverseRelationship());

        // create reverse

        DbRelationship rback = new DbRelationship("rback");
        e.addRelationship(rback);
        rback.setSourceEntity(e);
        rback.setTargetEntityName(e);

        assertNull(rforward.getReverseRelationship());

        // create reverse join
        rback.addJoin(new DbJoin(rback, "a2", "a1"));

        assertSame(rback, rforward.getReverseRelationship());
        assertSame(rforward, rback.getReverseRelationship());
    }
}
