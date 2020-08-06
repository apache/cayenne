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

import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 3.0
 */
class EJBQLIdColumnsTranslator extends EJBQLIdentifierColumnsTranslator {

    private EJBQLTranslationContext context;

    EJBQLIdColumnsTranslator(EJBQLTranslationContext context) {
        super(context);
        this.context = context;
    }

    @Override
    public boolean visitIdentifier(EJBQLExpression expression) {

        Map<String, String> fields = null;
        if (context.isAppendingResultColumns()) {
            fields = context.nextEntityResult().getFields();
        }

        String idVar = expression.getText();

        ClassDescriptor descriptor = context.getEntityDescriptor(idVar);
        ObjEntity oe = descriptor.getEntity();

        for (ObjAttribute oa : oe.getPrimaryKeys()) {
            DbAttribute t = oe.getDbEntity().getAttribute(oa.getDbAttributeName());
            appendColumn(idVar, oa, t, fields, oa.getType());
        }

        return false;
    }

}
