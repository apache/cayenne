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
 * @author Andrus Adamchik
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

    /**
     * A recursive visit method that passes a visitor to this node and all its children,
     * depth first.
     */
    public boolean visit(EJBQLExpressionVisitor visitor) {

        if (!nonRecursiveVisit(visitor)) {
            return false;
        }

        int len = getChildrenCount();
        for (int i = 0; i < len; i++) {
            if (!children[i].visit(visitor)) {
                return false;
            }
        }

        return true;
    }

    /**
     * A non-recursive version of accept method. Simply returns true. Subclasses normally
     * override this method instead of visit().
     */
    protected boolean nonRecursiveVisit(EJBQLExpressionVisitor visitor) {
        return true;
    }

    public EJBQLExpression getChild(int index) {
        return (EJBQLExpression) jjtGetChild(index);
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

    void setText(String text) {
        this.text = text;
    }

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
            for (int i = 0; i < children.length; ++i) {
                SimpleNode n = (SimpleNode) children[i];
                if (n != null) {
                    n.dump(out, prefix + " ", text);
                }
            }
        }
    }
}
