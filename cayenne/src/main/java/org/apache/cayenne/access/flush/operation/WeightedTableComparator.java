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
package org.apache.cayenne.access.flush.operation;

import org.apache.cayenne.annotation.SortWeight;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledTable;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The insert order of the tables of a compiled mapping: by the {@link SortWeight} of their entity classes, then by
 * the {@link CompiledTable#dependencyIndex() dependency index}. Built once per mapping, as both are facts of the
 * mapping and its classes. The delete order is the reverse.
 *
 * @since 6.0 replaces {@code WeightedAshwoodEntitySorter}, which had to be bound to take effect
 */
final class WeightedTableComparator implements Comparator<CompiledTable> {

    static final int DEFAULT_WEIGHT = 1;

    private final Map<CompiledTable, Integer> weights;

    WeightedTableComparator(CompiledMapping mapping) {
        this.weights = new IdentityHashMap<>();
        for (CompiledEntity entity : mapping.entities()) {
            Class<?> type = entity.objectClass();
            if (type != null && entity.table() != null) {
                int weight = weightOf(type);
                if (weight != DEFAULT_WEIGHT) {
                    weights.put(entity.table(), weight);
                }
            }
        }
    }

    /**
     * The weight declared on an entity class, by the current annotation or by its deprecated predecessor, or the
     * default when it declares none.
     */
    @SuppressWarnings("deprecation")
    static int weightOf(Class<?> type) {
        SortWeight weight = type.getAnnotation(SortWeight.class);
        if (weight != null) {
            return weight.value();
        }
        org.apache.cayenne.ashwood.SortWeight deprecated = type.getAnnotation(org.apache.cayenne.ashwood.SortWeight.class);
        return deprecated != null ? deprecated.value() : DEFAULT_WEIGHT;
    }

    @Override
    public int compare(CompiledTable t1, CompiledTable t2) {
        if (t1 == t2) {
            return 0;
        }
        int delta = weight(t1) - weight(t2);
        return delta != 0 ? delta : Integer.compare(t1.dependencyIndex(), t2.dependencyIndex());
    }

    private int weight(CompiledTable table) {
        return weights.isEmpty() ? DEFAULT_WEIGHT : weights.getOrDefault(table, DEFAULT_WEIGHT);
    }
}
