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

import org.apache.cayenne.access.sqlbuilder.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.sqlbuilder.StringBuilderAppendable;

/**
 * @since 4.2
 */
public abstract class Node {

    protected Node parent;

    protected Node[] children;

    protected int childrenCount;

    protected final NodeType type;

    public Node(NodeType type) {
        this.type = type;
    }

    public Node() {
        this(NodeType.UNDEFINED);
    }

    public Node addChild(int index, Node node) {
        if(children.length <= childrenCount) {
            // expand + copy with empty slot at index
            Node[] newChildren = new Node[children.length + 4];
            System.arraycopy(children, 0, newChildren, 0, index);
            System.arraycopy(children, index, newChildren, index + 1, (childrenCount - index));
            children = newChildren;
        } else {
            // move tail after index on one position
            System.arraycopy(children, index, children, index + 1, (childrenCount - index));
        }
        children[index] = node;
        childrenCount++;
        node.setParent(this);
        return this;
    }

    public Node addChild(Node node) {
        if(children == null) {
            children = new Node[4];
        } else if(children.length <= childrenCount) {
            Node[] newChildren = new Node[children.length + 4];
            System.arraycopy(children, 0, newChildren, 0, children.length);
            children = newChildren;
        }
        children[childrenCount++] = node;
        node.setParent(this);
        return this;
    }

    public Node getChild(int idx) {
        return children[idx];
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public void replaceChild(int idx, Node node) {
        children[idx].setParent(null);
        children[idx] = node;
        node.setParent(this);
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void visit(NodeTreeVisitor visitor) {
        if(!visitor.onNodeStart(this)) {
            return;
        }

        for(int i=0; i<childrenCount; i++) {
            boolean hasMore = i < (childrenCount - 1);
            if(!visitor.onChildNodeStart(this, children[i], i, hasMore)) {
                return;
            }
            children[i].visit(visitor);
            visitor.onChildNodeEnd(this, children[i], i, hasMore);
        }
        visitor.onNodeEnd(this);
    }

    /**
     * @return deep copy(i.e. with copies of all children) of this node
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> T deepCopy() {
        Node node = this.copy();
        if(children != null) {
            node.children = new Node[childrenCount];
            node.childrenCount = childrenCount;
            for(int i=0; i<childrenCount; i++) {
                node.children[i] = children[i].deepCopy();
            }
        }
        return (T)node;
    }

    @Override
    public String toString() {
        return "Node {" + append(new StringBuilderAppendable()).toString() + "}";
    }

    public NodeType getType() {
        return type;
    }

    public abstract Node copy();

    public abstract QuotingAppendable append(QuotingAppendable buffer);

    public void appendChildrenSeparator(QuotingAppendable buffer, int childInd) {
    }

    public void appendChildrenStart(QuotingAppendable buffer) {
    }

    public void appendChildrenEnd(QuotingAppendable buffer) {
    }

    /**
     * @param node to compare with
     * @return true if this node and all it's children are equal to the given node
     * @since 5.0
     */
    public boolean deepEquals(Node node) {
        if(!equals(node) || childrenCount != node.childrenCount) {
            return false;
        }
        for(int i=0; i<childrenCount; i++) {
            if(!children[i].deepEquals(node.children[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return type == node.type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }
}
