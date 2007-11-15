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
package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.access.jdbc.EJBQLConditionTranslator;
import org.apache.cayenne.access.jdbc.EJBQLTranslationContext;
import org.apache.cayenne.ejbql.EJBQLExpression;

/**
 * Customizes EJBQL conditions translation for MySQL.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class MySQLEJBQLConditionTranslator extends EJBQLConditionTranslator {

    MySQLEJBQLConditionTranslator(EJBQLTranslationContext context) {
        super(context);
    }

    public boolean visitTrim(EJBQLExpression expression, int finishedChildIndex) {
        if (finishedChildIndex < 0) {
            context.append(" TRIM(");
        }
        else if (finishedChildIndex + 2 == expression.getChildrenCount()) {
            context.append(" FROM");
        }
        else if (finishedChildIndex + 1 == expression.getChildrenCount()) {
            context.append(")");
        }

        return true;
    }

    public boolean visitTrimCharacter(EJBQLExpression expression) {
        context.append(' ').append(expression.getText());
        return false;
    }

    public boolean visitTrimLeading(EJBQLExpression expression) {
        context.append("LEADING");
        return false;
    }

    public boolean visitTrimTrailing(EJBQLExpression expression) {
        context.append("TRAILING");
        return false;
    }

    public boolean visitTrimBoth(EJBQLExpression expression) {
        context.append("BOTH");
        return false;
    }
}
