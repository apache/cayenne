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
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.project.ProjectLoadingMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DbRelationshipHandlerTest extends BaseHandlerTest {

    private static final String COMMENTED_RELATIONSHIP = """
            <dbRelationship name="toPainting" source="ARTIST" target="PAINTING">
                <comment>to painting</comment>
                <dbAttributePair source="PAINTING_ID" target="ID"/>
            </dbRelationship>
            """;

    @Test
    public void parsing() throws Exception {
        final DataMap map = new DataMap();
        DbEntity entity = new DbEntity("ARTIST");
        map.addDbEntity(entity);
        assertEquals(0, entity.getRelationships().size());

        parse("dbRelationship", new HandlerFactory() {
            @Override
            public NamespaceAwareNestedTagHandler createHandler(NamespaceAwareNestedTagHandler parent) {
                return new DbRelationshipHandler(parent, map);
            }
        });

        assertEquals(1, entity.getRelationships().size());
        DbRelationship relationship = entity.getRelationship("toFavoritePainting");
        assertNotNull(relationship);

        assertEquals("PAINTING", relationship.getTargetEntityName());
        assertTrue(relationship.isForeignKey());
        assertFalse(relationship.isToMany());
        assertEquals("FAVORITE_PAINTING_ID", relationship.getJoins().get(0).getSourceName());
        assertEquals("ID", relationship.getJoins().get(0).getTargetName());
    }

    @Test
    public void loadComments() throws Exception {
        DbRelationship relationship = parseRelationship(ProjectLoadingMode.FULL);
        assertEquals("to painting", relationship.getComment());
        assertEquals(1, relationship.getJoins().size());
    }

    @Test
    public void skipComments() throws Exception {
        DbRelationship relationship = parseRelationship(ProjectLoadingMode.RUNTIME);
        assertNull(relationship.getComment());
        assertEquals(1, relationship.getJoins().size());
    }

    private DbRelationship parseRelationship(ProjectLoadingMode loadingMode) throws Exception {
        DataMap map = new DataMap();
        DbEntity entity = new DbEntity("ARTIST");
        map.addDbEntity(entity);
        parse("dbRelationship", COMMENTED_RELATIONSHIP, loadingMode,
                parent -> new DbRelationshipHandler(parent, map));
        return entity.getRelationship("toPainting");
    }
}