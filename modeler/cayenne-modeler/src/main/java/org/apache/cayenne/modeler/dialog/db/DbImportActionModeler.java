package org.apache.cayenne.modeler.dialog.db; /*****************************************************************
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


import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.tools.dbimport.DbImportAction;
import org.apache.cayenne.tools.dbimport.DbImportActionDefault;
import org.apache.cayenne.tools.dbimport.DbImportConfiguration;
import org.apache.commons.logging.Log;

import java.io.File;
import java.sql.Connection;

public class DbImportActionModeler implements DbImportAction {

    private final Log logger;

    private final DbLoaderHelper dbLoaderHelper;

    @Inject
    private ProjectSaver projectSaver;

    @Inject
    private DataSourceFactory dataSourceFactory;

    @Inject
    private DbAdapterFactory adapterFactory;

    @Inject
    private MapLoader mapLoader;

    public DbImportActionModeler(Log logger, DbLoaderHelper dbLoaderHelper) {
        this.logger = logger;
        this.dbLoaderHelper = dbLoaderHelper;
    }

    @Override
    public void execute(DbImportConfiguration config) throws Exception {
        if (dbLoaderHelper == null) {
            throw new IllegalStateException("Before using execute method you must set dbLoaderHelper");
        }

        new DbImportActionDefault(logger, projectSaver, dataSourceFactory, adapterFactory, mapLoader) {

            @Override
            protected DataMap loadExistingDataMap(File dataMapFile) {
                return dbLoaderHelper.getDataMap();
            }

            @Override
            protected void saveLoaded(DataMap dataMap) {
                dbLoaderHelper.cleanup();

                ProjectController mediator = dbLoaderHelper.getMediator();

                if (mediator.getCurrentDataMap() != null) {
                    mediator.fireDataMapEvent(new DataMapEvent(Application.getFrame(), dataMap, MapEvent.REMOVE));
                    mediator.fireDataMapEvent(new DataMapEvent(Application.getFrame(), dataMap, MapEvent.ADD));
                } else {
                    DataChannelDescriptor currentDomain = (DataChannelDescriptor) mediator.getProject().getRootNode();
                    Resource baseResource = currentDomain.getConfigurationSource();

                    // this will be new data map so need to set configuration source
                    // for it
                    if (baseResource != null) {
                        Resource dataMapResource = baseResource.getRelativeResource(dataMap.getName());
                        dataMap.setConfigurationSource(dataMapResource);
                    }
                    mediator.addDataMap(Application.getFrame(), dataMap);
                }
            }

            @Override
            protected DataMap load(DbImportConfiguration config, 
                                   DbAdapter adapter, Connection connection) throws Exception {
                DataMap dataMap;

                try {
                    dataMap = dbLoaderHelper.getLoader().load(config.getDbLoaderConfig());
                } finally {
                    if (connection != null) {
                        connection.close();
                    }
                }

                return dataMap;
            }
        }.execute(config);
    }
}