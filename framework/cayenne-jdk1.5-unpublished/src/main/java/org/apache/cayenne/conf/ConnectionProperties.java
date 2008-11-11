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

package org.apache.cayenne.conf;

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
import org.apache.cayenne.project.CayenneUserDir;
import org.apache.commons.collections.ExtendedProperties;

/**
 * ConnectionProperties handles a set of DataSourceInfo objects using information stored
 * in $HOME/.cayenne/connection.properties. As of now this is purely a utility class. Its
 * features are not used in deployment.
 * 
 */
public class ConnectionProperties {

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

    protected static ConnectionProperties sharedInstance;
    protected Map<String, DataSourceInfo> connectionInfos = Collections.synchronizedMap(new HashMap<String, DataSourceInfo>());

    static {
        sharedInstance = loadDefaultProperties();
    }

    /**
     * Returns ConnectionProperties singleton.
     */
    public static ConnectionProperties getInstance() {
        return sharedInstance;
    }

    /**
     * Loads connection properties from $HOME/.cayenne/connection.properties.
     */
    protected static ConnectionProperties loadDefaultProperties() {
        File f = CayenneUserDir.getInstance().resolveFile(PROPERTIES_FILE);

        try {
            if (f.exists()) {
                return new ConnectionProperties(new ExtendedProperties(f
                        .getAbsolutePath()));
            }
            else {
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
            out.write("# example1." + URL_KEY + " = jdbc:mysql://noise/cayenne");
            out.newLine();
            out.write("# example1." + DRIVER_KEY + " = org.gjt.mm.mysql.Driver");
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
            out.write("# example2." + URL_KEY + " = jdbc:mysql://noise/cayenne");
            out.newLine();
            out.write("# example2." + DRIVER_KEY + " = org.gjt.mm.mysql.Driver");
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
    protected DataSourceInfo buildDataSourceInfo(ExtendedProperties props) {
        DataSourceInfo dsi = new DataSourceInfo();

        String adapter = props.getString(ADAPTER_KEY);
        
        // try legacy adapter key
        if(adapter == null) {
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
    protected List<String> extractNames(ExtendedProperties props) {
        Iterator it = props.getKeys();
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
