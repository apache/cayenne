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

package org.apache.cayenne.dbsync.reverse.dbload;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.NoStemStemmer;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.dba.UnitDbAdapter;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.TestDataSources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * All tests have been moved to corresponding loaders tests.
 */
public class DbLoaderIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private static final DbLoaderConfiguration CONFIG = new DbLoaderConfiguration();

    private CayenneRuntime runtime;
    private DbAdapter adapter;
    private TestDataSources dataSourceFactory;
    private UnitDbAdapter accessStackAdapter;

    private Connection connection;


    @Test
    public void loadingOrder() throws Exception {
        ReverseEngineering engineering = new ReverseEngineering();
        IncludeTable artistTableWithExclusion = new IncludeTable("ARTIST");
        artistTableWithExclusion.addExcludeColumn(new ExcludeColumn("DATE_OF_BIRTH"));
        engineering.addIncludeTable(artistTableWithExclusion);
        engineering.addIncludeTable(new IncludeTable("ARTIST"));

        IncludeTable paintingTableWithExclusion = new IncludeTable("PAINTING");
        paintingTableWithExclusion.addExcludeColumn(new ExcludeColumn("PAINTING_DESCRIPTION"));
        engineering.addIncludeTable(new IncludeTable("PAINTING"));
        engineering.addIncludeTable(paintingTableWithExclusion);

        FiltersConfigBuilder configBuilder = new FiltersConfigBuilder(engineering);
        FiltersConfig filtersConfig = configBuilder.build();

        DbLoaderConfiguration dbLoaderConfiguration = new DbLoaderConfiguration();
        dbLoaderConfiguration.setFiltersConfig(filtersConfig);

        DbLoader loader = createDbLoader(dbLoaderConfiguration);
        DataMap loaded = loader.load();
        assertNotNull(loaded);

        DbEntity artist = loaded.getDbEntity("ARTIST");
        DbEntity painting = loaded.getDbEntity("PAINTING");
        assertNotNull(artist);
        assertNotNull(painting);
        assertNull(getDbAttribute(artist,"DATE_OF_BIRTH"));
        assertNotNull(getDbAttribute(painting,"PAINTING_DESCRIPTION"));
    }


    /**
     * Test that parts of loader are in place
     */
    @Test
    public void simpleLoad() throws Exception {
        DbLoader loader = createDbLoader(true, true);
        DataMap loaded = loader.load();
        assertNotNull(loaded);
        assertEquals("__generated_by_dbloader__", loaded.getName());

        // DbEntity
        DbEntity artist = loaded.getDbEntity("ARTIST");
        assertNotNull(artist);

        // DbAttribute
        DbAttribute id = getDbAttribute(artist, "ARTIST_ID");
        assertNotNull(id);
        assertTrue(id.isMandatory());
        assertTrue(id.isPrimaryKey());

        DbAttribute name = getDbAttribute(artist,"ARTIST_NAME");
        assertNotNull(name);
        assertTrue(name.isMandatory());

        DbAttribute date = getDbAttribute(artist,"DATE_OF_BIRTH");
        assertNotNull(date);
        assertFalse(date.isMandatory());

        // DbRelationship
        assertEquals(5, artist.getRelationships().size());

        DbRelationship exhibits = artist.getRelationship("artistExhibits");
        assertNotNull(exhibits);
        assertEquals("ARTIST_EXHIBIT", exhibits.getTargetEntityName().toUpperCase());
        DbEntity target = exhibits.getTargetEntity();
        assertNotNull(target);
    }

    private DbAttribute getDbAttribute(DbEntity ent, String name) {
        DbAttribute da = ent.getAttribute(name);
        // sometimes table names get converted to lowercase
        if (da == null) {
            da = ent.getAttribute(name.toLowerCase());
        }

        return da;
    }

    @BeforeEach
    public void before() throws Exception {
        runtime = env.runtime();
        adapter = env.dataNode().getAdapter();
        dataSourceFactory = env.dataSourceFactory();
        accessStackAdapter = env.unitDbAdapter();
        this.connection = dataSourceFactory.getSharedDataSource().getConnection();
    }

    private DbLoader createDbLoader(boolean meaningfulPK, boolean meaningfulFK) {
        return new DbLoader(adapter, connection, CONFIG, null, new DefaultObjectNameGenerator(NoStemStemmer.getInstance()));
    }

    private DbLoader createDbLoader(DbLoaderConfiguration configuration) {
        return new DbLoader(adapter, connection, configuration, null, new DefaultObjectNameGenerator(NoStemStemmer.getInstance()));
    }

    @AfterEach
    public void after() throws Exception {
        connection.close();
    }
}
