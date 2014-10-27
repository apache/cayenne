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

import static org.apache.cayenne.access.loader.filters.FilterFactory.*;
import static org.junit.Assert.*;

public class EntityFiltersTest {

    @Test
    public void testJoinWithEmpty() throws Exception {
        EntityFilters filter1 = new EntityFilters(null, null, null, null);
        EntityFilters filter2 = new EntityFilters(null, include("table"), include("column"), include("procedure"));

        assertEquals(filter2, filter1.join(filter2));
        assertEquals(filter2, filter2.join(filter1));
    }

    @Test
    public void testJoinExcludeInclude() throws Exception {
        EntityFilters filter1 = new EntityFilters(null, exclude("table"), exclude("column"), exclude("procedure"));
        EntityFilters filter2 = new EntityFilters(null, include("table"), include("column"), include("procedure"));

        assertEquals(new EntityFilters(null,
                        list(exclude("table"), include("table")),
                        list(exclude("column"), include("column")),
                        list(exclude("procedure"), include("procedure"))),
                filter1.join(filter2));
        assertEquals(new EntityFilters(null,
                        list(include("table"), exclude("table")),
                        list(include("column"), exclude("column")),
                        list(include("procedure"), exclude("procedure"))),
                filter2.join(filter1));
    }

    @Test
    public void testEquals() throws Exception {
        EntityFilters filters = new EntityFilters(new DbPath(), NULL, NULL, NULL);
        assertTrue(filters.tableFilter().equals(NULL));
        assertTrue(filters.columnFilter().equals(NULL));
        assertTrue(filters.procedureFilter().equals(NULL));
    }
}