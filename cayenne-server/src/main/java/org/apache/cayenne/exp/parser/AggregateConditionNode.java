/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.exp.parser;

import org.apache.commons.collections.Transformer;
import org.apache.cayenne.exp.ExpressionException;

/**
 * Superclass of aggregated conditional nodes such as NOT, AND, OR. Performs
 * extra checks on parent and child expressions to validate conditions that
 * are not addressed in the Cayenne expressions grammar.
 * 
 * @since 1.1
 */
public abstract class AggregateConditionNode extends SimpleNode {
    AggregateConditionNode(int i) {
        super(i);
    }

    @Override
    protected boolean pruneNodeForPrunedChild(Object prunedChild) {
        return false;
    }

    @Override
    protected Object transformExpression(Transformer transformer) {
        Object transformed = super.transformExpression(transformer);

        if (!(transformed instanceof AggregateConditionNode)) {
            return transformed;
        }
        
        AggregateConditionNode condition = (AggregateConditionNode) transformed;

        // prune itself if the transformation resulted in 
        // no children or a single child
        switch (condition.getOperandCount()) {
            case 1 :
                if (condition instanceof ASTNot) {
                    return condition;
                }
                else {
                    return condition.getOperand(0);
                }
            case 0 :
                return PRUNED_NODE;
            default :
                return condition;
        }
    }

    @Override
    public void jjtSetParent(Node n) {
        // this is a check that we can't handle properly
        // in the grammar... do it here...

        // disallow non-aggregated condition parents...
        if (!(n instanceof AggregateConditionNode)) {
            String label =
                (n instanceof SimpleNode)
                    ? ((SimpleNode) n).expName()
                    : String.valueOf(n);
            throw new ExpressionException(expName() + ": invalid parent - " + label);
        }

        super.jjtSetParent(n);
    }

    @Override
    public void jjtAddChild(Node n, int i) {
        // this is a check that we can't handle properly
        // in the grammar... do it here...

        // only allow conditional nodes...no scalars
        if (!(n instanceof ConditionNode) && !(n instanceof AggregateConditionNode)) {
            String label =
                (n instanceof SimpleNode)
                    ? ((SimpleNode) n).expName()
                    : String.valueOf(n);
            throw new ExpressionException(expName() + ": invalid child - " + label);
        }

        super.jjtAddChild(n, i);
    }
}
