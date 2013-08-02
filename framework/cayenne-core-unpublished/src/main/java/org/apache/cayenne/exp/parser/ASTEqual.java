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

import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ValueInjector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * "Equal To" expression.
 * 
 * @since 1.1
 */
public class ASTEqual extends ConditionNode implements ValueInjector {
    
    private static final Log logObj = LogFactory.getLog(ASTEqual.class);

    /**
     * Constructor used by expression parser. Do not invoke directly.
     */
    ASTEqual(int id) {
        super(id);
    }

    public ASTEqual() {
        super(ExpressionParserTreeConstants.JJTEQUAL);
    }

    /**
     * Creates "Equal To" expression.
     */
    public ASTEqual(ASTPath path, Object value) {
        super(ExpressionParserTreeConstants.JJTEQUAL);
        jjtAddChild(path, 0);
        jjtAddChild(new ASTScalar(value), 1);
        connectChildren();
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        int len = jjtGetNumChildren();
        if (len != 2) {
            return Boolean.FALSE;
        }

        Object o1 = evaluateChild(0, o);
        Object o2 = evaluateChild(1, o);

        return evaluateImpl(o1, o2);
    }

    /**
     * Compares two objects, if one of them is array, 'in' operation is
     * performed
     */
    static boolean evaluateImpl(Object o1, Object o2) {
        // TODO: maybe we need a comparison "strategy" here, instead of
        // a switch of all possible cases? ... there were other requests for
        // more relaxed type-unsafe comparison (e.g. numbers to strings)

        if (o1 == null && o2 == null) {
            return true;
        } else if (o1 != null) {

            // Per CAY-419 we perform 'in' comparison if one object is a list,
            // and other is not

            if (o1 instanceof List && !(o2 instanceof List)) {
                for (Object element : ((List<?>) o1)) {
                    if (element != null && Evaluator.evaluator(element).eq(element, o2)) {
                        return true;
                    }
                }
                return false;
            }
            if (o2 instanceof List && !(o1 instanceof List)) {
                for (Object element : ((List<?>) o2)) {
                    if (element != null && Evaluator.evaluator(element).eq(element, o1)) {
                        return true;
                    }
                }
                return false;
            }

            return Evaluator.evaluator(o1).eq(o1, o2);
        }
        return false;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTEqual(id);
    }

    @Override
    protected String getExpressionOperator(int index) {
        return "=";
    }

    @Override
    protected String getEJBQLExpressionOperator(int index) {
        if (jjtGetChild(1) instanceof ASTScalar && ((ASTScalar) jjtGetChild(1)).getValue() == null) {
            // for ejbql, we need "is null" instead of "= null"
            return "is";
        }
        return getExpressionOperator(index);
    }

    @Override
    public int getType() {
        return Expression.EQUAL_TO;
    }

    public void injectValue(Object o) {
        // try to inject value, if one of the operands is scalar, and other is a
        // path

        Node[] args = new Node[] { jjtGetChild(0), jjtGetChild(1) };

        int scalarIndex = -1;
        if (args[0] instanceof ASTScalar) {
            scalarIndex = 0;
        } else if (args[1] instanceof ASTScalar) {
            scalarIndex = 1;
        }

        if (scalarIndex != -1 && args[1 - scalarIndex] instanceof ASTObjPath) {
            // inject
            ASTObjPath path = (ASTObjPath) args[1 - scalarIndex];
            try {
                path.injectValue(o, evaluateChild(scalarIndex, o));
            } catch (Exception ex) {
                logObj.warn("Failed to inject value " + " on path " + path.getPath() + " to " + o, ex);
            }
        }
    }
}
