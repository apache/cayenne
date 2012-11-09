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
package org.apache.cayenne.tools.dbimport;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Driver;

import javax.sql.DataSource;

import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.conn.DriverDataSource;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.naming.NamingStrategy;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.commons.logging.Log;
import org.xml.sax.InputSource;

/**
 * A thin wrapper around {@link DbLoader} that encapsulates DB import logic for
 * the benefit of Ant and Maven db importers.
 * 
 * @since 3.2
 */
public class DbImportAction {

    private DbAdapterFactory adapterFactory;
    private Log logger;

    public DbImportAction(@Inject Log logger, @Inject DbAdapterFactory adapterFactory) {
        this.logger = logger;
        this.adapterFactory = adapterFactory;
    }

    public void execute(DbImportParameters parameters) throws Exception {

        if (logger.isInfoEnabled()) {
            logger.debug(String.format("DB connection - [driver: %s, url: %s, username: %s, password: %s]",
                    parameters.getDriver(), parameters.getUrl(), parameters.getUsername(), "XXXXX"));
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Importer options - map: " + parameters.getMap());
            logger.debug("Importer options - overwrite: " + parameters.isOverwrite());
            logger.debug("Importer options - adapter: " + parameters.getAdapter());
            logger.debug("Importer options - catalog: " + parameters.getCatalog());
            logger.debug("Importer options - schema: " + parameters.getSchema());
            logger.debug("Importer options - defaultPackage: " + parameters.getDefaultPackage());
            logger.debug("Importer options - tablePattern: " + parameters.getTablePattern());
            logger.debug("Importer options - importProcedures: " + parameters.isImportProcedures());
            logger.debug("Importer options - procedurePattern: " + parameters.getProcedurePattern());
            logger.debug("Importer options - meaningfulPk: " + parameters.isMeaningfulPk());
            logger.debug("Importer options - namingStrategy: " + parameters.getNamingStrategy());
        }

        // TODO: load via DI
        DriverDataSource dataSource = new DriverDataSource(
                (Driver) Class.forName(parameters.getDriver()).newInstance(), parameters.getUrl(),
                parameters.getUsername(), parameters.getPassword());

        DbAdapter adapter = getAdapter(parameters.getAdapter(), dataSource);
        DataMap dataMap = getDataMap(parameters);

        DbImportDbLoaderDelegate loaderDelegate = new DbImportDbLoaderDelegate();
        DbLoader loader = new DbLoader(dataSource.getConnection(), adapter, loaderDelegate);
        loader.setCreatingMeaningfulPK(parameters.isMeaningfulPk());

        // TODO: load via DI AdhocObjectFactory
        String namingStrategy = parameters.getNamingStrategy();
        if (namingStrategy != null) {
            NamingStrategy namingStrategyInst = (NamingStrategy) Class.forName(namingStrategy).newInstance();
            loader.setNamingStrategy(namingStrategyInst);
        }

        String[] types = loader.getDefaultTableTypes();
        loader.load(dataMap, parameters.getCatalog(), parameters.getSchema(), parameters.getTablePattern(), types);

        for (ObjEntity addedObjEntity : loaderDelegate.getAddedObjEntities()) {
            DeleteRuleUpdater.updateObjEntity(addedObjEntity);
        }

        if (parameters.isImportProcedures()) {
            loader.loadProcedures(dataMap, parameters.getCatalog(), parameters.getSchema(),
                    parameters.getProcedurePattern());
        }

        parameters.getMap().delete();

        PrintWriter pw = new PrintWriter(parameters.getMap());
        XMLEncoder encoder = new XMLEncoder(pw, "\t");

        encoder.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        dataMap.encodeAsXML(encoder);

        pw.close();
    }

    DbAdapter getAdapter(String adapter, DataSource dataSource) throws Exception {

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setAdapterType(adapter);

        return adapterFactory.createAdapter(nodeDescriptor, dataSource);
    }

    DataMap getDataMap(DbImportParameters parameters) throws IOException {

        File dataMapFile = parameters.getMap();
        DataMap dataMap;

        if (dataMapFile.exists()) {
            InputSource in = new InputSource(dataMapFile.getCanonicalPath());
            dataMap = new MapLoader().loadDataMap(in);

            if (parameters.isOverwrite()) {
                dataMap.clearObjEntities();
                dataMap.clearEmbeddables();
                dataMap.clearProcedures();
                dataMap.clearDbEntities();
                dataMap.clearQueries();
                dataMap.clearResultSets();
            }
        } else {
            dataMap = new DataMap();
        }

        // update map defaults

        // do not override default package of existing DataMap unless it is
        // explicitly requested by the plugin caller
        String defaultPackage = parameters.getDefaultPackage();
        if (defaultPackage != null && defaultPackage.length() > 0) {
            dataMap.setDefaultPackage(defaultPackage);
        }

        // do not override default schema of existing DataMap unless it is
        // explicitly requested by the plugin caller, and the provided schema is
        // not a pattern
        String schema = parameters.getSchema();
        if (schema != null && schema.length() > 0 && schema.indexOf('%') >= 0) {
            dataMap.setDefaultSchema(schema);
        }

        return dataMap;
    }
}
