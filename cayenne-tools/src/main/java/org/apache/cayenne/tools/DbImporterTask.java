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

import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.naming.SmartNamingStrategy;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.tools.dbimport.DbImportAction;
import org.apache.cayenne.tools.dbimport.DbImportModule;
import org.apache.cayenne.tools.dbimport.DbImportParameters;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class DbImporterTask extends Task {

    private DbImportParameters parameters;

    /**
     * @deprecated since 3.2 in favor of "schema"
     */
    private String schemaName;

    /**
     * @deprecated since 3.2 in favor of "meaningfulPkTable"
     */
    private boolean meaningfulPk;

    public DbImporterTask() {
        parameters = new DbImportParameters();
        parameters.setOverwrite(true);
        parameters.setImportProcedures(false);
        parameters.setUsePrimitives(true);
        parameters.setNamingStrategy(SmartNamingStrategy.class.getName());
    }

    @Override
    public void execute() {

        initSchema();
        initMeaningfulPkTables();

        validateAttributes();

        Log logger = new AntLogger(this);
        Injector injector = DIBootstrap.createInjector(new ToolsModule(logger), new DbImportModule());

        try {
            injector.getInstance(DbImportAction.class).execute(parameters);
        } catch (final Exception ex) {
            final Throwable th = Util.unwindException(ex);

            String message = "Error importing database schema";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            log(message, Project.MSG_ERR);
            throw new BuildException(message, th);
        }
        finally {
            injector.shutdown();
        }
    }

    /**
     * Validates attributes that are not related to internal
     * DefaultClassGenerator. Throws BuildException if attributes are invalid.
     */
    protected void validateAttributes() throws BuildException {
        StringBuilder error = new StringBuilder("");

        if (parameters.getDataMapFile() == null) {
            error.append("The 'map' attribute must be set.\n");
        }

        if (parameters.getDriver() == null) {
            error.append("The 'driver' attribute must be set.\n");
        }

        if (parameters.getUrl() == null) {
            error.append("The 'adapter' attribute must be set.\n");
        }

        if (error.length() > 0) {
            throw new BuildException(error.toString());
        }
    }

    /**
     * @since 3.2
     */
    public void setOverwrite(boolean overwrite) {
        parameters.setOverwrite(overwrite);
    }

    /**
     * @deprecated since 3.2 use {@link #setSchema(String)}
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * @since 3.2
     */
    public void setSchema(String schema) {
        parameters.setSchema(schema);
    }

    /**
     * @since 3.2
     */
    public void setDefaultPackage(String defaultPackage) {
        parameters.setDefaultPackage(defaultPackage);
    }

    public void setTablePattern(String tablePattern) {
        parameters.setTablePattern(tablePattern);
    }

    public void setImportProcedures(boolean importProcedures) {
        parameters.setImportProcedures(importProcedures);
    }

    public void setProcedurePattern(String procedurePattern) {
        parameters.setProcedurePattern(procedurePattern);
    }

    /**
     * @deprecated since 3.2 use {@link #setMeaningfulPkTables(String)}
     */
    public void setMeaningfulPk(boolean meaningfulPk) {
        this.meaningfulPk = meaningfulPk;
    }

    /**
     * @since 3.2
     */
    public void setMeaningfulPkTables(String meaningfulPkTables) {
        parameters.setMeaningfulPkTables(meaningfulPkTables);
    }

    public void setNamingStrategy(String namingStrategy) {
        parameters.setNamingStrategy(namingStrategy);
    }

    public void setAdapter(String adapter) {
        parameters.setAdapter(adapter);
    }

    public void setDriver(String driver) {
        parameters.setDriver(driver);
    }

    public void setMap(File map) {
        parameters.setDataMapFile(map);
    }

    public void setPassword(String password) {
        parameters.setPassword(password);
    }

    public void setUrl(String url) {
        parameters.setUrl(url);
    }

    public void setUserName(String username) {
        parameters.setUsername(username);
    }

    /**
     * @since 3.2
     */
    public void setIncludeTables(String includeTables) {
        parameters.setIncludeTables(includeTables);
    }

    /**
     * @since 3.2
     */
    public void setExcludeTables(String excludeTables) {
        parameters.setExcludeTables(excludeTables);
    }

    /**
     * @since 3.2
     */
    public void setUsePrimitives(boolean usePrimitives) {
        parameters.setUsePrimitives(usePrimitives);
    }

    private void initSchema() {
        if (schemaName != null) {
            log("'schemaName' property is deprecated. Use 'schema' instead", Project.MSG_WARN);
        }

        if (parameters.getSchema() == null) {
            parameters.setSchema(schemaName);
        }
    }

    private void initMeaningfulPkTables() {
        if (meaningfulPk) {
            log("'meaningfulPk' property is deprecated. Use 'meaningfulPkTables' pattern instead", Project.MSG_WARN);
        }

        if (parameters.getMeaningfulPkTables() == null && meaningfulPk) {
            parameters.setMeaningfulPkTables("*");
        }
    }
}
