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
import java.util.Collections;

import org.apache.cayenne.exp.Expression;

/**
 * @since 4.0
 */
public class ASTMax extends ASTAggregateFunctionCall {

    ASTMax(int id) {
        super(id, "MAX");
    }

    public ASTMax(Expression expression) {
        super(ExpressionParserTreeConstants.JJTMAX, "MAX", expression);
    }

    @Override
    public Expression shallowCopy() {
        return new ASTMax(id);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected Object evaluateCollection(Collection<?> values) {
        return Collections.max((Collection)values);
    }
}
