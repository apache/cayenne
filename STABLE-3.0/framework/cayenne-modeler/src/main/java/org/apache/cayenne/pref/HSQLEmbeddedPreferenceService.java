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

package org.apache.cayenne.pref;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.ConnectionLogger;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.DataSourceFactory;
import org.apache.cayenne.conf.DefaultConfiguration;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.modeler.util.Version;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.Util;

/**
 * An implementation of preference service that stores the data using embedded HSQL DB
 * database with Cayenne.
 * 
 */
public class HSQLEmbeddedPreferenceService extends CayennePreferenceService {

    protected File dbDirectory;
    protected String baseName;
    protected String masterBaseName;
    protected String cayenneConfigPackage;

    protected Configuration configuration;

    /**
     * Creates a new PreferenceService that stores preferences using Cayenne and embedded
     * HSQLDB engine.
     * 
     * @param dbLocation path to an HSQL db.
     * @param cayenneConfigPackage a Java package that holds cayenne.xml for preferences
     *            access (can be null)
     * @param defaultDomain root domain name for this service.
     */
    public HSQLEmbeddedPreferenceService(String dbLocation, String cayenneConfigPackage,
            String defaultDomain) {
        super(defaultDomain);
        if (dbLocation == null) {
            throw new PreferenceException("Null DB location.");
        }

        File file = new File(dbLocation);

        this.dbDirectory = file.getParentFile();
        this.masterBaseName = file.getName();
        this.cayenneConfigPackage = cayenneConfigPackage;
    }

    /**
     * If true, this service updates a secondary HSQL instance that may need
     * synchronization with master.
     */
    public boolean isSecondaryDB() {
        return !Util.nullSafeEquals(masterBaseName, baseName);
    }

    public File getMasterLock() {
        return new File(dbDirectory, masterBaseName + ".lck");
    }

    /**
     * Creates a separate Cayenne stack used to work with preferences database only, so
     * that any other use of Cayenne in the app is not affected.
     */
    public void startService() {
        // use custom DataSourceFactory to prepare the DB...
        final HSQLDataSourceFactory dataSourceFactory = new HSQLDataSourceFactory();

        DefaultConfiguration configuration = new DefaultConfiguration() {

            @Override
            public DataSourceFactory getDataSourceFactory(String userFactoryName) {
                return dataSourceFactory;
            }
        };

        if (cayenneConfigPackage != null) {
            configuration.addClassPath(cayenneConfigPackage);
        }

        try {
            configuration.initialize();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error connecting to preference DB.", ex);
        }

        configuration.didInitialize();

        this.configuration = configuration;
        this.dataContext = configuration.getDomain().createDataContext();

        // create DB if it does not exist...
        if (dataSourceFactory.needSchemaUpdate && !upgradeDB()) {
            initSchema();
        }

        // bootstrap our own preferences...
        initPreferences();

        // start save timer...
        startTimer();
    }

    public void stopService() {

        if (saveTimer != null) {
            saveTimer.cancel();
        }

        if (dataContext != null) {

            // flush changes...
            savePreferences();

            // shutdown HSQL
            dataContext
                    .performNonSelectingQuery(new SQLTemplate(Domain.class, "SHUTDOWN"));
        }

        // shutdown Cayenne
        if (configuration != null) {
            configuration.shutdown();
        }

        // attempt to sync primary DB...
        if (isSecondaryDB()) {
            File lock = getMasterLock();
            if (!lock.exists()) {

                // TODO: according to JavaDoc this is not reliable enough...
                // Investigate HSQL API for a better solution.
                try {
                    if (lock.createNewFile()) {
                        try {
                            moveDB(baseName, masterBaseName);
                        }
                        finally {
                            lock.delete();
                        }
                    }
                }
                catch (Throwable th) {
                    throw new PreferenceException(
                            "Error shutting down database. Preferences may be in invalid state.");
                }
            }
        }
    }

    /**
     * Copies database with older version.
     */
    boolean upgradeDB() {
        String versionName = dbDirectory.getName();
        File prefsDir = dbDirectory.getParentFile();

        String[] prefs = prefsDir.list();

        if (prefs == null || prefs.length == 0) {
            return false;
        }

        // find older version
        Version currentVersion = new Version(versionName);
        Version previousVersion = new Version("0");
        File lastDir = null;
        for (String pref : prefs) {
            File dir = new File(prefsDir, pref);
            if (dir.isDirectory() && new File(dir, baseName + ".properties").isFile()) {

                // check that there are DB files

                Version v;
                try {
                    v = new Version(pref);
                }
                catch (NumberFormatException nfex) {
                    // ignore... not a version dir...
                    continue;
                }

                if (v.compareTo(currentVersion) < 0 && v.compareTo(previousVersion) > 0) {
                    previousVersion = v;
                    lastDir = dir;
                }
            }
        }

        if (lastDir != null) {
            copyDB(lastDir, baseName, baseName);
            return true;
        }

        return false;
    }

