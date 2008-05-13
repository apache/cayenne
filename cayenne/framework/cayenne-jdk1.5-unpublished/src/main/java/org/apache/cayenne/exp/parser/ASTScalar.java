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

import java.io.PrintWriter;

import org.apache.cayenne.exp.Expression;

/**
 * A scalar value wrapper expression.
 * 
 * @since 1.1
 * @author Andrus Adamchik
 */
public class ASTScalar extends SimpleNode {
    protected Object value;

    /**
     * Constructor used by expression parser. Do not invoke directly.
     */
    ASTScalar(int id) {
        super(id);
    }

    public ASTScalar() {
        super(ExpressionParserTreeConstants.JJTSCALAR);
    }

    public ASTScalar(Object value) {
        super(ExpressionParserTreeConstants.JJTSCALAR);
        setValue(value);
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        return value;
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        ASTScalar copy = new ASTScalar(id);
        copy.value = value;
        return copy;
    }

    @Override
    public void encodeAsString(PrintWriter pw) {
        SimpleNode.encodeScalarAsString(pw, value, '\"');
    }
    
    /**
     * @since 3.0
     */
    @Override
    public void encodeAsEJBQL(PrintWriter pw, String rootId) {
        SimpleNode.encodeScalarAsString(pw, value, '\'');
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    protected String getExpressionOperator(int index) {
        throw new UnsupportedOperationException(
            "No operator for '" + ExpressionParserTreeConstants.jjtNodeName[id] + "'");
    }
}
