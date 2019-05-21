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

/**
 * "Between" expression.
 * 
 * @since 1.1
 */
public class ASTBetween extends ConditionNode {

	private static final long serialVersionUID = -8739783546459651759L;

	ASTBetween(int id) {
		super(id);
	}

	public ASTBetween() {
		super(ExpressionParserTreeConstants.JJTBETWEEN);
	}

	public ASTBetween(SimpleNode path, Object value1, Object value2) {
		super(ExpressionParserTreeConstants.JJTBETWEEN);
		jjtAddChild(path, 0);
		jjtAddChild(new ASTScalar(value1), 1);
		jjtAddChild(new ASTScalar(value2), 2);

		connectChildren();
	}

	@Override
	protected int getRequiredChildrenCount() {
		return 3;
	}

	@Override
	protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
		Object o1 = evaluatedChildren[1];
		Object o2 = evaluatedChildren[2];
		Evaluator e = Evaluator.evaluator(o);

		Integer c1 = e.compare(o, o1);
		if (c1 == null) {
			return Boolean.FALSE;
		}

		Integer c2 = e.compare(o, o2);
		if (c2 == null) {
			return Boolean.FALSE;
		}

		return c1 >= 0 && c2 <= 0 ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new ASTBetween(id);
	}

	@Override
	protected String getExpressionOperator(int index) {
		return (index == 2) ? "and" : "between";
	}

	@Override
	public int getType() {
		return Expression.BETWEEN;
	}
}
