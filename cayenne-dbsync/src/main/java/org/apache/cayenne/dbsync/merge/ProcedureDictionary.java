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

package org.apache.cayenne.dbsync.merge;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;

/**
 * @since 4.2
 */
public class ProcedureDictionary extends MergerDictionary<Procedure> {

    private final DataMap container;

    private final FiltersConfig filtersConfig;

    ProcedureDictionary(DataMap container, FiltersConfig filtersConfig) {
        this.container = container;
        this.filtersConfig = filtersConfig;
    }

    @Override
    String getName(Procedure entity) {
        return entity.getName();
    }

    @Override
    Collection<Procedure> getAll() {
        return filter();
    }

    private Collection<Procedure> filter() {
        if(filtersConfig == null) {
            return container.getProcedures();
        }

        Collection<Procedure> existingFiltered = new LinkedList<>();
        for(Procedure procedure : container.getProcedures()) {
            PatternFilter patternFilter = filtersConfig
                    .proceduresFilter(procedure.getCatalog(), procedure.getSchema());
            if(patternFilter != null && patternFilter.isIncluded(procedure.getName())) {
                existingFiltered.add(procedure);
            }
        }
        return existingFiltered;
    }
}
