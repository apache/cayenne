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
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.GroupByNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class GroupByStageTest {

    private TranslatorContext context;

    @Before
    public void prepareContext() {
        TranslatableQueryWrapper wrapper = new MockQueryWrapperBuilder().build();
        context = new MockTranslatorContext(wrapper);
    }

    // no result columns
    @Test
    public void emptyContext() {
        GroupByStage stage = new GroupByStage();
        stage.perform(context);

        Node node = context.getSelectBuilder().build();
        assertEquals(0, node.getChildrenCount());
    }

    // result column but no aggregates
    @Test
    public void noAggregates() {
        context.addResultNode(new ColumnNode("t0", "column", null, null));

        GroupByStage stage = new GroupByStage();
        stage.perform(context);

        Node node = context.getSelectBuilder().build();
        assertEquals(0, node.getChildrenCount());
    }

    // result column + aggregate
    @Test
    public void groupByWithAggregates() {
        context.addResultNode(new ColumnNode("t0", "column", null, null));
        context.addResultNode(new EmptyNode(), true, PropertyFactory.COUNT, CayennePath.of("count"));

        GroupByStage stage = new GroupByStage();
        stage.perform(context);

        Node node = context.getSelectBuilder().build();
        assertEquals(1, node.getChildrenCount());
        assertThat(node.getChild(0), instanceOf(GroupByNode.class));
        assertEquals(1, node.getChild(0).getChildrenCount());
        assertThat(node.getChild(0).getChild(0), instanceOf(ColumnNode.class));
        ColumnNode columnNode = (ColumnNode)node.getChild(0).getChild(0);
        assertEquals("t0", columnNode.getTable());
        assertEquals("column", columnNode.getColumn());
    }
}