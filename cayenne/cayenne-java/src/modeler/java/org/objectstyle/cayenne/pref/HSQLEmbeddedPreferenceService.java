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
package org.objectstyle.cayenne.pref;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.ConnectionLogger;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DataSourceFactory;
import org.objectstyle.cayenne.conf.DefaultConfiguration;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.modeler.util.Version;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.util.Util;

/**
 * An implementation of preference service that stores the data using embedded HSQL DB
 * database with Cayenne.
 * 
 * @author Andrei Adamchik
 */
public class HSQLEmbeddedPreferenceService extends CayennePreferenceService {

    protected File dbDirectory;
    protected String baseName;
    protected String masterBaseName;
    protected String cayenneConfigPackage;

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
        HSQLDataSourceFactory dataSourceFactory = new HSQLDataSourceFactory();

        DefaultConfiguration config = new DefaultConfiguration();
        config.setDataSourceFactory(dataSourceFactory);

        if (cayenneConfigPackage != null) {
            config.addClassPath(cayenneConfigPackage);
        }

        try {
            config.initialize();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error connecting to preference DB.", ex);
        }

        config.didInitialize();
        dataContext = config.getDomain().createDataContext();

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

            // shutdown Cayenne
            dataContext.getParentDataDomain().shutdown();
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
        for (int i = 0; i < prefs.length; i++) {
            File dir = new File(prefsDir, prefs[i]);
            if (dir.isDirectory() && new File(dir, baseName + ".properties").isFile()) {

                // check that there are DB files

                Version v;
                try {
                    v = new Version(prefs[i]);
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
            for (int i = 0; i < filesToMove.length; i++) {
                String ext = Util.extractFileExtension(filesToMove[i].getName());

                File target = new File(dbDirectory, targetBaseName + "." + ext);
                if (filesToMove[i].exists()) {
                    filesToMove[i].renameTo(target);
                }
                else {
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
            for (int i = 0; i < filesToCopy.length; i++) {
                String ext = Util.extractFileExtension(filesToCopy[i].getName());

                File target = new File(dbDirectory, targetBaseName + "." + ext);
                if (filesToCopy[i].exists()) {
                    Util.copy(filesToCopy[i], target);
                }
                else {
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
                    && System.getProperty("os.name").toLowerCase().indexOf("windows") >= 0) {
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

        /**
         * @deprecated since 1.2
         */
        public DataSource getDataSource(String location, Level logLevel) throws Exception {
            return getDataSource(location);
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