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

import java.sql.Types;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.project.ProjectLoadingMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DbEntityHandlerTest extends BaseHandlerTest {

    // a comment may follow other children, such as a skipped tag of another namespace
    private static final String COMMENTED_ENTITY = """
            <dbEntity name="ARTIST">
                <comment>entity</comment>
                <dbAttribute name="ID" type="INTEGER" primaryKey="true" mandatory="true">
                    <comment>id</comment>
                </dbAttribute>
                <dbAttribute name="NAME" type="VARCHAR" length="100"/>
                <dbAttribute name="DATE" type="DATE">
                    <x:extra xmlns:x="urn:example"/>
                    <comment>date</comment>
                </dbAttribute>
                <qualifier><![CDATA[NAME = "test"]]></qualifier>
            </dbEntity>
            """;

    @Test
    public void load() throws Exception {

        final DataMap map = new DataMap();
        assertTrue(map.getDbEntities().isEmpty());

        parse("dbEntity", new HandlerFactory() {
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

    @Test
    public void loadComments() throws Exception {
        DataMap map = new DataMap();
        parse("dbEntity", COMMENTED_ENTITY, ProjectLoadingMode.FULL, parent -> new DbEntityHandler(parent, map));

        DbEntity entity = map.getDbEntity("ARTIST");
        assertEquals("entity", entity.getComment());
        assertEquals("id", entity.getAttribute("ID").getComment());
        assertNull(entity.getAttribute("NAME").getComment());
        assertEquals("date", entity.getAttribute("DATE").getComment());
        assertEquals(Types.DATE, entity.getAttribute("DATE").getType());
        assertEquals("NAME = \"test\"", entity.getQualifier().toString());
    }

    @Test
    public void skipComments() throws Exception {
        DataMap map = new DataMap();
        parse("dbEntity", COMMENTED_ENTITY, ProjectLoadingMode.RUNTIME, parent -> new DbEntityHandler(parent, map));

        DbEntity entity = map.getDbEntity("ARTIST");
        assertNull(entity.getComment());
        assertEquals(3, entity.getAttributes().size());
        entity.getAttributes().forEach(a -> assertNull(a.getComment()));
        assertEquals("NAME = \"test\"", entity.getQualifier().toString());
    }
}