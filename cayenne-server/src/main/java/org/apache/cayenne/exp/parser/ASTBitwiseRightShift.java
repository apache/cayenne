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

import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.util.ConversionUtil;

/**
 * Bitwise right shift '&gt;&gt;' operation.
 * 
 * @since 3.2
 */
public class ASTBitwiseRightShift extends SimpleNode {
	private static final long serialVersionUID = 1L;

	ASTBitwiseRightShift(int id) {
		super(id);
	}

	public ASTBitwiseRightShift() {
		super(ExpressionParserTreeConstants.JJTBITWISERIGHTSHIFT);
	}

	public ASTBitwiseRightShift(Object[] nodes) {
		super(ExpressionParserTreeConstants.JJTBITWISERIGHTSHIFT);
		int len = nodes.length;
		for (int i = 0; i < len; i++) {
			jjtAddChild(wrapChild(nodes[i]), i);
		}

		connectChildren();
	}

	public ASTBitwiseRightShift(Collection<Object> nodes) {
		super(ExpressionParserTreeConstants.JJTBITWISERIGHTSHIFT);
		int len = nodes.size();
		Iterator<Object> it = nodes.iterator();
		for (int i = 0; i < len; i++) {
			jjtAddChild(wrapChild(it.next()), i);
		}
	}

	@Override
	protected Object evaluateNode(Object o) throws Exception {
		int len = jjtGetNumChildren();
		if (len == 0) {
			return null;
		}

		Long result = null;
		for (int i = 0; i < len; i++) {
			Long value = ConversionUtil.toLong(evaluateChild(i, o),
					Long.MIN_VALUE);

			if (value == Long.MIN_VALUE) {
				return null;
			}

			result = (i == 0) ? value : result >> value;
		}

		return result;
	}

	@Override
	protected String getExpressionOperator(int index) {
		return ">>";
	}
	
	@Override
	public int getType() {
		return Expression.BITWISE_RIGHT_SHIFT;
	}
	
	@Override
	protected String getEJBQLExpressionOperator(int index) {
		throw new UnsupportedOperationException(
				"EJBQL 'bitwise not' is not supported");
	}

	@Override
	public Expression shallowCopy() {
		return new ASTBitwiseRightShift(id);
	}

	@Override
	public void jjtClose() {
		super.jjtClose();
		flattenTree();
	}
}
