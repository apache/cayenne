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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.unit.CayenneCase;

public class DbRelationshipTest extends CayenneCase {

    protected DbEntity artistEnt;
    protected DbEntity paintingEnt;
    protected DbEntity galleryEnt;

    @Override
    public void setUp() throws Exception {
        artistEnt = getDbEntity("ARTIST");
        paintingEnt = getDbEntity("PAINTING");
        galleryEnt = getDbEntity("GALLERY");
    }

    public void testSrcFkSnapshotWithTargetSnapshot() throws Exception {
        Map map = new HashMap();
        Integer id = new Integer(44);
        map.put("GALLERY_ID", id);

        DbRelationship dbRel = (DbRelationship) galleryEnt
                .getRelationship("paintingArray");
        Map targetMap = dbRel.getReverseRelationship().srcFkSnapshotWithTargetSnapshot(
                map);
        assertEquals(id, targetMap.get("GALLERY_ID"));
    }

    public void testGetReverseRelationship1() throws Exception {
        // start with "to many"
        DbRelationship r1 = (DbRelationship) artistEnt.getRelationship("paintingArray");
        DbRelationship r2 = r1.getReverseRelationship();

        assertNotNull(r2);
        assertSame(paintingEnt.getRelationship("toArtist"), r2);
    }

    public void testGetReverseRelationship2() throws Exception {
        // start with "to one"
        DbRelationship r1 = (DbRelationship) paintingEnt.getRelationship("toArtist");
        DbRelationship r2 = r1.getReverseRelationship();

        assertNotNull(r2);
        assertSame(artistEnt.getRelationship("paintingArray"), r2);
    }

    public void testGetReverseRelationshipToSelf() {

        // assemble mockup entity
        DataMap namespace = new DataMap();
        DbEntity e = new DbEntity("test");
        namespace.addDbEntity(e);
        DbRelationship rforward = new DbRelationship("rforward");
        e.addRelationship(rforward);
        rforward.setSourceEntity(e);
        rforward.setTargetEntity(e);

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
        rback.setTargetEntity(e);

        assertNull(rforward.getReverseRelationship());

        // create reverse join
        rback.addJoin(new DbJoin(rback, "a2", "a1"));

        assertSame(rback, rforward.getReverseRelationship());
        assertSame(rforward, rback.getReverseRelationship());
    }
}
