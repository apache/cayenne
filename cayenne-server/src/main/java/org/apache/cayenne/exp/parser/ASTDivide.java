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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.exp.Expression;

/**
 * "Divide" expression.
 * 
 * @since 1.1
 */
public class ASTDivide extends EvaluatedMathNode {

	private static final long serialVersionUID = -5086569683844539310L;

	ASTDivide(int id) {
		super(id);
	}

	public ASTDivide() {
		super(ExpressionParserTreeConstants.JJTDIVIDE);
	}

	public ASTDivide(Object... nodes) {
		this(Arrays.asList(nodes));
	}

	public ASTDivide(Collection<?> nodes) {
		super(ExpressionParserTreeConstants.JJTDIVIDE);
		int len = nodes.size();
		Iterator<?> it = nodes.iterator();
		for (int i = 0; i < len; i++) {
			jjtAddChild(wrapChild(it.next()), i);
		}

		connectChildren();
	}

	@Override
	protected BigDecimal op(BigDecimal result, BigDecimal arg) {
		return result.divide(arg, RoundingMode.HALF_EVEN);
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new ASTDivide(id);
	}

	@Override
	protected String getExpressionOperator(int index) {
		return "/";
	}

	@Override
	public int getType() {
		return Expression.DIVIDE;
	}

	@Override
	public void jjtClose() {
		super.jjtClose();
		flattenTree();
	}
}
