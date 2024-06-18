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

import java.util.Objects;

/**
 * @since 4.2
 */
public class FunctionNode extends Node {

    private final String functionName;
    private final boolean needParentheses;
    private String alias;

    static public FunctionNode wrap(Node node, String functionName) {
        FunctionNode functionNode = new FunctionNode(functionName, null);
        functionNode.addChild(node);
        return functionNode;
    }

    public FunctionNode(String functionName, String alias) {
        this(functionName, alias, true);
    }

    public FunctionNode(String functionName, String alias, boolean needParentheses) {
        super(NodeType.FUNCTION);
        this.functionName = functionName;
        this.alias = alias;
        this.needParentheses = needParentheses;
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        if(skipContent()) {
            buffer.append(' ').append(alias);
        } else {
            buffer.append(' ').append(functionName);
        }
        return buffer;
    }

    @Override
    public void visit(NodeTreeVisitor visitor) {
        if(skipContent()) {
            visitor.onNodeStart(this);
            visitor.onNodeEnd(this);
            return;
        }
        super.visit(visitor);
    }

    @Override
    public void appendChildrenStart(QuotingAppendable buffer) {
        if(skipContent()){
            return;
        }
        if (needParentheses) {
            buffer.append('(');
        }
    }

    @Override
    public void appendChildrenEnd(QuotingAppendable buffer) {
        if(skipContent()){
            return;
        }

        if (needParentheses) {
            buffer.append(" )");
        }

        if (alias != null) {
            buffer.append(" ").appendQuoted(alias);
        }
    }

    @Override
    public void appendChildrenSeparator(QuotingAppendable buffer, int childIdx) {
        if(skipContent()) {
            return;
        }
        buffer.append(',');
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public Node copy() {
        return new FunctionNode(functionName, alias, needParentheses);
    }

    private boolean notInResultNode() {
        // check if parent is of type RESULT
        Node parent = getParent();
        while(parent != null) {
            if(parent.getType() == NodeType.RESULT) {
                return false;
            }
            parent = parent.getParent();
        }
        return true;
    }

    protected boolean skipContent() {
        // has alias and not in result node
        return alias != null && notInResultNode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FunctionNode that = (FunctionNode) o;
        return Objects.equals(functionName, that.functionName) && Objects.equals(alias, that.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), functionName, alias);
    }
}
