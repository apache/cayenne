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
package org.apache.cayenne.dba.sybase;

import org.apache.cayenne.access.translator.ejbql.EJBQLConditionTranslator;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslationContext;
import org.apache.cayenne.ejbql.EJBQLExpression;

class SybaseEJBQLConditionTranslator extends EJBQLConditionTranslator {

    boolean isExists;

    public SybaseEJBQLConditionTranslator(EJBQLTranslationContext context) {
        super(context);
    }

    @Override
    public boolean visitSubselect(EJBQLExpression expression) {
        context.onSubselect();
        context.append(" (");
        expression.visit(new SybaseEJBQLSubselectTranslator(context, isExists));
        context.append(')');
        isExists = false;
        return false;
    }

    @Override
    public boolean visitExists(EJBQLExpression expression) {
        isExists = true;
        context.append(" EXISTS");
        return true;
    }

}
