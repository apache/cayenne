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

import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.ConversionUtil;

/**
 * "Or" expression.
 * 
 * @since 1.1
 */
public class ASTOr extends AggregateConditionNode {
    ASTOr(int id) {
        super(id);
    }

    public ASTOr() {
        super(ExpressionParserTreeConstants.JJTOR);
    }

    public ASTOr(Object[] nodes) {
        super(ExpressionParserTreeConstants.JJTOR);
        int len = nodes.length;
        for (int i = 0; i < len; i++) {
            jjtAddChild((Node) nodes[i], i);
        }
        connectChildren();
    }

    public ASTOr(Collection<? extends Node> nodes) {
        super(ExpressionParserTreeConstants.JJTOR);
        int len = nodes.size();
        Iterator<? extends Node> it = nodes.iterator();
        for (int i = 0; i < len; i++) {
            jjtAddChild(it.next(), i);
        }
        connectChildren();
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len == 0) {
            return Boolean.FALSE;
        }

        for (int i = 0; i < len; i++) {
            if (ConversionUtil.toBoolean(evaluateChild(i, o))) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTOr(id);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "or";
    }

    @Override
    public int getType() {
        return Expression.OR;
    }

    @Override
    public void jjtClose() {
        super.jjtClose();
        flattenTree();
    }
}
