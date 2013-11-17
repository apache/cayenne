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
import java.util.List;

import junit.framework.Assert;

/** 
 * Class that collects statistics of expression traversal. 
 */
public class TstTraversalHandler implements TraversalHandler {
    protected List treeFlatView = new ArrayList();
    protected int children;
    protected int nodes;
    protected int nodesStarted;
    protected int leafs;

    /**
     * Performs independent traversal of two expressions,
     * comparing the results. If expressions structure is different,
     * throws an exception.
     */
    public static void compareExps(Expression exp1, Expression exp2) {
        TstTraversalHandler handler1 = new TstTraversalHandler();
        handler1.traverseExpression(exp1);

        TstTraversalHandler handler2 = new TstTraversalHandler();
        handler2.traverseExpression(exp2);

        Assert.assertEquals(handler1.nodes, handler2.nodes);
    }

    public TstTraversalHandler() {

    }

    public void assertConsistency() throws Exception {
        Assert.assertEquals(nodesStarted, nodes);
    }

    public List getTreeFlatView() {
        return treeFlatView;
    }

    public void traverseExpression(Expression exp) {
        reset();
        exp.traverse(this);
    }

    public void reset() {
        children = 0;
        nodes = 0;
        nodesStarted = 0;
        leafs = 0;
    }

    public int getNodeCount() {
        return nodes;
    }

    public int getChildren() {
        return children;
    }

    public int getNodes() {
        return nodes;
    }

    public int getNodesStarted() {
        return nodesStarted;
    }

    public int getLeafs() {
        return leafs;
    }

    public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {
        children++;
    }

    public void startNode(Expression node, Expression parentNode) {
        treeFlatView.add(node);
        nodesStarted++;
    }

    public void endNode(Expression node, Expression parentNode) {
        nodes++;
    }

    public void objectNode(Object leaf, Expression parentNode) {
        treeFlatView.add(leaf);
        leafs++;
    }
}
