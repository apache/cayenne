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
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WeightedAshwoodEntitySorterIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.WEIGHTED_SORT_PROJECT);

    @Test
    public void sortDbEntities() {
        CompiledMapping mapping = env.mapping();

        // since it is impossible to ensure non-coincidental sort order of unrelated
        // DbEntities (without overriding DbEntity.hashCode()), we'll test on 2 entities
        // with a relationship, and reverse the topological order with SortWeight annotation.
        List<CompiledTable> eSorted = Arrays.asList(
                mapping.schema().table("SORT_DEP"),
                mapping.schema().table("SORT_ROOT"));
        List<CompiledTable> e1 = Arrays.asList(mapping.schema().table("SORT_ROOT"), mapping.schema().table("SORT_DEP"));
        List<CompiledTable> e2 = Arrays.asList(mapping.schema().table("SORT_DEP"), mapping.schema().table("SORT_ROOT"));

        WeightedAshwoodEntitySorter sorter = new WeightedAshwoodEntitySorter(mapping);

        sorter.sortDbEntities(e1, false);
        assertEquals(eSorted, e1);

        sorter.sortDbEntities(e2, false);
        assertEquals(eSorted, e2);
    }

    @Test
    public void dependencyIndexIgnoresWeights() {
        // the weights are a concern of the sorter alone: the mapping indexes the tables by their dependencies
        CompiledMapping mapping = env.mapping();
        assertTrue(mapping.schema().table("SORT_ROOT").dependencyIndex()
                < mapping.schema().table("SORT_DEP").dependencyIndex());
    }
}
