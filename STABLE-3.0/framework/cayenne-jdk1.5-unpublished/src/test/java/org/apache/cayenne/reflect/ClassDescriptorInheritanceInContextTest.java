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
package org.apache.cayenne.reflect;

import java.util.Collection;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.testdo.horizontalinherit.SubEntity2;
import org.apache.cayenne.unit.InheritanceCase;

public class ClassDescriptorInheritanceInContextTest extends InheritanceCase {

    public void testVisitDeclaredProperties() {

        final ClassDescriptor subentity1 = getDomain()
                .getEntityResolver()
                .getClassDescriptor("SubEntity1");

        final int[] attributeCount = new int[1];
        subentity1.visitDeclaredProperties(new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                attributeCount[0]++;
                assertTrue(attributeCount[0] < 2);
                ObjAttribute attribute = property.getAttribute();
                assertSame(subentity1.getEntity(), attribute.getEntity());
                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                fail("No relationships expected");
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                fail("No relationships expected");
                return true;
            }
        });

        assertEquals(1, attributeCount[0]);
    }

    public void testVisitProperties() {

        final ClassDescriptor subentity2 = getDomain()
                .getEntityResolver()
                .getClassDescriptor("SubEntity2");

        final int[] attributeCount = new int[1];
        subentity2.visitProperties(new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                attributeCount[0]++;
                assertTrue(attributeCount[0] < 4);
                ObjAttribute attribute = property.getAttribute();
                assertSame(subentity2.getEntity(), attribute.getEntity());

                if (attribute.getName().equals(SubEntity2.SUB_ENTITY_INT_ATTR_PROPERTY)) {
                    assertEquals("SUBENTITY_INT_DB_ATTR", attribute.getDbAttributePath());
                }
                else if (attribute.getName().equals(SubEntity2.SUPER_INT_ATTR_PROPERTY)) {
                    assertEquals("SUPER_INT_DB_ATTR", attribute.getDbAttributePath());
                }
                else if (attribute
                        .getName()
                        .equals(SubEntity2.SUPER_STRING_ATTR_PROPERTY)) {
                    assertEquals("OVERRIDDEN_STRING_DB_ATTR", attribute
                            .getDbAttributePath());
                }
                else {
                    fail("Unexpected attribute: " + attribute);
                }

                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                fail("No relationships expected");
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                fail("No relationships expected");
                return true;
            }
        });

        assertEquals(3, attributeCount[0]);
    }

    public void testGetRootDbEntities() {
        ClassDescriptor abstractSuper = getDomain()
                .getEntityResolver()
                .getClassDescriptor("AbstractSuperEntity");
        Collection<DbEntity> abstractSuperRoots = abstractSuper.getRootDbEntities();
        assertEquals(3, abstractSuperRoots.size());

        ClassDescriptor sub1 = getDomain().getEntityResolver().getClassDescriptor(
                "SubEntity1");
        Collection<DbEntity> sub1Roots = sub1.getRootDbEntities();
        assertEquals(1, sub1Roots.size());

        ClassDescriptor sub2 = getDomain().getEntityResolver().getClassDescriptor(
                "SubEntity2");
        Collection<DbEntity> sub2Roots = sub2.getRootDbEntities();
        assertEquals(1, sub2Roots.size());
    }
}
