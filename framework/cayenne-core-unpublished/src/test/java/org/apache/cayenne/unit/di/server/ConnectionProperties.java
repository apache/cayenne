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

package org.apache.cayenne.unit.di.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.commons.collections.ExtendedProperties;

/**
 * ConnectionProperties handles a set of DataSourceInfo objects using information stored
 * in $HOME/.cayenne/connection.properties. As of now this is purely a utility class. Its
 * features are not used in deployment.
 */
class ConnectionProperties {

    public static final String EMBEDDED_DATASOURCE = "internal_embedded_datasource";
    public static final String EMBEDDED_DATASOURCE_DBADAPTER = "org.apache.cayenne.dba.hsqldb.HSQLDBAdapter";
    public static final String EMBEDDED_DATASOURCE_USERNAME = "sa";
    public static final String EMBEDDED_DATASOURCE_PASSWORD = "";
    public static final String EMBEDDED_DATASOURCE_URL = "jdbc:hsqldb:mem:aname";
    public static final String EMBEDDED_DATASOURCE_JDBC_DRIVER = "org.hsqldb.jdbcDriver";

    public static final String PROPERTIES_FILE = "connection.properties";
    public static final String ADAPTER_KEY = "adapter";
    static final String ADAPTER20_KEY = "cayenne.adapter";
    public static final String USER_NAME_KEY = "jdbc.username";
    public static final String PASSWORD_KEY = "jdbc.password";
    public static final String URL_KEY = "jdbc.url";
    public static final String DRIVER_KEY = "jdbc.driver";

    public static final String ADAPTER_KEY_MAVEN = "cayenneAdapter";
    public static final String USER_NAME_KEY_MAVEN = "cayenneJdbcUsername";
    public static final String PASSWORD_KEY_MAVEN = "cayenneJdbcPassword";
    public static final String URL_KEY_MAVEN = "cayenneJdbcUrl";
    public static final String DRIVER_KEY_MAVEN = "cayenneJdbcDriver";

    private static ConnectionProperties sharedInstance;
    private static Map<String, DataSourceInfo> connectionInfos = Collections
            .synchronizedMap(new HashMap<String, DataSourceInfo>());

    static {
        sharedInstance = loadDefaultProperties();
    }

    /**
     * Returns ConnectionProperties singleton.
     */
    public static ConnectionProperties getInstance() {
        return sharedInstance;
    }

    // CayenneUserDir is defined in the Modeler, not accessible here, so hardcoding it for
    // the tests
    private static File cayenneUserDir() {
        File homeDir = new File(System.getProperty("user.home"));
        File cayenneDir = new File(homeDir, ".cayenne");
        cayenneDir.mkdirs();
        return cayenneDir;
    }

    /**
     * Loads connection properties from $HOME/.cayenne/connection.properties.
     */
    protected static ConnectionProperties loadDefaultProperties() {

        DataSourceInfo dsi = new DataSourceInfo();

        String adapter = System.getProperty(ADAPTER_KEY_MAVEN);
        String usr = System.getProperty(USER_NAME_KEY_MAVEN);
        String pass = System.getProperty(PASSWORD_KEY_MAVEN);
        String url = System.getProperty(URL_KEY_MAVEN);
        String driver = System.getProperty(DRIVER_KEY_MAVEN);

        File f = new File(cayenneUserDir(), PROPERTIES_FILE);

        try {
            if (f.exists()) {

                ConnectionProperties cp = new ConnectionProperties(
                        new ExtendedProperties(f.getAbsolutePath()));

                if (((adapter != null && !adapter.startsWith("$"))
                        || (usr != null && !usr.startsWith("$"))
                        || (pass != null && !pass.startsWith("$"))
                        || (url != null && !url.startsWith("$")) || (driver != null && !driver
                        .startsWith("$")))
                        && (System.getProperty("cayenneTestConnection") != null && !System
                                .getProperty("cayenneTestConnection")
                                .equals("null"))) {

                    DataSourceInfo dsiOld = null;
                    if (connectionInfos.get(System.getProperty("cayenneTestConnection")) != null) {
                        dsiOld = connectionInfos.get(System
                                .getProperty("cayenneTestConnection"));
                        connectionInfos.remove(System
                                .getProperty("cayenneTestConnection"));
                    }

                    if (adapter != null && !adapter.startsWith("$")) {
                        dsi.setAdapterClassName(adapter);
                    }
                    else if (dsiOld != null) {
                        dsi.setAdapterClassName(dsiOld.getAdapterClassName());
                    }
                    if (usr != null && !usr.startsWith("$")) {
                        dsi.setUserName(usr);
                    }
                    else if (dsiOld != null) {
                        dsi.setUserName(dsiOld.getUserName());
                    }
                    if (pass != null && !pass.startsWith("$")) {
                        dsi.setPassword(pass);
                    }
                    else if (dsiOld != null) {
                        dsi.setPassword(dsiOld.getPassword());
                    }
                    if (url != null && !url.startsWith("$")) {
                        dsi.setDataSourceUrl(url);
                    }
                    else if (dsiOld != null) {
                        dsi.setDataSourceUrl(dsiOld.getDataSourceUrl());
                    }
                    if (driver != null && !driver.startsWith("$")) {
                        dsi.setJdbcDriver(driver);
                    }
                    else if (dsiOld != null) {
                        dsi.setJdbcDriver(dsiOld.getJdbcDriver());
                    }
                    connectionInfos.put(System.getProperty("cayenneTestConnection"), dsi);
                }
                else {
                    return cp;
                }
            }
            else {
                if (((adapter != null && !adapter.startsWith("$"))
                        || (usr != null && !usr.startsWith("$"))
                        || (pass != null && !pass.startsWith("$"))
                        || (url != null && !url.startsWith("$")) || (driver != null && !driver
                        .startsWith("$")))
                        && (System.getProperty("cayenneTestConnection") != null && !System
                                .getProperty("cayenneTestConnection")
                                .equals("null"))) {

                    if (adapter != null && !adapter.startsWith("$")) {
                        dsi.setAdapterClassName(adapter);
                    }
                    if (usr != null && !usr.startsWith("$")) {
                        dsi.setUserName(usr);
                    }
                    if (pass != null && !pass.startsWith("$")) {
                        dsi.setPassword(pass);
                    }
                    if (url != null && !url.startsWith("$")) {
                        dsi.setDataSourceUrl(url);
                    }
                    if (driver != null && !driver.startsWith("$")) {
                        dsi.setJdbcDriver(driver);
                    }
                    connectionInfos.put(System.getProperty("cayenneTestConnection"), dsi);
                }

                // lets touch this file so that users would get a clue of what it is
                createSamplePropertiesFile(f);

            }
        }
        catch (IOException e) {
            // ignoring
        }

        return new ConnectionProperties(new ExtendedProperties());
    }

