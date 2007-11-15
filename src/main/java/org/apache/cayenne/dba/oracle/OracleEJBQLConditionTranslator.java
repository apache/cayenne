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
package org.apache.cayenne.dba.oracle;

import java.sql.Types;

import org.apache.cayenne.access.jdbc.EJBQLConditionTranslator;
import org.apache.cayenne.access.jdbc.EJBQLMultiColumnOperand;
import org.apache.cayenne.access.jdbc.EJBQLPathTranslator;
import org.apache.cayenne.access.jdbc.EJBQLTranslationContext;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.ObjAttribute;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class OracleEJBQLConditionTranslator extends EJBQLConditionTranslator {

    OracleEJBQLConditionTranslator(EJBQLTranslationContext context) {
        super(context);
    }

    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {

        expression.visit(new EJBQLPathTranslator(context) {

            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                OracleEJBQLConditionTranslator.this.addMultiColumnOperand(operand);
            }

            protected void processTerminatingAttribute(ObjAttribute attribute) {
                if (attribute.getDbAttribute().getType() == Types.CHAR) {
                    context.append(' ').append(OracleAdapter.TRIM_FUNCTION).append("(");
                    super.processTerminatingAttribute(attribute);
                    context.append(')');
                }
                else {
                    super.processTerminatingAttribute(attribute);
                }
            }
        });

        return false;
    }
}
