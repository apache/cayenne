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

package org.apache.cayenne.dbsync.reverse.dbload;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.naming.NoStemStemmer;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.After;
import org.junit.Before;

import java.sql.Connection;

/**
 * All tests have been moved to corresponding loaders tests.
 */
@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DbLoaderIT extends ServerCase {

    public static final DbLoaderConfiguration CONFIG = new DbLoaderConfiguration();
    @Inject
    private ServerRuntime runtime;

    @Inject
    private DbAdapter adapter;

    @Inject
    private ServerCaseDataSourceFactory dataSourceFactory;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    private Connection connection;

    private static String msgForTypeMismatch(DbAttribute origAttr, DbAttribute newAttr) {
        return msgForTypeMismatch(origAttr.getType(), newAttr);
    }

    private static String msgForTypeMismatch(int origType, DbAttribute newAttr) {
        String nt = TypesMapping.getSqlNameByType(newAttr.getType());
        String ot = TypesMapping.getSqlNameByType(origType);
        return attrMismatch(newAttr.getName(), "expected type: <" + ot + ">, but was <" + nt + ">");
    }

    private static String attrMismatch(String attrName, String msg) {
        return "[Error loading attribute '" + attrName + "': " + msg + "]";
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

    private void assertUniqueConstraintsInRelationships(DataMap map) {
        // unfortunately JDBC metadata doesn't provide info for UNIQUE
        // constraints....
        // cant reengineer them...
        // upd. actually it's provided:
        // http://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getIndexInfo%28java.lang.String,%20java.lang.String,%20java.lang.String,%20boolean,%20boolean%29

        // find rel to TO_ONEFK1
        /*
         * Iterator it = getDbEntity(map,
         * "TO_ONEFK2").getRelationships().iterator(); DbRelationship rel =
         * (DbRelationship) it.next(); assertEquals("TO_ONEFK1",
         * rel.getTargetEntityName());
         * assertFalse("UNIQUE constraint was ignored...", rel.isToMany());
         */
    }
}
