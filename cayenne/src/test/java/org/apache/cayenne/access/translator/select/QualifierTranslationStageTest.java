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
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QualifierTranslationStageTest {

    private SelectTranslatorContext context;

    @BeforeEach
    public void prepareContext() {
        DbEntity dbEntity = new DbEntity();
        dbEntity.setName("mock");
        DbAttribute dbAttribute = new DbAttribute();
        dbAttribute.setName("path");
        dbEntity.addAttribute(dbAttribute);

        TranslatableQueryWrapper wrapper = new MockQueryWrapperBuilder()
                .withQualifier(ExpressionFactory.greaterOrEqualDbExp("path", 10))
                .withMetaData(new MockQueryMetadataBuilder()
                        .withDbEntity(dbEntity)
                        .build())
                .build();
        context = new MockSelectTranslatorContext(wrapper);
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