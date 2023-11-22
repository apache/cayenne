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

import java.util.Collection;
import java.util.Map;

/**
 * Base class for all aggregation functions expressions
 * It's more like marker interface for now.
 * @since 4.0
 */
public abstract class ASTAggregateFunctionCall extends ASTFunctionCall {

    ASTAggregateFunctionCall(int id, String functionName) {
        super(id, functionName);
    }

    ASTAggregateFunctionCall(int id, String functionName, Object... nodes) {
        super(id, functionName, nodes);
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 0;
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if(len == 0) {
            throw new UnsupportedOperationException("Aggregate functions can be calculated only for Collection or Map.");
        }

        Object firstChild = evaluateChild(0, o);
        Collection<?> values;
        if(firstChild instanceof Map) {
            values = ((Map<?, ?>) firstChild).values();
        } else if (firstChild instanceof Collection) {
            values = (Collection<?>) firstChild;
        } else {
            throw new UnsupportedOperationException("Aggregate functions can be calculated only for Collection or Map.");
        }

        return evaluateCollection(values);
    }

    protected Object evaluateCollection(Collection<?> values) {
        throw new UnsupportedOperationException("In-memory evaluation of aggregate functions not implemented yet.");
    }

    @Override
    protected Object evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        throw new UnsupportedOperationException("In-memory evaluation of aggregate functions not implemented yet.");
    }
}
