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
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.util.ConversionUtil;

import java.io.IOException;
import java.util.List;

/**
 * "WHEN" part of the case-when expression.
 *
 * @since 5.0
 */
public class ASTWhen extends AggregateConditionNode {

    public ASTWhen(Expression condition) {
        super(0);
        jjtAddChild((Node) condition, 0);
        connectChildren();
    }

    public ASTWhen(int id) {
        super(id);
    }

    @Override
    public Expression shallowCopy() {
        return new ASTWhen(id);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "when";
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        if (jjtGetNumChildren() == 0) {
            return Boolean.FALSE;
        }
        Object value = evaluateChild(0, o);
        if (ConversionUtil.toBoolean(value)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public int getType() {
        return Expression.WHEN;
    }

    @Override
    public void jjtSetParent(Node n) {
        if (!(n instanceof ASTCaseWhen)) {
            throw new ExpressionException(expName() + ": invalid parent");
        }
        parent = n;
    }

    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
        throw new UnsupportedOperationException("EJBQL 'when' is not supported");
    }
}
