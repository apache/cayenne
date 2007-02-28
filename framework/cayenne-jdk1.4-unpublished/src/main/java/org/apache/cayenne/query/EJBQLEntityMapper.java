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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLBaseVisitor;
import org.apache.cayenne.ejbql.EJBQLException;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

/**
 * Maps EJBQL identification variables to entities.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class EJBQLEntityMapper extends EJBQLBaseVisitor {

    private Map entitiesById;
    private EntityResolver resolver;

    EJBQLEntityMapper(EntityResolver resolver) {
        this.entitiesById = new HashMap();
        this.resolver = resolver;
    }

    ObjEntity getMappedEntity(String idVariable) {
        if(idVariable == null) {
            return null;
        }
        
        // per JPA spec, 4.4.2, "Identification variables are case insensitive."
        idVariable = idVariable.toLowerCase();

        return (ObjEntity) entitiesById.get(idVariable);
    }

    public boolean visitFrom(EJBQLExpression expression) {

        // visit FROM node subtree children separately
        int len = expression.getChildrenCount();
        for (int i = 0; i < len; i++) {
            expression.getChild(i).visit(this);
        }

        // cancel parent visitor
        return false;
    }

    public boolean visitFromItem(EJBQLExpression expression) {

        if (expression.getChildrenCount() != 2) {
            throw new EJBQLException("Expected 2 children, got: "
                    + expression.getChildrenCount());
        }

        // TODO: andrus, 2/28/2007 - resolve path ... for now only support direct entity
        // names
        EJBQLExpression abstractSchemaName = expression.getChild(0);
        String schemaName = abstractSchemaName.getChild(0).getText();

        ObjEntity entity = resolver.getObjEntity(schemaName);
        if (entity == null) {
            throw new EJBQLException("Invalid abstract schema name: " + schemaName);
        }

        String idVariable = expression.getChild(1).getText();

        // per JPA spec, 4.4.2, "Identification variables are case insensitive."
        idVariable = idVariable.toLowerCase();

        ObjEntity old = (ObjEntity) entitiesById.put(idVariable, entity);
        if (old != null && old != entity) {
            throw new EJBQLException("Duplicate identification variable definition: "
                    + idVariable
                    + ", it is already used for "
                    + old.getName());
        }

        // cancel individual child visitation
        return false;
    }
}
