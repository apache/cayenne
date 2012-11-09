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

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Driver;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.access.AbstractDbLoaderDelegate;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.conn.DriverDataSource;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.naming.NamingStrategy;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.xml.sax.InputSource;

public class DbImporterTask extends CayenneTask {

    // DbImporter options.
    private boolean overwriteExisting = true;

    /**
     * @deprecated since 3.2 in favor of "schema"
     */
    private String schemaName;

    private String schema;

    /**
     * A default package for ObjEntity Java classes. If not specified, and the
     * existing DataMap already has the default package, the existing package
     * will be used.
     * 
     * @since 3.2
     */
    private String defaultPackage;

    private String catalog;
    private String tablePattern;
    private boolean importProcedures = false;
    private String procedurePattern;
    private boolean meaningfulPk = false;
    private String namingStrategy = "org.apache.cayenne.map.naming.SmartNamingStrategy";

    @Override
    public void execute() {

        log(String.format(
                "connection settings - [driver: %s, url: %s, username: %s, password: %s]",
                driver, url, userName, password), Project.MSG_VERBOSE);

        log(String.format(
                "importer options - [map: %s, overwriteExisting: %s, schema: %s, tablePattern: %s, importProcedures: %s, procedurePattern: %s, meaningfulPk: %s, namingStrategy: %s]",
                map, overwriteExisting, getSchema(), tablePattern,
                importProcedures, procedurePattern, meaningfulPk,
                namingStrategy), Project.MSG_VERBOSE);

        validateAttributes();

        try {

            // load driver taking custom CLASSPATH into account...
            DriverDataSource dataSource = new DriverDataSource((Driver) Class
                    .forName(driver).newInstance(), url, userName, password);

            Injector injector = getInjector();
            DbAdapter adapter = getAdapter(injector, dataSource);

            // Load the data map and run the db importer.
            final LoaderDelegate loaderDelegate = new LoaderDelegate();
            final DbLoader loader = new DbLoader(dataSource.getConnection(),
                    adapter, loaderDelegate);
            loader.setCreatingMeaningfulPK(meaningfulPk);

            if (namingStrategy != null) {
                final NamingStrategy namingStrategyInst = (NamingStrategy) Class
                        .forName(namingStrategy).newInstance();
                loader.setNamingStrategy(namingStrategyInst);
            }

            String schema = getSchema();

            DataMap dataMap = getDataMap();

            String[] types = loader.getDefaultTableTypes();
            loader.load(dataMap, catalog, schema, tablePattern, types);

            for (ObjEntity addedObjEntity : loaderDelegate
                    .getAddedObjEntities()) {
                DeleteRuleUpdater.updateObjEntity(addedObjEntity);
            }

            if (importProcedures) {
                loader.loadProcedures(dataMap, catalog, schema,
                        procedurePattern);
            }

            // Write the new DataMap out to disk.
            map.delete();
            PrintWriter pw = new PrintWriter(map);

            XMLEncoder encoder = new XMLEncoder(pw, "\t");
            encoder.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            dataMap.encodeAsXML(encoder);

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

    DataMap getDataMap() throws IOException {

        DataMap dataMap;

        if (map.exists()) {
            InputSource in = new InputSource(map.getCanonicalPath());
            return new MapLoader().loadDataMap(in);
        } else {
            dataMap = new DataMap();
        }

        // update map defaults

        // do not override default package of existing DataMap unless it is
        // explicitly requested by the plugin caller
        if (defaultPackage != null && defaultPackage.length() > 0) {
            dataMap.setDefaultPackage(defaultPackage);
        }

        // do not override default schema of existing DataMap unless it is
        // explicitly requested by the plugin caller, and the provided schema is
        // not a pattern
        if (schema != null && schema.length() > 0 && schema.indexOf('%') >= 0) {
            dataMap.setDefaultSchema(schema);
        }

        return dataMap;
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

    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
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
        this.schema = schema;
    }

    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
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

    private String getSchema() {
        if (schemaName != null) {
            log("'schemaName' property is deprecated. Use 'schema' instead",
                    Project.MSG_WARN);
        }

        return schema != null ? schema : schemaName;
    }

    final class LoaderDelegate extends AbstractDbLoaderDelegate {

        @Override
        public boolean overwriteDbEntity(final DbEntity ent)
                throws CayenneException {
            return overwriteExisting;
        }

        @Override
        public void dbEntityAdded(final DbEntity ent) {
            super.dbEntityAdded(ent);
            log("Added DB entity: " + ent.getName());
        }

        @Override
        public void dbEntityRemoved(final DbEntity ent) {
            super.dbEntityRemoved(ent);
            log("Removed DB entity: " + ent.getName());
        }

        @Override
        public void objEntityAdded(final ObjEntity ent) {
            super.objEntityAdded(ent);
            log("Added obj entity: " + ent.getName());
        }

        @Override
        public void objEntityRemoved(final ObjEntity ent) {
            super.objEntityRemoved(ent);
            log("Removed obj entity: " + ent.getName());
        }
    }
}
