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

package org.apache.cayenne.dbsync.merge;

import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeSet;

import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;

class DbRelationshipDictionary extends MergerDictionary<DbRelationship> {

    private final DbEntity container;

    private final FiltersConfig filtersConfig;

    DbRelationshipDictionary(DbEntity container, FiltersConfig filtersConfig) {
        this.container = container;
        this.filtersConfig = filtersConfig;
    }

    @Override
    String getName(DbRelationship entity) {
        return new Signature(entity).getName();
    }

    @Override
    Collection<DbRelationship> getAll() {
        return filter();
    }

    /**
     * @since 4.1
     */
    private Collection<DbRelationship> filter() {
        if(filtersConfig == null) {
            return container.getRelationships();
        }

        Collection<DbRelationship> existingFiltered = new LinkedList<>();
        TableFilter tableFilter = filtersConfig.tableFilter(container.getCatalog(), container.getSchema());
        if(tableFilter != null && tableFilter.isIncludeTable(container.getName())){
            PatternFilter patternFilter = tableFilter.getIncludeTableRelationshipFilter(container.getName());
            for(DbRelationship rel : container.getRelationships()){
                if(patternFilter.isIncluded(rel.getName())){
                    existingFiltered.add(rel);
                }
            }
        }
        return existingFiltered;
    }

    /**
     * Signature of DbRelationship is sorted strings generated from its DbJoins
     */
    private static class Signature {
        private final DbRelationship relationship;

        private String[] joinSignature;

        private Signature(DbRelationship relationship) {
            this.relationship = relationship;
            build();
        }

        public String getName() {
            if(joinSignature.length == 0) {
                return "";
            }
            String name = joinSignature[0];
            for(int i=1; i<joinSignature.length; i++) {
                name += "|" + joinSignature[i];
            }
            return name;
        }

        private void build() {
            TreeSet<String> joins = new TreeSet<>();
            for(DbJoin join : relationship.getJoins()) {
                joins.add(
                        (join.getSource() == null ? "~" : join.getSource().getName()) + "." + join.getSourceName()
                        + ">" +
                        (join.getTarget() == null ? "~" : join.getTarget().getName()) + "." + join.getTargetName()
                );
            }
            joinSignature = joins.toArray(new String[0]);
        }
    }
}
