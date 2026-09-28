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

import org.apache.cayenne.util.ConversionUtil;

/**
 * "Not" expression.
 * 
 * @since 5.0
 */
public final class NotExp extends AggregateConditionExp {
	public NotExp(Object... operands) {
		super(operands);
	}

	@Override
	protected Object evaluateNode(Object o) throws Exception {
		int len = getOperandCount();
		if (len == 0) {
			return Boolean.FALSE;
		}

		Object o1 = evaluateOperand(0, o);
		if (o1 == null) {
			return null;
		}

		return ConversionUtil.toBoolean(o1) ? Boolean.FALSE : Boolean.TRUE;
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new NotExp();
	}

	@Override
	public void appendAsString(Appendable out) throws IOException {
		out.append("not ");
		super.appendAsString(out);
	}

	@Override
	protected String getExpressionOperator(int index) {
		throw new UnsupportedOperationException("No operator for '" + expName() + "'");
	}
}
