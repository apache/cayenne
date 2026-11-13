/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dbsync.reverse.dbimport;

import org.apache.cayenne.CoreModule;
import org.apache.cayenne.project.ConfigurationNameMapper;
import org.apache.cayenne.project.DataMapLoader;
import org.apache.cayenne.project.DefaultConfigurationNameMapper;
import org.apache.cayenne.project.upgrade.ProjectFileUpgrader;
import org.apache.cayenne.project.xml.XMLDataMapLoader;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.projecttools.FileProjectSaver;
import org.apache.cayenne.projecttools.ProjectSaver;

/**
 * A DI module that bootstraps {@link DbImportAction}.
 * Should be used in conjunction with {@link org.apache.cayenne.dbsync.reverse.configuration.ToolsModule}
 * and {@link org.apache.cayenne.dbsync.DbSyncModule}.
 *
 * @since 4.0
 */
public class DbImportModule implements Module {

    public void configure(Binder binder) {
        // dbimport saves the tool-only parts of the project back, so they must be loaded
        CoreModule.extend(binder).loadFullProjectModel();

        binder.bind(DbImportAction.class).to(DefaultDbImportAction.class);
        binder.bind(ProjectSaver.class).to(FileProjectSaver.class);
        binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
        binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
        binder.bind(ProjectFileUpgrader.class).to(ProjectFileUpgrader.class);
    }

}
