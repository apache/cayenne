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

import java.sql.Types;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.generic.DefaultValueComparisonStrategyFactory;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @since 4.2
 */
public class DescriptorColumnExtractorTest extends BaseColumnExtractorTest {

    @Test
    public void testExtractNoPrefix() {
        DbEntity mockDbEntity = createMockDbEntity("mock");
        TranslatableQueryWrapper wrapper = new MockQueryWrapperBuilder()
                .withMetaData(new MockQueryMetadataBuilder()
                        .withDbEntity(mockDbEntity)
                        .build())
                .build();

        TranslatorContext context = new MockTranslatorContext(wrapper);

        DataMap dataMap = new DataMap();
        dataMap.addDbEntity(mockDbEntity);

        ObjEntity entity = new ObjEntity();
        entity.setName("mock");
        entity.setDataMap(dataMap);
        entity.setDbEntity(mockDbEntity);

        ObjAttribute attribute = new ObjAttribute();
        attribute.setName("not_name");
        attribute.setDbAttributePath("name");
        attribute.setType("java.lang.Integer");
        entity.addAttribute(attribute);

        dataMap.addObjEntity(entity);

        EntityResolver resolver = new EntityResolver();
        resolver.addDataMap(dataMap);
        resolver.setValueComparisonStrategyFactory(new DefaultValueComparisonStrategyFactory(mock(ValueObjectTypeRegistry.class)));
        resolver.setObjectFactory(new DefaultAdhocObjectFactory(null, new DefaultClassLoaderManager()));

        DescriptorColumnExtractor extractor = new DescriptorColumnExtractor(context, resolver.getClassDescriptor("mock"));
        extractor.extract();

        assertEquals(2, context.getResultNodeList().size());

        ResultNodeDescriptor descriptor0 = context.getResultNodeList().get(0);
        ResultNodeDescriptor descriptor1 = context.getResultNodeList().get(1);

        assertNull(descriptor0.getProperty());
        assertNotNull(descriptor0.getNode());
        assertThat(descriptor0.getNode(), instanceOf(ColumnNode.class));
        assertFalse(descriptor0.isAggregate());
        assertTrue(descriptor0.isInDataRow());
        assertNotNull(descriptor0.getDbAttribute());
        assertEquals("name", descriptor0.getDataRowKey());
        assertEquals(Types.VARBINARY, descriptor0.getJdbcType());
        assertEquals("java.lang.Integer", descriptor0.getJavaType());

        assertNull(descriptor1.getProperty());
        assertNotNull(descriptor1.getNode());
        assertThat(descriptor1.getNode(), instanceOf(ColumnNode.class));
        assertFalse(descriptor1.isAggregate());
        assertTrue(descriptor1.isInDataRow());
        assertEquals("id", descriptor1.getDataRowKey());
        assertNotNull(descriptor1.getDbAttribute());
        assertEquals(Types.BIGINT, descriptor1.getJdbcType());
    }
}