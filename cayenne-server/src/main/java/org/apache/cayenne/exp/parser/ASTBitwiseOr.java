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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.exp.Expression;


/**
 * Bitwise disjunction (OR or '|') expression.
 * 
 * @since 3.1
 */
public class ASTBitwiseOr extends EvaluatedBitwiseNode {
	private static final long serialVersionUID = 1L;

	ASTBitwiseOr(int id) {
		super(id);
	}
	
	public ASTBitwiseOr() {
		super(ExpressionParserTreeConstants.JJTBITWISEOR);
	}
	
	public ASTBitwiseOr(Object[] nodes) {
        this(Arrays.asList(nodes));
	}
	
    public ASTBitwiseOr(Collection<Object> nodes) {
        super(ExpressionParserTreeConstants.JJTBITWISEOR);
        int len = nodes.size();
        Iterator<Object> it = nodes.iterator();
        for (int i = 0; i < len; i++) {
            jjtAddChild(wrapChild(it.next()), i);
        }
        connectChildren();
    }

    @Override
    protected long op(long result, long arg) {
        return result | arg;
    }

    @Override
	protected String getExpressionOperator(int index) {
		return "|";
	}
	
	@Override
	public int getType() {
		return Expression.BITWISE_OR;
	}
	
	@Override
	protected String getEJBQLExpressionOperator(int index) {
		throw new UnsupportedOperationException(
				"EJBQL 'bitwise not' is not supported");
	}
	
	@Override
	public Expression shallowCopy() {
		return new ASTBitwiseOr(id);
	}
	
    @Override
    public void jjtClose() {
        super.jjtClose();
        flattenTree();
    }

}