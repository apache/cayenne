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
package org.apache.cayenne.tools;

import org.apache.cayenne.dbsync.filter.NameFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Performs entity filtering to build a collection of entities that should be used in
 * class generation.
 * 
 * @since 3.0
 */
class CayenneGeneratorEntityFilterAction {

    private NameFilter nameFilter;

    Collection<ObjEntity> getFilteredEntities(DataMap mainDataMap)
            throws MalformedURLException {

        Collection<ObjEntity> entities = new ArrayList<>(mainDataMap.getObjEntities());

        // filter out excluded entities...
        entities.removeIf(e -> e.isGeneric() || !nameFilter.isIncluded(e.getName()));

        return entities;
    }

    public void setNameFilter(NameFilter nameFilter) {
        this.nameFilter = nameFilter;
    }
}
