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

/**
 * Class for custom aggregation functions expressions.
 * @since 5.0
 */
public non-sealed class CustomAggregateExp extends AggregateFunctionCallExp {
    private String functionName;

    public CustomAggregateExp(Object... operands) {
        super(operands);
    }

    @Override
    public Expression shallowCopy() {
        CustomAggregateExp copy = new CustomAggregateExp();
        copy.setFunctionName(functionName);
        return copy;
    }

    @Override
    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        out.append("agg").append('(').append('"').append(functionName).append('"');
        for (int i = 0; i < getOperandCount(); i++) {
            out.append(", ");
            appendOperandAsString(i, out);
        }
        out.append(')');
    }
}
