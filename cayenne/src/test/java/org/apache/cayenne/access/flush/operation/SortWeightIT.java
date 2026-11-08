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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.testdo.weighted_sort.SortDep;
import org.apache.cayenne.testdo.weighted_sort.SortRoot;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code @SortWeight} on an entity class overrides the FK order of the tables in a flush, with no configuration.
 * In the test model SORT_DEP references SORT_ROOT, and SortRoot carries {@code @SortWeight(2)}, which puts its
 * table after SORT_DEP on insert, the reverse of what the foreign key says.
 */
public class SortWeightIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.WEIGHTED_SORT_PROJECT);

    @Test
    public void dependencyIndexIgnoresWeights() {
        // the weights are a concern of the flush alone: the mapping indexes the tables by their dependencies
        CompiledMapping mapping = env.mapping();
        assertTrue(mapping.schema().table("SORT_ROOT").dependencyIndex()
                < mapping.schema().table("SORT_DEP").dependencyIndex());
    }

    @Test
    public void tableComparatorFollowsWeights() {
        CompiledMapping mapping = env.mapping();
        CompiledTable root = mapping.schema().table("SORT_ROOT");
        CompiledTable dep = mapping.schema().table("SORT_DEP");

        WeightedTableComparator comparator = new WeightedTableComparator(mapping);
        assertTrue(comparator.compare(dep, root) < 0);
        assertTrue(comparator.compare(root, dep) > 0);
        assertEquals(0, comparator.compare(root, root));
    }

    @Test
    public void flushInsertsAndDeletesByWeight() {
        CompiledMapping mapping = env.mapping();
        ObjectContext context = env.context();
        SortRoot root = context.newObject(SortRoot.class);
        SortDep dep = context.newObject(SortDep.class);
        dep.setRoot(root);

        DbRowOp insertRoot = new InsertDbRowOp(root, mapping.schema().table("SORT_ROOT"), root.getObjectId());
        DbRowOp insertDep = new InsertDbRowOp(dep, mapping.schema().table("SORT_DEP"), dep.getObjectId());
        DbRowOp deleteRoot = new DeleteDbRowOp(root, mapping.schema().table("SORT_ROOT"), root.getObjectId());
        DbRowOp deleteDep = new DeleteDbRowOp(dep, mapping.schema().table("SORT_DEP"), dep.getObjectId());

        DefaultDbRowOpSorter sorter = new DefaultDbRowOpSorter(() -> env.runtime().getDataDomain());
        List<DbRowOp> sorted = sorter.sort(new ArrayList<>(List.of(deleteRoot, insertRoot, deleteDep, insertDep)));

        // inserts first, the heavier SortRoot after SortDep; deletes last, in the reverse table order
        assertEquals(List.of(insertDep, insertRoot, deleteRoot, deleteDep), sorted);
        context.rollbackChanges();
    }

    @Test
    public void deprecatedAnnotationIsRecognized() {
        assertEquals(1, WeightedTableComparator.weightOf(Unweighted.class));
        assertEquals(3, WeightedTableComparator.weightOf(NewAnnotation.class));
        assertEquals(4, WeightedTableComparator.weightOf(OldAnnotation.class));
        // the current annotation wins when a class carries both
        assertEquals(3, WeightedTableComparator.weightOf(BothAnnotations.class));
    }

    private static class Unweighted {
    }

    @org.apache.cayenne.annotation.SortWeight(3)
    private static class NewAnnotation {
    }

    @SuppressWarnings("deprecation")
    @org.apache.cayenne.ashwood.SortWeight(4)
    private static class OldAnnotation {
    }

    @SuppressWarnings("deprecation")
    @org.apache.cayenne.annotation.SortWeight(3)
    @org.apache.cayenne.ashwood.SortWeight(4)
    private static class BothAnnotations {
    }
}
