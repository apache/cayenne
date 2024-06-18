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
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.junit.Test;

import java.sql.Types;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class DbEntityColumnExtractorTest extends BaseColumnExtractorTest {

    @Test
    public void testExtractNoPrefix() {
        TranslatableQueryWrapper wrapper = new MockQueryWrapperBuilder()
                .withMetaData(new MockQueryMetadataBuilder()
                        .withDbEntity(createMockDbEntity("mock"))
                        .build())
                .build();
        TranslatorContext context = new MockTranslatorContext(wrapper);

        DbEntityColumnExtractor extractor = new DbEntityColumnExtractor(context);
        extractor.extract(CayennePath.EMPTY_PATH);

        assertEquals(2, context.getResultNodeList().size());

        ResultNodeDescriptor descriptor0 = context.getResultNodeList().get(0);
        ResultNodeDescriptor descriptor1 = context.getResultNodeList().get(1);

        assertNull(descriptor0.getProperty());
        assertNotNull(descriptor0.getNode());
        assertThat(descriptor0.getNode(), instanceOf(ColumnNode.class));
        assertFalse(descriptor0.isAggregate());
        assertTrue(descriptor0.isInDataRow());
        assertEquals("id", descriptor0.getDataRowKey());
        assertNotNull(descriptor0.getDbAttribute());
        assertEquals(Types.BIGINT, descriptor0.getJdbcType());

        assertNull(descriptor1.getProperty());
        assertNotNull(descriptor1.getNode());
        assertThat(descriptor1.getNode(), instanceOf(ColumnNode.class));
        assertFalse(descriptor1.isAggregate());
        assertTrue(descriptor1.isInDataRow());
        assertNotNull(descriptor1.getDbAttribute());
        assertEquals("name", descriptor1.getDataRowKey());
        assertEquals(Types.VARBINARY, descriptor1.getJdbcType());
        assertEquals("byte[]", descriptor1.getJavaType());
    }

    @Test
    public void testExtractWithPrefix() {
        DbEntity mockDbEntity = createMockDbEntity("mock1");
        DbEntity mock2DbEntity = createMockDbEntity("mock2");
        DataMap dataMap = new DataMap();
        dataMap.addDbEntity(mockDbEntity);
        dataMap.addDbEntity(mock2DbEntity);
        mockDbEntity.setDataMap(dataMap);

        TranslatableQueryWrapper wrapper = new MockQueryWrapperBuilder()
                .withMetaData(new MockQueryMetadataBuilder()
                        .withDbEntity(mockDbEntity)
                        .build())
                .build();
        TranslatorContext context = new MockTranslatorContext(wrapper);

        DbRelationship relationship = new DbRelationship();
        relationship.setSourceEntity(mockDbEntity);
        relationship.setTargetEntityName("mock1");
        CayennePath prefix = CayennePath.of("prefix");
        context.getTableTree().addJoinTable(prefix, relationship, JoinType.INNER);

        DbEntityColumnExtractor extractor = new DbEntityColumnExtractor(context);
        extractor.extract(prefix);

        assertEquals(2, context.getResultNodeList().size());

        ResultNodeDescriptor descriptor0 = context.getResultNodeList().get(0);
        ResultNodeDescriptor descriptor1 = context.getResultNodeList().get(1);

        assertNull(descriptor0.getProperty());
        assertNotNull(descriptor0.getNode());
        assertThat(descriptor0.getNode(), instanceOf(ColumnNode.class));
        assertFalse(descriptor0.isAggregate());
        assertTrue(descriptor0.isInDataRow());
        assertEquals("prefix.id", descriptor0.getDataRowKey());
        assertNotNull(descriptor0.getDbAttribute());
        assertEquals(Types.BIGINT, descriptor0.getJdbcType());

        assertNull(descriptor1.getProperty());
        assertNotNull(descriptor1.getNode());
        assertThat(descriptor1.getNode(), instanceOf(ColumnNode.class));
        assertFalse(descriptor1.isAggregate());
        assertTrue(descriptor1.isInDataRow());
        assertNotNull(descriptor1.getDbAttribute());
        assertEquals("prefix.name", descriptor1.getDataRowKey());
        assertEquals(Types.VARBINARY, descriptor1.getJdbcType());
    }

}