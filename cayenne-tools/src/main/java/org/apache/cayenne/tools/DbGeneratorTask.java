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

import java.sql.Driver;
import java.util.Collections;

import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.conn.DriverDataSource;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * An Ant Task that is a frontend to Cayenne DbGenerator allowing schema
 * generation from DataMap using Ant.
 * 
 * @since 1.2
 */
// TODO: support classpath attribute for loading the driver
public class DbGeneratorTask extends CayenneTask {

    // DbGenerator options... setup defaults similar to DbGenerator itself:
    // all DROP set to false, all CREATE - to true
    protected boolean dropTables;
    protected boolean dropPK;
    protected boolean createTables = true;
    protected boolean createPK = true;
    protected boolean createFK = true;

    @Override
    public void execute() {

        Log logger = new AntLogger(this);
       

        log(String.format("connection settings - [driver: %s, url: %s, username: %s]", driver, url, userName),
                Project.MSG_VERBOSE);

        log(String.format(
                "generator options - [dropTables: %s, dropPK: %s, createTables: %s, createPK: %s, createFK: %s]",
                dropTables, dropPK, createTables, createPK, createFK), Project.MSG_VERBOSE);

        validateAttributes();

        ClassLoader loader = null;
        Injector injector = DIBootstrap.createInjector(new ToolsModule(logger));
        try {
            loader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(DbGeneratorTask.class.getClassLoader());

            // Load the data map and run the db generator.
            DataMap dataMap = loadDataMap();

            // load driver taking custom CLASSPATH into account...
            DriverDataSource dataSource = new DriverDataSource((Driver) Class.forName(driver).newInstance(), url,
                    userName, password);

            DbAdapter adapter = getAdapter(injector, dataSource);

            DbGenerator generator = new DbGenerator(adapter, dataMap, Collections.<DbEntity> emptyList(), null,
                    NoopJdbcEventLogger.getInstance());
            generator.setShouldCreateFKConstraints(createFK);
            generator.setShouldCreatePKSupport(createPK);
            generator.setShouldCreateTables(createTables);
            generator.setShouldDropPKSupport(dropPK);
            generator.setShouldDropTables(dropTables);

            generator.runGenerator(dataSource);
        } catch (Exception ex) {
            Throwable th = Util.unwindException(ex);

            String message = "Error generating database";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            log(message, Project.MSG_ERR);
            throw new BuildException(message, th);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
            injector.shutdown();
        }
    }

    /**
     * Validates attributes that are not related to internal
     * DefaultClassGenerator. Throws BuildException if attributes are invalid.
     */
    protected void validateAttributes() throws BuildException {
        StringBuilder error = new StringBuilder("");

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
}
