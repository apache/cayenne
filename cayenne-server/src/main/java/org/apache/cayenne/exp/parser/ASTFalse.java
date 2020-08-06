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

import java.io.IOException;
import java.util.List;

import org.apache.cayenne.exp.Expression;

/**
 * Boolean false expression element
 * 
 * Notice that there is one ASTTrue and one ASTFalse instead of a ASTBoolean
 * with a Boolean value. The main reason for doing this is that a common
 * ASTBoolean will have operand count of 1 and that will default to a prepared
 * statement like " where ? and (...)", but we only need " where true and
 * (...)".
 * 
 * @see ASTTrue
 * @since 3.0
 */
public class ASTFalse extends ConditionNode {

	private static final long serialVersionUID = -8441997825701749863L;

	/**
	 * Constructor used by expression parser. Do not invoke directly.
	 */
	ASTFalse(int id) {
		super(id);
	}

	public ASTFalse() {
		super(ExpressionParserTreeConstants.JJTFALSE);
	}

	@Override
	protected int getRequiredChildrenCount() {
		return 0;
	}

	@Override
	protected Boolean evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
		return Boolean.FALSE;
	}

	@Override
	protected String getExpressionOperator(int index) {
		throw new UnsupportedOperationException("No operator for '" + ExpressionParserTreeConstants.jjtNodeName[id] + "'");
	}

	@Override
	public Expression shallowCopy() {
		return new ASTFalse(id);
	}

	@Override
	public int getType() {
		return Expression.FALSE;
	}

	/**
	 * @since 4.0
	 */
	@Override
	public void appendAsString(Appendable out) throws IOException {
		out.append("false");
	}

	/**
	 * @since 4.0
	 */
	@Override
	public void appendAsEJBQL(List<Object> parameterAccumulator, Appendable out, String rootId) throws IOException {
		out.append("false");
	}
}
