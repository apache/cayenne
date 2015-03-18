package de.jexp.jequel.expression;

import static de.jexp.jequel.expression.Expressions.*;

import de.jexp.jequel.expression.logical.BooleanExpression;
import de.jexp.jequel.expression.numeric.NumericLiteral;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ExpressionsTest extends TestCase {
    public void testNumericConversion() {
        NumericLiteral expression = (NumericLiteral) e(1);
        assertEquals(1, expression.getValue().intValue());
    }

    public void testBooleanTrueConversion() {
        BooleanExpression expression = (BooleanExpression) e(true);
        assertSame(TRUE, expression);
    }

    public void testBooleanFalseConversion() {
        BooleanExpression expression = (BooleanExpression) e(false);
        assertSame(FALSE, expression);
    }

    public void testStringConversion() {
        StringExpression expression = (StringExpression) e("abc");
        assertEquals("abc", expression.getValue());
        assertEquals("'abc'", expression.toString());
    }

    public void testNullConversion() {
        Expression expression = e((Object) null);
        assertEquals("NULL", expression.toString());
    }

    public void testIterableConversion() {
        CompoundExpression expressions = (CompoundExpression) e(3, 7);
        Iterable iterable = expressions.getExpressions();
        Iterator it = iterable.iterator();
        assertEquals(3, ((NumericLiteral) it.next()).getValue().intValue());
        assertEquals(7, ((NumericLiteral) it.next()).getValue().intValue());
    }

    public void testSqlHackString() {
        assertEquals("nvl(A,1)", sql("nvl(A,1)").toString());
    }

    public void testNamedParameter() {
        assertEquals(":article_oid", named("article_oid").toString());
    }

    public void testNamedParameterWithValue() {
        ParamExpression<Integer> expression = named("article_oid", 10);
        assertTrue(expression.isNamedExpression());
        assertEquals("article_oid", expression.getLiteral());
        assertEquals(":article_oid", expression.toString());
        assertEquals(10, expression.getValue().intValue());
    }

    public void testParameter() {
        ParamExpression paramExpression = param("article_oid");
        assertEquals(null, paramExpression.getLiteral());
        assertFalse(paramExpression.isNamedExpression());
        assertEquals("?", paramExpression.toString());
        assertEquals("article_oid", paramExpression.getValue());
    }

    public void testCollectionParameter() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        ParamExpression paramExpression = param(list);
        assertFalse(paramExpression.isNamedExpression());
        assertEquals(null, paramExpression.getLiteral());
        assertEquals("?, ?, ?", paramExpression.toString());
        assertEquals(list, paramExpression.getValue());
    }
}
