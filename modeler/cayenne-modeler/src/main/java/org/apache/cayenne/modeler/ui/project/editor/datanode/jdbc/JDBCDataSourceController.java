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

package org.apache.cayenne.modeler.ui.project.editor.datanode.jdbc;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.modeler.ui.ModelerController;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.ui.project.editor.datanode.DataSourceEditorController;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.util.Util;

import java.awt.*;

public class JDBCDataSourceController extends DataSourceEditorController {

    protected JDBCDataSourceView view;

    public JDBCDataSourceController(ProjectController parent, Runnable nodeChangeProcessor) {
        super(parent, nodeChangeProcessor);
    }

    public Component getView() {
        return view;
    }

    @Override
    public void setNode(DataNodeDescriptor node) {
        if (!Util.nullSafeEquals(getNode(), node)) {
            if (node.getDataSourceDescriptor() == null) {
                node.setDataSourceDescriptor(new DataSourceDescriptor());
            }
            super.setNode(node);
        }
    }

    @Override
    protected void initFieldListeners() {
        this.view = new JDBCDataSourceView(application.getUndoManager());

        view.getUserName().addCommitListener(v -> {
            getNode().getDataSourceDescriptor().setUserName(v);
            nodeChangeProcessor.run();
        });
        view.getPassword().addCommitListener(v -> {
            getNode().getDataSourceDescriptor().setPassword(v);
            nodeChangeProcessor.run();
        });
        view.getUrl().addCommitListener(v -> {
            getNode().getDataSourceDescriptor().setDataSourceUrl(v);
            nodeChangeProcessor.run();
        });
        view.getDriver().addCommitListener(v -> {
            getNode().getDataSourceDescriptor().setJdbcDriver(v);
            nodeChangeProcessor.run();
        });
        view.getMaxConnections().addCommitListener(v -> {
            if (v != null) {
                try {
                    getNode().getDataSourceDescriptor().setMaxConnections(Integer.parseInt(v));
                    nodeChangeProcessor.run();
                } catch (NumberFormatException ignored) {
                }
            }
        });
        view.getMinConnections().addCommitListener(v -> {
            if (v != null) {
                try {
                    getNode().getDataSourceDescriptor().setMinConnections(Integer.parseInt(v));
                    nodeChangeProcessor.run();
                } catch (NumberFormatException ignored) {
                }
            }
        });

        view.getSyncWithLocal().addActionListener(e -> syncDataSourceAction());
    }

    @Override
    protected void refreshView() {
        DataSourceDescriptor d = getNode().getDataSourceDescriptor();
        view.getUserName().setText(d.getUserName());
        view.getPassword().setText(d.getPassword());
        view.getUrl().setText(d.getDataSourceUrl());
        view.getDriver().setText(d.getJdbcDriver());
        view.getMaxConnections().setText(String.valueOf(d.getMaxConnections()));
        view.getMinConnections().setText(String.valueOf(d.getMinConnections()));
    }

    public void syncDataSourceAction() {
        ModelerController mainController = getApplication().getFrameController();

        if (getNode() == null || getNode().getDataSourceDescriptor() == null) {
            return;
        }

        DataSourceDescriptor projectDataSourceDescriptor = getNode().getDataSourceDescriptor();

        String key = parent.getSelectedDataNodePreferences().getLocalDataSource();
        if (key == null) {
            mainController.updateStatus("No Local DataSource selected for node...");
            return;
        }

        DBConnectionInfo dataSource = (DBConnectionInfo) getApplication()
            .getCayenneProjectPreferences()
            .getDetailObject(DBConnectionInfo.class)
            .getObject(key);

        if (dataSource != null) {
            if (dataSource.copyTo(projectDataSourceDescriptor)) {
                refreshView();
                super.nodeChangeProcessor.run();
                mainController.updateStatus(null);
            } else {
                mainController.updateStatus("DataNode is up to date...");
            }
        } else {
            mainController.updateStatus("Invalid Local DataSource selected for node...");
        }
    }
}