    protected static void createSamplePropertiesFile(File f) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(f));

        try {
            out.write("# Cayenne named connections configuration file.");
            out.newLine();

            out.write("#");
            out.newLine();
            out.write("# Sample named connections (named 'example1' and 'example2'): ");
            out.newLine();

            out.write("#");
            out.newLine();
            out.write("# example1."
                    + ADAPTER_KEY
                    + " = org.apache.cayenne.dba.mysql.MySQLAdapter");
            out.newLine();
            out.write("# example1." + USER_NAME_KEY + " = some_user");
            out.newLine();
            out.write("# example1." + PASSWORD_KEY + " = some_passwd");
            out.newLine();
            out.write("# example1." + URL_KEY + " = jdbc:mysql://localhost/cayenne");
            out.newLine();
            out.write("# example1." + DRIVER_KEY + " = com.mysql.jdbc.Driver");
            out.newLine();

            // example 2
            out.write("#");
            out.newLine();
            out.write("# example2."
                    + ADAPTER_KEY
                    + " = org.apache.cayenne.dba.mysql.MySQLAdapter");
            out.newLine();
            out.write("# example2." + USER_NAME_KEY + " = some_user");
            out.newLine();
            out.write("# example2." + PASSWORD_KEY + " = some_passwd");
            out.newLine();
            out.write("# example2." + URL_KEY + " = jdbc:mysql://localhost/cayenne");
            out.newLine();
            out.write("# example2." + DRIVER_KEY + " = com.mysql.jdbc.Driver");
            out.newLine();
        }
        finally {
            out.close();
        }
    }

    /**
     * Constructor for ConnectionProperties.
     */
    public ConnectionProperties(ExtendedProperties props) {
        for (final String name : extractNames(props)) {
            DataSourceInfo dsi = buildDataSourceInfo(props.subset(name));
            connectionInfos.put(name, dsi);
        }
    }

    /**
     * Returns DataSourceInfo object for a symbolic name. If name does not match an
     * existing object, returns null.
     */
    public DataSourceInfo getConnectionInfo(String name) {

        if (EMBEDDED_DATASOURCE.equals(name)) {
            // Create embedded data source instead
            DataSourceInfo connectionInfo = new DataSourceInfo();
            connectionInfo.setAdapterClassName(EMBEDDED_DATASOURCE_DBADAPTER);
            connectionInfo.setUserName(EMBEDDED_DATASOURCE_USERNAME);
            connectionInfo.setPassword(EMBEDDED_DATASOURCE_PASSWORD);
            connectionInfo.setDataSourceUrl(EMBEDDED_DATASOURCE_URL);
            connectionInfo.setJdbcDriver(EMBEDDED_DATASOURCE_JDBC_DRIVER);
            return connectionInfo;
        }

        synchronized (connectionInfos) {
            return connectionInfos.get(name);
        }
    }

    /**
     * Creates a DataSourceInfo object from a set of properties.
     */
    public DataSourceInfo buildDataSourceInfo(ExtendedProperties props) {
        DataSourceInfo dsi = new DataSourceInfo();

        String adapter = props.getString(ADAPTER_KEY);

        // try legacy adapter key
        if (adapter == null) {
            adapter = props.getString(ADAPTER20_KEY);
        }

        dsi.setAdapterClassName(adapter);
        dsi.setUserName(props.getString(USER_NAME_KEY));
        dsi.setPassword(props.getString(PASSWORD_KEY));
        dsi.setDataSourceUrl(props.getString(URL_KEY));
        dsi.setJdbcDriver(props.getString(DRIVER_KEY));

        return dsi;
    }

    /**
     * Returns a list of connection names configured in the properties object.
     */
    public List<String> extractNames(ExtendedProperties props) {
        Iterator<?> it = props.getKeys();
        List<String> list = new ArrayList<String>();

        while (it.hasNext()) {
            String key = (String) it.next();

            int dotInd = key.indexOf('.');
            if (dotInd <= 0 || dotInd >= key.length()) {
                continue;
            }

            String name = key.substring(0, dotInd);
            if (!list.contains(name)) {
                list.add(name);
            }
        }

        return list;
    }
}
