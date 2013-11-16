/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
 * "Like" expression.
 * 
 */
public class ASTLike extends PatternMatchNode {
    ASTLike(int id) {
        super(id, false);
    }
    
    ASTLike(int id, char escapeChar) {
        super(id, false, escapeChar);
    }

    public ASTLike() {
        super(ExpressionParserTreeConstants.JJTLIKE, false);
    }

    public ASTLike(ASTPath path, Object pattern) {
        super(ExpressionParserTreeConstants.JJTLIKE, false);
        jjtAddChild(path, 0);
        jjtAddChild(new ASTScalar(pattern), 1);
        connectChildren();
    }

    public ASTLike(ASTPath path, Object pattern, char escapeChar) {
        super(ExpressionParserTreeConstants.JJTLIKE, false, escapeChar);
        jjtAddChild(path, 0);
        jjtAddChild(new ASTScalar(pattern), 1);
        connectChildren();
    }
    
    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len != 2) {
            return Boolean.FALSE;
        }

        String s1 = ConversionUtil.toString(evaluateChild(0, o));
        if (s1 == null) {
            return Boolean.FALSE;
        }

        return matchPattern(s1) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTLike(id, escapeChar);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "like";
    }

    @Override
    public int getType() {
        return Expression.LIKE;
    }
}
