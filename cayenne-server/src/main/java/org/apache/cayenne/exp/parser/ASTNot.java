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

import java.io.IOException;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.ConversionUtil;

/**
 * "Not" expression.
 * 
 * @since 1.1
 */
public class ASTNot extends AggregateConditionNode {

	private static final long serialVersionUID = 7418894098531106347L;

	ASTNot(int id) {
		super(id);
	}

	public ASTNot() {
		super(ExpressionParserTreeConstants.JJTNOT);
	}

	public ASTNot(Node expression) {
		super(ExpressionParserTreeConstants.JJTNOT);
		jjtAddChild(expression, 0);
		connectChildren();
	}

	@Override
	protected Object evaluateNode(Object o) throws Exception {
		int len = jjtGetNumChildren();
		if (len == 0) {
			return Boolean.FALSE;
		}

		Object o1 = evaluateChild(0, o);
		if (o1 == null) {
			return null;
		}

		return ConversionUtil.toBoolean(o1) ? Boolean.FALSE : Boolean.TRUE;
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new ASTNot(id);
	}

    @Override
	public int getType() {
		return Expression.NOT;
	}

	/**
	 * @since 4.0
	 */
	@Override
	public void appendAsString(Appendable out) throws IOException {
		out.append("not ");
		super.appendAsString(out);
	}

    /**
     * @since 4.0
     */
    @Override
    public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
        out.append("not ");
        super.appendAsEJBQL(parameterAccumulator, out, rootId);
    }

	@Override
	protected String getExpressionOperator(int index) {
		throw new UnsupportedOperationException("No operator for '" + ExpressionParserTreeConstants.jjtNodeName[id] + "'");
	}
}
