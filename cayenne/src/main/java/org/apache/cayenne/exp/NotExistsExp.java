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
public final class NotExistsExp extends ConditionExp {
    public NotExistsExp(Object... operands) {
        super(operands);
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 1;
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        if (getChildCount() != 1) {
            return Boolean.FALSE;
        }
        Object firstChild = evaluateChild(0, o);
        return evaluateSubNode(firstChild, null);
    }

    @Override
    protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        return !ExistsExp.notEmpty(o);
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        // the operator goes before the single operand, so it is never printed by the superclass
        out.append("not exists ");
        appendChildAsString(0, out);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return null;
    }

    @Override
    protected boolean isValidParent(BaseExp parent) {
        // unlike other conditions, may be an operand of anything
        return true;
    }

    @Override
    public Expression shallowCopy() {
        return new NotExistsExp();
    }

    @Override
    public Expression exists() {
        return ExpressionFactory.exists((Expression) getOperand(0));
    }

    @Override
    public Expression notExists() {
        return this;
    }
}
