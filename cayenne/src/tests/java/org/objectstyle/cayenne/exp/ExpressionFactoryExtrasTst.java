/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.exp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.unit.CayenneTestCase;

public class ExpressionFactoryExtrasTst extends CayenneTestCase {
    protected TstTraversalHandler handler;

    protected void setUp() throws java.lang.Exception {
        handler = new TstTraversalHandler();
    }

    /**
     * @deprecated binaryPathExp is deprecated.
     */
    public void testBinaryPathExp() throws Exception {
        String path = "path1.path2";
        Object o2 = new Object();
        Expression e1 = ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, path, o2);
        assertEquals(2, e1.getOperandCount());

        assertTrue(e1.getOperand(0) instanceof Expression);
        assertSame(o2, e1.getOperand(1));
        assertEquals(Expression.EQUAL_TO, e1.getType());

        Expression pathExp = (Expression) e1.getOperand(0);
        assertEquals(Expression.OBJ_PATH, pathExp.getType());
        assertEquals(path, pathExp.getOperand(0));
    }

    public void testMatchAllExp() throws Exception {
        // create expressions and check the counts,
        // leaf count should be (2N) : 2 leafs for each pair
        // node count should be (2N + 1) for nodes with more than 1 pair
        // and 2N for a single pair : 2 nodes for each pair + 1 list node
        // where N is map size

        // check for N in (1..3)
        for (int n = 1; n <= 3; n++) {
            Map map = new HashMap();

            // populate map
            for (int i = 1; i <= n; i++) {
                map.put("k" + i, "v" + i);
            }

            Expression exp = ExpressionFactory.matchAllExp(map, Expression.LESS_THAN);
            assertNotNull(exp);
            handler.traverseExpression(exp);

            // assert statistics
            handler.assertConsistency();
            assertEquals("Failed: " + exp, 2 * n, handler.getLeafs());
            assertEquals(
                "Failed: " + exp,
                n < 2 ? 2 * n : 2 * n + 1,
                handler.getNodeCount());
        }
    }

    public void testJoinExp() throws Exception {
        // create expressions and check the counts,
        // leaf count should be (2N) : 2 leafs for each expression
        // node count should be N > 1 ? 2 * N + 1 : 2 * N
        // where N is map size

        // check for N in (1..5)
        for (int n = 1; n <= 5; n++) {
            List list = new ArrayList();

            // populate map
            for (int i = 1; i <= n; i++) {
                list.add(ExpressionFactory.matchExp(("k" + i), "v" + i));
            }

            Expression exp = ExpressionFactory.joinExp(Expression.AND, list);
            assertNotNull(exp);
            handler.traverseExpression(exp);

            // assert statistics
            handler.assertConsistency();
            assertEquals("Failed: " + exp, 2 * n, handler.getLeafs());
            assertEquals(
                "Failed: " + exp,
                n > 1 ? 2 * n + 1 : 2 * n,
                handler.getNodeCount());
        }
    }
}
