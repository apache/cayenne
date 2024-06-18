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

package org.apache.cayenne.configuration.xml;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
public class ObjEntityHandlerTest extends BaseHandlerTest {

    @Test
    public void testLoad() throws Exception {

        final DataMap map = new DataMap();
        assertEquals(0, map.getObjEntities().size());

        parse("obj-entity", parent -> new ObjEntityHandler(parent, map));

        assertEquals(1, map.getObjEntities().size());
        ObjEntity entity = map.getObjEntity("ArtistCallback");
        assertNotNull(entity);
        assertTrue(entity.isAbstract());
        assertTrue(entity.isReadOnly());
        assertEquals(3, entity.getAttributes().size());
        assertEquals(8, entity.getCallbackMethods().size());
        assertEquals(ObjEntity.LOCK_TYPE_OPTIMISTIC, entity.getDeclaredLockType());
        assertEquals("org.apache.cayenne.testdo.testmap.ArtistCallback", entity.getClassName());
        assertNull("super.class should be suppressed by super entity", entity.getSuperClassName());
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

}