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

package org.apache.cayenne.modeler.dialog.db.load;

import javax.swing.JFrame;

import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.modeler.util.LongRunningTask;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.tools.dbimport.DbImportAction;
import org.apache.cayenne.tools.dbimport.DbImportModule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @since 4.0
 */
final public class LoadDataMapTask extends LongRunningTask {

    private static Log LOGGER = LogFactory.getLog(DbLoaderContext.class);

    private DbLoaderContext context;

    public LoadDataMapTask(JFrame frame, String title, DbLoaderContext context) {
        super(frame, title);
        setMinValue(0);
        setMaxValue(10);
        this.context = context;
    }

    @Override
    protected void execute() {
        context.setStatusNote("Preparing...");
        try {
            createAction().execute(context.getConfig());
        } catch (Exception e) {
            context.processException(e, "Error importing database schema.");
        }
        ProjectUtil.cleanObjMappings(context.getDataMap());
    }

    private DbImportAction createAction() {
        Injector injector = DIBootstrap.createInjector(new DbSyncModule(),
                new ToolsModule(LOGGER),
                new DbImportModule(),
                new ModelerSyncModule(context));
        return injector.getInstance(DbImportAction.class);
    }

    @Override
    protected String getCurrentNote() {
        return context.getStatusNote();
    }

    @Override
    protected int getCurrentValue() {
        return getMinValue();
    }

    @Override
    protected boolean isIndeterminate() {
        return true;
    }

    @Override
    public boolean isCanceled() {
        return context.isStopping();
    }

    @Override
    public void setCanceled(boolean canceled) {
        if (canceled) {
            context.setStatusNote("Canceling..");
        }
        context.setStopping(canceled);
    }
}
