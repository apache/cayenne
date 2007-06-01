package org.objectstyle.cayenne.exp.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.exp.TstTraversalHandler;
import org.objectstyle.cayenne.unit.BasicTestCase;

/**
 * Tests parameterized expressions of the new form introduced in 1.1
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class ParameterizedExpressionTst extends BasicTestCase {
    /**
     * Tests how parameter substitution algorithm works on an expression
     * with no parameters.
     * 
     * @throws Exception
     */
    public void testCopy1() throws Exception {
        Expression e1 = Expression.fromString("k1 = 'v1' or k2 = 'v2' or k3 = 'v3'");
        Expression e2 = e1.expWithParameters(Collections.EMPTY_MAP, true);
        TstTraversalHandler.compareExps(e1, e2);
    }

    /**
     * Tests how parameter substitution algorithm works on an expression
     * with no parameters.
     *
     * @throws Exception
     */
    public void testCopy2() throws Exception {
        Expression e1 =
            Expression.fromString(
                "(k1 = 'v1' and k2 = 'v2' and k3 = 'v3') or (k1 = 'v1')");
        Expression e2 = e1.expWithParameters(Collections.EMPTY_MAP, true);
        TstTraversalHandler.compareExps(e1, e2);
    }

    /**
     * Tests how parameter substitution algorithm works on an expression
     * with no parameters.
     *
     * @throws Exception
     */
    public void testFailOnMissingParams() throws Exception {
        Expression e1 = Expression.fromString("k1 = $test or k2 = 'v2' or k3 = 'v3'");

        try {
            e1.expWithParameters(Collections.EMPTY_MAP, false);
            fail("Parameter was missing, but no exception was thrown.");
        }
        catch (ExpressionException ex) {
            // exception expected
        }
    }

    public void testParams1() throws Exception {
        Expression e1 = Expression.fromString("k1 = $test");

        Map map = new HashMap();
        map.put("test", "xyz");
        Expression e2 = e1.expWithParameters(map, false);
        assertNotNull(e2);
        assertEquals(2, e2.getOperandCount());
        assertEquals(Expression.EQUAL_TO, e2.getType());
        assertEquals("xyz", e2.getOperand(1));
    }

    public void testParams2() throws Exception {
        Expression e1 = Expression.fromString("k1 like $test");

        Map map = new HashMap();
        map.put("test", "xyz");
        Expression e2 = e1.expWithParameters(map, false);
        assertNotNull(e2);
        assertEquals(2, e2.getOperandCount());
        assertEquals(Expression.LIKE, e2.getType());
        assertEquals("xyz", e2.getOperand(1));
    }

    public void testNoParams1() throws Exception {
        Expression e1 = Expression.fromString("k1 = $test");
        Expression e2 = e1.expWithParameters(Collections.EMPTY_MAP, true);

        // all expression nodes must be pruned
        assertNull(e2);
    }

    public void testNoParams2() throws Exception {
        Expression e1 =
            Expression.fromString(
                "k1 = $test1 or k2 = $test2 or k3 = $test3 or k4 = $test4");

        Map params = new HashMap();
        params.put("test2", "abc");
        params.put("test3", "xyz");
        Expression e2 = e1.expWithParameters(params, true);

        // some expression nodes must be pruned
        assertNotNull(e2);
        assertEquals(2, e2.getOperandCount());

        Expression k2 = (Expression) e2.getOperand(0);
        assertEquals("abc", k2.getOperand(1));

        Expression k3 = (Expression) e2.getOperand(1);
        assertEquals("xyz", k3.getOperand(1));
    }

    public void testNoParams3() throws Exception {
        Expression e1 =
            Expression.fromString(
                "k1 = $test1 or k2 = $test2 or k3 = $test3 or k4 = $test4");

        Map params = new HashMap();
        params.put("test4", "123");
        Expression e2 = e1.expWithParameters(params, true);

        // some expression nodes must be pruned
        assertNotNull(e2);
        assertEquals(2, e2.getOperandCount());
        assertEquals("123", e2.getOperand(1));
        assertEquals("k4", ((Expression) e2.getOperand(0)).getOperand(0));
    }
}
