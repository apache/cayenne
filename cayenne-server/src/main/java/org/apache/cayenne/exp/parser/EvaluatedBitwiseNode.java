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

import org.apache.cayenne.util.ConversionUtil;

/**
 * @since 4.0
 */
public abstract class EvaluatedBitwiseNode extends EvaluatedNode {

    protected EvaluatedBitwiseNode(int i) {
        super(i);
    }

    @Override
    protected Object evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        Long result = ConversionUtil.toLong(o, Long.MIN_VALUE);
        if(result == Long.MIN_VALUE) {
            return null;
        }
        for (int i = 1; i < evaluatedChildren.length; i++) {
            Long value = ConversionUtil.toLong(evaluateChild(i, o), Long.MIN_VALUE);
            if (value == Long.MIN_VALUE) {
                return null;
            }

            result = op(result, value);
        }

        return result;
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 1;
    }

    protected abstract long op(long result, long arg);
}
