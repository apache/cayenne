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
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

/**
 * A strategy for creating names for object layer metadata artifacts based on their DB counterpart naming or structure.
 * Generated names should normally be further cleaned by passing them through
 * {@link org.apache.cayenne.dbsync.naming.NameBuilder}, that will resolve duplication conflicts.
 *
 * @since 4.0
 */
public interface ObjectNameGenerator {

    /**
     * Generates a name for ObjEntity derived from DbEntity name.
     */
    String objEntityName(DbEntity dbEntity);

    /**
     * Generates a name for ObjAttribute derived from DbAttribute name.
     */
    String objAttributeName(DbAttribute dbAttribute);

    /**
     * Generates a String that can be used as a name of an ObjRelationship, derived from join semantics of a chain of
     * connected DbRelationships.
     * <p>The chain must contain at least one relationship. Though if we are dealing with a flattened
     * relationship, more than one can be passed, in the same order as they are present in a flattened
     * relationship.
     * <p>Generated name can be used for DbRelationship itself (in which case the chain must have exactly one parameter).
     */
    String relationshipName(DbRelationship... relationshipChain);

}
