/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.exp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class ExpressionFactoryExtrasTest extends TestCase {

    protected TstTraversalHandler handler;

    @Override
    protected void setUp() throws Exception {
        handler = new TstTraversalHandler();
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
            assertEquals("Failed: " + exp, n < 2 ? 2 * n : 2 * n + 1, handler
                    .getNodeCount());
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
            assertEquals("Failed: " + exp, n > 1 ? 2 * n + 1 : 2 * n, handler
                    .getNodeCount());
        }
    }
}
