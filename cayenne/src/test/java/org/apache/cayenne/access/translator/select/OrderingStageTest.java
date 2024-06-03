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

import java.util.Collections;

import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.OrderByNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Ordering;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class OrderingStageTest {

    private TranslatorContext context;

    @Before
    public void prepareContext() {
        DbEntity dbEntity = new DbEntity();
        dbEntity.setName("mock");
        DbAttribute dbAttribute = new DbAttribute();
        dbAttribute.setName("path");
        dbEntity.addAttribute(dbAttribute);

        ObjEntity objEntity = new ObjEntity();
        objEntity.setName("mock");
        objEntity.setDbEntity(dbEntity);

        ObjAttribute objAttribute = new ObjAttribute();
        objAttribute.setName("path");
        objAttribute.setDbAttributePath("path");
        objEntity.addAttribute(objAttribute);

        DataMap dataMap = new DataMap();
        dataMap.addObjEntity(objEntity);
        dataMap.addDbEntity(dbEntity);

        Ordering ordering = new Ordering("path");
        ordering.setDescending();

        TranslatableQueryWrapper wrapper = new MockQueryWrapperBuilder()
                .withOrderings(Collections.singleton(ordering))
                .withMetaData(new MockQueryMetadataBuilder()
                        .withDbEntity(dbEntity)
                        .withObjEntity(objEntity)
                        .build())
                .withDistinct( true )
                .build();
        context = new MockTranslatorContext(wrapper);
    }

    @Test
    public void perform() {
        OrderingStage orderingStage = new OrderingStage();
        orderingStage.perform(context);

        Node select = context.getSelectBuilder().build();

        // Content of "select" node:
        //
        //     OrderBy
        //        |
        //      Empty
        //     /     \
        // Column    "DESC"

        Node child = select.getChild(0);
        assertEquals(1, select.getChildrenCount());
        assertThat(child, instanceOf(OrderByNode.class));
        assertEquals(1, child.getChildrenCount());
        assertThat(child.getChild(0), instanceOf(EmptyNode.class));
        assertEquals(2, child.getChild(0).getChildrenCount());
        assertThat(child.getChild(0).getChild(0), instanceOf(ColumnNode.class));
        assertThat(child.getChild(0).getChild(1), instanceOf(TextNode.class));

        ColumnNode columnNode = (ColumnNode)child.getChild(0).getChild(0);
        assertEquals("path", columnNode.getColumn());
        assertEquals("Node { DESC}", child.getChild(0).getChild(1).toString());
    }
    
    @Test
    public void testNoDuplicateColumnsWhenDistinct() {
        ColumnExtractorStage columnStage = new ColumnExtractorStage();
        columnStage.perform(context);

        OrderingDistinctStage orderingStage = new OrderingDistinctStage();
        orderingStage.perform(context);

        assertTrue(context.getQuery().isDistinct());
        assertEquals(1, context.getResultNodeList().size());
    }
}