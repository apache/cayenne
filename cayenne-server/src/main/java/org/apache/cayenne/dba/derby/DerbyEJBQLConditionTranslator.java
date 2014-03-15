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
package org.apache.cayenne.dba.derby;

import org.apache.cayenne.access.translator.ejbql.EJBQLConditionTranslator;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslationContext;
import org.apache.cayenne.ejbql.EJBQLExpression;

/**
 * @since 3.1
 */
public class DerbyEJBQLConditionTranslator extends EJBQLConditionTranslator {

    public DerbyEJBQLConditionTranslator(EJBQLTranslationContext context) {
        super(context);
    }

    @Override
    public boolean visitCurrentTimestamp(EJBQLExpression expression) {
        context.append(" CURRENT_TIMESTAMP");
        return false;
    }

    @Override
    public boolean visitConcat(EJBQLExpression expression, int finishedChildIndex) {

        // note that for CHAR columns, CONCAT would include padding ... Should
        // we use TRIM here?
        if (finishedChildIndex < 0) {
            context.append(" (");
        } else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")");
        } else {
            context.append(" ||");
        }

        return true;
    }
}
