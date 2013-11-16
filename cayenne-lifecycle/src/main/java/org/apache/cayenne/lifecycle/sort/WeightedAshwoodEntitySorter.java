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
package org.apache.cayenne.lifecycle.sort;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ashwood.AshwoodEntitySorter;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.commons.collections.comparators.ReverseComparator;

/**
 * EntitySorter that takes into account entity "weights", and otherwise delegating to
 * another (topological) sorter.
 * 
 * @since 3.1
 */
public class WeightedAshwoodEntitySorter extends AshwoodEntitySorter {

    private Comparator<DbEntity> weightedDbEntityComparator;
    private Comparator<ObjEntity> weightedObjEntityComparator;

    private Map<DbEntity, Integer> entityWeights;

    public WeightedAshwoodEntitySorter() {
        this.weightedDbEntityComparator = new WeightedDbEntityComparator();
        this.weightedObjEntityComparator = new WeightedObjEntityComparator();
        this.entityWeights = Collections.EMPTY_MAP;
    }

    @Override
    protected void doIndexSorter() {
        super.doIndexSorter();

        entityWeights = new HashMap<DbEntity, Integer>();

        for (ObjEntity entity : entityResolver.getObjEntities()) {

            Class<?> type = entityResolver
                    .getClassDescriptor(entity.getName())
                    .getObjectClass();
            SortWeight weight = type.getAnnotation(SortWeight.class);
            if (weight != null) {
                entityWeights.put(entity.getDbEntity(), weight.value());
            }
        }
    }

    @Override
    protected Comparator<DbEntity> getDbEntityComparator(boolean dependantFirst) {
        Comparator<DbEntity> c = weightedDbEntityComparator;
        if (dependantFirst) {
            c = new ReverseComparator(c);
        }
        return c;
    }

    @Override
    protected Comparator<ObjEntity> getObjEntityComparator(boolean dependantFirst) {
        Comparator<ObjEntity> c = weightedObjEntityComparator;
        if (dependantFirst) {
            c = new ReverseComparator(c);
        }
        return c;
    }

    private int getWeight(DbEntity e) {
        Integer w = entityWeights.get(e);
        return w != null ? w.intValue() : 1;
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
