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

/**
 * A named expression parameter.
 * 
 * @since 5.0
 */
public final class NamedParameterExp extends ScalarExp {
	public NamedParameterExp(Object... operands) {
		super(operands);
	}

	@Override
	protected Object evaluateNode(Object o) throws Exception {
		throw new ExpressionException("Uninitialized parameter: " + value + ", call 'expWithParameters' first.");
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 */
	@Override
	public Expression shallowCopy() {
		NamedParameterExp copy = new NamedParameterExp();
		copy.value = value;
		return copy;
	}

	@Override
	public void setValue(Object value) {
		if (value == null) {
			throw new ExpressionException("Null Parameter value");
		}

		String name = value.toString().trim();
		if (name.length() == 0) {
			throw new ExpressionException("Empty Parameter value");
		}

		super.setValue(new ExpressionParameter(name));
	}

}
