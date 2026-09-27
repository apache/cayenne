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

import java.util.Collection;


/**
 * "Equal To" expression.
 * 
 * @since 5.0
 */
public final class EqualExp extends ConditionExp {
	public EqualExp(Object... operands) {
		super(operands);
	}

	@Override
	protected int getRequiredChildrenCount() {
		return 2;
	}

	@Override
	protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
		Object o2 = evaluatedChildren[1];
		return evaluateImpl(o, o2) ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Compares two objects, if one of them is array, 'in' operation is
	 * performed
	 */
	static boolean evaluateImpl(Object o1, Object o2) {
		// TODO: maybe we need a comparison "strategy" here, instead of
		// a switch of all possible cases? ... there were other requests for
		// more relaxed type-unsafe comparison (e.g. numbers to strings)

		if (o1 == null && o2 == null) {
			return true;
		} else if (o1 != null) {
			// Per CAY-419 we perform 'in' comparison if one object is a list, and other is not
			if (o2 instanceof Collection) {
				for (Object element : ((Collection<?>) o2)) {
					if (element != null && Evaluator.evaluator(element).eq(element, o1)) {
						return true;
					}
				}
				return false;
			}

			return Evaluator.evaluator(o1).eq(o1, o2);
		}
		return false;
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new EqualExp();
	}

	@Override
	protected String getExpressionOperator(int index) {
		return "=";
	}

	@Override
	protected String getEJBQLExpressionOperator(int index) {
		if (getOperandCount() > 1 && getOperand(1) == null) {
			// for ejbql, we need "is null" instead of "= null"
			return "is";
		}
		return getExpressionOperator(index);
	}

}
