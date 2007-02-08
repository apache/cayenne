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


package org.apache.cayenne.access.util;

import java.util.List;

import org.apache.cayenne.map.ObjEntity;

/**
 * Interface that defines API to algorithms used for sorting Cayenne entities based on
 * their dependencies.
 * 
 * @author Andrus Adamchik
 * @deprecated In 1.2 replaced with {@link org.apache.cayenne.map.EntitySorter}
 */
public interface DependencySorter {

    /**
     * Sorts a list of DbEntities.
     */
    public void sortDbEntities(List dbEntities, boolean deleteOrder);

    /**
     * Sorts a list of ObjEntities.
     */
    public void sortObjEntities(List objEntities, boolean deleteOrder);

    /**
     * Sorts a list of objects belonging to the ObjEntity.
     */
    public void sortObjectsForEntity(ObjEntity entity, List objects, boolean deleteOrder);
}
