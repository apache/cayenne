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

package org.apache.cayenne.dba.firebird;

import org.apache.cayenne.access.translator.ejbql.EJBQLConditionTranslator;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslationContext;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.ejbql.parser.EJBQLTrimSpecification;

/**
 * @since 4.0
 */
public class FirebirdEJBQLConditionTranslator extends EJBQLConditionTranslator {

    public FirebirdEJBQLConditionTranslator(EJBQLTranslationContext context) {
        super(context);
    }

    @Override
    public boolean visitTrim(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            if (!(expression.getChild(0) instanceof EJBQLTrimSpecification)) {
                context.append(" {fn TRIM(");
            }
        } else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")}");
        }

        return true;
    }

    @Override
    public boolean visitTrimLeading(EJBQLExpression expression) {
        context.append(" {fn TRIM(LEADING FROM ");
        return false;
    }

    @Override
    public boolean visitTrimTrailing(EJBQLExpression expression) {
        context.append(" {fn TRIM(TRAILING FROM ");
        return false;
    }

    @Override
    public boolean visitTrimBoth(EJBQLExpression expression) {
        context.append(" {fn TRIM(");
        return false;
    }

    @Override
    public boolean visitLower(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn LOWER(");
            return true;
        } else {
            return super.visitLower(expression, finishedChildIndex);
        }
    }

    @Override
    public boolean visitLocate(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" {fn POSITION(");
            return true;
        } else {
            return super.visitLocate(expression, finishedChildIndex);
        }
    }

    @Override
    public boolean visitSubstring(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" SUBSTRING(");
        } else {
            if (finishedChildIndex + 1 == expression.getChildrenCount()) {
                context.append(" AS INTEGER))");
            } else {
                if(finishedChildIndex == 0) {
                    context.append(" FROM CAST(");
                } else if(finishedChildIndex == 1) {
                    context.append(" AS INTEGER) FOR CAST(");
                }
            }
        }

        return true;
    }
}
