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
import java.util.Iterator;

import org.apache.cayenne.util.ConversionUtil;

/**
 * "And" expression.
 * 
 * @since 5.0
 */
public final class AndExp extends AggregateConditionExp {
	public AndExp(Object... operands) {
		super(operands);
	}

	@Override
	protected Object evaluateNode(Object o) throws Exception {
		int len = getChildCount();
		if (len == 0) {
			return Boolean.FALSE;
		}

		// https://en.wikipedia.org/wiki/Three-valued_logic
		boolean unknown = false;
		boolean result = true;
		for (int i = 0; i < len; i++) {
			Object value = evaluateChild(i, o);
			if (value == null) {
				unknown = true;
			} else if (!ConversionUtil.toBoolean(value)) {
				result = false;
				break;
			}
		}

		return result ? (unknown ? null : Boolean.TRUE) : Boolean.FALSE;
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		return new AndExp();
	}

	@Override
	public void childrenAdded() {
		super.childrenAdded();
		flattenTree();
	}

	@Override
	protected String getExpressionOperator(int index) {
		return "and";
	}

}
