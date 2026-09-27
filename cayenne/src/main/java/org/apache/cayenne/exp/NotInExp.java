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

import java.util.function.Function;

/**
 * "Not In" expression.
 * 
 *
 * @since 5.0
 */
public class NotInExp extends ConditionExp {    public NotInExp(Object... operands) {
        super(operands);
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 2;
    }

    @Override
    protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        if (o == null || evaluatedChildren[1] == null) {
            return Boolean.FALSE;
        }

        Object[] objects = (Object[]) evaluatedChildren[1];
        for (Object object : objects) {
            if (object != null && Evaluator.evaluator(o).eq(o, object)) {
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new NotInExp();
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "not in";
    }

    @Override
    public int getType() {
        return Expression.NOT_IN;
    }
    
    @Override
    protected Object transformExpression(Function<Object, Object> transformer) {
        Object transformed = super.transformExpression(transformer);
        
        // transform empty NotInExp to TrueExp
        if (transformed instanceof NotInExp) {
            NotInExp exp = (NotInExp) transformed;
            if (exp.getChildCount() == 2) {
                BaseExp child = exp.getChild(1);
                if(child instanceof ListExp) {
                    ListExp list = (ListExp) child;
                    Object[] objects = (Object[]) list.evaluate(null);
                    if (objects.length == 0) {
                        transformed = new TrueExp();
                    }
                }
            }
        }

        return transformed;
    }

}
