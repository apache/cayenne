package org.objectstyle.cayenne.exp;

import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ListExpressionTst extends CayenneTestCase {
    // non-existent type
    private static final int defaultType = -33;
    protected ListExpression expr;

    protected void setUp() throws Exception {
        expr = new ListExpression(defaultType);
    }

    public void testGetType() throws Exception {
        assertEquals(defaultType, expr.getType());
    }

    public void testGetOperandCount() throws Exception {
        assertEquals(0, expr.getOperandCount());

        expr.appendOperand(new Object());
        assertEquals(1, expr.getOperandCount());

        expr.appendOperand(new Object());
        assertEquals(2, expr.getOperandCount());
    }

    public void testGetOperandAtIndex() throws Exception {
        try {
            expr.getOperand(0);
            fail();
        } catch (Exception ex) {
            // exception expected..
        }
        
        Object obj = new Object();
        expr.appendOperand(obj);
        assertSame(obj, expr.getOperand(0));
    }

    public void testSetOperandAtIndex() throws Exception {
        Object o1 = new Object();
        Object o2 = new Object();

        expr.setOperand(0, o1);
        expr.setOperand(1, o2);
        assertSame(o1, expr.getOperand(0));
        assertSame(o2, expr.getOperand(1));

        try {
            expr.setOperand(3, o1);
            fail();
        } catch (Exception ex) {
            // exception expected..
        }
    }

}
