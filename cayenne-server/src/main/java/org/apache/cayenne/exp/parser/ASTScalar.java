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

import java.io.IOException;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;

/**
 * A scalar value wrapper expression.
 * 
 * @since 1.1
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

    /**
     * @since 3.2
     */
    @Override
    public void appendAsString(Appendable out) throws IOException {
        SimpleNode.appendScalarAsString(out, value, '\"');
    }

    /**
     * @since 3.2
     */
    @Override
    public void appendAsEJBQL(Appendable out, String rootId) throws IOException {
        // TODO: see CAY-1111
        // Persistent processing is a hack for a rather special case of a single
        // column PK
        // object.. full implementation pending...
        //
        // cay1796 : change check for Persistent object by check for ObjectId
        Object scalar = value;

        if(scalar instanceof ObjectId) {
            ObjectId temp = (ObjectId)value;
            if (!temp.isTemporary() && temp.getIdSnapshot().size() == 1) {
                scalar = temp.getIdSnapshot().values().iterator().next();
            }
        }

        SimpleNode.appendScalarAsString(out, scalar, '\'');
    }

    public void setValue(Object value) {
    	if (value instanceof Persistent){
    		this.value = ((Persistent)value).getObjectId();
    	} else {
    		this.value = value; 
    	}
    }

    public Object getValue() {
        return value;
    }

    @Override
    protected String getExpressionOperator(int index) {
        throw new UnsupportedOperationException("No operator for '" + ExpressionParserTreeConstants.jjtNodeName[id]
                + "'");
    }
}
