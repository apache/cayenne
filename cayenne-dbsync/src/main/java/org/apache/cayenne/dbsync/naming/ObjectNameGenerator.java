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
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;

import java.util.List;

/**
 * A strategy for creating mapping artifact names based on DB tier metadata.
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
     * Generates a name for an ObjRelationship, derived from join semantics of a chain of connected DbRelationships.
     * <p>The chain must contain at least one relationship. Though if we are dealing with a flattened
     * relationship, more than one can be passed, in the same order as they are present in a flattened
     * relationship.
     *
     * @since 5.0
     */
    String objRelationshipName(DbRelationship... relationshipChain);

    /**
     * Generates a name for a DbRelationship, derived from the semantics of its joins and direction.
     *
     * @since 5.0
     */
    String dbRelationshipName(List<DbJoin> joins, boolean toMany);
}
