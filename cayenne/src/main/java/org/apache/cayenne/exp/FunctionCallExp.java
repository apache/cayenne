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

package org.apache.cayenne.exp;

import java.io.IOException;
import java.util.List;

/**
 * @since 5.0
 */
public abstract sealed class FunctionCallExp extends ValueExp permits AbsExp, AggregateFunctionCallExp, ConcatExp,
        CurrentDateExp, CurrentTimeExp, CurrentTimestampExp, CustomFunctionExp, ExtractExp, LengthExp, LocateExp,
        LowerExp, ModExp, SqrtExp, SubstringExp, TrimExp, UpperExp {
    protected FunctionCallExp(Object... operands) {
        super(operands);
    }

    public boolean needParenthesis() {
        return true;
    }

    public abstract String getFunctionName();

    @Override
    protected String getExpressionOperator(int index) {
        return ",";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FunctionCallExp that = (FunctionCallExp) o;
        return getFunctionName().equals(that.getFunctionName());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + getFunctionName().hashCode();
    }

    protected void appendFunctionNameAsString(Appendable out) throws IOException {
        out.append(nameToCamelCase(getFunctionName()));
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        appendFunctionNameAsString(out);
        out.append("(");
        super.appendAsString(out);
        out.append(")");
    }

    @Override
    protected boolean parenthesizeAsOperand() {
        // the arguments are already delimited
        return false;
    }

    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
        out.append(getFunctionName());
        out.append("(");
        super.appendOperandsAsEJBQL(parameterAccumulator, out, rootId);
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
