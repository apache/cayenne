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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.query.resolved.ResolvedSelect;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;

public class IdColumnExtractorTest extends BaseColumnExtractorTest {

    @Test
    public void extractNoPrefix() {
        DbEntity mockDbEntity = createMockDbEntity("mock");
        DataMap dataMap = new DataMap("mock");
        dataMap.addDbEntity(mockDbEntity);

        ObjEntity entity = new ObjEntity("Mock");
        entity.setDbEntity(mockDbEntity);
        dataMap.addObjEntity(entity);

        CompiledMapping mapping = compile(dataMap);
        ResolvedSelect query = new MockResolvedSelectBuilder()
                .withDbEntity(mapping.schema().table("mock"))
                .build();
        SelectTranslatorContext context = new MockSelectTranslatorContext(query);

        IdColumnExtractor extractor = new IdColumnExtractor(context, mapping.entity("Mock"));
        extractor.extract();

        assertEquals(1, context.getResultNodeList().size());

        ResultNodeDescriptor descriptor0 = context.getResultNodeList().get(0);

        assertNull(descriptor0.getProperty());
        assertNotNull(descriptor0.getNode());
        assertInstanceOf(ColumnNode.class, descriptor0.getNode());
        assertFalse(descriptor0.isAggregate());
        assertTrue(descriptor0.isInDataRow());
        assertEquals("id", descriptor0.getDataRowKey());
        assertNotNull(descriptor0.getDbAttribute());
        assertEquals(Types.BIGINT, descriptor0.getJdbcType());
    }

    @Test
    public void extractWithPrefix() {
        DbEntity mockDbEntity = createMockDbEntity("mock1");
        DbEntity mock2DbEntity = createMockDbEntity("mock2");
        DataMap dataMap = new DataMap("mock");
        dataMap.addDbEntity(mockDbEntity);
        dataMap.addDbEntity(mock2DbEntity);

        ObjEntity entity = new ObjEntity("Mock");
        entity.setDbEntity(mockDbEntity);
        dataMap.addObjEntity(entity);

        DbRelationship relationship = new DbRelationship("prefix");
        relationship.setSourceEntity(mockDbEntity);
        relationship.setTargetEntityName("mock2");
        relationship.addJoin(new DbJoin(relationship, "id", "id"));
        mockDbEntity.addRelationship(relationship);

        CompiledMapping mapping = compile(dataMap);
        ResolvedSelect query = new MockResolvedSelectBuilder()
                .withDbEntity(mapping.schema().table("mock1"))
                .build();
        SelectTranslatorContext context = new MockSelectTranslatorContext(query);

        CayennePath prefix = CayennePath.of("prefix");
        context.getTableTree().addJoinTable(prefix, mapping.schema().table("mock1").join("prefix"), JoinType.INNER);

        IdColumnExtractor extractor = new IdColumnExtractor(context, mapping.entity("Mock"));
        extractor.extract(prefix);

        assertEquals(1, context.getResultNodeList().size());

        ResultNodeDescriptor descriptor0 = context.getResultNodeList().get(0);

        assertNull(descriptor0.getProperty());
        assertNotNull(descriptor0.getNode());
        assertInstanceOf(ColumnNode.class, descriptor0.getNode());
        assertFalse(descriptor0.isAggregate());
        assertTrue(descriptor0.isInDataRow());
        assertEquals("prefix.id", descriptor0.getDataRowKey());
        assertNotNull(descriptor0.getDbAttribute());
        assertEquals(Types.BIGINT, descriptor0.getJdbcType());
    }
}