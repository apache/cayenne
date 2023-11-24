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
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class EntityIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    @Test
    public void testSerializability() throws Exception {
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
    public void testName() {
        MockEntity entity = new MockEntity();
        String tstName = "tst_name";
        entity.setName(tstName);
        assertEquals(tstName, entity.getName());
    }

    @Test
    public void testAttribute() {
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
    public void testRelationship() {
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
    public void testAttributeClashWithRelationship() {
        MockEntity entity = new MockEntity();
        MockRelationship rel = new MockRelationship("tst_name");

        entity.addRelationship(rel);

        try {
            MockAttribute attribute = new MockAttribute("tst_name");
            entity.addAttribute(attribute);

            fail("Exception should have been thrown due to clashing attribute and relationship names.");
        } catch (Exception e) {
            // Exception expected.
        }
    }

    @Test
    public void testRelationshipClashWithAttribute() {
        MockEntity entity = new MockEntity();
        MockAttribute attribute = new MockAttribute("tst_name");

        entity.addAttribute(attribute);

        try {
            MockRelationship rel = new MockRelationship("tst_name");
            entity.addRelationship(rel);

            fail("Exception should have been thrown due to clashing attribute and relationship names.");
        } catch (Exception e) {
            // Exception expected.
        }
    }

    @Test
    public void testResolveBadObjPath1() {
        // test invalid expression path
        Expression pathExpr = ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        pathExpr.setOperand(0, "invalid.invalid");

        // itertator should be returned, but when trying to read 1st component,
        // it should throw an exception....
        ObjEntity galleryEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Gallery");
        Iterator<CayenneMapEntry> it = galleryEnt.resolvePathComponents(pathExpr);
        assertTrue(it.hasNext());

        try {
            it.next();
            fail();
        } catch (Exception ex) {
            // exception expected
        }
    }

    @Test
    public void testResolveBadObjPath2() {
        // test invalid expression type
        Expression badPathExpr = ExpressionFactory.expressionOfType(Expression.IN);
        badPathExpr.setOperand(0, "a.b.c");
        ObjEntity galleryEnt = runtime.getDataDomain().getEntityResolver().getObjEntity("Gallery");

        try {
            galleryEnt.resolvePathComponents(badPathExpr);
            fail();
        } catch (Exception ex) {
            // exception expected
        }
    }

    @Test
    public void testResolveObjPath1() {
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
    public void testRemoveAttribute() {
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
