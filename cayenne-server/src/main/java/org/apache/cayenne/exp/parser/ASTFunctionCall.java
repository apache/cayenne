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

package org.apache.cayenne.exp.parser;

import org.apache.cayenne.exp.Expression;

/**
 * @since 4.0
 */
public abstract class ASTFunctionCall extends SimpleNode {

    private String functionName;

    ASTFunctionCall(int id, String functionName) {
        super(id);
        this.functionName = functionName;
    }

    public ASTFunctionCall(int id, String functionName, Object... nodes) {
        this(id, functionName);
        this.functionName = functionName;
        int len = nodes.length;
        for (int i = 0; i < len; i++) {
            jjtAddChild(wrapChild(nodes[i]), i);
        }

        connectChildren();
    }

    @Override
    public int getType() {
        return Expression.FUNCTION_CALL;
    }

    public boolean needParenthesis() {
        return true;
    }

    public String getFunctionName() {
        return functionName;
    }

    /**
     * TODO what should this method return?
     */
    @Override
    protected String getExpressionOperator(int index) {
        return functionName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ASTFunctionCall that = (ASTFunctionCall) o;
        return functionName.equals(that.functionName);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + functionName.hashCode();
    }
}
