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

package org.apache.cayenne.exp.parser;

import java.io.IOException;

import org.apache.cayenne.exp.Expression;

/**
 * @since 4.0
 */
public class ASTFullObject extends SimpleNode {

    public ASTFullObject(Expression expression) {
        this();
        Node node = wrapChild(expression);
        jjtAddChild(node, 0);
        node.jjtSetParent(this);
    }

    public ASTFullObject() {
        this(0);
    }

    protected ASTFullObject(int i) {
        super(i);
    }

    @Override
    protected String getExpressionOperator(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        return o;
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        out.append(":FULL_OBJECT:");
        super.appendAsString(out);
    }

    @Override
    public Expression shallowCopy() {
        return new ASTFullObject(id);
    }

    @Override
    public int getType() {
        return Expression.FULL_OBJECT;
    }
}
