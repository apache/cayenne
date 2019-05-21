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

import org.apache.cayenne.CayenneRuntimeException;

/**
 * RuntimeException thrown on errors during expressions creation/parsing.
 */
public class ExpressionException extends CayenneRuntimeException {

	private static final long serialVersionUID = -4933472762330859309L;
	
	protected String expressionString;

	public ExpressionException() {
		super();
	}

	public ExpressionException(String messageFormat, Object... messageArgs) {
		super(messageFormat, messageArgs);
	}

	public ExpressionException(String messageFormat, Throwable cause, Object... messageArgs) {
		super(messageFormat, cause, messageArgs);
	}

	public ExpressionException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructor for ExpressionException.
	 * 
	 * @since 1.1
	 */
	public ExpressionException(String messageFormat, String expressionString, Throwable th, Object... messageArgs) {
		super(messageFormat, th, messageArgs);
		this.expressionString = expressionString;
	}

	public String getExpressionString() {
		return expressionString;
	}
}
