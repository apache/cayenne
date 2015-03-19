package de.jexp.jequel.expression;

import static de.jexp.jequel.expression.Expressions.*;
import junit.framework.TestCase;

public class BooleanExpressionTest extends TestCase {
    public void testTrue() {
        assertEquals("TRUE", Expressions.TRUE.toString());
    }

    public void testFalse() {
        assertEquals("FALSE", Expressions.FALSE.toString());
    }

    public void testNotFalse() {
        assertEquals("not(FALSE)", Expressions.not(Expressions.FALSE).toString());
    }

}