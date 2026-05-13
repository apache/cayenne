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
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

public class DbGeneratorIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DbAdapter adapter;
    private CayenneRuntime runtime;
    private DbGenerator generator;
    
    @BeforeEach
    public void setUp() throws Exception {
        adapter = env.dbAdapter();
        runtime = env.runtime();
        generator = new DbGenerator(adapter, runtime
                .getDataDomain()
                .getDataMap("testmap"), NoopJdbcEventLogger.getInstance());
    }

    @Test
    public void adapter() throws Exception {
        assertSame(adapter, generator.getAdapter());
    }

    @Test
    public void pkFilteringLogic() throws Exception {
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
    public void createPkSupport() throws Exception {
        assertTrue(generator.shouldCreatePKSupport());
        generator.setShouldCreatePKSupport(false);
        assertFalse(generator.shouldCreatePKSupport());

    }

    @Test
    public void shouldCreateTables() throws Exception {
        assertTrue(generator.shouldCreateTables());
        generator.setShouldCreateTables(false);
        assertFalse(generator.shouldCreateTables());
    }

    @Test
    public void dropPkSupport() throws Exception {

        assertFalse(generator.shouldDropPKSupport());
        generator.setShouldDropPKSupport(true);
        assertTrue(generator.shouldDropPKSupport());
    }

    @Test
    public void shouldDropTables() throws Exception {
        assertFalse(generator.shouldDropTables());
        generator.setShouldDropTables(true);
        assertTrue(generator.shouldDropTables());
    }
}
