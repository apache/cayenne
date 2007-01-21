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
import org.apache.commons.collections.Transformer;

/**
 * "In" expression.
 * 
 * @author Andrus Adamchik
 */
public class ASTIn extends ConditionNode {

    /**
     * Constructor used by expression parser. Do not invoke directly.
     */
    ASTIn(int id) {
        super(id);
    }

    public ASTIn() {
        super(ExpressionParserTreeConstants.JJTIN);
    }

    public ASTIn(ASTPath path, ASTList list) {
        super(ExpressionParserTreeConstants.JJTIN);
        jjtAddChild(path, 0);
        jjtAddChild(list, 1);
    }

    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len != 2) {
            return Boolean.FALSE;
        }

        Object o1 = evaluateChild(0, o);
        if (o1 == null) {
            return Boolean.FALSE;
        }

        Object[] objects = (Object[]) evaluateChild(1, o);
        if (objects == null) {
            return Boolean.FALSE;
        }

        int size = objects.length;
        for (int i = 0; i < size; i++) {
            if (o1.equals(objects[i])) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    public Expression shallowCopy() {
        return new ASTIn(id);
    }

    protected String getExpressionOperator(int index) {
        return "in";
    }

    public int getType() {
        return Expression.IN;
    }

    protected Object transformExpression(Transformer transformer) {
        Object transformed = super.transformExpression(transformer);
        
        // transform empty ASTIn to ASTFalse
        if (transformed instanceof ASTIn) {
            ASTIn exp = (ASTIn) transformed;
            if (exp.jjtGetNumChildren() == 2) {
                ASTList list = (ASTList) exp.jjtGetChild(1);
                Object[] objects = (Object[]) list.evaluate(null);
                if (objects.length == 0) {
                    transformed = new ASTFalse();
                }
            }
        }

        return transformed;
    }

}
