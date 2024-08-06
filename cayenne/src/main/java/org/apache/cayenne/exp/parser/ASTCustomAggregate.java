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

import org.apache.cayenne.exp.Expression;

import java.io.IOException;

/**
 * Class for custom aggregation functions expressions.
 * @since 5.0
 */
public class ASTCustomAggregate extends ASTAggregateFunctionCall {

    private boolean isNameSet;

    ASTCustomAggregate(int id) {
        super(id, "");
    }

    /**
     * Creates a new aggregate function expression with provided name.
     *
     * @param functionName name of the aggregate function
     */
    protected ASTCustomAggregate(String functionName) {
        super(ExpressionParserTreeConstants.JJTCUSTOMAGGREGATE, functionName);
    }

    /**
     * Creates a new aggregate function expression with provided name.
     *
     * @param functionName name of the aggregate function
     * @param expressions the expressions this aggregate function is applied to
     */
    public ASTCustomAggregate(String functionName, Expression... expressions) {
        super(ExpressionParserTreeConstants.JJTCUSTOMAGGREGATE, functionName, (Object[]) expressions);
    }

    @Override
    public Expression shallowCopy() {
        return new ASTCustomAggregate(functionName);
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
        out.append("agg").append('(').append('"').append(functionName).append('"');
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
