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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.ConversionUtil;

/**
 * @since 4.0
 */
public class ASTLocate extends ASTFunctionCall {

    ASTLocate(int id) {
        super(id, "LOCATE");
    }

    public ASTLocate(Expression substring, Expression path) {
        super(ExpressionParserTreeConstants.JJTLOCATE, "LOCATE", substring, path);
    }

    public ASTLocate(Expression substring, Expression path, Expression offset) {
        super(ExpressionParserTreeConstants.JJTLOCATE, "LOCATE", substring, path, offset);
    }

    @Override
    protected Object evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        String substr = ConversionUtil.toString(o);
        String str = ConversionUtil.toString(evaluatedChildren[1]);
        int offset = 0;
        if(evaluatedChildren.length > 2) {
            offset = ConversionUtil.toInt(evaluatedChildren[2], 0);
        }
        // +1 to comply with SQL
        return str.indexOf(substr, offset) + 1;
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 2;
    }

    @Override
    public Expression shallowCopy() {
        return new ASTLocate(id);
    }
}