    /**
     * Copies one database to another. Caller must provide HSQLDB locks on target for this
     * to work reliably.
     */
    void moveDB(String masterBaseName, String targetBaseName) {

        File[] filesToMove = dbDirectory.listFiles(new HSQLDBFileFilter(masterBaseName));
        if (filesToMove != null) {
            for (File fileToMove : filesToMove) {
                String ext = Util.extractFileExtension(fileToMove.getName());

                File target = new File(dbDirectory, targetBaseName + "." + ext);
                if (fileToMove.exists()) {
                    fileToMove.renameTo(target);
                } else {
                    target.delete();
                }
            }
        }
    }

    /**
     * Copies one database to another. Caller must provide HSQLDB locks for this to work
     * reliably.
     */
    void copyDB(String masterBaseName, String targetBaseName) {
        copyDB(dbDirectory, masterBaseName, targetBaseName);
    }

    void copyDB(File sourceDirectory, String masterBaseName, String targetBaseName) {

        File[] filesToCopy = sourceDirectory.listFiles(new HSQLDBFileFilter(
                masterBaseName));
        if (filesToCopy != null) {
            for (File fileToCopy : filesToCopy) {
                String ext = Util.extractFileExtension(fileToCopy.getName());

                File target = new File(dbDirectory, targetBaseName + "." + ext);
                if (fileToCopy.exists()) {
                    Util.copy(fileToCopy, target);
                } else {
                    target.delete();
                }
            }
        }
    }

    // filers HSQLDB files
    final class HSQLDBFileFilter implements FileFilter {

        String baseName;

        HSQLDBFileFilter(String baseName) {
            this.baseName = baseName;
        }

        public boolean accept(File pathname) {
            if (!pathname.isFile()) {
                return false;
            }

            String fullName = pathname.getName();
            if (fullName.endsWith(".lck")) {
                return false;
            }

            int dot = fullName.indexOf('.');
            String name = (dot > 0) ? fullName.substring(0, dot) : fullName;

            return baseName.equals(name);
        }
    }

    // addresses various issues with embedded database...
    final class HSQLDataSourceFactory implements DataSourceFactory {

        boolean needSchemaUpdate;
        String url;

        void prepareDB() throws IOException {

            // try master DB
            if (checkMainDB(masterBaseName)) {
                return;
            }

            // try last active DB
            if (baseName != null && checkMainDB(baseName)) {
                return;
            }

            // file locked... need to switch to a secondary DB
            // arbitrary big but finite number of attempts...
            for (int i = 1; i < 200; i++) {
                String name = masterBaseName + i;
                File lock = new File(dbDirectory, name + ".lck");
                if (!lock.exists()) {

                    // TODO: according to JavaDoc this is not reliable enough...
                    // Investigate HSQL API for a better solution.
                    if (!lock.createNewFile()) {
                        continue;
                    }

                    try {
                        copyDB(masterBaseName, name);
                    }
                    finally {
                        lock.delete();
                    }

                    needSchemaUpdate = false;
                    url = "jdbc:hsqldb:file:"
                            + Util.substBackslashes(new File(dbDirectory, name)
                                    .getAbsolutePath());
                    baseName = name;
                    return;
                }
            }

            throw new IOException("Can't create preferences DB");
        }

        boolean checkMainDB(String sessionBaseName) {
            File dbFile = new File(dbDirectory, sessionBaseName + ".properties");

            // no db file exists
            if (!dbFile.exists()) {
                needSchemaUpdate = true;
                url = "jdbc:hsqldb:file:"
                        + Util.substBackslashes(new File(dbDirectory, sessionBaseName)
                                .getAbsolutePath());
                baseName = sessionBaseName;
                return true;
            }

            // no lock exists... continue...
            File lockFile = new File(dbDirectory, sessionBaseName + ".lck");

            // on Windows try deleting the lock... OS locking should prevent
            // this operation if another process is running...
            if (lockFile.exists()
                    && System.getProperty("os.name").toLowerCase().contains("windows")) {
                lockFile.delete();
            }

            if (!lockFile.exists()) {
                needSchemaUpdate = false;
                url = "jdbc:hsqldb:file:"
                        + Util.substBackslashes(new File(dbDirectory, sessionBaseName)
                                .getAbsolutePath());
                baseName = sessionBaseName;
                return true;
            }

            return false;
        }

        public DataSource getDataSource(String location) throws Exception {
            try {
                prepareDB();

                PoolManager pm = new PoolManager(
                        org.hsqldb.jdbcDriver.class.getName(),
                        url,
                        1,
                        1,
                        "sa",
                        null,
                        new ConnectionLogger());

                return pm;
            }
            catch (Throwable th) {
                QueryLogger.logConnectFailure(th);
                throw new PreferenceException("Error connecting to DB", th);
            }
        }

        public void initializeWithParentConfiguration(Configuration conf) {
        }
    }

}
