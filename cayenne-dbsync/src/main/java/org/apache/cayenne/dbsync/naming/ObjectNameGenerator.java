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
import org.apache.cayenne.dbsync.reverse.db.ExportedKey;

/**
 * A strategy for creating names for object layer metadata artifacts based on their DB counterpart naming. Generated
 * names should normally be further cleaned by passing them through {@link org.apache.cayenne.dbsync.naming.NameBuilder},
 * that will resolve duplication conflicts.
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
     * Generates a name for DbRelationship derived from the DB foreign key name.
     */
    // TODO: the class is called Object* , but here it is generating a DB-layer name... Better naming?
    String dbRelationshipName(ExportedKey key, boolean toMany);

    /**
     * Generates a name for ObjRelationship derived from DbRelationship name.
     */
    String objRelationshipName(DbRelationship dbRelationship);
}
