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
package org.apache.cayenne.query;

import org.apache.cayenne.ejbql.EJBQLDelegatingVisitor;
import org.apache.cayenne.ejbql.EJBQLExpression;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLConditionTranslator extends EJBQLDelegatingVisitor {

    private EJBQLSelectTranslator parent;

    EJBQLConditionTranslator(EJBQLSelectTranslator parent) {
        this.parent = parent;
    }

    public boolean visitAnd(EJBQLExpression expression, int finishedChildIndex) {
        afterChild(expression, " AND", finishedChildIndex);
        return true;
    }

    public boolean visitEquals(EJBQLExpression expression, int finishedChildIndex) {
        afterChild(expression, " #", finishedChildIndex);
        return true;
    }

    public boolean visitOr(EJBQLExpression expression, int finishedChildIndex) {
        afterChild(expression, " OR", finishedChildIndex);
        return true;
    }

    protected void afterChild(EJBQLExpression e, String text, int childIndex) {
        if (childIndex >= 0) {
            if (childIndex + 1 < e.getChildrenCount()) {
                parent.getParent().getBuffer().append(text);
            }

            // reset child-specific delegate
            setDelegate(null);
        }
    }

    public boolean visitPath(EJBQLExpression expression) {
        setDelegate(new EJBQLPathVisitor());
        return true;
    }
    
    public boolean visitStringLiteral(EJBQLExpression expression) {
        
        return true;
    }
}
