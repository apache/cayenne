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

package org.apache.cayenne.template.parser;

import java.util.Arrays;

import org.apache.cayenne.template.Context;

public class ASTArray extends ASTExpression {

    public ASTArray(int id) {
        super(id);
    }

    @Override
    public void evaluate(Context context) {
        context.getBuilder().append(evaluateAsString(context));
    }

    @Override
    public String evaluateAsString(Context context) {
        return Arrays.toString(evaluateAsArray(context));
    }

    @Override
    public Object evaluateAsObject(Context context) {
        return evaluateAsArray(context);
    }

    protected Object[] evaluateAsArray(Context context) {
        Object[] evaluated = new Object[jjtGetNumChildren()];
        for(int i=0; i<jjtGetNumChildren(); i++) {
            ExpressionNode node = (ExpressionNode)jjtGetChild(i);
            evaluated[i] = node.evaluateAsObject(context);
        }
        return evaluated;
    }
}
