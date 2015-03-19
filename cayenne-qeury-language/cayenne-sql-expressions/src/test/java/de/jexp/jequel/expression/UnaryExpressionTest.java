package de.jexp.jequel.expression;

import static de.jexp.jequel.expression.Expressions.*;
import de.jexp.jequel.tables.TEST_TABLES;
import junit.framework.TestCase;

public class UnaryExpressionTest extends TestCase {
    public void testMin() {
        assertEquals("min(1)", min(1).toString());
    }

    public void testMax() {
        assertEquals("max(NULL)", max(NULL).toString());
    }

    public void testCombined() {
        assertEquals("min(max(FALSE))", min(max(FALSE)).toString());
    }

    public void testCombinedField() {
        assertEquals("not(max(ARTICLE.OID) = 25)", not(max(TEST_TABLES.ARTICLE.OID).eq(25)).toString());
    }
}