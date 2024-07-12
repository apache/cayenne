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

import java.util.function.Function;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;

/**
 * Superclass of aggregated conditional nodes such as NOT, AND, OR. Performs
 * extra checks on parent and child expressions to validate conditions that are
 * not addressed in the Cayenne expressions grammar.
 * 
 * @since 1.1
 */
public abstract class AggregateConditionNode extends SimpleNode {

	private static final long serialVersionUID = -636699350691988809L;

	AggregateConditionNode(int i) {
		super(i);
	}

	@Override
	protected boolean pruneNodeForPrunedChild(Object prunedChild) {
		return false;
	}

	@Override
	protected Object transformExpression(Function<Object, Object> transformer) {
		Object transformed = super.transformExpression(transformer);

		if (!(transformed instanceof AggregateConditionNode)) {
			return transformed;
		}

		AggregateConditionNode condition = (AggregateConditionNode) transformed;

		// prune itself if the transformation resulted in
		// no children or a single child
		switch (condition.getOperandCount()) {
		case 1:
			if (condition instanceof ASTNot) {
				return condition;
			} else {
				return condition.getOperand(0);
			}
		case 0:
			return PRUNED_NODE;
		default:
			return condition;
		}
	}

	@Override
	protected boolean isValidParent(Node n) {
		return n instanceof AggregateConditionNode
				|| n instanceof ASTExists
				|| n instanceof ASTNotExists;
	}

	@Override
	public void jjtAddChild(Node n, int i) {
		// this is a check that we can't handle properly
		// in the grammar... do it here...

		// only allow conditional nodes...no scalars
		if (!(n instanceof ConditionNode) && !(n instanceof AggregateConditionNode)) {
			String label = (n instanceof SimpleNode) ? ((SimpleNode) n).expName() : String.valueOf(n);
			throw new ExpressionException(expName() + ": invalid child - " + label);
		}

		super.jjtAddChild(n, i);
	}

	/**
	 * @inheritDoc
	 * @since 5.0
	 */
	@Override
	public Expression exists() {
		return ExpressionFactory.exists(this);
	}

	/**
	 * @inheritDoc
	 * @since 5.0
	 */
	@Override
	public Expression notExists() {
		return ExpressionFactory.notExists(this);
	}
}
