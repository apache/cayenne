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

package org.apache.cayenne.dbsync.reverse.dbimport;

import org.apache.cayenne.dbsync.filter.NameFilter;
import org.junit.Test;

import static org.junit.Assert.*;

public class DbImportConfigurationTest {

    @Test
    public void testCreateMeaningfulPKFilter_NullExcludesAll() {
        DbImportConfiguration config = new DbImportConfiguration();
        config.setMeaningfulPkTables(null);
        NameFilter filter = config.createMeaningfulPKFilter();

        assertFalse(filter.isIncluded("ARTIST"));
        assertFalse(filter.isIncluded("PAINTING"));
        assertFalse(filter.isIncluded("ANY_TABLE"));
    }

    @Test
    public void testCreateMeaningfulPKFilter_EmptyStringExcludesAll() {
        DbImportConfiguration config = new DbImportConfiguration();
        config.setMeaningfulPkTables("");
        NameFilter filter = config.createMeaningfulPKFilter();

        assertFalse(filter.isIncluded("ARTIST"));
        assertFalse(filter.isIncluded("ANY_TABLE"));
    }

    @Test
    public void testCreateMeaningfulPKFilter_WhitespaceStringExcludesAll() {
        DbImportConfiguration config = new DbImportConfiguration();
        config.setMeaningfulPkTables("   ");
        NameFilter filter = config.createMeaningfulPKFilter();

        assertFalse(filter.isIncluded("ARTIST"));
        assertFalse(filter.isIncluded("ANY_TABLE"));
    }

    @Test
    public void testCreateMeaningfulPKFilter_CommaSeparatedPatterns() {
        DbImportConfiguration config = new DbImportConfiguration();
        config.setMeaningfulPkTables("^ART.*$,,^T1$"); // Note double comma
        NameFilter filter = config.createMeaningfulPKFilter();

        assertTrue(filter.isIncluded("ARTIST"));
        assertTrue(filter.isIncluded("T1"));

        assertFalse("Empty tokens should not match everything", filter.isIncluded("PAINTING"));
    }
}
