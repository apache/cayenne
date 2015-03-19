package de.jexp.jequel.expression;

import static de.jexp.jequel.tables.TEST_TABLES.*;
import junit.framework.TestCase;

public class NumericExpressionTest extends TestCase {
    public void testNumericExpression() {
        assertEquals("1", Expressions.e(1).toString());
    }

    public void testEqExpression() {
        TestCase.assertEquals("ARTICLE.OID = 1", TEST_TABLES.ARTICLE.OID.eq(1).toString());
    }

    public void testGtExpression() {
        TestCase.assertEquals("ARTICLE.OID > 1", TEST_TABLES.ARTICLE.OID.gt(1).toString());
    }

    public void testGeExpression() {
        TestCase.assertEquals("ARTICLE.OID >= 1", TEST_TABLES.ARTICLE.OID.ge(1).toString());
    }

    public void testLtExpression() {
        TestCase.assertEquals("ARTICLE.OID < 1", TEST_TABLES.ARTICLE.OID.lt(1).toString());
    }

    public void testLeExpression() {
        TestCase.assertEquals("ARTICLE.OID <= 1", TEST_TABLES.ARTICLE.OID.le(1).toString());
    }

    public void testNeExpression() {
        TestCase.assertEquals("ARTICLE.OID != 1", TEST_TABLES.ARTICLE.OID.ne(1).toString());
    }

    public void testBetweenExpression() {
        TestCase.assertEquals("ARTICLE.OID between 1 and 100", TEST_TABLES.ARTICLE.OID.between(1, 100).toString());
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
        TestCase.assertEquals("ARTICLE.OID + 1", TEST_TABLES.ARTICLE.OID.plus(1).toString());
    }

    public void testMinusExpression() {
        TestCase.assertEquals("ARTICLE.OID - 1", TEST_TABLES.ARTICLE.OID.minus(1).toString());
    }

    public void testTimesExpression() {
        TestCase.assertEquals("ARTICLE.OID * 1", TEST_TABLES.ARTICLE.OID.times(1).toString());
    }

    public void testByExpression() {
        TestCase.assertEquals("ARTICLE.OID / 1", TEST_TABLES.ARTICLE.OID.by(1).toString());
    }


}