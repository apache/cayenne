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

/**
 * @since 5.0
 */
public final class ModExp extends FunctionCallExp {
    public ModExp(Object... operands) {
        super(operands);
    }

    @Override
    public String getFunctionName() {
        return "MOD";
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        double x = ConversionUtil.toDouble(evaluateOperand(0, o), 0.0);
        double y = ConversionUtil.toDouble(evaluateOperand(1, o), 0.0);
        if(y == 0.0) {
            return 0.0;
        }
        return x % y;
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 0;
    }

    @Override
    protected Object evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        return null;
    }

    @Override
    public Expression shallowCopy() {
        return new ModExp();
    }
}
