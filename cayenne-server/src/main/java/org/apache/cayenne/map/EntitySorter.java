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

package org.apache.cayenne.map;

import java.util.Comparator;
import java.util.List;

/**
 * Defines API for sorting of Cayenne entities based on their mutual dependencies.
 * 
 * @since 1.1
 */
public interface EntitySorter {

    /**
     * Sets EntityResolver for this sorter. All entities present in the resolver will be
     * used to determine sort ordering.
     * 
     * @since 3.1
     */
    void setEntityResolver(EntityResolver resolver);

    /**
     * Sorts a list of DbEntities.
     */
    void sortDbEntities(List<DbEntity> dbEntities, boolean deleteOrder);

    /**
     * Sorts a list of ObjEntities.
     */
    void sortObjEntities(List<ObjEntity> objEntities, boolean deleteOrder);

    /**
     * Sorts a list of objects belonging to the ObjEntity.
     */
    void sortObjectsForEntity(ObjEntity entity, List<?> objects, boolean deleteOrder);

    /**
     * @return comparator for {@link DbEntity}
     * @since 4.2
     */
    Comparator<DbEntity> getDbEntityComparator();

    /**
     * @return comparator for {@link ObjEntity}
     * @since 4.2
     */
    Comparator<ObjEntity> getObjEntityComparator();

    /**
     * @param entity to check
     * @return is entity has reflexive relationships
     *
     * @since 4.2
     */
    boolean isReflexive(DbEntity entity);
}
