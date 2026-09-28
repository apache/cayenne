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
 * Boolean true expression element Notice that there is one TrueExp and one
 * FalseExp instead of a single BooleanExp with a Boolean value. The main reason for
 * doing this is that a common BooleanExp will have operand count of 1 and that
 * will default to a prepared statmenet like " where ? and (...)", but we only
 * need " where true and (...)".
 * 
 * @see FalseExp
 * @since 5.0
 */
public final class TrueExp extends ConditionExp {
    public TrueExp(Object... operands) {
        super(operands);
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 0;
    }

    @Override
    protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        return Boolean.TRUE;
    }

    @Override
    protected String getExpressionOperator(int index) {
        throw new UnsupportedOperationException("No operator for '" + expName() + "'");
    }

    @Override
    public Expression shallowCopy() {
        return new TrueExp();
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        out.append("true");
    }

    @Override
    protected boolean parenthesizeAsOperand() {
        return false;
    }
}
