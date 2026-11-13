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

package org.apache.cayenne.project.xml;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.project.ProjectLoadingMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ObjRelationshipHandlerTest extends BaseHandlerTest {

    private static final String COMMENTED_RELATIONSHIP = """
            <objRelationship name="artists" source="ArtGroup" target="Artist" dbRelationshipPath="toArtists">
                <comment>artists</comment>
            </objRelationship>
            """;

    @Test
    public void load() throws Exception {

        final DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("ArtGroup");
        map.addObjEntity(entity);
        assertEquals(0, entity.getRelationships().size());

        parse("objRelationship", new HandlerFactory() {
            @Override
            public NamespaceAwareNestedTagHandler createHandler(NamespaceAwareNestedTagHandler parent) {
                return new ObjRelationshipHandler(parent, map);
            }
        });

        assertEquals(1, entity.getRelationships().size());
        ObjRelationship relationship = entity.getRelationship("artistArray");
        assertNotNull(relationship);

        assertEquals(DeleteRule.CASCADE, relationship.getDeleteRule());
        assertEquals("java.util.Map", relationship.getCollectionType());
        assertEquals("artistName", relationship.getMapKey());
        assertEquals("Artist", relationship.getTargetEntityName());
        assertTrue(relationship.isUsedForLocking());
    }

    @Test
    public void loadComments() throws Exception {
        ObjRelationship relationship = parseRelationship(ProjectLoadingMode.FULL);
        assertEquals("artists", relationship.getComment());
        assertEquals("Artist", relationship.getTargetEntityName());
    }

    @Test
    public void skipComments() throws Exception {
        ObjRelationship relationship = parseRelationship(ProjectLoadingMode.RUNTIME);
        assertNull(relationship.getComment());
        assertEquals("Artist", relationship.getTargetEntityName());
    }

    private ObjRelationship parseRelationship(ProjectLoadingMode loadingMode) throws Exception {
        DataMap map = new DataMap();
        ObjEntity entity = new ObjEntity("ArtGroup");
        map.addObjEntity(entity);
        parse("objRelationship", COMMENTED_RELATIONSHIP, loadingMode,
                parent -> new ObjRelationshipHandler(parent, map));
        return entity.getRelationship("artists");
    }
}