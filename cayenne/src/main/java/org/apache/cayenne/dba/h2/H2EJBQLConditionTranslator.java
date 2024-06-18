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

package org.apache.cayenne.dba.h2;

import java.sql.Types;

import org.apache.cayenne.access.translator.ejbql.EJBQLConditionTranslator;
import org.apache.cayenne.access.translator.ejbql.EJBQLMultiColumnOperand;
import org.apache.cayenne.access.translator.ejbql.EJBQLPathTranslator;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslationContext;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.ObjAttribute;

/**
 * @since 5.0
 */
public class H2EJBQLConditionTranslator extends EJBQLConditionTranslator {

    H2EJBQLConditionTranslator(EJBQLTranslationContext context) {
        super(context);
    }

    @Override
    public boolean visitPath(EJBQLExpression expression, int finishedChildIndex) {

        expression.visit(new EJBQLPathTranslator(context) {

            @Override
            protected void appendMultiColumnPath(EJBQLMultiColumnOperand operand) {
                H2EJBQLConditionTranslator.this.addMultiColumnOperand(operand);
            }

            @Override
            protected void processTerminatingAttribute(ObjAttribute attribute) {
                if (attribute.getDbAttribute().getType() == Types.CHAR) {
                    context.append(' ').append(HSQLDBAdapter.TRIM_FUNCTION).append("(");
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