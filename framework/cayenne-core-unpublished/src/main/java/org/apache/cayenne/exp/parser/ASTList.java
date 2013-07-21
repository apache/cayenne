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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.exp.Expression;

/**
 * A leaf expression representing an immutable collection of values.
 * 
 * @since 1.1
 */
public class ASTList extends SimpleNode {
    protected Object[] values;

    ASTList(int id) {
        super(id);
    }

    public ASTList() {
        super(ExpressionParserTreeConstants.JJTLIST);
    }

    /**
     * Initializes a list expression with an Object[].
     */
    public ASTList(Object[] objects) {
        super(ExpressionParserTreeConstants.JJTLIST);
        setValues(objects);
    }

    /**
     * Initializes a list expression with a Java Collection
     */
    public ASTList(Collection objects) {
        super(ExpressionParserTreeConstants.JJTLIST);
        setValues(objects);
    }

    /**
     * Initializes a list expression with a Java Iterator.
     */
    public ASTList(Iterator objects) {
        super(ExpressionParserTreeConstants.JJTLIST);
        setValues(objects);
    }

    /**
     * Creates a copy of this expression node, without copying children.
     */
    @Override
    public Expression shallowCopy() {
        return new ASTList(id);
    }

    @Override
    protected Object evaluateNode(Object o) throws Exception {
        return values;
    }

    @Override
    public int getType() {
        return Expression.LIST;
    }

    @Override
    protected String getExpressionOperator(int index) {
        return ",";
    }

    /**
     * @since 3.2
     */
    @Override
    public void appendAsString(Appendable out) throws IOException {

        out.append('(');

        if ((values != null) && (values.length > 0)) {
            for (int i = 0; i < values.length; ++i) {
                if (i > 0) {
                    out.append(getExpressionOperator(i));
                    out.append(' ');
                }

                if (values[i] instanceof Expression) {
                    ((Expression) values[i]).appendAsString(out);
                } else {
                    appendScalarAsString(out, values[i], '\"');
                }
            }
        }

        out.append(')');
    }

    @Override
    public void appendAsEJBQL(Appendable out, String rootId) throws IOException {

        if (parent != null) {
            out.append("(");
        }

        if ((values != null) && (values.length > 0)) {
            for (int i = 0; i < values.length; ++i) {
                if (i > 0) {
                    out.append(getEJBQLExpressionOperator(i));
                    out.append(' ');
                }

                if (values[i] == null) {
                    out.append("null");
                } else {
                    SimpleNode.appendScalarAsString(out, values[i], '\'');
                }
            }
        }

        if (parent != null) {
            out.append(')');
        }
    }

    @Override
    public int getOperandCount() {
        return 1;
    }

    @Override
    public Object getOperand(int index) {
        if (index == 0) {
            return values;
        }

        throw new ArrayIndexOutOfBoundsException(index);
    }

    @Override
    public void setOperand(int index, Object value) {
        if (index != 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }

        setValues(value);
    }

    /**
     * Sets an internal collection of values. Value argument can be an Object[],
     * a Collection or an iterator.
     */
    protected void setValues(Object value) {
        if (value == null) {
            this.values = null;
        } else if (value instanceof Object[]) {
            this.values = (Object[]) value;
        } else if (value instanceof Collection) {
            this.values = ((Collection) value).toArray();
        } else if (value instanceof Iterator) {
            List values = new ArrayList();
            Iterator it = (Iterator) value;
            while (it.hasNext()) {
                values.add(it.next());
            }

            this.values = values.toArray();
        } else {
            throw new IllegalArgumentException("Invalid value class '" + value.getClass().getName()
                    + "', expected null, Object[], Collection, Iterator");
        }
    }

    @Override
    public void jjtClose() {
        super.jjtClose();

        // For backwards compatibility set a List value wrapping the nodes.
        // or maybe we should rewrite the parser spec to insert children
        // directly into internal collection?
        int size = jjtGetNumChildren();
        Object[] listValue = new Object[size];
        for (int i = 0; i < size; i++) {
            listValue[i] = unwrapChild(jjtGetChild(i));
        }

        setValues(listValue);

        // clean children - we are not supposed to use them anymore
        children = null;
    }
}
