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

import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.WEIGHTED_SORT_PROJECT)
public class WeightedAshwoodEntitySorterIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    EntityResolver resolver;

    @Before
    public void setUp() throws Exception {
        this.resolver = context.getEntityResolver();
    }

    @Test
    public void testSortDbEntities() {
        // since it is impossible to ensure non-coincidental sort order of unrelated
        // DbEntities (without overriding DbEntity.hashCode()), we'll test on 2 entities
        // with a relationship, and reverse the topological order with SortWeight annotation.

        List<DbEntity> eSorted = Arrays.asList(resolver.getDbEntity("SORT_DEP"), resolver.getDbEntity("SORT_ROOT"));

        List<DbEntity> e1 = Arrays.asList(resolver.getDbEntity("SORT_ROOT"), resolver.getDbEntity("SORT_DEP"));

        List<DbEntity> e2 = Arrays.asList(resolver.getDbEntity("SORT_DEP"), resolver.getDbEntity("SORT_ROOT"));

        WeightedAshwoodEntitySorter sorter = new WeightedAshwoodEntitySorter();
        sorter.setEntityResolver(resolver);

        sorter.sortDbEntities(e1, false);
        assertEquals(eSorted, e1);

        sorter.sortDbEntities(e2, false);
        assertEquals(eSorted, e2);
    }
}
