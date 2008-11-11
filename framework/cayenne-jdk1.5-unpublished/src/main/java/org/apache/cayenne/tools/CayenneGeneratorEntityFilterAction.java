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
package org.apache.cayenne.tools;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;

/**
 * Performs entity filtering to build a collection of entities that should be used in
 * class generation.
 * 
 * @since 3.0
 */
class CayenneGeneratorEntityFilterAction {

    private NamePatternMatcher nameFilter;
    private boolean client;

    Collection<Embeddable> getFilteredEmbeddables(DataMap mainDataMap) {
        List<Embeddable> embeddables = new ArrayList<Embeddable>(mainDataMap
                .getEmbeddables());

        // filter out excluded entities...
        Iterator<Embeddable> it = embeddables.iterator();

        while (it.hasNext()) {
            Embeddable e = it.next();

            // note that unlike entity, embeddable is matched by class name as it doesn't
            // have a symbolic name...
            if (!nameFilter.isIncluded(e.getClassName())) {
                it.remove();
            }
        }

        return embeddables;
    }

    Collection<ObjEntity> getFilteredEntities(DataMap mainDataMap)
            throws MalformedURLException {

        List<ObjEntity> entities = new ArrayList<ObjEntity>(mainDataMap.getObjEntities());

        // filter out excluded entities...
        Iterator<ObjEntity> it = entities.iterator();

        while (it.hasNext()) {
            ObjEntity e = it.next();
            if (e.isGeneric()) {
                it.remove();
            }
            else if (client && !e.isClientAllowed()) {
                it.remove();
            }
            else if (!nameFilter.isIncluded(e.getName())) {
                it.remove();
            }
        }

        return entities;
    }

    void setClient(boolean client) {
        this.client = client;
    }

    public void setNameFilter(NamePatternMatcher nameFilter) {
        this.nameFilter = nameFilter;
    }
}
