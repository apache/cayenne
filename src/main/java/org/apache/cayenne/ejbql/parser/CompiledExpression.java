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

import java.util.Map;

import org.apache.cayenne.ejbql.EJBQLCompiledExpression;
import org.apache.cayenne.ejbql.EJBQLExpression;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * A compiled EJBQL expression.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class CompiledExpression implements EJBQLCompiledExpression {

    private String source;
    private Map descriptorsById;
    private EJBQLExpression expression;

    public ClassDescriptor getEntityDescriptor(String idVariable) {
        if (idVariable == null) {
            return null;
        }

        // per JPA spec, 4.4.2, "Identification variables are case insensitive."
        idVariable = idVariable.toLowerCase();

        return (ClassDescriptor) descriptorsById.get(idVariable);
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

    void setSource(String source) {
        this.source = source;
    }
}
