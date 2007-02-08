/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.pref;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.pref.DomainPreference;
import org.objectstyle.cayenne.pref.HSQLEmbeddedPreferenceService;
import org.objectstyle.cayenne.project.CayenneUserDir;
import org.objectstyle.cayenne.query.SelectQuery;

/**
 * A DataSourceFactory that loads DataSources from CayenneModeler preferences. Allows
 * integrating Cayenne runtime with preferences engine. Currently JNDIDataSourceFactory
 * uses this factory as a failover loading mechanism, instantiating it via reflection.
 * 
 * @since 1.2
 * @author Andrus Adamchik
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
     * @deprecated since 1.2
     */
    public DataSource getDataSource(String location, Level logLevel) throws Exception {
        return getDataSource(location);
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
