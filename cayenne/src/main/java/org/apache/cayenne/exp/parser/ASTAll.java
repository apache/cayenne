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

public class ASTAll extends ConditionNode {
    public ASTAll(ASTSubquery subquery) {
        super(0);
        jjtAddChild(subquery, 0);
    }

    ASTAll(int id) {
        super(id);
    }

    @Override
    public Expression shallowCopy() {
        return new ASTAll(id);
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
        return "ALL";
    }

    @Override
    protected boolean isValidParent(Node n) {
        return true;
    }

    @Override
    public int getType() {
        return Expression.ALL;
    }

    /**
     * @inheritDoc
     * @since 5.0
     */
    @Override
    public Expression exists() {
        throw new UnsupportedOperationException("Can't use exists() with ALL");
    }

    /**
     * @inheritDoc
     * @since 5.0
     */
    @Override
    public Expression notExists() {
        throw new UnsupportedOperationException("Can't use not exists() with ALL");
    }
}
