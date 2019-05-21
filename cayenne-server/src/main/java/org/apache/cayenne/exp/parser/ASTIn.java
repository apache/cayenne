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

import java.util.function.Function;

import org.apache.cayenne.exp.Expression;

/**
 * "In" expression.
 * 
 */
public class ASTIn extends ConditionNode {

	private static final long serialVersionUID = -211084571117172965L;

	/**
	 * Constructor used by expression parser. Do not invoke directly.
	 */
	ASTIn(int id) {
		super(id);
	}

	public ASTIn() {
		super(ExpressionParserTreeConstants.JJTIN);
	}

	public ASTIn(SimpleNode path, SimpleNode node) {
		super(ExpressionParserTreeConstants.JJTIN);
		jjtAddChild(path, 0);
		jjtAddChild(node, 1);
		connectChildren();
	}

	@Override
	protected int getRequiredChildrenCount() {
		return 2;
	}

	@Override
	protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
		if (o == null || evaluatedChildren[1] == null) {
			// Even if there is NULL value in list we should return false,
			// as check against NULL can be done only with IS NULL operator
			// and moreover not all DB accept syntax like 'value IN (NULL)'
			return Boolean.FALSE;
		}

		Object[] objects = (Object[]) evaluatedChildren[1];
		for (Object object : objects) {
			if (object != null && Evaluator.evaluator(o).eq(o, object)) {
				return Boolean.TRUE;
			}
		}

		return Boolean.FALSE;
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new ASTIn(id);
	}

	@Override
	protected String getExpressionOperator(int index) {
		return "in";
	}

	@Override
	public int getType() {
		return Expression.IN;
	}

	@Override
	protected Object transformExpression(Function<Object, Object> transformer) {
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
