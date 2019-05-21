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
import java.util.List;

import org.apache.cayenne.exp.Expression;

/**
 * @since 4.0
 */
public abstract class ASTFunctionCall extends EvaluatedNode {

    protected String functionName;

    ASTFunctionCall(int id, String functionName) {
        super(id);
        setFunctionName(functionName);
    }

    public ASTFunctionCall(int id, String functionName, Object... nodes) {
        this(id, functionName);
        setFunctionName(functionName);
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

    protected void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    @Override
    protected String getExpressionOperator(int index) {
        return ",";
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

    protected void appendFunctionNameAsString(Appendable out) throws IOException {
        out.append(nameToCamelCase(getFunctionName()));
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        appendFunctionNameAsString(out);
        if(parent == null) {
            // else call to super method will append parenthesis
            out.append("(");
        }
        super.appendAsString(out);
        if(parent == null) {
            out.append(")");
        }
    }

    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
        out.append(getFunctionName());
        out.append("(");
        super.appendChildrenAsEJBQL(parameterAccumulator, out, rootId);
        out.append(")");
    }

    /**
     *
     * @param functionName in UPPER_UNDERSCORE convention
     * @return functionName in camelCase convention
     */
    protected static String nameToCamelCase(String functionName) {
        String[] parts = functionName.split("_");
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(String part : parts) {
            if(first) {
                sb.append(part.toLowerCase());
                first = false;
            } else {
                char[] chars = part.toLowerCase().toCharArray();
                chars[0] = Character.toTitleCase(chars[0]);
                sb.append(chars);
            }
        }
        return sb.toString();
    }
}
