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

import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.ConversionUtil;

/**
 * "Or" expression.
 * 
 * @since 1.1
 */
public class ASTOr extends AggregateConditionNode {

	private static final long serialVersionUID = 780157841581581297L;

	ASTOr(int id) {
		super(id);
	}

	public ASTOr() {
		super(ExpressionParserTreeConstants.JJTOR);
	}

	public ASTOr(Object[] nodes) {
		super(ExpressionParserTreeConstants.JJTOR);
		int len = nodes.length;
		for (int i = 0; i < len; i++) {
			jjtAddChild((Node) nodes[i], i);
		}
		connectChildren();
	}

	public ASTOr(Collection<? extends Node> nodes) {
		super(ExpressionParserTreeConstants.JJTOR);
		int len = nodes.size();
		Iterator<? extends Node> it = nodes.iterator();
		for (int i = 0; i < len; i++) {
			jjtAddChild(it.next(), i);
		}
		connectChildren();
	}

	@Override
	protected Object evaluateNode(Object o) throws Exception {
		int len = jjtGetNumChildren();
		if (len == 0) {
			return Boolean.FALSE;
		}

		// https://en.wikipedia.org/wiki/Three-valued_logic
		boolean unknown = false;
		boolean result = false;
		for (int i = 0; i < len; i++) {
			Object value = evaluateChild(i, o);
			if (value == null) {
				unknown = true;
			} else if (ConversionUtil.toBoolean(value)) {
				result = true;
				break;
			}
		}

		return result ? Boolean.TRUE : (unknown ? null : Boolean.FALSE);
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new ASTOr(id);
	}

	@Override
	protected String getExpressionOperator(int index) {
		return "or";
	}

	@Override
	public int getType() {
		return Expression.OR;
	}

	@Override
	public void jjtClose() {
		super.jjtClose();
		flattenTree();
	}
}
