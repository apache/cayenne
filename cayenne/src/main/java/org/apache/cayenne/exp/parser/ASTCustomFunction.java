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
 * @since 4.2
 */
public class ASTCustomFunction extends ASTFunctionCall {

    private boolean isNameSet;

    ASTCustomFunction(int id) {
        super(id, "");
    }

    public ASTCustomFunction(String name, Object... arguments) {
        super(ExpressionParserTreeConstants.JJTCUSTOMFUNCTION, name, arguments);
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
        return new ASTCustomFunction(getFunctionName());
    }

    @Override
    protected void setFunctionName(String functionName) {
        super.setFunctionName(functionName);
        if(!functionName.isEmpty()) {
            isNameSet = true;
        }
    }

    @Override
    public void jjtAddChild(Node n, int i) {
        // First argument should be used as a function name when created by parser
        if(!isNameSet && i == 0) {
            if(!(n instanceof ASTScalar)) {
                throw new IllegalArgumentException("ASTScalar expected, got " + n);
            }
            setFunctionName(((ASTScalar) n).getValue().toString());
            return;
        }
        super.jjtAddChild(n, isNameSet ? i : --i);
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        out.append("fn").append('(').append('"').append(functionName).append('"');
        if (children != null) {
            for (Node child : children) {
                out.append(", ");
                if (child == null) {
                    out.append("null");
                } else {
                    ((SimpleNode) child).appendAsString(out);
                }
            }
        }
        out.append(')');
    }
}
