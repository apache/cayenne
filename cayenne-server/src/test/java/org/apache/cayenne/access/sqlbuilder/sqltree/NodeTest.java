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

package org.apache.cayenne.access.sqlbuilder.sqltree;

import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class NodeTest {

    static private class TestNode extends Node {
        @Override
        public Node copy() {
            return new TestNode();
        }

        @Override
        public QuotingAppendable append(QuotingAppendable buffer) {
            return buffer.append("test");
        }
    }

    private Node node;

    @Before
    public void createNewNode() {
        node = new TestNode();
    }

    @Test
    public void testInsert() {
        assertEquals(0, node.childrenCount);
        assertNull(node.children);

        node.addChild(new TestNode());

        assertEquals(1, node.childrenCount);
        assertNotNull(node.children);
        assertEquals(4, node.children.length);

        TestNode node1 = new TestNode();
        this.node.addChild(0, node1);

        assertSame(node1, node.children[0]);
        assertEquals(2, this.node.childrenCount);
        assertEquals(4, this.node.children.length);

        TestNode node2 = new TestNode();
        this.node.addChild(0, node2);
        assertSame(node2, node.children[0]);

        TestNode node3 = new TestNode();
        this.node.addChild(0, node3);
        assertSame(node3, node.children[0]);

        TestNode node4 = new TestNode();
        this.node.addChild(0, node4);
        assertSame(node4, node.children[0]);

        assertEquals(5, this.node.childrenCount);
        assertEquals(8, this.node.children.length);
    }

    @Test
    public void testAdd() {
        assertEquals(0, node.childrenCount);
        assertNull(node.children);

        node.addChild(new TestNode());

        assertEquals(1, node.childrenCount);
        assertNotNull(node.children);
        assertEquals(4, node.children.length);

        node.addChild(new TestNode());
        node.addChild(new TestNode());
        node.addChild(new TestNode());

        assertEquals(4, node.childrenCount);
        assertEquals(4, node.children.length);

        node.addChild(new TestNode());

        assertEquals(5, node.childrenCount);
        assertEquals(8, node.children.length);
    }

}