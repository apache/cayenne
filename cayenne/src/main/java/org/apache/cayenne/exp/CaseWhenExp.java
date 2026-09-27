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

import org.apache.cayenne.util.ConversionUtil;

import java.io.IOException;
import java.util.List;

/**
 * "CASE-WHEN" expression root node.
 *
 * @see org.apache.cayenne.exp.ExpressionFactory#caseWhen(List, List, Expression)
 * @since 5.0
 */
public final class CaseWhenExp extends Expression {
    public CaseWhenExp(Object... operands) {
        super(operands);
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new CaseWhenExp();
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "case";
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int numChildren = getOperandCount();
        if (numChildren == 0) {
            return null;
        }
        for (int i = 0; i < numChildren - 1; i = i + 2) {
            Object evaluatedWhen = evaluateOperand(i, o);
            if (ConversionUtil.toBoolean(evaluatedWhen)) {
                return evaluateOperand(i + 1, o);
            }
        }
        if (numChildren % 2 == 1) {
            return evaluateOperand(numChildren - 1, o);
        }
        return null;
    }

    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
        throw new UnsupportedOperationException("EJBQL 'case when' is not supported");
    }
}
