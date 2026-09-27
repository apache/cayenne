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
public class AllExp extends ConditionExp {    public AllExp(Object... operands) {
        super(operands);
    }

    @Override
    public Expression shallowCopy() {
        return new AllExp();
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 1;
    }

    @Override
    protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        return null;
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        // the operator goes before the single operand, so it is never printed by the superclass
        out.append("all ");
        appendChildAsString(0, out);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "ALL";
    }

    @Override
    protected boolean isValidParent(BaseExp parent) {
        // an operand of a comparison, not a standalone condition
        return true;
    }

    @Override
    protected boolean parenthesizeAsOperand() {
        return false;
    }

    @Override
    public int getType() {
        return Expression.ALL;
    }

    @Override
    public Expression exists() {
        throw new UnsupportedOperationException("Can't use exists() with ALL");
    }

    @Override
    public Expression notExists() {
        throw new UnsupportedOperationException("Can't use not exists() with ALL");
    }
}
