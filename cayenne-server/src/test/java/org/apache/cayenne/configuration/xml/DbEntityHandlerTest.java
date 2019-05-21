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

import java.sql.Types;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.1
 */
public class DbEntityHandlerTest extends BaseHandlerTest {

    @Test
    public void testLoad() throws Exception {

        final DataMap map = new DataMap();
        assertTrue(map.getDbEntities().isEmpty());

        parse("db-entity", new HandlerFactory() {
            @Override
            public NamespaceAwareNestedTagHandler createHandler(NamespaceAwareNestedTagHandler parent) {
                return new DbEntityHandler(parent, map);
            }
        });

        assertEquals(1, map.getDbEntities().size());

        DbEntity entity = map.getDbEntity("ARTGROUP");
        assertNotNull(entity);
        assertNull(entity.getPrimaryKeyGenerator());
        assertEquals(3, entity.getAttributes().size());
        assertEquals("catalog", entity.getCatalog());
        assertEquals("schema", entity.getSchema());
        assertEquals("name = \"test\"", entity.getQualifier().toString());

        DbAttribute attribute = entity.getAttribute("GROUP_ID");
        assertNotNull(attribute);
        assertTrue(attribute.isMandatory());
        assertTrue(attribute.isPrimaryKey());
        assertTrue(attribute.isGenerated());
        assertEquals(Types.INTEGER, attribute.getType());

        attribute = entity.getAttribute("NAME");
        assertNotNull(attribute);
        assertTrue(attribute.isMandatory());
        assertFalse(attribute.isPrimaryKey());
        assertFalse(attribute.isGenerated());
        assertEquals(100, attribute.getMaxLength());
        assertEquals(Types.VARCHAR, attribute.getType());

        attribute = entity.getAttribute("PARENT_GROUP_ID");
        assertNotNull(attribute);
        assertFalse(attribute.isMandatory());
        assertFalse(attribute.isPrimaryKey());
        assertFalse(attribute.isGenerated());
        assertEquals(10, attribute.getScale());
        assertEquals(Types.BIT, attribute.getType());
    }
}