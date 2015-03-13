package de.jexp.jequel.expression;

import static de.jexp.jequel.expression.Expressions.*;
import de.jexp.jequel.tables.TEST_TABLES;
import junit.framework.TestCase;

/**
 * @author mh14 @ jexp.de
 * @copyright (c) 2007 jexp.de
 * @since 18.10.2007 15:17:02
 */
public class UnaryExpressionTest extends TestCase {
    public void testMin() {
        assertEquals("min(1)", Expressions.min(1).toString());
    }

    public void testMax() {
        assertEquals("max(NULL)", Expressions.max(Expressions.NULL).toString());
    }

    public void testCombined() {
        assertEquals("not(max(FALSE))", Expressions.not(Expressions.max(Expressions.FALSE)).toString());
    }

    public void testCombinedField() {
        assertEquals("not(max(ARTICLE.OID))", Expressions.not(Expressions.max(TEST_TABLES.ARTICLE.OID)).toString());
    }
}