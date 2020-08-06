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
package org.apache.cayenne.access.translator.ejbql;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.EJBQLExpressionVisitor;

/**
 * @since 3.0
 */
class EJBQLOrderByTranslator extends EJBQLBaseVisitor {

    private EJBQLTranslationContext context;
    private int itemCount;

    EJBQLOrderByTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    @Override
    public boolean visitOrderByItem(EJBQLExpression expression) {
        if (itemCount++ > 0) {
            context.append(',');
        }

        return true;
    }

    @Override
    public boolean visitDescending(EJBQLExpression expression) {
        context.append(" DESC");
        return true;
    }
    
	@Override
	public boolean visitAggregate(EJBQLExpression expression) {
		expression.visit(context.getTranslatorFactory().getAggregateColumnTranslator(context));
		return false;
	}

    @Override
    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {

        EJBQLExpressionVisitor childVisitor = new EJBQLPathTranslator(context) {

            @Override
            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                throw new EJBQLException("Can't order on multi-column paths or objects");
            }
        };
        expression.visit(childVisitor);
        return false;
    }

    @Override
    public boolean visitDbPath(EJBQLExpression expression, int finishedChildIndex) {
        expression.visit(new EJBQLDbPathTranslator(context) {
            @Override
            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                throw new EJBQLException("Can't order on multi-column paths or objects");
            }
        });
        return false;
    }
}
