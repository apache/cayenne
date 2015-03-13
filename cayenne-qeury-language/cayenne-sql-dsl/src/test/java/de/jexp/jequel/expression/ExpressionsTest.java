package de.jexp.jequel.expression;

import static de.jexp.jequel.expression.Expressions.*;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ExpressionsTest extends TestCase {
    public void testNumericConversion() {
        final NumericExpression expression = (NumericExpression) Expressions.e(1);
        assertEquals(1, expression.getValue().intValue());
    }

    public void testBooleanTrueConversion() {
        final BooleanExpression expression = (BooleanExpression) Expressions.e(true);
        assertSame(Expressions.TRUE, expression);
    }

    public void testBooleanFalseConversion() {
        final BooleanExpression expression = (BooleanExpression) Expressions.e(false);
        assertSame(Expressions.FALSE, expression);
    }

    public void testStringConversion() {
        final StringExpression expression = (StringExpression) Expressions.e("abc");
        assertEquals("abc", expression.getValue());
        assertEquals("'abc'", expression.toString());
    }

    public void testNullConversion() {
        final Expression expression = Expressions.e((Object) null);
        assertEquals("NULL", expression.toString());
    }

    public void testIterableConversion() {
        final TupleExpression expressions = (TupleExpression) Expressions.e(3, 7);
        final Iterable iterable = expressions.getExpressions();
        final Iterator it = iterable.iterator();
        assertEquals(3, ((NumericExpression) it.next()).getValue().intValue());
        assertEquals(7, ((NumericExpression) it.next()).getValue().intValue());
    }

    public void testSqlHackString() {
        assertEquals("nvl(A,1)", Expressions.sql("nvl(A,1)").toString());
    }

    public void testNamedParameter() {
        assertEquals(":article_oid", Expressions.named("article_oid").toString());
    }

    public void testNamedParameterWithValue() {
        final ParamExpression<Integer> expression = Expressions.named("article_oid", 10);
        assertTrue(expression.isNamedExpression());
        assertEquals("article_oid", expression.getLiteral());
        assertEquals(":article_oid", expression.toString());
        assertEquals(10, expression.getValue().intValue());
    }

    public void testParameter() {
        final ParamExpression paramExpression = Expressions.param("article_oid");
        assertEquals(null, paramExpression.getLiteral());
        assertFalse(paramExpression.isNamedExpression());
        assertEquals("?", paramExpression.toString());
        assertEquals("article_oid", paramExpression.getValue());
    }

    public void testCollectionParameter() {
        final List<Integer> list = Arrays.asList(1, 2, 3);
        final ParamExpression paramExpression = Expressions.param(list);
        assertFalse(paramExpression.isNamedExpression());
        assertEquals(null, paramExpression.getLiteral());
        assertEquals("?, ?, ?", paramExpression.toString());
        assertEquals(list, paramExpression.getValue());
    }
}
