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

package org.apache.cayenne.exp;

import java.io.IOException;
import java.util.List;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;

/**
 * A scalar value wrapper expression.
 * 
 * @since 5.0
 */
public class ScalarExp extends BaseExp {
    protected Object value;

    /**
     * Creates a scalar with the value passed as the only argument, or a null-valued scalar without arguments.
     */
    public ScalarExp(Object... operands) {
        if (operands != null && operands.length > 0) {
            if (operands.length > 1) {
                throw new IllegalArgumentException("A scalar takes a single value, got " + operands.length);
            }
            setValue(operands[0]);
        }
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
        ScalarExp copy = new ScalarExp();
        copy.value = value;
        return copy;
    }

    @Override
    public void appendAsString(Appendable out) throws IOException {
        ExpHelper.appendScalarAsString(out, value, '\"');
    }

    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
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

        ExpHelper.encodeScalarAsEJBQL(parameterAccumulator, out, scalar);
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
        throw new UnsupportedOperationException("No operator for '" + expName() + "'");
    }

    @Override
    public int getType() {
        return Expression.SCALAR;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalarExp other = (ScalarExp) o;
        return value != null ? value.equals(other.value) : other.value == null;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    protected boolean parenthesizeAsOperand() {
        return false;
    }
}
