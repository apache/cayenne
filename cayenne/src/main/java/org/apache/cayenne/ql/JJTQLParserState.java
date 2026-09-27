/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.ql;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.exp.BaseExp;

/**
 * The node stack that {@link QLParser} builds expression trees on. Adapted from the JJTree-generated tree state and
 * maintained by hand, together with the node classes it operates on. The class name is what the generated parser
 * expects.
 *
 * @since 5.0
 */
public class JJTQLParserState {

    private final List<BaseExp> nodes = new ArrayList<>();
    private final List<Integer> marks = new ArrayList<>();

    // number of nodes on the stack
    private int sp;
    // current mark
    private int mk;
    private boolean nodeCreated;

    /**
     * Determines whether the current node was actually closed and pushed. This should only be called in the final user
     * action of a node scope.
     */
    public boolean nodeCreated() {
        return nodeCreated;
    }

    /**
     * Reinitializes the node stack. Called by the parser's ReInit() method.
     */
    public void reset() {
        nodes.clear();
        marks.clear();
        sp = 0;
        mk = 0;
    }

    /**
     * Returns the root node of the tree. Only makes sense after a successful parse.
     */
    public BaseExp rootNode() {
        return nodes.get(0);
    }

    public void pushNode(BaseExp n) {
        nodes.add(n);
        ++sp;
    }

    /**
     * Returns the node on the top of the stack, removing it from the stack.
     */
    public BaseExp popNode() {
        if (--sp < mk) {
            mk = marks.remove(marks.size() - 1);
        }
        return nodes.remove(nodes.size() - 1);
    }

    public BaseExp peekNode() {
        return nodes.get(nodes.size() - 1);
    }

    /**
     * Returns the number of children on the stack in the current node scope.
     */
    public int nodeArity() {
        return sp - mk;
    }

    public void clearNodeScope(BaseExp n) {
        while (sp > mk) {
            popNode();
        }
        mk = marks.remove(marks.size() - 1);
    }

    public void openNodeScope(BaseExp n) {
        marks.add(mk);
        mk = sp;
    }

    /**
     * A definite node is constructed from a specified number of children. That many nodes are popped from the stack
     * and made the children of the definite node, which is then pushed on to the stack.
     */
    public void closeNodeScope(BaseExp n, int num) {
        mk = marks.remove(marks.size() - 1);
        while (num-- > 0) {
            n.addChild(popNode(), num);
        }
        n.childrenAdded();
        pushNode(n);
        nodeCreated = true;
    }

    /**
     * A conditional node is constructed if its condition is true. All the nodes that have been pushed since the node
     * was opened are made children of the conditional node, which is then pushed on to the stack. If the condition is
     * false the node is not constructed and they are left on the stack.
     */
    public void closeNodeScope(BaseExp n, boolean condition) {
        if (condition) {
            int a = nodeArity();
            mk = marks.remove(marks.size() - 1);
            while (a-- > 0) {
                n.addChild(popNode(), a);
            }
            n.childrenAdded();
            pushNode(n);
            nodeCreated = true;
        } else {
            mk = marks.remove(marks.size() - 1);
            nodeCreated = false;
        }
    }
}
