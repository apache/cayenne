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

import org.apache.cayenne.conn.DriverDataSource;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.AbstractDbLoaderDelegate;
import org.apache.cayenne.map.naming.NamingStrategy;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.CayenneException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.PrintWriter;
import java.sql.Driver;

public class DbImporterTask extends CayenneTask {

    // DbImporter options.
    private boolean overwriteExisting = true;
    private String schemaName;
    private String tablePattern;
    private boolean importProcedures = false;
    private String procedurePattern;
    private boolean meaningfulPk = false;
    private String namingStrategy = "org.apache.cayenne.map.naming.SmartNamingStrategy";

    @Override
    public void execute() {

        log(String.format("connection settings - [driver: %s, url: %s, username: %s, password: %s]", driver, url, userName, password), Project.MSG_VERBOSE);

        log(String.format("importer options - [map: %s, overwriteExisting: %s, schemaName: %s, tablePattern: %s, importProcedures: %s, procedurePattern: %s, meaningfulPk: %s, namingStrategy: %s]",
                map, overwriteExisting, schemaName, tablePattern, importProcedures, procedurePattern, meaningfulPk, namingStrategy), Project.MSG_VERBOSE);

        validateAttributes();

        try {

            // load driver taking custom CLASSPATH into account...
            DriverDataSource dataSource = new DriverDataSource((Driver) Class.forName(driver).newInstance(), url, userName, password);

            // Load the data map and run the db importer.
            final LoaderDelegate loaderDelegate = new LoaderDelegate();
            final DbLoader loader = new DbLoader(dataSource.getConnection(), adapter, loaderDelegate);
            loader.setCreatingMeaningfulPK(meaningfulPk);

            if (namingStrategy != null) {
                final NamingStrategy namingStrategyInst = (NamingStrategy) Class.forName(namingStrategy).newInstance();
                loader.setNamingStrategy(namingStrategyInst);
            }

            final DataMap dataMap = map.exists() ? loadDataMap() : new DataMap();
            loader.loadDataMapFromDB(schemaName, tablePattern, dataMap);

            for (ObjEntity addedObjEntity : loaderDelegate.getAddedObjEntities()) {
                DeleteRuleUpdater.updateObjEntity(addedObjEntity);
            }

            if (importProcedures) {
                loader.loadProceduresFromDB(schemaName, procedurePattern, dataMap);
            }

            // Write the new DataMap out to disk.
            map.delete();
            PrintWriter pw = new PrintWriter(map);
            dataMap.encodeAsXML(pw);
            pw.close();
        } catch (final Exception ex) {
            final Throwable th = Util.unwindException(ex);

            String message = "Error importing database schema";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            log(message, Project.MSG_ERR);
            throw new BuildException(message, th);
        }
    }

    /**
     * Validates atttributes that are not related to internal DefaultClassGenerator.
     * Throws BuildException if attributes are invalid.
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

    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public void setTablePattern(String tablePattern) {
        this.tablePattern = tablePattern;
    }

    public void setImportProcedures(boolean importProcedures) {
        this.importProcedures = importProcedures;
    }

    public void setProcedurePattern(String procedurePattern) {
        this.procedurePattern = procedurePattern;
    }

    public void setMeaningfulPk(boolean meaningfulPk) {
        this.meaningfulPk = meaningfulPk;
    }

    public void setNamingStrategy(String namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    final class LoaderDelegate extends AbstractDbLoaderDelegate {

        public boolean overwriteDbEntity(final DbEntity ent) throws CayenneException {
            return overwriteExisting;
        }

        public void dbEntityAdded(final DbEntity ent) {
            super.dbEntityAdded(ent);
            log("Added DB entity: " + ent.getName());
        }

        public void dbEntityRemoved(final DbEntity ent) {
            super.dbEntityRemoved(ent);
            log("Removed DB entity: " + ent.getName());
        }

        public void objEntityAdded(final ObjEntity ent) {
            super.objEntityAdded(ent);
            log("Added obj entity: " + ent.getName());
        }

        public void objEntityRemoved(final ObjEntity ent) {
            super.objEntityRemoved(ent);
            log("Removed obj entity: " + ent.getName());
        }
    }
}
