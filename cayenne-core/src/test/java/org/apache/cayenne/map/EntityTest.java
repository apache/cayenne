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

import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class EntityTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    public void testSerializability() throws Exception {
        Entity entity = new MockEntity("entity");

        Entity d1 = Util.cloneViaSerialization(entity);
        assertEquals(entity.getName(), d1.getName());

        entity.addAttribute(new MockAttribute("abc"));
        entity.addRelationship(new MockRelationship("xyz"));
        Entity d2 = Util.cloneViaSerialization(entity);
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

    public void testName() {
        Entity entity = new MockEntity();
        String tstName = "tst_name";
        entity.setName(tstName);
        assertEquals(tstName, entity.getName());
    }

    public void testAttribute() {
        Entity entity = new MockEntity();
        Attribute attribute = new MockAttribute("tst_name");

        entity.addAttribute(attribute);
        assertSame(attribute, entity.getAttribute(attribute.getName()));

        // attribute must have its entity switched to our entity.
        assertSame(entity, attribute.getEntity());

        // remove attribute
        entity.removeAttribute(attribute.getName());
        assertNull(entity.getAttribute(attribute.getName()));
    }

    public void testRelationship() {
        Entity entity = new MockEntity();
        Relationship rel = new MockRelationship("tst_name");

        entity.addRelationship(rel);
        assertSame(rel, entity.getRelationship(rel.getName()));

        // attribute must have its entity switched to our entity.
        assertSame(entity, rel.getSourceEntity());

        // remove attribute
        entity.removeRelationship(rel.getName());
        assertNull(entity.getRelationship(rel.getName()));
    }

    public void testAttributeClashWithRelationship() {
        Entity entity = new MockEntity();
        Relationship rel = new MockRelationship("tst_name");

        entity.addRelationship(rel);

        try {
            Attribute attribute = new MockAttribute("tst_name");
            entity.addAttribute(attribute);

            fail("Exception should have been thrown due to clashing attribute and relationship names.");
        } catch (Exception e) {
            // Exception expected.
        }
    }

    public void testRelationshipClashWithAttribute() {
        Entity entity = new MockEntity();
        Attribute attribute = new MockAttribute("tst_name");

        entity.addAttribute(attribute);

        try {
            Relationship rel = new MockRelationship("tst_name");
            entity.addRelationship(rel);

            fail("Exception should have been thrown due to clashing attribute and relationship names.");
        } catch (Exception e) {
            // Exception expected.
        }
    }

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

    public void testRemoveAttribute() {
        Entity entity = new MockEntity();

        entity.setName("test");
        ObjAttribute attribute1 = new ObjAttribute("a1");
        ObjAttribute attribute2 = new ObjAttribute("a2");

        entity.addAttribute(attribute1);
        entity.addAttribute(attribute2);

        Collection<? extends Attribute> attributes = entity.getAttributes();
        assertEquals(2, attributes.size());

        entity.removeAttribute("a1");
        attributes = entity.getAttributes();
        assertEquals(1, attributes.size());
    }
}
