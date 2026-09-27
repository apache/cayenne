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

import java.util.function.Function;

/**
 * Superclass of aggregated conditional nodes such as NOT, AND, OR. Performs
 * extra checks on parent and child expressions to validate conditions that are
 * not addressed in the Cayenne expressions grammar.
 * 
 * @since 5.0
 */
public abstract sealed class AggregateConditionExp extends BaseExp permits AndExp, NotExp, OrExp, WhenExp {
	protected AggregateConditionExp(Object... operands) {
		super(operands);
	}

	@Override
	protected boolean pruneNodeForPrunedChild(Object prunedChild) {
		return false;
	}

	@Override
	protected Object transformExpression(Function<Object, Object> transformer) {
		Object transformed = super.transformExpression(transformer);

		if (!(transformed instanceof AggregateConditionExp)) {
			return transformed;
		}

		AggregateConditionExp condition = (AggregateConditionExp) transformed;

		// prune itself if the transformation resulted in
		// no children or a single child
		switch (condition.getOperandCount()) {
		case 1:
			if (condition instanceof NotExp || condition instanceof WhenExp) {
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
	protected boolean isValidParent(BaseExp parent) {
		return parent instanceof AggregateConditionExp
				|| parent instanceof ExistsExp
				|| parent instanceof NotExistsExp;
	}

	@Override
	public void setOperand(int index, Object value) {
		// this is a check that we can't handle properly in the grammar... do it here...
		// only allow conditional nodes... no scalars
		if (!(value instanceof ConditionExp) && !(value instanceof AggregateConditionExp)) {
			String label = (value instanceof BaseExp node) ? node.expName() : String.valueOf(value);
			throw new ExpressionException(expName() + ": invalid operand - " + label);
		}

		super.setOperand(index, value);
	}

	@Override
	public Expression exists() {
		return ExpressionFactory.exists(this);
	}

	@Override
	public Expression notExists() {
		return ExpressionFactory.notExists(this);
	}
}
