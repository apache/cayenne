package de.jexp.jequel;

import de.jexp.jequel.expression.DefaultExpressionsFactory;
import de.jexp.jequel.expression.Expression;
import org.junit.Assert;
import org.junit.Test;


public class Sql92ExpressionVisitorTest {

    private Expressions e = new Expressions(new DefaultExpressionsFactory());

    @Test
    public void testMin() {
        assertEquals("min(1)", e.min(1));
    }

    private void assertEquals(String s, Expression expression) {
        Assert.assertEquals(s, expression.toString());
    }

    @Test
    public void testMax() {
        assertEquals("max(NULL)", e.max(e.boolNull()));
    }

    @Test
    public void testCombined() {
        assertEquals("min(max(FALSE))", e.min(e.max(e.boolFalse())));
    }

    @Test
    public void testCombinedField() {
        assertEquals("not(max(ARTICLE.OID) = 25)", e.not(e.max(e.pathNumeric("ARTICLE.OID")).eq(25)));
    }

    @Test
    public void testNumericExpression() {
        assertEquals("1", e.e(1));
    }

    @Test
    public void testEqExpression() {
        assertEquals("ARTICLE.OID = 1", e.path("ARTICLE.OID").eq(1));
    }

    @Test
    public void testGtExpression() {
        assertEquals("ARTICLE.OID > 1", e.path("ARTICLE.OID").gt(1));
    }

    @Test
    public void testGeExpression() {
        assertEquals("ARTICLE.OID >= 1", e.path("ARTICLE.OID").ge(1));
    }

    @Test
    public void testLtExpression() {
        assertEquals("ARTICLE.OID < 1", e.path("ARTICLE.OID").lt(1));
    }

    @Test
    public void testLeExpression() {
        assertEquals("ARTICLE.OID <= 1", e.path("ARTICLE.OID").le(1));
    }

    @Test
    public void testNeExpression() {
        assertEquals("ARTICLE.OID != 1", e.path("ARTICLE.OID").ne(1));
    }

    @Test
    public void testBetweenExpression() {
        assertEquals("ARTICLE.OID between 1 and 100", e.path("ARTICLE.OID").between(1, 100));
    }

    @Test
    public void testSumExpression() {
        assertEquals("sum(1)", e.sum(1));
    }

    @Test
    public void testAvgExpression() {
        assertEquals("avg(1)", e.avg(1));
    }

    @Test
    public void testCountExpression() {
        assertEquals("count(1)", e.count(1));
    }

    @Test
    public void testCountStarExpression() {
        assertEquals("count(*)", e.count());
    }

    @Test
    public void testPlusExpression() {
        assertEquals("ARTICLE.OID + 1", e.pathNumeric("ARTICLE.OID").plus(1));
    }

    @Test
    public void testMinusExpression() {
        assertEquals("ARTICLE.OID - 1", e.pathNumeric("ARTICLE.OID").minus(1));
    }

    @Test
    public void testTimesExpression() {
        assertEquals("ARTICLE.OID * 1", e.pathNumeric("ARTICLE.OID").times(1));
    }

    @Test
    public void testByExpression() {
        assertEquals("ARTICLE.OID / 1", e.pathNumeric("ARTICLE.OID").by(1));
    }

    @Test
    public void testTrue() {
        assertEquals("TRUE", e.boolTrue());
    }

    @Test
    public void testFalse() {
        assertEquals("FALSE", e.boolFalse());
    }

    @Test
    public void testNotFalse() {
        assertEquals("not(FALSE)", e.not(e.boolFalse()));
    }


}