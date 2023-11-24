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

package org.apache.cayenne.modeler.dialog.db.load;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.runtime.DataSourceFactory;
import org.apache.cayenne.configuration.runtime.DbAdapterFactory;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportConfiguration;
import org.apache.cayenne.dbsync.reverse.dbimport.DefaultDbImportAction;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.editor.DbImportController;
import org.apache.cayenne.project.ProjectSaver;
import org.slf4j.Logger;

public class ModelerDbImportAction extends DefaultDbImportAction {

    @Inject
    private DataMap targetMap;

    private DataMap sourceDataMap;
    private DbImportConfiguration config;

    private DbLoadResultDialog resultDialog;

    private DbImportController dbImportController;

    public ModelerDbImportAction(@Inject Logger logger,
                                 @Inject ProjectSaver projectSaver,
                                 @Inject DataSourceFactory dataSourceFactory,
                                 @Inject DbAdapterFactory adapterFactory,
                                 @Inject DataMapLoader mapLoader,
                                 @Inject MergerTokenFactoryProvider mergerTokenFactoryProvider,
                                 @Inject DataChannelMetaData metaData,
                                 @Inject DataChannelDescriptorLoader dataChannelDescriptorLoader) {
        super(logger, projectSaver, dataSourceFactory, adapterFactory, mapLoader, mergerTokenFactoryProvider, dataChannelDescriptorLoader, metaData);
        dbImportController = Application.getInstance().getFrameController().getDbImportController();
    }

    @Override
    public void execute(DbImportConfiguration config) throws Exception {
        this.config = config;
        this.sourceDataMap = loadDataMap(config);
    }


    public void commit() throws Exception {
        commit(config, sourceDataMap);
    }

    @Override
    protected Collection<MergerToken> log(List<MergerToken> tokens) {
        resultDialog = dbImportController.createDialog();
        resultDialog.refreshElements();
        resultDialog.getOkButton().addActionListener(e -> {
            try {
                if(resultDialog.getTableForMap().containsKey(targetMap)) {
                    commit();
                    checkForUnusedImports();
                }
            } catch (Exception ex) {
                throw new CayenneRuntimeException("Nothing to commit.");
            }
        });

        resultDialog.getRevertButton().addActionListener(e -> resetDialog());

        resultDialog.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                resetDialog();
            }
        });

        logger.info("");

        if (tokens.isEmpty()) {
            logger.info("Detected changes: No changes to import.");
            String logString = String.format("    %-20s", "Nothing to import");
            resultDialog.getRevertButton().setVisible(false);
            resultDialog.addRowToOutput(logString, targetMap);
            return tokens;
        }

        logger.info("Detected changes: ");
        for (MergerToken token : tokens) {
            String logString = String.format("    %-20s %s", token.getTokenName(), token.getTokenValue());
            logger.info(logString);
            resultDialog.addRowToOutput(logString, targetMap);
        }

        logger.info("");

        return tokens;
    }

    private void resetDialog() {
        resultDialog.setVisible(false);
        dbImportController.resetDialog();
    }

    private void checkForUnusedImports() {
        resetDialog();
        dbImportController.setGlobalImport(false);
    }

    @Override
    protected DataMap existingTargetMap(DbImportConfiguration configuration) {
        return targetMap;
    }
}