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
package org.apache.cayenne.ejbql;

import java.util.List;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.SQLResult;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Represents an EJB QL expression "compiled" in the context of a certain mapping.
 * 
 * @since 3.0
 */
public interface EJBQLCompiledExpression {

    /**
     * Returns a tree representation of an EJBQL expression.
     */
    EJBQLExpression getExpression();

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
     * Returns a collection of relationships that joins identifier with a parent entity.
     * Returns null if the identifier corresponds to one of the query roots.
     */
    List<DbRelationship> getIncomingRelationships(String identifier);

    /**
     * Returns EJB QL source of the compiled expression if available.
     */
    String getSource();

    /**
     * Returns a mapping of the result set columns, or null if this is not a select
     * expression.
     */
    SQLResult getResult();
    
    /**
     * Returns prefetched columns tree for fetch joins.
     */
    PrefetchTreeNode getPrefetchTree();
}
