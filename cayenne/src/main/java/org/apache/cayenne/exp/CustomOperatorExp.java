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
package org.apache.cayenne.exp;

import java.io.IOException;

/**
 * @since 5.0
 */
public non-sealed class CustomOperatorExp extends BaseExp {
    private String operator;

    public CustomOperatorExp(Object... operands) {
        super(operands);
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        throw new UnsupportedOperationException("Can't evaluate custom operator in memory");
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        out.append("op(\"").append(operator).append("\"");
        if ((children != null) && (children.length > 0)) {
            for (int i = 0; i < children.length; i++) {
                out.append(", ");
                appendChildAsString(i, out);
            }
        }
        out.append(")");
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    protected String getExpressionOperator(int index) {
        return operator;
    }

    @Override
    public Expression shallowCopy() {
        CustomOperatorExp copy = new CustomOperatorExp();
        copy.setOperator(operator);
        return copy;
    }

    @Override
    protected boolean parenthesizeAsOperand() {
        // printed as a function call
        return false;
    }

    @Override
    protected boolean parenthesizeAsEJBQLOperand() {
        // printed as an infix operator
        return true;
    }
}
