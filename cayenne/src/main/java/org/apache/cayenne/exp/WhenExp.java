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
 * "WHEN" part of the case-when expression.
 *
 * @since 5.0
 */
public final class WhenExp extends AggregateConditionExp {
    public WhenExp(Object... operands) {
        super(operands);
    }

    @Override
    public Expression shallowCopy() {
        return new WhenExp();
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "when";
    }

    @Override
    protected boolean isValidParent(BaseExp parent) {
        return parent instanceof CaseWhenExp;
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        if (getChildCount() == 0) {
            return Boolean.FALSE;
        }
        Object value = evaluateChild(0, o);
        if (ConversionUtil.toBoolean(value)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
        throw new UnsupportedOperationException("EJBQL 'when' is not supported");
    }
}
