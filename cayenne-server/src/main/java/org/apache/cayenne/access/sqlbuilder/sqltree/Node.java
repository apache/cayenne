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

package org.apache.cayenne.access.sqlbuilder.sqltree;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.sqlbuilder.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.sqlbuilder.StringBuilderAppendable;


/**
 * @since 4.2
 */
public abstract class Node {

    protected Node parent;

    protected List<Node> children;

    protected final NodeType type;

    public Node(NodeType type) {
        this.type = type;
    }

    public Node() {
        this(NodeType.UNDEFINED);
    }

    public Node addChild(int index, Node node) {
        children.add(index, node);
        node.setParent(this);
        return this;
    }

    public Node addChild(Node node) {
        if(children == null) {
            children = new ArrayList<>(4);
        }
        children.add(node);
        node.setParent(this);
        return this;
    }

    public Node getChild(int idx) {
        return children.get(idx);
    }

    public int getChildrenCount() {
        if(children == null) {
            return 0;
        }
        return children.size();
    }

    public void replaceChild(int idx, Node node) {
        children.set(idx, node).setParent(null);
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
        int count = getChildrenCount();
        for(int i=0; i<count; i++) {
            if(!visitor.onChildNodeStart(this, getChild(i), i, i < (count - 1))) {
                return;
            }
            getChild(i).visit(visitor);
            visitor.onChildNodeEnd(this, getChild(i), i, i < (count - 1));
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
            node.children = new ArrayList<>(children.size());
            for(Node child : children) {
                node.children.add(child.deepCopy());
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
}
