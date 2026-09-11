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
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.OpExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.FluentSelect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class QualifierTranslationStageTest {

    private SelectTranslatorContext context;

    @BeforeEach
    public void prepareContext() {
        context = contextWithWhere(ExpressionFactory.greaterOrEqualDbExp("path", 10));
    }

    private static SelectTranslatorContext contextWithWhere(Expression where) {
        DbEntity dbEntity = new DbEntity();
        dbEntity.setName("mock");
        DbAttribute dbAttribute = new DbAttribute();
        dbAttribute.setName("path");
        dbEntity.addAttribute(dbAttribute);

        FluentSelect<?, ?> query = new MockFluentSelectBuilder()
                .withWhere(where)
                .withMetaData(new MockQueryMetadataBuilder()
                        .withDbEntity(dbEntity)
                        .build())
                .build();
        return new MockSelectTranslatorContext(query);
    }

    @Test
    public void performAlwaysTrue() {
        context = contextWithWhere(ExpressionFactory.expTrue());
        new QualifierTranslationStage().perform(context);
        assertNull(context.getQualifierNode());
    }

    @Test
    public void performAlwaysFalse() {
        context = contextWithWhere(ExpressionFactory.expFalse());
        new QualifierTranslationStage().perform(context);

        Node qualifier = context.getQualifierNode();
        assertInstanceOf(TextNode.class, qualifier);
        assertEquals(" 1=0", ((TextNode) qualifier).getText());
    }

    @Test
    public void performFoldsTrue() {
        // "x AND true AND true", the shape produced by chaining "andExp(nin(emptyCollection))"
        context = contextWithWhere(ExpressionFactory.greaterOrEqualDbExp("path", 10)
                .andExp(ExpressionFactory.notInDbExp("path", Collections.emptyList()))
                .andExp(ExpressionFactory.expTrue()));
        new QualifierTranslationStage().perform(context);

        Node op = context.getQualifierNode();
        assertInstanceOf(OpExpressionNode.class, op);
        assertEquals(">=", ((OpExpressionNode)op).getOp());
        assertEquals(2, op.getChildrenCount());
        assertInstanceOf(ColumnNode.class, op.getChild(0));
        assertInstanceOf(ValueNode.class, op.getChild(1));
    }

    @Test
    public void perform() {
        QualifierTranslationStage stage = new QualifierTranslationStage();
        stage.perform(context);

        assertNotNull(context.getQualifierNode());

        // Content of "Qualifier" node:
        //
        //   OpExpression
        //    /        \
        // Column     Value

        Node op = context.getQualifierNode();
        assertInstanceOf(OpExpressionNode.class, op);
        assertEquals(">=", ((OpExpressionNode)op).getOp());
        assertEquals(2, op.getChildrenCount());
        assertInstanceOf(ColumnNode.class, op.getChild(0));
        assertInstanceOf(ValueNode.class, op.getChild(1));

        ColumnNode columnNode = (ColumnNode)op.getChild(0);
        ValueNode valueNode = (ValueNode)op.getChild(1);
        assertEquals("path", columnNode.getColumn());
        assertEquals(10, valueNode.getValue());
    }
}