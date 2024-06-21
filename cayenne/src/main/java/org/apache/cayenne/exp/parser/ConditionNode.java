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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;

/**
 * Superclass of conditional expressions.
 * 
 * @since 1.1
 */
public abstract class ConditionNode extends SimpleNode {

    public ConditionNode(int i) {
        super(i);
    }

    @Override
    public void jjtSetParent(Node n) {
        // this is a check that we can't handle properly
        // in the grammar... do it here...

        // disallow non-aggregated condition parents...
        if (!(n instanceof AggregateConditionNode)) {
            String label = (n instanceof SimpleNode) ? ((SimpleNode)n).expName() : String.valueOf(n);
            throw new ExpressionException(expName() + ": invalid parent - " + label);
        }

        super.jjtSetParent(n);
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        int requiredLen = getRequiredChildrenCount();
        if (len != requiredLen) {
            return Boolean.FALSE;
        }

        if(requiredLen == 0) {
            return evaluateSubNode(null, null);
        }

        Object[] evaluatedChildren = new Object[requiredLen];
        for(int i=0; i<requiredLen; i++) {
            evaluatedChildren[i] = evaluateChild(i, o);
        }

        Object firstChild = evaluatedChildren[0];
        // don't care here for keys
        if(firstChild instanceof Map) {
            firstChild = ((Map) firstChild).values();
        }
        if (firstChild instanceof Collection) {
            for(Object c : (Collection)firstChild) {
                if(evaluateSubNode(c, evaluatedChildren) == Boolean.TRUE) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        } else {
            return evaluateSubNode(firstChild, evaluatedChildren);
        }
    }

    abstract protected int getRequiredChildrenCount();

    abstract protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception;

    /**
     * Returns expression that will be dynamically resolved to proper subqueries based on a relationships used
     * (if no relationships are present in the original expression no subqueries will be used).
     *
     * @return exists expression
     *
     * @see ExpressionFactory#exists(Expression)
     * @since 5.0
     */
    public Expression exists() {
        return ExpressionFactory.exists(this);
    }

    /**
     * Returns expression that will be dynamically resolved to proper subqueries based on a relationships used
     * (if no relationships are present in the original expression no subqueries will be used).
     *
     * @return not exists expression
     *
     * @see ExpressionFactory#notExists(Expression)
     * @since 5.0
     */
    public Expression notExists() {
        return ExpressionFactory.notExists(this);
    }
}
