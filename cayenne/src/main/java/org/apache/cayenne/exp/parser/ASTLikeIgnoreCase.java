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
import org.apache.cayenne.util.ConversionUtil;

/**
 * "Case insensitive like" expression.
 * 
 */
public class ASTLikeIgnoreCase extends IgnoreCaseNode {

	private static final long serialVersionUID = 1624594678553400695L;

	ASTLikeIgnoreCase(int id) {
		super(id, true);
	}

	ASTLikeIgnoreCase(int id, char escapeChar) {
		super(id, true, escapeChar);
	}

	public ASTLikeIgnoreCase() {
		super(ExpressionParserTreeConstants.JJTLIKEIGNORECASE, true);
	}

	public ASTLikeIgnoreCase(SimpleNode path, Object pattern) {
		super(ExpressionParserTreeConstants.JJTLIKEIGNORECASE, true);
		jjtAddChild(path, 0);
		jjtAddChild(wrap(pattern), 1);
		connectChildren();
	}

	public ASTLikeIgnoreCase(SimpleNode path, Object pattern, char escapeChar) {
		super(ExpressionParserTreeConstants.JJTLIKEIGNORECASE, true, escapeChar);
		jjtAddChild(path, 0);
		jjtAddChild(wrap(pattern), 1);
		connectChildren();
	}

	@Override
	protected int getRequiredChildrenCount() {
		return 2;
	}

	@Override
	protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
		String s1 = ConversionUtil.toString(o);
		if (s1 == null) {
			return Boolean.FALSE;
		}

		return matchPattern(s1) ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new ASTLikeIgnoreCase(id, escapeChar);
	}

	@Override
	protected String getExpressionOperator(int index) {
		return "likeIgnoreCase";
	}

	@Override
	public int getType() {
		return Expression.LIKE_IGNORE_CASE;
	}

	@Override
	protected String getEJBQLExpressionOperator(int index) {
		return "like";
	}
}
