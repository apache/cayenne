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
 * "Not like, ignore case" expression.
 * 
 */
public class ASTNotLikeIgnoreCase extends IgnoreCaseNode {

	private static final long serialVersionUID = -1837593236671099985L;

	ASTNotLikeIgnoreCase(int id) {
		super(id, true);
	}

	public ASTNotLikeIgnoreCase() {
		super(ExpressionParserTreeConstants.JJTNOTLIKEIGNORECASE, true);
	}

	public ASTNotLikeIgnoreCase(SimpleNode path, Object value) {
		super(ExpressionParserTreeConstants.JJTNOTLIKEIGNORECASE, true);
		jjtAddChild(path, 0);
		jjtAddChild(wrap(value), 1);
		connectChildren();
	}

	public ASTNotLikeIgnoreCase(SimpleNode path, Object value, char escapeChar) {
		super(ExpressionParserTreeConstants.JJTNOTLIKEIGNORECASE, true, escapeChar);
		jjtAddChild(path, 0);
		jjtAddChild(wrap(value), 1);
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

		return matchPattern(s1) ? Boolean.FALSE : Boolean.TRUE;
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new ASTNotLikeIgnoreCase(id);
	}

	@Override
	protected String getExpressionOperator(int index) {
		return "not likeIgnoreCase";
	}

	@Override
	protected String getEJBQLExpressionOperator(int index) {
		return "not like";
	}

	@Override
	public int getType() {
		return Expression.NOT_LIKE_IGNORE_CASE;
	}
}
