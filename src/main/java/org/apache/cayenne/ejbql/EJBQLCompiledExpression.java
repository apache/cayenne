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
package org.apache.cayenne.ejbql;

import java.util.Collection;

import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Represents an EJB QL expression "compiled" in the context of a certain mapping.
 * 
 * @author Andrus Adamchik
 * @since 3.0
 */
public interface EJBQLCompiledExpression {

    /**
     * Returns a tree representation of an EJBQL expression.
     */
    EJBQLExpression getExpression();

    /**
     * Returns a collection of EJBQLExpressions, each representing an implicit join in the
     * query. The most common example of implicit joins are joins introduced by
     * relationships in the WHERE clause. Some implicit joins may also have matching
     * explicit joins in the same query. Such joins are not included in the returned
     * collection.
     */
    Collection getImplicitJoins();

    /**
     * Returns a descriptor of the root of this expression such as entity being fetched or
     * updated.
     */
    ClassDescriptor getRootDescriptor();

    /**
     * Returns a ClassDescriptor for the id variable.
     */
    ClassDescriptor getEntityDescriptor(String identifier);

    /**
     * Returns a relationship that joins identifier with a parent entity. Returns null if
     * the identifier corresponds to one of the query roots.
     */
    ObjRelationship getIncomingRelationship(String identifier);

    /**
     * Returns EJB QL source of the compiled expression if available.
     */
    String getSource();
}
