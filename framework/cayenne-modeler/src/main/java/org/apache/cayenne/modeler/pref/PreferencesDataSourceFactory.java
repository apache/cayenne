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

package org.apache.cayenne.modeler.pref;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.DataSourceFactory;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.pref.DomainPreference;
import org.apache.cayenne.pref.HSQLEmbeddedPreferenceService;
import org.apache.cayenne.project.CayenneUserDir;
import org.apache.cayenne.query.SelectQuery;

/**
 * A DataSourceFactory that loads DataSources from CayenneModeler preferences. Allows
 * integrating Cayenne runtime with preferences engine. Currently JNDIDataSourceFactory
 * uses this factory as a failover loading mechanism, instantiating it via reflection.
 * 
 * @since 1.2
 */
public class PreferencesDataSourceFactory implements DataSourceFactory {

    protected int minPoolSize;
    protected int maxPoolSize;

    public PreferencesDataSourceFactory() {
        // init pool size default
        this(1, 5);
    }

    public PreferencesDataSourceFactory(int minPoolSize, int maxPoolSize) {
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void initializeWithParentConfiguration(Configuration configuaration) {
        // noop
    }

    /**
     * Attempts to read named DataSource info from preferences and create a DataSource out
     * of it. If no matching DataSource is found, throws CayenneRuntimeException.
     */
    public DataSource getDataSource(final String location) throws Exception {
        if (location == null) {
            throw new NullPointerException("Null location");
        }

        // figure out preferences DB location...

        // TODO: once prefs package becomes a part of Cayenne, remove dependency on
        // Application class... also this code is redundant with what Application does in
        // constructor
        String configuredName = System.getProperty(Application.APPLICATION_NAME_PROPERTY);
        String name = (configuredName != null)
                ? configuredName
                : Application.DEFAULT_APPLICATION_NAME;

        String subdir = System.getProperty(Application.PREFERENCES_VERSION_PROPERTY);

        if (subdir == null) {
            subdir = Application.PREFERENCES_VERSION;
        }

        File dbDir = new File(CayenneUserDir.getInstance().resolveFile(
                Application.PREFERENCES_DB_SUBDIRECTORY), subdir);

        // check if preferences even exist...
        if (!dbDir.isDirectory()) {
            throw new CayenneRuntimeException(
                    "No preferences database directory exists: " + dbDir);
        }
        else if (!new File(dbDir, "db.properties").exists()) {
            throw new CayenneRuntimeException(
                    "No preferences database exists in directory " + dbDir);
        }

        String preferencesDB = new File(dbDir, "db").getAbsolutePath();

        // create custom preferences service...
        HSQLEmbeddedPreferenceService service = new HSQLEmbeddedPreferenceService(
                preferencesDB,
                Application.PREFERENCES_MAP_PACKAGE,
                name) {

            protected void startTimer() {
                // noop: disable commit timer
            }

            protected void initPreferences() {
                // noop: disable commit timer
            }

            protected void initSchema() {
                // throw - no schema means no DataSource data
                throw new CayenneRuntimeException("No preferences matching location: "
                        + location);
            }
        };

        try {
            service.startService();
            return toDataSource(service.getDataContext(), location);
        }
        finally {
            // make sure we cleanup after ourselves...
            try {
                service.stopService();
            }
            catch (Throwable th) {
                // ignore..
            }
        }
    }

    DataSource toDataSource(DataContext context, String location) throws Exception {

        // grep through all domains ... maybe a bit naive...
        Expression locationFilter = ExpressionFactory.matchExp(
                DomainPreference.KEY_PROPERTY,
                location);
        List preferences = context.performQuery(new SelectQuery(
                DomainPreference.class,
                locationFilter));

        if (preferences.isEmpty()) {
            throw new CayenneRuntimeException("No preferences matching location: "
                    + location);
        }

        Collection ids = new ArrayList(preferences.size());
        Iterator it = preferences.iterator();
        while (it.hasNext()) {
            DomainPreference pref = (DomainPreference) it.next();
            ids.add(DataObjectUtils.pkForObject(pref));
        }

        Expression qualifier = Expression.fromString("db:"
                + DBConnectionInfo.ID_PK_COLUMN
                + " in $ids");
        Map params = Collections.singletonMap("ids", ids);
        SelectQuery query = new SelectQuery(DBConnectionInfo.class, qualifier
                .expWithParameters(params));

        // narrow down the results to just DBConnectionInfo
        List connectionData = context.performQuery(query);

        if (connectionData.isEmpty()) {
            throw new CayenneRuntimeException("No preferences matching location: "
                    + location);
        }

        if (connectionData.size() > 1) {
            throw new CayenneRuntimeException(
                    "More than one preference matched location: " + location);
        }

        DBConnectionInfo info = (DBConnectionInfo) connectionData.get(0);

        if (info.getJdbcDriver() == null) {
            throw new CayenneRuntimeException(
                    "Incomplete connection info: no JDBC driver set.");
        }

        if (info.getUrl() == null) {
            throw new SQLException("Incomplete connection info: no DB URL set.");
        }

        // use default values for connection pool ... no info is available from
        // preferences...
        return new PoolManager(info.getJdbcDriver(), info.getUrl(), 1, 5, info
                .getUserName(), info.getPassword());
    }
}
