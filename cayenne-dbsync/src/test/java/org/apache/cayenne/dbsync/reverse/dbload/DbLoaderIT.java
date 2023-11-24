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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.RuntimeCaseDataSourceFactory;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * All tests have been moved to corresponding loaders tests.
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DbLoaderIT extends RuntimeCase {

    private static final DbLoaderConfiguration CONFIG = new DbLoaderConfiguration();

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private DbAdapter adapter;

    @Inject
    private RuntimeCaseDataSourceFactory dataSourceFactory;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    private Connection connection;


    /**
     * Test that parts of loader are in place
     */
    @Test
    public void testSimpleLoad() throws Exception {
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

    @Before
    public void before() throws Exception {
        this.connection = dataSourceFactory.getSharedDataSource().getConnection();
    }

    private DbLoader createDbLoader(boolean meaningfulPK, boolean meaningfulFK) {
        return new DbLoader(adapter, connection, CONFIG, null, new DefaultObjectNameGenerator(NoStemStemmer.getInstance()));
    }

    @After
    public void after() throws Exception {
        connection.close();
    }
}
