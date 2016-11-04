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

package org.apache.cayenne.dbsync.reverse.db;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;

/**
 * Defines API for progress tracking and altering the folow of reverse-engineering.
 */
public interface DbLoaderDelegate {

    void dbEntityAdded(DbEntity entity);

    void dbEntityRemoved(DbEntity entity);

    /**
     * Called before relationship loading for a {@link DbEntity}.
     *
     * @param entity DbEntity for which {@link DbRelationship} is about to be loaded.
     * @return true in case you want process relationships for this entity, false otherwise.
     */
    boolean dbRelationship(DbEntity entity);

    /**
     * Called before relationship will be added into db-entity but after it was loaded from db
     *
     * @param entity
     * @return true in case you want add this relationship into entity
     * false otherwise
     */
    boolean dbRelationshipLoaded(DbEntity entity, DbRelationship relationship);

    /**
     * @deprecated since 4.0 no longer invoked as DbLoader does not deal with object layer anymore.
     */
    @Deprecated
    void objEntityAdded(ObjEntity entity);

    /**
     * @deprecated since 4.0 no longer invoked as DbLoader does not deal with object layer anymore.
     */
    @Deprecated
    void objEntityRemoved(ObjEntity entity);
}
