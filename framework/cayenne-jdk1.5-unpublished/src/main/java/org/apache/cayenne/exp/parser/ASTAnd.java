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
import org.apache.cayenne.exp.ValueInjector;
import org.apache.cayenne.util.ConversionUtil;

/**
 * "And" expression.
 * 
 * @since 1.1
 */
public class ASTAnd extends AggregateConditionNode implements ValueInjector {
    /**
     * Constructor used by expression parser. Do not invoke directly.
     */
    ASTAnd(int id) {
        super(id);
    }

    public ASTAnd() {
        super(ExpressionParserTreeConstants.JJTAND);
    }

    public ASTAnd(Object[] nodes) {
        super(ExpressionParserTreeConstants.JJTAND);
        int len = nodes.length;
        for (int i = 0; i < len; i++) {
            jjtAddChild((Node) nodes[i], i);
        }
        
        connectChildren();
    }

    public ASTAnd(Collection<? extends Node> nodes) {
        super(ExpressionParserTreeConstants.JJTAND);
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
            if (!ConversionUtil.toBoolean(evaluateChild(i, o))) {
                return Boolean.FALSE;
            }
        }

        return Boolean.TRUE;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTAnd(id);
    }

    @Override
    public int getType() {
        return Expression.AND;
    }

    @Override
    public void jjtClose() {
        super.jjtClose();
        flattenTree();
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "and";
    }

    public void injectValue(Object o) {
        //iterate through all operands, inject all possible
        int len = jjtGetNumChildren();
        for (int i = 0; i < len; i++) {
            Node node = jjtGetChild(i);
            if (node instanceof ValueInjector) {
                ((ValueInjector) node).injectValue(o);
            }
        }
    }
}
