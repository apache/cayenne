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
package org.apache.cayenne.ejbql.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A compiled EJBQL expression.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class CompiledExpression implements EJBQLCompiledExpression {

    private String source;
    private String rootId;
    private Map descriptorsById;
    private Map incomingById;
    private EJBQLExpression expression;
    private Collection implicitJoins;

    public Collection getImplicitJoins() {
        return implicitJoins != null ? implicitJoins : Collections.EMPTY_SET;
    }

    public ClassDescriptor getEntityDescriptor(String idVariable) {
        if (idVariable == null) {
            return null;
        }

        // per JPA spec, 4.4.2, "Identification variables are case insensitive."
        idVariable = idVariable.toLowerCase();

        return (ClassDescriptor) descriptorsById.get(idVariable);
    }

    public ClassDescriptor getRootDescriptor() {
        return rootId != null ? getEntityDescriptor(rootId) : null;
    }

    public ObjRelationship getIncomingRelationship(String identifier) {
        return (ObjRelationship) incomingById.get(identifier);
    }

    public EJBQLExpression getExpression() {
        return expression;
    }

    public String getSource() {
        return source;
    }

    void setExpression(EJBQLExpression expression) {
        this.expression = expression;
    }

    void setDescriptorsById(Map descriptorsById) {
        this.descriptorsById = descriptorsById;
    }

    void setIncomingById(Map incomingById) {
        this.incomingById = incomingById;
    }

    void setSource(String source) {
        this.source = source;
    }

    void setRootId(String rootId) {
        this.rootId = rootId;
    }

    void setImplicitJoins(Collection implicitJoins) {
        this.implicitJoins = implicitJoins;
    }
}
