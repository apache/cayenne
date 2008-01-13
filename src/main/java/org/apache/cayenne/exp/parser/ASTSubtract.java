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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.ConversionUtil;

/**
 * "Subtract" expression.
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public class ASTSubtract extends SimpleNode {
    ASTSubtract(int id) {
        super(id);
    }

    public ASTSubtract() {
        super(ExpressionParserTreeConstants.JJTSUBTRACT);
    }

    public ASTSubtract(Object[] nodes) {
        super(ExpressionParserTreeConstants.JJTSUBTRACT);
        int len = nodes.length;
        for (int i = 0; i < len; i++) {
            jjtAddChild(wrapChild(nodes[i]), i);
        }
        connectChildren();
    }

    public ASTSubtract(Collection nodes) {
        super(ExpressionParserTreeConstants.JJTSUBTRACT);
        int len = nodes.size();
        Iterator it = nodes.iterator();
        for (int i = 0; i < len; i++) {
            jjtAddChild(wrapChild(it.next()), i);
        }
        connectChildren();
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len == 0) {
            return null;
        }

        BigDecimal result = null;
        for (int i = 0; i < len; i++) {
            BigDecimal value = ConversionUtil.toBigDecimal(evaluateChild(i, o));

            if (value == null) {
                return null;
            }

            result = (i == 0) ? value : result.subtract(value);
        }

        return result;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTSubtract(id);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "-";
    }

    @Override
    public int getType() {
        return Expression.SUBTRACT;
    }

    @Override
    public void jjtClose() {
        super.jjtClose();
        flattenTree();
    }
}
