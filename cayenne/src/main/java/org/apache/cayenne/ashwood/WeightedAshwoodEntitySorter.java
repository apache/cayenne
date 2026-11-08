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
package org.apache.cayenne.ashwood;

import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledEntity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A sorter of tables and entities that takes into account entity "weights" (see {@link SortWeight}), and otherwise
 * follows the {@link CompiledTable#dependencyIndex() dependency order} of the tables. As the compiled mapping
 * is immutable, a sorter is built for a mapping and indexes the weights once.
 *
 * @since 3.1, since 4.0 moved to cayenne-server from cayenne-lifecycle
 */
public class WeightedAshwoodEntitySorter {

    protected final CompiledMapping mapping;
    protected final Comparator<CompiledTable> dbEntityComparator;
    protected final Map<CompiledTable, Integer> entityWeights;

    private final Comparator<CompiledTable> weightedDbEntityComparator;
    private final Comparator<CompiledEntity> weightedObjEntityComparator;

    /**
     * @since 6.0
     */
    public WeightedAshwoodEntitySorter(CompiledMapping mapping) {
        this.mapping = Objects.requireNonNull(mapping);
        this.dbEntityComparator = Comparator.comparingInt(CompiledTable::dependencyIndex);
        this.entityWeights = new HashMap<>();

        for (CompiledEntity entity : mapping.entities()) {
            addWeightForEntity(entity);
        }

        this.weightedDbEntityComparator = new WeightedDbEntityComparator();
        this.weightedObjEntityComparator = new WeightedObjEntityComparator();
    }

    protected void addWeightForEntity(CompiledEntity entity) {
        // an unbound mapping has no classes, and hence no annotations
        Class<?> type = entity.objectClass();
        if (type == null) {
            return;
        }

        SortWeight weight = type.getAnnotation(SortWeight.class);
        if (weight != null) {
            entityWeights.put(entity.table(), weight.value());
        }
    }

    /**
     * Sorts the tables in place, in the order of insertion, or, if "deleteOrder" is true, in the order of deletion.
     *
     * @since 6.0
     */
    public void sortDbEntities(List<CompiledTable> tables, boolean deleteOrder) {
        tables.sort(getDbEntityComparator(deleteOrder));
    }

    /**
     * Sorts the entities in place, in the order of insertion, or, if "deleteOrder" is true, in the order of deletion.
     *
     * @since 6.0
     */
    public void sortObjEntities(List<CompiledEntity> entities, boolean deleteOrder) {
        entities.sort(getObjEntityComparator(deleteOrder));
    }

    /**
     * Returns a comparator ordering the tables in the order of insertion.
     *
     * @since 6.0
     */
    public Comparator<CompiledTable> getDbEntityComparator() {
        return weightedDbEntityComparator;
    }

    /**
     * Returns a comparator ordering the entities in the order of insertion.
     *
     * @since 6.0
     */
    public Comparator<CompiledEntity> getObjEntityComparator() {
        return weightedObjEntityComparator;
    }

    protected Comparator<CompiledTable> getDbEntityComparator(boolean dependantFirst) {
        Comparator<CompiledTable> c = weightedDbEntityComparator;
        if (dependantFirst) {
            c = c.reversed();
        }
        return c;
    }

    protected Comparator<CompiledEntity> getObjEntityComparator(boolean dependantFirst) {
        Comparator<CompiledEntity> c = weightedObjEntityComparator;
        if (dependantFirst) {
            c = c.reversed();
        }
        return c;
    }

    private int getWeight(CompiledTable e) {
        Integer w = entityWeights.get(e);
        return w != null ? w : 1;
    }

    private final class WeightedDbEntityComparator implements Comparator<CompiledTable> {

        public int compare(CompiledTable t1, CompiledTable t2) {
            if (t1 == t2) {
                return 0;
            }

            int delta = getWeight(t1) - getWeight(t2);
            return delta != 0 ? delta : dbEntityComparator.compare(t1, t2);
        }
    }

    private final class WeightedObjEntityComparator implements Comparator<CompiledEntity> {

        public int compare(CompiledEntity o1, CompiledEntity o2) {
            if (o1 == o2) {
                return 0;
            }

            CompiledTable t1 = o1.table();
            CompiledTable t2 = o2.table();

            return weightedDbEntityComparator.compare(t1, t2);
        }
    }
}
