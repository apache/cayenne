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
import java.math.BigDecimal;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.ConversionUtil;

/**
 * "Negate" expression.
 * 
 * @since 1.1
 */
public class ASTNegate extends SimpleNode {

	private static final long serialVersionUID = -9161722951926428414L;

	ASTNegate(int id) {
		super(id);
	}

	public ASTNegate() {
		super(ExpressionParserTreeConstants.JJTNEGATE);
	}

	public ASTNegate(Object node) {
		super(ExpressionParserTreeConstants.JJTNEGATE);
		jjtAddChild(wrapChild(node), 0);
		connectChildren();
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new ASTNegate(id);
	}

	@Override
	protected Object evaluateNode(Object o) throws Exception {
		int len = jjtGetNumChildren();
		if (len == 0) {
			return null;
		}

		BigDecimal result = ConversionUtil.toBigDecimal(evaluateChild(0, o));
		return result != null ? result.negate() : null;
	}

	/**
	 * @since 4.0
	 */
	@Override
	public void appendAsString(Appendable out) throws IOException {

		if ((children != null) && (children.length > 0)) {
			out.append("-");

			SimpleNode child = (SimpleNode) children[0];

			// don't call super - we have our own parenthesis policy
			boolean useParen = parent != null && !((child instanceof ASTScalar) || (child instanceof ASTPath));
			if (useParen) {
				out.append("(");
			}

			child.appendAsString(out);

			if (useParen) {
				out.append(')');
			}
		}
	}

	/**
	 * @since 4.0
	 */
	@Override
	public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {

		if ((children != null) && (children.length > 0)) {
			out.append("-");

			SimpleNode child = (SimpleNode) children[0];

			// don't call super - we have our own parenthesis policy
			boolean useParen = parent != null && !((child instanceof ASTScalar) || (child instanceof ASTPath));
			if (useParen) {
				out.append("(");
			}

			child.appendAsEJBQL(parameterAccumulator, out, rootId);

			if (useParen) {
				out.append(')');
			}
		}
	}

	@Override
	protected String getExpressionOperator(int index) {
		throw new UnsupportedOperationException("No operator for '" + ExpressionParserTreeConstants.jjtNodeName[id]
				+ "'");
	}

	@Override
	public int getType() {
		return Expression.NEGATIVE;
	}

	@Override
	public int getOperandCount() {
		return 1;
	}
}
