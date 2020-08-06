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
 * "Greater Than" expression.
 */
public class ASTGreater extends ConditionNode {

	private static final long serialVersionUID = 2796092142001150018L;

	/**
	 * Constructor used by expression parser. Do not invoke directly.
	 */
	ASTGreater(int id) {
		super(id);
	}

	public ASTGreater() {
		super(ExpressionParserTreeConstants.JJTGREATER);
	}

	public ASTGreater(SimpleNode path, Object value) {
		super(ExpressionParserTreeConstants.JJTGREATER);
		jjtAddChild(path, 0);
		jjtAddChild(new ASTScalar(value), 1);
		connectChildren();
	}

	@Override
	protected int getRequiredChildrenCount() {
		return 2;
	}

	@Override
	protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
		Object o2 = evaluatedChildren[1];
		Integer c = Evaluator.evaluator(o).compare(o, o2);
		if(c == null) {
			return null;
		}
		return c > 0 ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new ASTGreater(id);
	}

	@Override
	protected String getExpressionOperator(int index) {
		return ">";
	}

	@Override
	public int getType() {
		return Expression.GREATER_THAN;
	}
}
