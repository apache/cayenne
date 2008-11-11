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
package org.apache.cayenne.ejbql.parser;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;

/**
 * A base node for the EJBQL concrete nodes that satisfies JJTree requirements.
 * 
 * @since 3.0
 */
public abstract class SimpleNode implements Node, Serializable, EJBQLExpression {

    final int id;
    SimpleNode parent;
    SimpleNode[] children;
    boolean not;
    String text;

    public SimpleNode(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public boolean isNegated() {
        return not;
    }

    /**
     * A recursive visit method that passes a visitor to this node and all its children,
     * depth first.
     */
    public void visit(EJBQLExpressionVisitor visitor) {

        if (visitNode(visitor)) {

            int len = getChildrenCount();
            for (int i = 0; i < len; i++) {
                if (!visitChild(visitor, i)) {
                    break;
                }
            }
        }
    }

    /**
     * Visits this node without recursion. Default implementation simply returns true.
     * Subclasses override this method to call an appropriate visitor method.
     */
    protected boolean visitNode(EJBQLExpressionVisitor visitor) {
        return true;
    }

    /**
     * Recursively visits a child at the specified index. Subclasses override this method
     * if they desire to implement callbacks after visiting each child.
     */
    protected boolean visitChild(EJBQLExpressionVisitor visitor, int childIndex) {
        children[childIndex].visit(visitor);
        return true;
    }

    public EJBQLExpression getChild(int index) {
        return jjtGetChild(index);
    }

    public int getChildrenCount() {
        return jjtGetNumChildren();
    }

    public String getName() {
        String className = getClass().getName();
        int i = className.lastIndexOf("EJBQL");
        return i >= 0 ? className.substring(i + 5) : className;
    }

    public void jjtOpen() {
    }

    public void jjtClose() {
    }

    public void jjtSetParent(Node parent) {
        this.parent = (SimpleNode) parent;
    }

    public Node jjtGetParent() {
        return this.parent;
    }

    public void jjtAddChild(Node n, int i) {
        if (children == null) {
            children = new SimpleNode[i + 1];
        }
        else if (i >= children.length) {
            SimpleNode c[] = new SimpleNode[i + 1];
            System.arraycopy(children, 0, c, 0, children.length);
            children = c;
        }

        children[i] = (SimpleNode) n;
    }

    public Node jjtGetChild(int i) {
        return children[i];
    }

    public int jjtGetNumChildren() {
        return (children == null) ? 0 : children.length;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        StringWriter buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        dump(pw, "", true);
        pw.close();
        buffer.flush();
        return buffer.toString();
    }

    void dump(PrintWriter out, String prefix, boolean text) {
        out.println(prefix
                + getName()
                + (text && this.text != null ? " [" + this.text + "]" : ""));
        if (children != null) {
            for (SimpleNode n : children) {
                if (n != null) {
                    n.dump(out, prefix + " ", text);
                }
            }
        }
    }
}
