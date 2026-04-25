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

package org.apache.cayenne.modeler.dbimport;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.project.ProjectSaver;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportAction;

public class ModelerDbImportModule implements Module {

    private final ModelerDbLoaderContext loaderContext;

    public ModelerDbImportModule(ModelerDbLoaderContext loaderContext) {
        this.loaderContext = loaderContext;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Application.class).toInstance(loaderContext.getApplication());
        binder.bind(ProjectController.class).toInstance(loaderContext.getProjectController());
        binder.bind(GlobalActions.class).toInstance(loaderContext.getApplication().getActionManager());
        binder.bind(ProjectSaver.class).to(DbImportProjectSaver.class);
        binder.bind(DbImportAction.class).to(DbSyncDbImportAction.class);
        binder.bind(DataMap.class).toInstance(loaderContext.getDataMap());
    }
}
