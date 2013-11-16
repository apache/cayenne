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

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;

public class WeightedAshwoodEntitySorterTest extends TestCase {

    private ServerRuntime runtime;

    @Override
    protected void setUp() throws Exception {
        runtime = new ServerRuntime("cayenne-lifecycle.xml");
    }

    @Override
    protected void tearDown() throws Exception {
        runtime.shutdown();
    }

    public void testSortDbEntities() {

        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

        // since it is impossible to ensure non-coincidental sort order of
        // unrelated
        // DbEntities (without overriding DbEntity.hashCode()), we'll test on 2
        // entities
        // with a relationship, and reverse the topological order with
        // SortWeight
        // annotation.

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
