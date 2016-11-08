/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.modeler.dialog.db;

import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.naming.ObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.db.DbLoader;
import org.apache.cayenne.dbsync.reverse.db.DbLoaderDelegate;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.tools.dbimport.DbImportConfiguration;
import org.apache.cayenne.tools.dbimport.DefaultDbImportAction;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.sql.Connection;

public class ModelerDbImportAction extends DefaultDbImportAction {

    private DataMap targetMap;
    private DbLoader dbLoader;

    public ModelerDbImportAction(Log logger,
                                 ProjectSaver projectSaver,
                                 DataSourceFactory dataSourceFactory,
                                 DbAdapterFactory adapterFactory,
                                 MapLoader mapLoader,
                                 MergerTokenFactoryProvider mergerTokenFactoryProvider,
                                 DataMap targetMap,
                                 DbLoader dbLoader
                                 ) {

        super(logger, projectSaver, dataSourceFactory, adapterFactory, mapLoader, mergerTokenFactoryProvider);

        this.targetMap = targetMap;
        this.dbLoader = dbLoader;
    }

    @Override
    protected DbLoader createDbLoader(DbAdapter adapter,
                                      Connection connection,
                                      DbLoaderDelegate dbLoaderDelegate,
                                      ObjectNameGenerator objectNameGenerator) {
        return dbLoader;
    }

    @Override
    protected DataMap existingTargetMap(DbImportConfiguration configuration) throws IOException {
        return targetMap;
    }
}