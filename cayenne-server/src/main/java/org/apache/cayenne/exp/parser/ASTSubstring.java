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
public class ASTSubstring extends ASTFunctionCall {

    ASTSubstring(int id) {
        super(id, "SUBSTRING");
    }

    public ASTSubstring(Expression path, Expression length, Expression offset) {
        super(ExpressionParserTreeConstants.JJTSUBSTRING, "SUBSTRING", path, length, offset);
    }

    @Override
    protected Object evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        String s1 = ConversionUtil.toString(o);
        if (s1 == null) {
            return null;
        }

        int offset = ConversionUtil.toInt(evaluatedChildren[1], 0);
        int length = ConversionUtil.toInt(evaluatedChildren[2], 0);
        if(length == 0) {
            return null;
        }

        return s1.substring(offset - 1, offset - 1 + length); // - 1 to comply with SQL
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 3;
    }


    @Override
    public Expression shallowCopy() {
        return new ASTSubstring(id);
    }
}
