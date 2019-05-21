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

import java.io.IOException;
import java.util.Date;

import org.apache.cayenne.exp.Expression;

/**
 * @since 4.0
 */
public class ASTCurrentTimestamp extends ASTFunctionCall {

    public ASTCurrentTimestamp() {
        this(ExpressionParserTreeConstants.JJTCURRENTTIMESTAMP);
    }

    ASTCurrentTimestamp(int id) {
        super(id, "CURRENT_TIMESTAMP");
    }

    @Override
    public boolean needParenthesis() {
        return false;
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 0;
    }

    @Override
    protected Object evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        return new Date();
    }

    @Override
    public Expression shallowCopy() {
        return new ASTCurrentTimestamp(id);
    }
}
