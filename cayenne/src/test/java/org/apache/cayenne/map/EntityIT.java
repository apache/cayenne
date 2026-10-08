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

import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.util.CayenneMapEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void name() {
        MockEntity entity = new MockEntity();
        String tstName = "tst_name";
        entity.setName(tstName);
        assertEquals(tstName, entity.getName());
    }

    @Test
    public void attribute() {
        MockEntity entity = new MockEntity();
        MockAttribute attribute = new MockAttribute("tst_name");

        entity.addAttribute(attribute);
        assertSame(attribute, entity.getAttribute(attribute.getName()));

        // attribute must have its entity switched to our entity.
        assertSame(entity, attribute.getEntity());

        // remove attribute
        entity.removeAttribute(attribute.getName());
        assertNull(entity.getAttribute(attribute.getName()));
    }

    @Test
    public void relationship() {
        MockEntity entity = new MockEntity();
        MockRelationship rel = new MockRelationship("tst_name");

        entity.addRelationship(rel);
        assertSame(rel, entity.getRelationship(rel.getName()));

        // attribute must have its entity switched to our entity.
        assertSame(entity, rel.getSourceEntity());

        // remove attribute
        entity.removeRelationship(rel.getName());
        assertNull(entity.getRelationship(rel.getName()));
    }

    @Test
    public void attributeClashWithRelationship() {
        MockEntity entity = new MockEntity();
        MockRelationship rel = new MockRelationship("tst_name");

        entity.addRelationship(rel);

        assertThrows(Exception.class, () -> {
            MockAttribute attribute = new MockAttribute("tst_name");
            entity.addAttribute(attribute);
        });
    }

    @Test
    public void relationshipClashWithAttribute() {
        MockEntity entity = new MockEntity();
        MockAttribute attribute = new MockAttribute("tst_name");

        entity.addAttribute(attribute);

        assertThrows(Exception.class, () -> {
            MockRelationship rel = new MockRelationship("tst_name");
            entity.addRelationship(rel);
        });
    }

    @Test
    public void resolveBadObjPath() {
        ObjEntity galleryEnt = env.runtime().getDataDomain().getEntityResolver().getObjEntity("Gallery");
        assertThrows(ExpressionException.class,
                () -> galleryEnt.resolvePath(CayennePath.of("invalid.invalid")));
    }

    @Test
    public void resolveAttributeNotLast() {
        ObjEntity galleryEnt = env.runtime().getDataDomain().getEntityResolver().getObjEntity("Gallery");
        assertThrows(ExpressionException.class,
                () -> galleryEnt.resolvePath(CayennePath.of("galleryName.paintingArray")));
    }

    @Test
    public void resolveObjPathExpandedAlias() {
        ObjEntity artistEnt = env.runtime().getDataDomain().getEntityResolver().getObjEntity("Artist");
        List<CayenneMapEntry> components = artistEnt.resolvePath(
                CayennePath.of("a.paintingTitle").expandAliases(Map.of("a", "paintingArray")));

        assertEquals(2, components.size());
        assertSame(artistEnt.getRelationship("paintingArray"), components.get(0));
        assertSame(artistEnt.getDataMap().getObjEntity("Painting").getAttribute("paintingTitle"), components.get(1));
    }

    @Test
    public void resolveObjPath1() {
        ObjEntity galleryEnt = env.runtime().getDataDomain().getEntityResolver().getObjEntity("Gallery");
        List<CayenneMapEntry> components = galleryEnt.resolvePath(CayennePath.of("galleryName"));

        // must contain a single ObjAttribute
        assertEquals(1, components.size());
        assertSame(galleryEnt.getAttribute("galleryName"), components.get(0));
    }

    @Test
    public void removeAttribute() {
        MockEntity entity = new MockEntity();

        entity.setName("test");
        MockAttribute attribute1 = new MockAttribute("a1");
        MockAttribute attribute2 = new MockAttribute("a2");

        entity.addAttribute(attribute1);
        entity.addAttribute(attribute2);

        Collection<MockAttribute> attributes = entity.getAttributes();
        assertEquals(2, attributes.size());

        entity.removeAttribute("a1");
        attributes = entity.getAttributes();
        assertEquals(1, attributes.size());
    }
}
