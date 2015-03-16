package de.jexp.jequel.expression;

import de.jexp.jequel.tables.TEST_TABLES;
import junit.framework.TestCase;

public class NumericExpressionTest extends TestCase {
    public void testNumericExpression() {
        assertEquals("1", Expressions.e(1).toString());
    }

    public void testEqExpression() {
        assertEquals("ARTICLE.OID = 1", TEST_TABLES.ARTICLE.OID.eq(1).toString());
    }

    public void testGtExpression() {
        assertEquals("ARTICLE.OID > 1", TEST_TABLES.ARTICLE.OID.gt(1).toString());
    }

    public void testGeExpression() {
        assertEquals("ARTICLE.OID >= 1", TEST_TABLES.ARTICLE.OID.ge(1).toString());
    }

    public void testLtExpression() {
        assertEquals("ARTICLE.OID < 1", TEST_TABLES.ARTICLE.OID.lt(1).toString());
    }

    public void testLeExpression() {
        assertEquals("ARTICLE.OID <= 1", TEST_TABLES.ARTICLE.OID.le(1).toString());
    }

    public void testNeExpression() {
        assertEquals("ARTICLE.OID != 1", TEST_TABLES.ARTICLE.OID.ne(1).toString());
    }

    public void testBetweenExpression() {
        assertEquals("ARTICLE.OID between 1 and 100", TEST_TABLES.ARTICLE.OID.between(1, 100).toString());
    }

    public void testSumExpression() {
        assertEquals("sum(1)", Expressions.sum(1).toString());
    }

    public void testAvgExpression() {
        assertEquals("avg(1)", Expressions.avg(1).toString());
    }

    public void testCountExpression() {
        assertEquals("count(1)", Expressions.count(1).toString());
    }

    public void testCountStarExpression() {
        assertEquals("count(*)", Expressions.count().toString());
    }

    public void testPlusExpression() {
        assertEquals("ARTICLE.OID + 1", TEST_TABLES.ARTICLE.OID.plus(1).toString());
    }

    public void testMinusExpression() {
        assertEquals("ARTICLE.OID - 1", TEST_TABLES.ARTICLE.OID.minus(1).toString());
    }

    public void testTimesExpression() {
        assertEquals("ARTICLE.OID * 1", TEST_TABLES.ARTICLE.OID.times(1).toString());
    }

    public void testByExpression() {
        assertEquals("ARTICLE.OID / 1", TEST_TABLES.ARTICLE.OID.by(1).toString());
    }


}