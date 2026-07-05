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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationVisitor;
import org.apache.cayenne.access.sqlbuilder.DefaultSQLAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.*;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTAsterisk;
import org.apache.cayenne.exp.parser.ASTScalar;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.map.*;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QualifierTranslatorTest {

    private QualifierTranslator translator;

    @BeforeEach
    public void prepareTranslator() {
        DbEntity dbEntity = new DbEntity();
        dbEntity.setName("mock");

        DbAttribute dbAttributeA = new DbAttribute();
        dbAttributeA.setName("a");
        dbEntity.addAttribute(dbAttributeA);

        DbAttribute dbAttributeB = new DbAttribute();
        dbAttributeB.setName("b");
        dbAttributeB.setPrimaryKey(true);
        dbEntity.addAttribute(dbAttributeB);

        ObjEntity entity = new ObjEntity();
        entity.setName("mock");
        ObjAttribute attribute = new ObjAttribute();
        attribute.setName("a");
        attribute.setDbAttributePath("a");
        entity.addAttribute(attribute);

        ObjAttribute attribute2 = new ObjAttribute();
        attribute2.setName("b");
        attribute2.setDbAttributePath("b");
        entity.addAttribute(attribute2);

        entity.setDbEntity(dbEntity);

        DataMap dataMap = new DataMap();
        dataMap.addDbEntity(dbEntity);
        dataMap.addObjEntity(entity);

        EntityResolver resolver = new EntityResolver();
        resolver.addDataMap(dataMap);

        TranslatableQueryWrapper wrapper = new MockQueryWrapperBuilder()
                .withMetaData(new MockQueryMetadataBuilder()
                        .withDbEntity(dbEntity)
                        .withObjEntity(entity)
                        .build())
                .build();
        SelectTranslatorContext context = new MockSelectTranslatorContext(wrapper, resolver);
        translator = new QualifierTranslator(context);
    }

    @Test
    public void translateNull() {
        assertNull(translator.translate((Expression)null));
        assertNull(translator.translate((BaseProperty<?>)null));
    }

    @Test
    public void translateIn() {
        {
            Node in = translate("db:a in (1,2)");
            assertInstanceOf(InNode.class, in);
            assertEquals(2, in.getChildrenCount());
            assertFalse(((InNode) in).isNot());
            assertInstanceOf(ColumnNode.class, in.getChild(0));
            assertInstanceOf(ValueNode.class, in.getChild(1));
            assertEquals("a", ((ColumnNode) in.getChild(0)).getColumn());
            assertArrayEquals(new Object[]{1, 2}, (Object[]) ((ValueNode) in.getChild(1)).getValue());
        }

        {
            Node in = translator.translate(ExpressionFactory.inExp("a", Arrays.asList(1, 2, 3)));
            assertInstanceOf(InNode.class, in);
            assertEquals(2, in.getChildrenCount());
            assertFalse(((InNode) in).isNot());
            assertInstanceOf(ColumnNode.class, in.getChild(0));
            assertInstanceOf(ValueNode.class, in.getChild(1));
            assertEquals("a", ((ColumnNode) in.getChild(0)).getColumn());
            assertArrayEquals(new Object[]{1, 2, 3}, (Object[]) ((ValueNode) in.getChild(1)).getValue());
        }

        {
            Node notIn = translate("db:a not in (1,2)");
            assertInstanceOf(InNode.class, notIn);
            assertEquals(2, notIn.getChildrenCount());
            assertTrue(((InNode) notIn).isNot());
            assertInstanceOf(ColumnNode.class, notIn.getChild(0));
            assertInstanceOf(ValueNode.class, notIn.getChild(1));
            assertEquals("a", ((ColumnNode) notIn.getChild(0)).getColumn());
            assertArrayEquals(new Object[]{1, 2}, (Object[]) ((ValueNode) notIn.getChild(1)).getValue());
        }

    }

    @Test
    public void translateBetween() {
        {
            Node between = translate("db:a between 1 and 5");
            assertInstanceOf(BetweenNode.class, between);
            assertEquals(3, between.getChildrenCount());
            assertFalse(((BetweenNode) between).isNot());
            assertInstanceOf(ColumnNode.class, between.getChild(0));
            assertInstanceOf(ValueNode.class, between.getChild(1));
            assertInstanceOf(ValueNode.class, between.getChild(2));
        }

        {
            Node notBetween = translate("db:b not between 2 and 6");
            assertInstanceOf(BetweenNode.class, notBetween);
            assertEquals(3, notBetween.getChildrenCount());
            assertTrue(((BetweenNode) notBetween).isNot());
            assertInstanceOf(ColumnNode.class, notBetween.getChild(0));
            assertInstanceOf(ValueNode.class, notBetween.getChild(1));
            assertInstanceOf(ValueNode.class, notBetween.getChild(2));
        }
    }

    @Test
    public void translateNegate_NullScalar() {
        Node node = translate("-(null)");
        assertInstanceOf(ValueNode.class, node);
        assertNull(((ValueNode) node).getValue());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new DefaultSQLAppendable(null), null);
        node.visit(visitor);
        assertEquals(" NULL", visitor.getSQLString());
    }

    @Test
    public void translateNegate_NumberScalar() {
        Node node = translate("-1");
        assertInstanceOf(NegateNode.class, node);

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new DefaultSQLAppendable(null), null);
        node.visit(visitor);
        assertEquals(" -1", visitor.getSQLString());
    }

    @Test
    public void translateNegate_NullParam() {
        Expression exp = ExpressionFactory.exp("-$v").params(Collections.singletonMap("v", null));
        Node node = translator.translate(exp);
        assertInstanceOf(ValueNode.class, node);
        assertNull(((ValueNode) node).getValue());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new DefaultSQLAppendable(null), null);
        node.visit(visitor);
        assertEquals(" NULL", visitor.getSQLString());
    }

    @Test
    public void translateNot() {
        Node not = translate("not true");
        assertInstanceOf(NotNode.class, not);
        assertEquals(1, not.getChildrenCount());
        assertInstanceOf(TextNode.class, not.getChild(0));
        assertEquals(" 1=1", ((TextNode)not.getChild(0)).getText());
    }

    @Test
    public void translateBitwiseNot() {
        Node not = translate("~123");
        assertInstanceOf(BitwiseNotNode.class, not);
        assertEquals(1, not.getChildrenCount());
        assertInstanceOf(ValueNode.class, not.getChild(0));
        assertEquals(123, ((ValueNode)not.getChild(0)).getValue());
    }

    @Test
    public void translateEqual() {
        {
            Node eq = translate("db:a = 123");
            assertInstanceOf(EqualNode.class, eq);
            assertEquals(2, eq.getChildrenCount());
            assertInstanceOf(ColumnNode.class, eq.getChild(0));
            assertInstanceOf(ValueNode.class, eq.getChild(1));
            assertEquals("a", ((ColumnNode) eq.getChild(0)).getColumn());
            assertEquals(123, ((ValueNode) eq.getChild(1)).getValue());
        }

        {
            Node neq = translate("db:b != 321");
            assertInstanceOf(NotEqualNode.class, neq);
            assertEquals(2, neq.getChildrenCount());
            assertInstanceOf(ColumnNode.class, neq.getChild(0));
            assertInstanceOf(ValueNode.class, neq.getChild(1));
            assertEquals("b", ((ColumnNode) neq.getChild(0)).getColumn());
            assertEquals(321, ((ValueNode) neq.getChild(1)).getValue());
        }
    }

    @Test
    public void translateLike() {
        {
            // no support for escape char in exp parser
            Node like = translator.translate(ExpressionFactory.likeExp("a", "abc", '~'));
            assertInstanceOf(LikeNode.class, like);
            assertEquals('~', ((LikeNode) like).getEscape());
            assertFalse(((LikeNode) like).isIgnoreCase());
            assertFalse(((LikeNode) like).isNot());
            assertEquals(2, like.getChildrenCount());
            assertInstanceOf(ColumnNode.class, like.getChild(0));
            assertInstanceOf(ValueNode.class, like.getChild(1));
            assertEquals("a", ((ColumnNode) like.getChild(0)).getColumn());
            assertEquals("abc", ((ValueNode) like.getChild(1)).getValue());
        }

        {
            Node notLike = translate("db:a not like 'abc'");
            assertInstanceOf(LikeNode.class, notLike);
            assertEquals(0, ((LikeNode) notLike).getEscape());
            assertFalse(((LikeNode) notLike).isIgnoreCase());
            assertTrue(((LikeNode) notLike).isNot());
            assertEquals(2, notLike.getChildrenCount());
            assertInstanceOf(ColumnNode.class, notLike.getChild(0));
            assertInstanceOf(ValueNode.class, notLike.getChild(1));
            assertEquals("a", ((ColumnNode) notLike.getChild(0)).getColumn());
            assertEquals("abc", ((ValueNode) notLike.getChild(1)).getValue());
        }

        {
            Node likeIgnoreCase = translate("db:a likeIgnoreCase 'abc'");
            assertInstanceOf(LikeNode.class, likeIgnoreCase);
            assertEquals(0, ((LikeNode) likeIgnoreCase).getEscape());
            assertTrue(((LikeNode) likeIgnoreCase).isIgnoreCase());
            assertFalse(((LikeNode) likeIgnoreCase).isNot());
            assertEquals(2, likeIgnoreCase.getChildrenCount());
            assertInstanceOf(ColumnNode.class, likeIgnoreCase.getChild(0));
            assertInstanceOf(ValueNode.class, likeIgnoreCase.getChild(1));
            assertEquals("a", ((ColumnNode) likeIgnoreCase.getChild(0)).getColumn());
            assertEquals("abc", ((ValueNode) likeIgnoreCase.getChild(1)).getValue());
        }

        {
            Node notLikeIgnoreCase = translate("db:a not likeIgnoreCase 'abc'");
            assertInstanceOf(LikeNode.class, notLikeIgnoreCase);
            assertEquals(0, ((LikeNode) notLikeIgnoreCase).getEscape());
            assertTrue(((LikeNode) notLikeIgnoreCase).isIgnoreCase());
            assertTrue(((LikeNode) notLikeIgnoreCase).isNot());
            assertEquals(2, notLikeIgnoreCase.getChildrenCount());
            assertInstanceOf(ColumnNode.class, notLikeIgnoreCase.getChild(0));
            assertInstanceOf(ValueNode.class, notLikeIgnoreCase.getChild(1));
            assertEquals("a", ((ColumnNode) notLikeIgnoreCase.getChild(0)).getColumn());
            assertEquals("abc", ((ValueNode) notLikeIgnoreCase.getChild(1)).getValue());
        }
    }

    @Test
    public void translateFunctionCall() {
        {
            Node function = translate("trim(a)");
            assertInstanceOf(FunctionNode.class, function);
            assertEquals("TRIM", ((FunctionNode) function).getFunctionName());
            assertNull(((FunctionNode) function).getAlias());
            assertEquals(1, function.getChildrenCount());
            assertInstanceOf(ColumnNode.class, function.getChild(0));
            assertEquals("a", ((ColumnNode) function.getChild(0)).getColumn());
        }

        {
            Node function = translate("year(a)");
            assertInstanceOf(FunctionNode.class, function);
            assertEquals("YEAR", ((FunctionNode) function).getFunctionName());
            assertNull(((FunctionNode) function).getAlias());
            assertEquals(1, function.getChildrenCount());
            assertInstanceOf(ColumnNode.class, function.getChild(0));
            assertEquals("a", ((ColumnNode) function.getChild(0)).getColumn());
        }
    }

    @Test
    public void translateMathExp() {
        {
            Node op = translate("1 + 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals("+", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 - 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals("-", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 / 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals("/", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 * 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals("*", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("-2");
            assertInstanceOf(NegateNode.class, op);
            assertEquals(1, op.getChildrenCount());
        }

        {
            Node op = translate("1 & 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals("&", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 | 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals("|", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 ^ 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals("^", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 << 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals("<<", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 >> 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals(">>", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

    }

    @Test
    public void translateComparison() {
        {
            Node op = translate("a < 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals("<", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("a > 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals(">", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("a <= 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals("<=", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("a >= 2");
            assertInstanceOf(OpExpressionNode.class, op);
            assertEquals(">=", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }
    }

    @Test
    public void translateConst() {
        {
            Node op = translate("true");
            assertInstanceOf(TextNode.class, op);
            assertEquals(" 1=1", ((TextNode)op).getText());
            assertEquals(0, op.getChildrenCount());
        }

        {
            Node op = translate("false");
            assertInstanceOf(TextNode.class, op);
            assertEquals(" 1=0", ((TextNode)op).getText());
            assertEquals(0, op.getChildrenCount());
        }

        {
            Node op = translator.translate(new ASTAsterisk());
            assertInstanceOf(TextNode.class, op);
            assertEquals(" *", ((TextNode)op).getText());
            assertEquals(0, op.getChildrenCount());
        }
    }

    @Test
    public void translateExists() {
        Node exists = translator.translate(ExpressionFactory.exists(ObjectSelect.dbQuery("mock")));
        assertInstanceOf(FunctionNode.class, exists);
        assertEquals("EXISTS", ((FunctionNode) exists).getFunctionName());
        assertEquals(1, exists.getChildrenCount());
        assertInstanceOf(SelectNode.class, exists.getChild(0));
    }

    @Test
    public void translateFullObject() {
        Node fullObj = translator.translate(ExpressionFactory.fullObjectExp());
        assertInstanceOf(ColumnNode.class, fullObj);
        ColumnNode columnNode = (ColumnNode)fullObj;
        assertEquals("b", columnNode.getColumn());
    }

    @Test
    public void translateEnclosingObject() {
        // can't translate enclosing exp not in nested query
        assertThrows(CayenneRuntimeException.class, () ->
            translator.translate(ExpressionFactory.enclosingObjectExp(ExpressionFactory.dbPathExp("a"))));
    }

    @Test
    public void translateAnd() {
        Node and = translate("true and false");
        assertNotNull(and);
        assertInstanceOf(OpExpressionNode.class, and);
        assertEquals("AND", ((OpExpressionNode)and).getOp());
        assertEquals(2, and.getChildrenCount());
        assertInstanceOf(TextNode.class, and.getChild(0));
        assertEquals(" 1=1", ((TextNode)and.getChild(0)).getText());
        assertInstanceOf(TextNode.class, and.getChild(1));
        assertEquals(" 1=0", ((TextNode)and.getChild(1)).getText());
    }

    @Test
    public void translateComplexAnd() {
        Node and = translate("a < 2 and b in (5,6) and b = 7");
        assertNotNull(and);

        assertInstanceOf(OpExpressionNode.class, and);
        assertEquals("AND", ((OpExpressionNode)and).getOp());
        assertEquals(3, and.getChildrenCount());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new DefaultSQLAppendable(null), null);
        and.visit(visitor);
        assertEquals(" m.a < 2 AND m.b IN (5, 6) AND m.b = 7", visitor.getSQLString());
    }

    @Test
    public void translateOr() {
        Node or = translate("true or false");
        assertNotNull(or);
        assertInstanceOf(OpExpressionNode.class, or);
        assertEquals("OR", ((OpExpressionNode)or).getOp());
        assertEquals(2, or.getChildrenCount());
        assertInstanceOf(TextNode.class, or.getChild(0));
        assertInstanceOf(TextNode.class, or.getChild(1));
    }

    @Test
    public void translateNullComparison() {
        Node or = translate("a > null");
        assertNotNull(or);
        assertInstanceOf(OpExpressionNode.class, or);
        assertEquals(">", ((OpExpressionNode)or).getOp());
        assertEquals(2, or.getChildrenCount());
        assertInstanceOf(ColumnNode.class, or.getChild(0));
        assertInstanceOf(ValueNode.class, or.getChild(1));
    }

    @Test
    public void translateComplexExp() {
        Node result = translate("(a >= 1 + 2 / 3 << 4) and (db:b != true)");

        {
            assertNotNull(result);
            assertInstanceOf(OpExpressionNode.class, result);
            assertEquals("AND", ((OpExpressionNode) result).getOp());
            assertEquals(2, result.getChildrenCount());
            assertInstanceOf(OpExpressionNode.class, result.getChild(0));
            assertInstanceOf(NotEqualNode.class, result.getChild(1));
        }

        {
            OpExpressionNode left = (OpExpressionNode) result.getChild(0);
            assertEquals(">=", left.getOp());
            assertEquals(2, left.getChildrenCount());
            assertInstanceOf(ColumnNode.class, left.getChild(0));
            assertEquals("a", ((ColumnNode)left.getChild(0)).getColumn());
            assertInstanceOf(OpExpressionNode.class, left.getChild(1));
            {
                OpExpressionNode shift = (OpExpressionNode)left.getChild(1);
                assertEquals("<<", shift.getOp());
                assertInstanceOf(OpExpressionNode.class, shift.getChild(0));
                {
                    OpExpressionNode plus = (OpExpressionNode)shift.getChild(0);
                    assertEquals("+", plus.getOp());
                    assertEquals(2, plus.getChildrenCount());
                    assertInstanceOf(ValueNode.class, plus.getChild(0));
                    assertEquals(1, ((ValueNode)plus.getChild(0)).getValue());
                    assertInstanceOf(OpExpressionNode.class, plus.getChild(1));
                    {
                        OpExpressionNode div = (OpExpressionNode)plus.getChild(1);
                        assertEquals("/", div.getOp());
                        assertEquals(2, div.getChildrenCount());
                        assertInstanceOf(ValueNode.class, div.getChild(0));
                        assertEquals(2, ((ValueNode)div.getChild(0)).getValue());
                        assertInstanceOf(ValueNode.class, div.getChild(0));
                        assertEquals(3, ((ValueNode)div.getChild(1)).getValue());
                    }
                }
                assertInstanceOf(ValueNode.class, shift.getChild(1));
                assertEquals(4, ((ValueNode)shift.getChild(1)).getValue());
            }
        }

        {
            NotEqualNode right = (NotEqualNode) result.getChild(1);
            assertEquals(2, right.getChildrenCount());
            assertInstanceOf(ColumnNode.class, right.getChild(0));
            assertEquals("b", ((ColumnNode)right.getChild(0)).getColumn());
            assertInstanceOf(ValueNode.class, right.getChild(1));
            assertEquals(Boolean.TRUE, ((ValueNode)right.getChild(1)).getValue());
        }
    }

    @Test
    public void translateStringScalar() {
        Expression scalarValue = ExpressionFactory.wrapScalarValue("abc");
        Node translate = translator.translate(scalarValue);
        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new DefaultSQLAppendable(null), null);
        translate.visit(visitor);
        assertInstanceOf(ASTScalar.class, scalarValue);
        assertEquals(" 'abc'",visitor.getSQLString());
    }

    @Test
    public void translateNumberScalar() {
        Expression scalarValue = ExpressionFactory.wrapScalarValue(123);
        Node translate = translator.translate(scalarValue);
        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new DefaultSQLAppendable(null), null);
        translate.visit(visitor);
        assertInstanceOf(ASTScalar.class, scalarValue);
        assertEquals(" 123",visitor.getSQLString());
    }

    @Test
    public void needBindingValueNode() {
        Expression scalarValue = ExpressionFactory.wrapScalarValue(123);
        Node translatedNode = translator.translate(scalarValue);
        assertInstanceOf(ValueNode.class, translatedNode);
        assertFalse(((ValueNode)translatedNode).isNeedBinding());
    }

    @Test
    public void translateArrayScalar() {
        Expression value = ExpressionFactory.wrapScalarValue( new int[]{ 1,2,3,4,5,6,7,8,9,10 } );
        CayenneRuntimeException exception = assertThrows(
                CayenneRuntimeException.class,
                () -> translator.translate(value)
        );
        assertTrue(exception.getMessage().contains(QualifierTranslator.ERR_MSG_ARRAYS_NOT_SUPPORTED));
    }

    @Test
    public void translateCollectionScalar() {
        Expression value = ExpressionFactory.wrapScalarValue(List.of(1,2,3));
        CayenneRuntimeException exception = assertThrows(CayenneRuntimeException.class,
                () -> translator.translate(value)
        );
        assertTrue(exception.getMessage().contains(QualifierTranslator.ERR_MSG_ARRAYS_NOT_SUPPORTED));
    }

    private Node translate(String s) {
        return translator.translate(ExpressionFactory.exp(s));
    }

}
