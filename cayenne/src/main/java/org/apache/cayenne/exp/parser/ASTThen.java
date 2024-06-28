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

import java.io.IOException;
import java.util.List;

/**
 * "THEN" part of the case-when expression.
 *
 * @since 5.0
 */
public class ASTThen extends SimpleNode {

    public ASTThen(Object node) {
        super(0);
        jjtAddChild((Node) node, 0);
        connectChildren();
    }

    public ASTThen(int id) {
        super(id);
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTThen(id);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "then";
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        return evaluateChild(0, o);
    }

    @Override
    public int getType() {
        return Expression.THEN;
    }

    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
        throw new UnsupportedOperationException("EJBQL 'then' is not supported");
    }


}
