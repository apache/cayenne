/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.access.loader.filters;

import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.apache.cayenne.access.loader.filters.FilterFactory.*;
import static org.apache.cayenne.access.loader.filters.FiltersFactory.path;
import static org.apache.cayenne.access.loader.filters.FiltersFactory.eFilters;
import static org.junit.Assert.*;

public class FiltersConfigTest {

    @Test
    public void testSorting() throws Exception {
        FiltersConfig filters = new FiltersConfig(
                entityFilter("", ""),
                entityFilter("aaa", ""),
                entityFilter("aa", ""),
                entityFilter("aa", "a"),
                entityFilter("aa", "aa"),
                entityFilter("aa", "aa"),
                entityFilter("b", "b")
        );

        Iterator<DbPath> iterator = filters.getDbPaths().iterator();
        assertEquals(path("", ""), iterator.next());
        assertEquals(path("aa", ""), iterator.next());
        assertEquals(path("aa", "a"), iterator.next());
        assertEquals(path("aa", "aa"), iterator.next());
        assertEquals(path("aaa", ""), iterator.next());
        assertEquals(path("b", "b"), iterator.next());
    }

    private EntityFilters entityFilter(String s, String s1) {
        return new EntityFilters(new DbPath(s, s1), include("IncludeTable"), TRUE, TRUE);
    }

    @Test
    public void testActionsWithEmptyCatalog() throws Exception {
        FiltersConfig filters = new FiltersConfig(
                entityFilter(null, null),
                entityFilter("aaa", null),
                entityFilter("aa", null)
        );

        List<DbPath> actions = filters.pathsForQueries();
        assertEquals(1L, actions.size());
        assertEquals(path(), actions.get(0));
    }

    @Test
    public void testActionsWithEmptySchemas() throws Exception {
        FiltersConfig filters = new FiltersConfig(
                entityFilter("aaa", null),
                entityFilter("aaa", "11"),
                entityFilter("aa", null),
                entityFilter("aa", "a"),
                entityFilter("aa", "aa"),
                entityFilter("aa", "aa")
        );


        List<DbPath> actions = filters.pathsForQueries();
        assertEquals(2L, actions.size());
        assertEquals(path("aa", null), actions.get(0));
        assertEquals(path("aaa", null), actions.get(1));
    }

    @Test
    public void testActionsWithSchemas() throws Exception {
        FiltersConfig filters = new FiltersConfig(
                entityFilter("aaa", ""),
                entityFilter("aa", "a"),
                entityFilter("aa", "aa"),
                entityFilter("aa", "aa"),
                entityFilter("aa", "b"),
                entityFilter("aa", "b"),
                entityFilter("aa", "b")
        );


        List<DbPath> actions = filters.pathsForQueries();
        assertEquals(4L, actions.size());
        assertEquals(path("aa", "a"), actions.get(0));
        assertEquals(path("aa", "aa"), actions.get(1));
        assertEquals(path("aa", "b"), actions.get(2));
        assertEquals(path("aaa", ""), actions.get(3));
    }

    @Test
    public void testActionsWithSchemasAndEmptyCatalog() throws Exception {
        FiltersConfig filters = new FiltersConfig(
                entityFilter("", "a"),
                entityFilter("", "aa"),
                entityFilter("", "aa"),
                entityFilter("", "b"),
                entityFilter("", "b"),
                entityFilter("", "b")
        );


        List<DbPath> actions = filters.pathsForQueries();
        assertEquals(3L, actions.size());
        assertEquals(path("", "b"), actions.get(2));
        assertEquals(path("", "aa"), actions.get(1));
        assertEquals(path("", "a"), actions.get(0));

    }

    @Test
    public void testFiltersOneFilter() throws Exception {
        FiltersConfig filters = new FiltersConfig(
                eFilters(path("", "a"), include("table")),
                entityFilter("", "aa"),
                entityFilter("", "aa"),
                entityFilter("", "b"),
                entityFilter("", "b"),
                entityFilter("", "b")
        );

        assertEquals(eFilters(path("", "a"), include("table")),
                     filters.filter(path("", "a")));
    }

    @Test
    public void testFiltersJoinFilters() throws Exception {
        FiltersConfig filters = new FiltersConfig(
                eFilters(path("", "a"), include("table")),
                eFilters(path("", "a"), exclude("table")),
                entityFilter("", "aa"),
                entityFilter("", "aa")
        );

        assertEquals(eFilters(path("", "a"), list(include("table"), exclude("table"))),
                filters.filter(path("", "a")));

        assertEquals(entityFilter("", "aa"), filters.filter(path("", "aa")));
    }

    @Test
    public void testFiltersJoinFiltersWithNull() throws Exception {
        FiltersConfig filters = new FiltersConfig(
                eFilters(path("", "a"), include("table")),
                eFilters(path("", "a"), exclude("table")),
                eFilters(path("", "a"), null)
        );

        assertEquals(eFilters(path("", "a"), list(include("table"), exclude("table"))),
                     filters.filter(path("", "a")));
    }

    @Test
    public void testFiltersTopLevelTables() throws Exception {
        FiltersConfig filters = new FiltersConfig(
            eFilters(path(null, null), include("TableName"))
        );

        assertEquals(eFilters(path(null, null), include("TableName")),
                     filters.filter(path("", "APP")));
    }

    @Test
    public void testFiltersFor2Schemas() throws Exception {
        FiltersConfig filters = new FiltersConfig(
                eFilters(path(null, "schema_01"), include("TableName_01")),
                eFilters(path(null, "schema_02"), include("TableName_01"))
        );

        assertEquals(
                eFilters(path(null, "schema_01"), include("TableName_01")),
                filters.filter(path("", "schema_01")));

        assertEquals("In case we don't have filter that cover path we should return null filter ",
                eFilters(path("", "app"), null),
                filters.filter(path("", "app")));
    }
}