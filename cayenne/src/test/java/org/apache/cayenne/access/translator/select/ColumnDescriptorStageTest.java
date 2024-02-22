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

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class ColumnDescriptorStageTest {

    @Test
    public void perform() {
        TranslatableQueryWrapper wrapper = new MockQueryWrapperBuilder()
                .withDistinct(true)
                .withMetaData(new MockQueryMetadataBuilder()
                        .withSuppressDistinct()
                        .build())
                .build();
        TranslatorContext context = new MockTranslatorContext(wrapper);

        context.addResultNode(new EmptyNode());
        context.addResultNode(new EmptyNode(), CayennePath.of("key"));
        context.addResultNode(new EmptyNode(), false, PropertyFactory.COUNT, CayennePath.of("key2"));

        ColumnDescriptorStage stage = new ColumnDescriptorStage();
        stage.perform(context);

        assertEquals(1, context.getColumnDescriptors().size());
        ColumnDescriptor descriptor = context.getColumnDescriptors().iterator().next();
        assertEquals("key", descriptor.getDataRowKey());
    }
}