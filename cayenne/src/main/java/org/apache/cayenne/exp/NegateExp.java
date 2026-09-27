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

package org.apache.cayenne.exp;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.cayenne.util.ConversionUtil;

/**
 * "Negate" expression.
 * 
 * @since 5.0
 */
public final class NegateExp extends Expression {
	public NegateExp(Object... operands) {
		super(operands);
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new NegateExp();
	}

	@Override
	protected Object evaluateNode(Object o) throws Exception {
		int len = getOperandCount();
		if (len == 0) {
			return null;
		}

		BigDecimal result = ConversionUtil.toBigDecimal(evaluateOperand(0, o));
		return result != null ? result.negate() : null;
	}

	@Override
	public void appendAsString(Appendable out) throws IOException {

		if (getOperandCount() > 0) {
			out.append("-");
			appendOperandAsString(0, out);
		}
	}

	@Override
	public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {

		if (getOperandCount() > 0) {
			out.append("-");
			appendOperandAsEJBQL(0, parameterAccumulator, out, rootId);
		}
	}

	@Override
	protected String getExpressionOperator(int index) {
		throw new UnsupportedOperationException("No operator for '" + expName()
				+ "'");
	}

	@Override
	public int getOperandCount() {
		return 1;
	}

	@Override
	protected boolean parenthesizeAsOperand() {
		// a unary minus binds tighter than any operator, so it needs no parentheses
		return false;
	}
}
