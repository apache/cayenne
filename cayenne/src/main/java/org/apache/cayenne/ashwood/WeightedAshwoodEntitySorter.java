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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * EntitySorter that takes into account entity "weights", and otherwise delegating to
 * another (topological) sorter.
 * 
 * @since 3.1, since 4.0 moved to cayenne-server from cayenne-lifecycle
 */
public class WeightedAshwoodEntitySorter extends AshwoodEntitySorter {

    private final Comparator<DbEntity> weightedDbEntityComparator;
    private final Comparator<ObjEntity> weightedObjEntityComparator;

    protected volatile Map<DbEntity, Integer> entityWeights;

    public WeightedAshwoodEntitySorter(EntityResolver entityResolver) {
        super(entityResolver);

        // the comparators are stateless, reading the sorter fields only when actually comparing, so creating them
        // after super() (i.e. after the initial indexing) is safe
        this.weightedDbEntityComparator = new WeightedDbEntityComparator();
        this.weightedObjEntityComparator = new WeightedObjEntityComparator();
    }

    @Override
    public synchronized void reindex() {
        super.reindex();

        entityWeights = new HashMap<>();

        for (ObjEntity entity : entityResolver.getObjEntities()) {
            addWeightForEntity(entity);
        }
    }

    protected void addWeightForEntity(ObjEntity entity) {
        Class<?> type = entityResolver
                .getClassDescriptor(entity.getName())
                .getObjectClass();
        SortWeight weight = type.getAnnotation(SortWeight.class);
        if (weight != null) {
            entityWeights.put(entity.getDbEntity(), weight.value());
        }
    }

    @Override
    protected Comparator<DbEntity> getDbEntityComparator(boolean dependantFirst) {
        Comparator<DbEntity> c = weightedDbEntityComparator;
        if (dependantFirst) {
            c = c.reversed();
        }
        return c;
    }

    @Override
    protected Comparator<ObjEntity> getObjEntityComparator(boolean dependantFirst) {
        Comparator<ObjEntity> c = weightedObjEntityComparator;
        if (dependantFirst) {
            c = c.reversed();
        }
        return c;
    }

    private int getWeight(DbEntity e) {
        Integer w = entityWeights.get(e);
        return w != null ? w : 1;
    }

    private final class WeightedDbEntityComparator implements Comparator<DbEntity> {

        public int compare(DbEntity t1, DbEntity t2) {
            if (t1 == t2) {
                return 0;
            }

            int delta = getWeight(t1) - getWeight(t2);
            return delta != 0 ? delta : dbEntityComparator.compare(t1, t2);
        }
    }

    private final class WeightedObjEntityComparator implements Comparator<ObjEntity> {

        public int compare(ObjEntity o1, ObjEntity o2) {
            if (o1 == o2) {
                return 0;
            }

            DbEntity t1 = o1.getDbEntity();
            DbEntity t2 = o2.getDbEntity();

            return weightedDbEntityComparator.compare(t1, t2);
        }
    }
}
