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

package org.apache.cayenne.access;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DbGeneratorIT extends RuntimeCase {

    @Inject
    private DbAdapter adapter;

    @Inject
    private CayenneRuntime runtime;
    
    @Inject
    private JdbcEventLogger logger;

    private DbGenerator generator;

    @Before
    public void setUp() throws Exception {
        generator = new DbGenerator(adapter, runtime
                .getDataDomain()
                .getDataMap("testmap"), logger);
    }

    @Test
    public void testAdapter() throws Exception {
        assertSame(adapter, generator.getAdapter());
    }

    @Test
    public void testPkFilteringLogic() throws Exception {
        DataMap map = runtime.getDataDomain().getDataMap("testmap");
        DbEntity artistExhibit = map.getDbEntity("ARTIST_EXHIBIT");
        DbEntity exhibit = map.getDbEntity("EXHIBIT");

        // sanity check
        assertNotNull(artistExhibit);
        assertNotNull(exhibit);
        assertNotNull(generator.dbEntitiesRequiringAutoPK);

        // real test
        assertTrue(generator.dbEntitiesRequiringAutoPK.contains(exhibit));
        assertFalse(generator.dbEntitiesRequiringAutoPK.contains(artistExhibit));
    }

    @Test
    public void testCreatePkSupport() throws Exception {
        assertTrue(generator.shouldCreatePKSupport());
        generator.setShouldCreatePKSupport(false);
        assertFalse(generator.shouldCreatePKSupport());

    }

    @Test
    public void testShouldCreateTables() throws Exception {
        assertTrue(generator.shouldCreateTables());
        generator.setShouldCreateTables(false);
        assertFalse(generator.shouldCreateTables());
    }

    @Test
    public void testDropPkSupport() throws Exception {

        assertFalse(generator.shouldDropPKSupport());
        generator.setShouldDropPKSupport(true);
        assertTrue(generator.shouldDropPKSupport());
    }

    @Test
    public void testShouldDropTables() throws Exception {
        assertFalse(generator.shouldDropTables());
        generator.setShouldDropTables(true);
        assertTrue(generator.shouldDropTables());
    }
}
