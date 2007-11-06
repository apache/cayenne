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

package org.apache.cayenne.tools;

import java.io.File;
import java.sql.Driver;

import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.conn.DriverDataSource;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.util.Util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.xml.sax.InputSource;

/**
 * An Ant Task that is a frontend to Cayenne DbGenerator allowing schema generation from
 * DataMap using Ant.
 * 
 * @author Kevin Menard, Andrus Adamchik
 * @since 1.2
 */
// TODO: support classpath attribute for loading the driver
public class DbGeneratorTask extends CayenneTask {

    protected DbAdapter adapter;
    protected File map;
    protected String driver;
    protected String url;
    protected String userName;
    protected String password;

    // DbGenerator options... setup defaults similar to DbGenerator itself:
    // all DROP set to false, all CREATE - to true
    protected boolean dropTables;
    protected boolean dropPK;
    protected boolean createTables = true;
    protected boolean createPK = true;
    protected boolean createFK = true;

    public void execute() {

        // prepare defaults
        if (adapter == null) {
            adapter = new JdbcAdapter();
        }

        log("connection settings - [driver: "
                + driver
                + ", url: "
                + url
                + ", username: "
                + userName
                + "]", Project.MSG_VERBOSE);

        log("generator options - [dropTables: "
                + dropTables
                + ", dropPK: "
                + dropPK
                + ", createTables: "
                + createTables
                + ", createPK: "
                + createPK
                + ", createFK: "
                + createFK
                + "]", Project.MSG_VERBOSE);

        validateAttributes();

        try {

            // Load the data map and run the db generator.
            DataMap dataMap = loadDataMap();
            DbGenerator generator = new DbGenerator(adapter, dataMap);
            generator.setShouldCreateFKConstraints(createFK);
            generator.setShouldCreatePKSupport(createPK);
            generator.setShouldCreateTables(createTables);
            generator.setShouldDropPKSupport(dropPK);
            generator.setShouldDropTables(dropTables);

            // load driver taking custom CLASSPATH into account...
            DriverDataSource dataSource = new DriverDataSource((Driver) Class.forName(
                    driver).newInstance(), url, userName, password);

            generator.runGenerator(dataSource);
        }
        catch (Exception ex) {
            Throwable th = Util.unwindException(ex);

            String message = "Error generating database";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            super.log(message);
            throw new BuildException(message, th);
        }
    }

    /**
     * Validates atttributes that are not related to internal DefaultClassGenerator.
     * Throws BuildException if attributes are invalid.
     */
    protected void validateAttributes() throws BuildException {
        StringBuffer error = new StringBuffer("");

        if (map == null) {
            error.append("The 'map' attribute must be set.\n");
        }

        if (driver == null) {
            error.append("The 'driver' attribute must be set.\n");
        }

        if (url == null) {
            error.append("The 'adapter' attribute must be set.\n");
        }

        if (error.length() > 0) {
            throw new BuildException(error.toString());
        }
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap loadDataMap() throws Exception {
        InputSource in = new InputSource(map.getCanonicalPath());
        return new MapLoader().loadDataMap(in);
    }

    public void setCreateFK(boolean createFK) {
        this.createFK = createFK;
    }

    public void setCreatePK(boolean createPK) {
        this.createPK = createPK;
    }

    public void setCreateTables(boolean createTables) {
        this.createTables = createTables;
    }

    public void setDropPK(boolean dropPK) {
        this.dropPK = dropPK;
    }

    public void setDropTables(boolean dropTables) {
        this.dropTables = dropTables;
    }

    /**
     * Sets the map.
     * 
     * @param map The map to set
     */
    public void setMap(File map) {
        this.map = map;
    }

    /**
     * Sets the db adapter.
     * 
     * @param adapter The db adapter to set.
     */
    public void setAdapter(String adapter) {

        if (adapter != null) {
            // Try to create an instance of the DB adapter.
            try {
                Class c = Class.forName(adapter);
                this.adapter = (DbAdapter) c.newInstance();
            }
            catch (Exception e) {
                throw new BuildException("Can't load DbAdapter: " + adapter);
            }
        }
    }

    /**
     * Sets the JDBC driver used to connect to the database server.
     * 
     * @param driver The driver to set.
     */
    public void setDriver(String driver) {
        this.driver = driver;
    }

    /**
     * Sets the JDBC URL used to connect to the database server.
     * 
     * @param url The url to set.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the username used to connect to the database server.
     * 
     * @param username The username to set.
     */
    public void setUserName(String username) {
        this.userName = username;
    }

    /**
     * Sets the password used to connect to the database server.
     * 
     * @param password The password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
