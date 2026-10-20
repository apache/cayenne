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

import org.apache.cayenne.CayenneRuntime;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbRelationshipIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private CayenneRuntime runtime;

    protected DbEntity artistEnt;
    protected DbEntity paintingEnt;

    @BeforeEach
    public void setUp() throws Exception {
        runtime = env.runtime();
        artistEnt = runtime.getDataDomain().getEntityResolver().getDbEntity("ARTIST");
        paintingEnt = runtime.getDataDomain().getEntityResolver().getDbEntity("PAINTING");
    }

    @Test
    public void isForeignKey() {
        // FK to PK
        assertTrue(paintingEnt.getRelationship("toArtist").isForeignKey());

        // to-many
        assertFalse(artistEnt.getRelationship("paintingArray").isForeignKey());

        // master to dependent PK: the join is to the target PK, but the key lives in the target row
        DbRelationship toPaintingInfo = paintingEnt.getRelationship("toPaintingInfo");
        assertTrue(toPaintingInfo.isToPrimaryKey());
        assertFalse(toPaintingInfo.isForeignKey());

        // dependent to master PK: the dependent PK is also the FK
        assertTrue(toPaintingInfo.getReverseRelationship().isForeignKey());
    }

    @Test
    public void getReverseRelationship1() throws Exception {
        // start with "to many"
        DbRelationship r1 = artistEnt.getRelationship("paintingArray");
        DbRelationship r2 = r1.getReverseRelationship();

        assertNotNull(r2);
        assertSame(paintingEnt.getRelationship("toArtist"), r2);
    }

    @Test
    public void getReverseRelationship2() throws Exception {
        // start with "to one"
        DbRelationship r1 = paintingEnt.getRelationship("toArtist");
        DbRelationship r2 = r1.getReverseRelationship();

        assertNotNull(r2);
        assertSame(artistEnt.getRelationship("paintingArray"), r2);
    }

    @Test
    public void getReverseRelationshipToSelf() {

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
