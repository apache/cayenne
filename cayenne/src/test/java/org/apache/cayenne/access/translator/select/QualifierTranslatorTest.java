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
import org.apache.cayenne.access.sqlbuilder.StringBuilderAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.*;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTAsterisk;
import org.apache.cayenne.exp.parser.ASTScalar;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.map.*;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class QualifierTranslatorTest {

    private QualifierTranslator translator;

    @Before
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
        TranslatorContext context = new MockTranslatorContext(wrapper, resolver);
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
            assertThat(in, instanceOf(InNode.class));
            assertEquals(2, in.getChildrenCount());
            assertFalse(((InNode) in).isNot());
            assertThat(in.getChild(0), instanceOf(ColumnNode.class));
            assertThat(in.getChild(1), instanceOf(ValueNode.class));
            assertEquals("a", ((ColumnNode) in.getChild(0)).getColumn());
            assertArrayEquals(new Object[]{1, 2}, (Object[]) ((ValueNode) in.getChild(1)).getValue());
        }

        {
            Node in = translator.translate(ExpressionFactory.inExp("a", Arrays.asList(1, 2, 3)));
            assertThat(in, instanceOf(InNode.class));
            assertEquals(2, in.getChildrenCount());
            assertFalse(((InNode) in).isNot());
            assertThat(in.getChild(0), instanceOf(ColumnNode.class));
            assertThat(in.getChild(1), instanceOf(ValueNode.class));
            assertEquals("a", ((ColumnNode) in.getChild(0)).getColumn());
            assertArrayEquals(new Object[]{1, 2, 3}, (Object[]) ((ValueNode) in.getChild(1)).getValue());
        }

        {
            Node notIn = translate("db:a not in (1,2)");
            assertThat(notIn, instanceOf(InNode.class));
            assertEquals(2, notIn.getChildrenCount());
            assertTrue(((InNode) notIn).isNot());
            assertThat(notIn.getChild(0), instanceOf(ColumnNode.class));
            assertThat(notIn.getChild(1), instanceOf(ValueNode.class));
            assertEquals("a", ((ColumnNode) notIn.getChild(0)).getColumn());
            assertArrayEquals(new Object[]{1, 2}, (Object[]) ((ValueNode) notIn.getChild(1)).getValue());
        }

    }

    @Test
    public void translateBetween() {
        {
            Node between = translate("db:a between 1 and 5");
            assertThat(between, instanceOf(BetweenNode.class));
            assertEquals(3, between.getChildrenCount());
            assertFalse(((BetweenNode) between).isNot());
            assertThat(between.getChild(0), instanceOf(ColumnNode.class));
            assertThat(between.getChild(1), instanceOf(ValueNode.class));
            assertThat(between.getChild(2), instanceOf(ValueNode.class));
        }

        {
            Node notBetween = translate("db:b not between 2 and 6");
            assertThat(notBetween, instanceOf(BetweenNode.class));
            assertEquals(3, notBetween.getChildrenCount());
            assertTrue(((BetweenNode) notBetween).isNot());
            assertThat(notBetween.getChild(0), instanceOf(ColumnNode.class));
            assertThat(notBetween.getChild(1), instanceOf(ValueNode.class));
            assertThat(notBetween.getChild(2), instanceOf(ValueNode.class));
        }
    }

    @Test
    public void translateNegate_NullScalar() {
        Node node = translate("-(null)");
        assertThat(node, instanceOf(ValueNode.class));
        assertNull(((ValueNode) node).getValue());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new StringBuilderAppendable());
        node.visit(visitor);
        assertEquals(" NULL", visitor.getSQLString());
    }

    @Test
    public void translateNegate_NumberScalar() {
        Node node = translate("-1");
        assertThat(node, instanceOf(FunctionNode.class));

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new StringBuilderAppendable());
        node.visit(visitor);
        assertEquals(" - 1", visitor.getSQLString());
    }

    @Test
    public void translateNegate_NullParam() {
        Expression exp = ExpressionFactory.exp("-$v").params(Collections.singletonMap("v", null));
        Node node = translator.translate(exp);
        assertThat(node, instanceOf(ValueNode.class));
        assertNull(((ValueNode) node).getValue());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new StringBuilderAppendable());
        node.visit(visitor);
        assertEquals(" NULL", visitor.getSQLString());
    }

    @Test
    public void translateNot() {
        Node not = translate("not true");
        assertThat(not, instanceOf(NotNode.class));
        assertEquals(1, not.getChildrenCount());
        assertThat(not.getChild(0), instanceOf(TextNode.class));
        assertEquals(" 1=1", ((TextNode)not.getChild(0)).getText());
    }

    @Test
    public void translateBitwiseNot() {
        Node not = translate("~123");
        assertThat(not, instanceOf(BitwiseNotNode.class));
        assertEquals(1, not.getChildrenCount());
        assertThat(not.getChild(0), instanceOf(ValueNode.class));
        assertEquals(123, ((ValueNode)not.getChild(0)).getValue());
    }

    @Test
    public void translateEqual() {
        {
            Node eq = translate("db:a = 123");
            assertThat(eq, instanceOf(EqualNode.class));
            assertEquals(2, eq.getChildrenCount());
            assertThat(eq.getChild(0), instanceOf(ColumnNode.class));
            assertThat(eq.getChild(1), instanceOf(ValueNode.class));
            assertEquals("a", ((ColumnNode) eq.getChild(0)).getColumn());
            assertEquals(123, ((ValueNode) eq.getChild(1)).getValue());
        }

        {
            Node neq = translate("db:b != 321");
            assertThat(neq, instanceOf(NotEqualNode.class));
            assertEquals(2, neq.getChildrenCount());
            assertThat(neq.getChild(0), instanceOf(ColumnNode.class));
            assertThat(neq.getChild(1), instanceOf(ValueNode.class));
            assertEquals("b", ((ColumnNode) neq.getChild(0)).getColumn());
            assertEquals(321, ((ValueNode) neq.getChild(1)).getValue());
        }
    }

    @Test
    public void translateLike() {
        {
            // no support for escape char in exp parser
            Node like = translator.translate(ExpressionFactory.likeExp("a", "abc", '~'));
            assertThat(like, instanceOf(LikeNode.class));
            assertEquals('~', ((LikeNode) like).getEscape());
            assertFalse(((LikeNode) like).isIgnoreCase());
            assertFalse(((LikeNode) like).isNot());
            assertEquals(2, like.getChildrenCount());
            assertThat(like.getChild(0), instanceOf(ColumnNode.class));
            assertThat(like.getChild(1), instanceOf(ValueNode.class));
            assertEquals("a", ((ColumnNode) like.getChild(0)).getColumn());
            assertEquals("abc", ((ValueNode) like.getChild(1)).getValue());
        }

        {
            Node notLike = translate("db:a not like 'abc'");
            assertThat(notLike, instanceOf(LikeNode.class));
            assertEquals(0, ((LikeNode) notLike).getEscape());
            assertFalse(((LikeNode) notLike).isIgnoreCase());
            assertTrue(((LikeNode) notLike).isNot());
            assertEquals(2, notLike.getChildrenCount());
            assertThat(notLike.getChild(0), instanceOf(ColumnNode.class));
            assertThat(notLike.getChild(1), instanceOf(ValueNode.class));
            assertEquals("a", ((ColumnNode) notLike.getChild(0)).getColumn());
            assertEquals("abc", ((ValueNode) notLike.getChild(1)).getValue());
        }

        {
            Node likeIgnoreCase = translate("db:a likeIgnoreCase 'abc'");
            assertThat(likeIgnoreCase, instanceOf(LikeNode.class));
            assertEquals(0, ((LikeNode) likeIgnoreCase).getEscape());
            assertTrue(((LikeNode) likeIgnoreCase).isIgnoreCase());
            assertFalse(((LikeNode) likeIgnoreCase).isNot());
            assertEquals(2, likeIgnoreCase.getChildrenCount());
            assertThat(likeIgnoreCase.getChild(0), instanceOf(ColumnNode.class));
            assertThat(likeIgnoreCase.getChild(1), instanceOf(ValueNode.class));
            assertEquals("a", ((ColumnNode) likeIgnoreCase.getChild(0)).getColumn());
            assertEquals("abc", ((ValueNode) likeIgnoreCase.getChild(1)).getValue());
        }

        {
            Node notLikeIgnoreCase = translate("db:a not likeIgnoreCase 'abc'");
            assertThat(notLikeIgnoreCase, instanceOf(LikeNode.class));
            assertEquals(0, ((LikeNode) notLikeIgnoreCase).getEscape());
            assertTrue(((LikeNode) notLikeIgnoreCase).isIgnoreCase());
            assertTrue(((LikeNode) notLikeIgnoreCase).isNot());
            assertEquals(2, notLikeIgnoreCase.getChildrenCount());
            assertThat(notLikeIgnoreCase.getChild(0), instanceOf(ColumnNode.class));
            assertThat(notLikeIgnoreCase.getChild(1), instanceOf(ValueNode.class));
            assertEquals("a", ((ColumnNode) notLikeIgnoreCase.getChild(0)).getColumn());
            assertEquals("abc", ((ValueNode) notLikeIgnoreCase.getChild(1)).getValue());
        }
    }

    @Test
    public void translateFunctionCall() {
        {
            Node function = translate("trim(a)");
            assertThat(function, instanceOf(FunctionNode.class));
            assertEquals("TRIM", ((FunctionNode) function).getFunctionName());
            assertNull(((FunctionNode) function).getAlias());
            assertEquals(1, function.getChildrenCount());
            assertThat(function.getChild(0), instanceOf(ColumnNode.class));
            assertEquals("a", ((ColumnNode) function.getChild(0)).getColumn());
        }

        {
            Node function = translate("year(a)");
            assertThat(function, instanceOf(FunctionNode.class));
            assertEquals("YEAR", ((FunctionNode) function).getFunctionName());
            assertNull(((FunctionNode) function).getAlias());
            assertEquals(1, function.getChildrenCount());
            assertThat(function.getChild(0), instanceOf(ColumnNode.class));
            assertEquals("a", ((ColumnNode) function.getChild(0)).getColumn());
        }
    }

    @Test
    public void translateMathExp() {
        {
            Node op = translate("1 + 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals("+", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 - 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals("-", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 / 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals("/", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 * 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals("*", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("-2");
            assertThat(op, instanceOf(FunctionNode.class));
            assertEquals("-", ((FunctionNode)op).getFunctionName());
            assertNull(((FunctionNode) op).getAlias());
            assertEquals(1, op.getChildrenCount());
        }

        {
            Node op = translate("1 & 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals("&", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 | 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals("|", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 ^ 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals("^", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 << 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals("<<", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("1 >> 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals(">>", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

    }

    @Test
    public void translateComparison() {
        {
            Node op = translate("a < 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals("<", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("a > 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals(">", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("a <= 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals("<=", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }

        {
            Node op = translate("a >= 2");
            assertThat(op, instanceOf(OpExpressionNode.class));
            assertEquals(">=", ((OpExpressionNode)op).getOp());
            assertEquals(2, op.getChildrenCount());
        }
    }

    @Test
    public void translateConst() {
        {
            Node op = translate("true");
            assertThat(op, instanceOf(TextNode.class));
            assertEquals(" 1=1", ((TextNode)op).getText());
            assertEquals(0, op.getChildrenCount());
        }

        {
            Node op = translate("false");
            assertThat(op, instanceOf(TextNode.class));
            assertEquals(" 1=0", ((TextNode)op).getText());
            assertEquals(0, op.getChildrenCount());
        }

        {
            Node op = translator.translate(new ASTAsterisk());
            assertThat(op, instanceOf(TextNode.class));
            assertEquals(" *", ((TextNode)op).getText());
            assertEquals(0, op.getChildrenCount());
        }
    }

    @Test
    public void translateExists() {
        Node exists = translator.translate(ExpressionFactory.exists(ObjectSelect.dbQuery("mock")));
        assertThat(exists, instanceOf(FunctionNode.class));
        assertEquals("EXISTS", ((FunctionNode) exists).getFunctionName());
        assertEquals(1, exists.getChildrenCount());
        assertThat(exists.getChild(0), instanceOf(SelectNode.class));
    }

    @Test
    public void translateFullObject() {
        Node fullObj = translator.translate(ExpressionFactory.fullObjectExp());
        assertThat(fullObj, instanceOf(ColumnNode.class));
        ColumnNode columnNode = (ColumnNode)fullObj;
        assertEquals("b", columnNode.getColumn());
    }

    @Test(expected = CayenneRuntimeException.class)
    public void translateEnclosingObject() {
        // can't translate enclosing exp not in nested query
        translator.translate(ExpressionFactory.enclosingObjectExp(ExpressionFactory.dbPathExp("a")));
    }

    @Test
    public void translateAnd() {
        Node and = translate("true and false");
        assertNotNull(and);
        assertThat(and, instanceOf(OpExpressionNode.class));
        assertEquals("AND", ((OpExpressionNode)and).getOp());
        assertEquals(2, and.getChildrenCount());
        assertThat(and.getChild(0), instanceOf(TextNode.class));
        assertEquals(" 1=1", ((TextNode)and.getChild(0)).getText());
        assertThat(and.getChild(1), instanceOf(TextNode.class));
        assertEquals(" 1=0", ((TextNode)and.getChild(1)).getText());
    }

    @Test
    public void translateComplexAnd() {
        Node and = translate("a < 2 and b in (5,6) and b = 7");
        assertNotNull(and);

        assertThat(and, instanceOf(OpExpressionNode.class));
        assertEquals("AND", ((OpExpressionNode)and).getOp());
        assertEquals(3, and.getChildrenCount());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new StringBuilderAppendable());
        and.visit(visitor);
        assertEquals(" ( t0.a < 2 ) AND t0.b IN ( 5, 6) AND ( t0.b = 7 )", visitor.getSQLString());
    }

    @Test
    public void translateOr() {
        Node or = translate("true or false");
        assertNotNull(or);
        assertThat(or, instanceOf(OpExpressionNode.class));
        assertEquals("OR", ((OpExpressionNode)or).getOp());
        assertEquals(2, or.getChildrenCount());
        assertThat(or.getChild(0), instanceOf(TextNode.class));
        assertThat(or.getChild(1), instanceOf(TextNode.class));
    }

    @Test
    public void translateNullComparison() {
        Node or = translate("a > null");
        assertNotNull(or);
        assertThat(or, instanceOf(OpExpressionNode.class));
        assertEquals(">", ((OpExpressionNode)or).getOp());
        assertEquals(2, or.getChildrenCount());
        assertThat(or.getChild(0), instanceOf(ColumnNode.class));
        assertThat(or.getChild(1), instanceOf(ValueNode.class));
    }

    @Test
    public void translateComplexExp() {
        Node result = translate("(a >= 1 + 2 / 3 << 4) and (db:b != true)");

        {
            assertNotNull(result);
            assertThat(result, instanceOf(OpExpressionNode.class));
            assertEquals("AND", ((OpExpressionNode) result).getOp());
            assertEquals(2, result.getChildrenCount());
            assertThat(result.getChild(0), instanceOf(OpExpressionNode.class));
            assertThat(result.getChild(1), instanceOf(NotEqualNode.class));
        }

        {
            OpExpressionNode left = (OpExpressionNode) result.getChild(0);
            assertEquals(">=", left.getOp());
            assertEquals(2, left.getChildrenCount());
            assertThat(left.getChild(0), instanceOf(ColumnNode.class));
            assertEquals("a", ((ColumnNode)left.getChild(0)).getColumn());
            assertThat(left.getChild(1), instanceOf(OpExpressionNode.class));
            {
                OpExpressionNode shift = (OpExpressionNode)left.getChild(1);
                assertEquals("<<", shift.getOp());
                assertThat(shift.getChild(0), instanceOf(OpExpressionNode.class));
                {
                    OpExpressionNode plus = (OpExpressionNode)shift.getChild(0);
                    assertEquals("+", plus.getOp());
                    assertEquals(2, plus.getChildrenCount());
                    assertThat(plus.getChild(0), instanceOf(ValueNode.class));
                    assertEquals(1, ((ValueNode)plus.getChild(0)).getValue());
                    assertThat(plus.getChild(1), instanceOf(OpExpressionNode.class));
                    {
                        OpExpressionNode div = (OpExpressionNode)plus.getChild(1);
                        assertEquals("/", div.getOp());
                        assertEquals(2, div.getChildrenCount());
                        assertThat(div.getChild(0), instanceOf(ValueNode.class));
                        assertEquals(2, ((ValueNode)div.getChild(0)).getValue());
                        assertThat(div.getChild(0), instanceOf(ValueNode.class));
                        assertEquals(3, ((ValueNode)div.getChild(1)).getValue());
                    }
                }
                assertThat(shift.getChild(1), instanceOf(ValueNode.class));
                assertEquals(4, ((ValueNode)shift.getChild(1)).getValue());
            }
        }

        {
            NotEqualNode right = (NotEqualNode) result.getChild(1);
            assertEquals(2, right.getChildrenCount());
            assertThat(right.getChild(0), instanceOf(ColumnNode.class));
            assertEquals("b", ((ColumnNode)right.getChild(0)).getColumn());
            assertThat(right.getChild(1), instanceOf(ValueNode.class));
            assertEquals(Boolean.TRUE, ((ValueNode)right.getChild(1)).getValue());
        }
    }

    @Test
    public void translateStringScalar() {
        Expression scalarValue = ExpressionFactory.wrapScalarValue("abc");
        Node translate = translator.translate(scalarValue);
        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new StringBuilderAppendable());
        translate.visit(visitor);
        assertThat(scalarValue, instanceOf(ASTScalar.class));
        assertEquals(" 'abc'",visitor.getSQLString());
    }

    @Test
    public void translateNumberScalar() {
        Expression scalarValue = ExpressionFactory.wrapScalarValue(123);
        Node translate = translator.translate(scalarValue);
        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new StringBuilderAppendable());
        translate.visit(visitor);
        assertThat(scalarValue, instanceOf(ASTScalar.class));
        assertEquals(" 123",visitor.getSQLString());
    }

    @Test
    public void needBindingValueNode() {
        Expression scalarValue = ExpressionFactory.wrapScalarValue(123);
        Node translatedNode = translator.translate(scalarValue);
        assertTrue(translatedNode instanceof ValueNode);
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
