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
import org.apache.cayenne.exp.ExpressionFactory;

/**
 * @since 4.2
 */
public class ASTNotExists extends ConditionNode {

    public ASTNotExists(Expression expression) {
        super(0);
        jjtAddChild((SimpleNode)expression, 0);
    }

    ASTNotExists(int id) {
        super(id);
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 1;
    }

    @Override
    protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        return null;
    }

    @Override
    protected String getExpressionOperator(int index) {
        return null;
    }

    @Override
    protected boolean isValidParent(Node n) {
        return true;
    }

    @Override
    public Expression shallowCopy() {
        return new ASTNotExists(id);
    }

    @Override
    public int getType() {
        return Expression.NOT_EXISTS;
    }

    /**
     * @inheritDoc
     * @since 5.0
     */
    @Override
    public Expression exists() {
        return ExpressionFactory.exists((Expression) getOperand(0));
    }

    /**
     * @inheritDoc
     * @since 5.0
     */
    @Override
    public Expression notExists() {
        return this;
    }
}
