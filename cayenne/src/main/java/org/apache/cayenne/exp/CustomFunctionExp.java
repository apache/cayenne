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
 * @since 5.0
 */
public non-sealed class CustomFunctionExp extends FunctionCallExp {
    private String functionName;

    public CustomFunctionExp(Object... operands) {
        super(operands);
    }

    @Override
    protected Object evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        throw new UnsupportedOperationException("Can't evaluate custom function in memory");
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 0;
    }

    @Override
    public Expression shallowCopy() {
        CustomFunctionExp copy = new CustomFunctionExp();
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
        out.append("fn").append('(').append('"').append(functionName).append('"');
        for (int i = 0; i < getOperandCount(); i++) {
            out.append(", ");
            appendOperandAsString(i, out);
        }
        out.append(')');
    }
}
