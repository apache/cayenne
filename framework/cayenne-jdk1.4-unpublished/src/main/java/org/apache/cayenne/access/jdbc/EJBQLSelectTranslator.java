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
package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.ejbql.EJBQLDelegatingVisitor;
import org.apache.cayenne.ejbql.EJBQLExpression;

/**
 * A translator of EJBQL select statements into SQL.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLSelectTranslator extends EJBQLDelegatingVisitor {

    private EJBQLTranslationContext context;

    EJBQLSelectTranslator(EJBQLTranslationContext context) {
        this.context = context;
    }

    EJBQLTranslationContext getContext() {
        return context;
    }

    public boolean visitDistinct(EJBQLExpression expression) {
        context.append(" DISTINCT");
        return true;
    }

    public boolean visitFrom(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" FROM");
            setDelegate(new EJBQLFromTranslator(context));
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.markCurrentPosition(EJBQLTranslationContext.FROM_TAIL_MARKER);
        }
        
        return true;
    }

    public boolean visitOrderBy(EJBQLExpression expression) {
        context.append(" ORDER BY");
        setDelegate(new EJBQLSelectOrderByTranslator());
        return true;
    }

    public boolean visitSelect(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append("SELECT");
            setDelegate(new EJBQLSelectColumnsTranslator(context));
        }

        return true;
    }

    public boolean visitWhere(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" WHERE");
            setDelegate(new EJBQLConditionTranslator(context));
        }
        return true;
    }
}
