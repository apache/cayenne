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

import org.apache.cayenne.util.Util;

import java.io.Serializable;
import java.util.Objects;

/**
 * Named parameter for parameterized expressions.
 */
public class ExpressionParameter implements Serializable {

	private static final long serialVersionUID = -8324061115570177022L;
	
	protected String name;

	/**
	 * Constructor for ExpressionParam.
	 */
	public ExpressionParameter(String name) {
		this.name = name;
	}

	/**
	 * Returns the name of the expression parameter.
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return '$' + name;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ExpressionParameter)) {
			return false;
		}

		ExpressionParameter parameter = (ExpressionParameter) o;
		return Util.nullSafeEquals(name, parameter.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}
}
