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

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.project.ProjectLoadingMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ObjEntityHandlerTest extends BaseHandlerTest {

    // a comment may follow other children
    private static final String COMMENTED_ENTITY = """
            <objEntity name="Artist" className="org.example.Artist" dbEntityName="ARTIST">
                <qualifier><![CDATA[name = "test"]]></qualifier>
                <comment>entity</comment>
                <embeddedAttribute name="address" type="org.example.Address">
                    <embeddableAttributeOverride name="city" dbAttributePath="CITY"/>
                    <comment>address</comment>
                </embeddedAttribute>
                <objAttribute name="name" type="java.lang.String" dbAttributePath="NAME">
                    <comment>name</comment>
                </objAttribute>
                <objAttribute name="dateOfBirth" type="java.util.Date"/>
            </objEntity>
            """;

    @Test
    public void load() throws Exception {

        final DataMap map = new DataMap();
        assertEquals(0, map.getObjEntities().size());

        parse("objEntity", parent -> new ObjEntityHandler(parent, map));

        assertEquals(1, map.getObjEntities().size());
        ObjEntity entity = map.getObjEntity("ArtistCallback");
        assertNotNull(entity);
        assertTrue(entity.isAbstract());
        assertTrue(entity.isReadOnly());
        assertEquals(3, entity.getAttributes().size());
        assertEquals(ObjEntity.LOCK_TYPE_OPTIMISTIC, entity.getDeclaredLockType());
        assertEquals("org.apache.cayenne.testdo.testmap.ArtistCallback", entity.getClassName());
        assertNull(entity.getSuperClassName(), "super.class should be suppressed by super entity");
        assertEquals("Artist", entity.getSuperEntityName());
        assertEquals("ARTIST_CT", entity.getDbEntityName());

        ObjAttribute attribute = entity.getAttribute("artistName");
        assertNotNull(attribute);
        assertEquals("NAME", attribute.getDbAttributeName());
        assertEquals("java.lang.String", attribute.getType());
        assertTrue(attribute.isUsedForLocking());

        attribute = entity.getAttribute("dateOfBirth");
        assertNotNull(attribute);
        assertNull(attribute.getDbAttributeName());
        assertEquals("java.util.Date", attribute.getType());
        assertFalse(attribute.isUsedForLocking());

        attribute = entity.getAttribute("embeddable1");
        assertNotNull(attribute);
        assertNull(attribute.getDbAttributeName());
        assertEquals("org.apache.cayenne.testdo.embeddable.Embeddable1", attribute.getType());
        assertFalse(attribute.isUsedForLocking());

        CayennePath override = entity.getDeclaredAttributeOverrides().get("name");
        assertEquals("parent.child.name", override.value());
    }

    @Test
    public void loadComments() throws Exception {
        DataMap map = new DataMap();
        parse("objEntity", COMMENTED_ENTITY, ProjectLoadingMode.FULL, parent -> new ObjEntityHandler(parent, map));

        ObjEntity entity = map.getObjEntity("Artist");
        assertEquals("entity", entity.getComment());
        assertEquals("address", entity.getAttribute("address").getComment());
        assertEquals("name", entity.getAttribute("name").getComment());
        assertNull(entity.getAttribute("dateOfBirth").getComment());
        assertEquals("name = \"test\"", entity.getDeclaredQualifier().toString());
        assertEquals("CITY", ((EmbeddedAttribute) entity.getAttribute("address")).getAttributeOverrides().get("city"));
    }

    @Test
    public void skipComments() throws Exception {
        DataMap map = new DataMap();
        parse("objEntity", COMMENTED_ENTITY, ProjectLoadingMode.RUNTIME, parent -> new ObjEntityHandler(parent, map));

        ObjEntity entity = map.getObjEntity("Artist");
        assertNull(entity.getComment());
        assertEquals(3, entity.getAttributes().size());
        entity.getAttributes().forEach(a -> assertNull(a.getComment()));
        assertEquals("name = \"test\"", entity.getDeclaredQualifier().toString());
    }

}