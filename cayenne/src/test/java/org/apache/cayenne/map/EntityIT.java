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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collection;
import java.util.Iterator;

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

    private CayenneRuntime runtime;

    @BeforeEach
    public void setUp() {
        runtime = env.runtime();
    }

    @Test
    public void serializability() throws Exception {
        MockEntity entity = new MockEntity("entity");

        MockEntity d1 = Util.cloneViaSerialization(entity);
        assertEquals(entity.getName(), d1.getName());

        entity.addAttribute(new MockAttribute("abc"));
        entity.addRelationship(new MockRelationship("xyz"));
        MockEntity d2 = Util.cloneViaSerialization(entity);
        assertNotNull(d2.getAttribute("abc"));

        // test that ref collection wrappers are still working
        assertNotNull(d2.getAttributes());
        assertEquals(entity.getAttributes().size(), d2.getAttributes().size());
        assertTrue(d2.getAttributes().contains(d2.getAttribute("abc")));

        assertNotNull(d2.getRelationships());
        assertEquals(entity.getRelationships().size(), d2.getRelationships().size());
        assertTrue(d2.getRelationships().contains(d2.getRelationship("xyz")));

        assertNotNull(d2.getAttributeMap());
        assertEquals(entity.getAttributes().size(), d2.getAttributeMap().size());
        assertSame(d2.getAttribute("abc"), d2.getAttributeMap().get("abc"));

        assertNotNull(d2.getRelationshipMap());
        assertEquals(entity.getRelationships().size(), d2.getRelationshipMap().size());
        assertSame(d2.getRelationship("xyz"), d2.getRelationshipMap().get("xyz"));
    }

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
    public void resolveBadObjPath1() {
        // test invalid expression path
        Expression pathExpr = ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        pathExpr.setOperand(0, "invalid.invalid");

        // itertator should be returned, but when trying to read 1st component,
        // it should throw an exception....
        ObjEntity galleryEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Gallery");
        Iterator<CayenneMapEntry> it = galleryEnt.resolvePathComponents(pathExpr);
        assertTrue(it.hasNext());

        assertThrows(Exception.class, it::next);
    }

    @Test
    public void resolveBadObjPath2() {
        // test invalid expression type
        Expression badPathExpr = ExpressionFactory.expressionOfType(Expression.IN);
        badPathExpr.setOperand(0, "a.b.c");
        ObjEntity galleryEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Gallery");

        assertThrows(Exception.class, () -> galleryEnt.resolvePathComponents(badPathExpr));
    }

    @Test
    public void resolveObjPath1() {
        Expression pathExpr = ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        pathExpr.setOperand(0, "galleryName");

        ObjEntity galleryEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Gallery");
        Iterator<CayenneMapEntry> it = galleryEnt.resolvePathComponents(pathExpr);

        // iterator must contain a single ObjAttribute
        assertNotNull(it);
        assertTrue(it.hasNext());
        ObjAttribute next = (ObjAttribute) it.next();
        assertNotNull(next);
        assertFalse(it.hasNext());
        assertSame(galleryEnt.getAttribute("galleryName"), next);
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
