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
import org.apache.cayenne.access.sqlbuilder.sqltree.HavingNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.OpExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @since 4.2
 */
public class HavingTranslationStageTest {

    private TranslatorContext context;

    @Before
    public void prepareContext() {
        DbEntity dbEntity = new DbEntity();
        dbEntity.setName("mock");
        DbAttribute dbAttribute = new DbAttribute();
        dbAttribute.setName("path");
        dbEntity.addAttribute(dbAttribute);

        TranslatableQueryWrapper wrapper = new MockQueryWrapperBuilder()
                .withHavingQualifier(ExpressionFactory.greaterOrEqualDbExp("path", 10))
                .withMetaData(new MockQueryMetadataBuilder()
                        .withDbEntity(dbEntity)
                        .build())
                .build();
        context = new MockTranslatorContext(wrapper);
    }

    @Test
    public void perform() {
        HavingTranslationStage stage = new HavingTranslationStage();
        stage.perform(context);

        Node select = context.getSelectBuilder().build();

        // Content of "select" node:
        //
        //      Having
        //        |
        //   OpExpression
        //    /        \
        // Column     Value

        assertEquals(1, select.getChildrenCount());
        assertThat(select.getChild(0), instanceOf(HavingNode.class));
        Node op = select.getChild(0).getChild(0);
        assertThat(op, instanceOf(OpExpressionNode.class));
        assertEquals(">=", ((OpExpressionNode)op).getOp());
        assertEquals(2, op.getChildrenCount());
        assertThat(op.getChild(0), instanceOf(ColumnNode.class));
        assertThat(op.getChild(1), instanceOf(ValueNode.class));

        ColumnNode columnNode = (ColumnNode)op.getChild(0);
        ValueNode valueNode = (ValueNode)op.getChild(1);
        assertEquals("path", columnNode.getColumn());
        assertEquals(10, valueNode.getValue());
    }
}