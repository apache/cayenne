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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dbconnector.DBConnector;
import org.apache.cayenne.modeler.pref.DataNodePrefs;
import org.apache.cayenne.modeler.toolkit.text.CMPasswordField;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.ui.MainFrame;
import org.apache.cayenne.modeler.ui.project.editor.datanode.DataSourcePanel;

import javax.swing.JButton;
import java.awt.BorderLayout;

public class JDBCDataSourcePanel extends DataSourcePanel {

    private final CMUndoableTextField driver;
    private final CMUndoableTextField url;
    private final CMUndoableTextField userName;
    private final CMPasswordField password;
    private final CMUndoableTextField minConnections;
    private final CMUndoableTextField maxConnections;
    private final JButton syncWithLocal;

    public JDBCDataSourcePanel(Application app, Runnable nodeChangeProcessor) {
        super(app, nodeChangeProcessor);

        this.driver = new CMUndoableTextField(app.getUndoManager());
        this.driver.setTrim(true);
        this.url = new CMUndoableTextField(app.getUndoManager());
        this.url.setTrim(true);
        this.userName = new CMUndoableTextField(app.getUndoManager());
        this.password = new CMPasswordField();
        this.minConnections = new CMUndoableTextField(app.getUndoManager(), 6);
        this.maxConnections = new CMUndoableTextField(app.getUndoManager(), 6);
        this.syncWithLocal = new JButton("Sync with Local");
        this.syncWithLocal.setToolTipText("Update from local DataSource");

        initLayout();
        initBindings();
    }

    @Override
    public void setNode(DataNodeDescriptor node) {
        if (node.getDataSourceDescriptor() == null) {
            node.setDataSourceDescriptor(new DataSourceDescriptor());
        }
        super.setNode(node);
    }

    private void initLayout() {
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:80dlu, 3dlu, fill:50dlu, 3dlu, fill:74dlu, 3dlu, fill:70dlu",
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("JDBC Configuration", cc.xywh(1, 1, 7, 1));
        builder.addLabel("JDBC Driver:", cc.xy(1, 3));
        builder.add(driver, cc.xywh(3, 3, 5, 1));
        builder.addLabel("DB URL:", cc.xy(1, 5));
        builder.add(url, cc.xywh(3, 5, 5, 1));
        builder.addLabel("Username:", cc.xy(1, 7));
        builder.add(userName, cc.xywh(3, 7, 5, 1));
        builder.addLabel("Password:", cc.xy(1, 9));
        builder.add(password, cc.xywh(3, 9, 5, 1));
        builder.addLabel("Min Connections:", cc.xy(1, 11));
        builder.add(minConnections, cc.xy(3, 11));
        builder.addLabel("Max Connections:", cc.xy(1, 13));
        builder.add(maxConnections, cc.xy(3, 13));
        builder.add(syncWithLocal, cc.xy(7, 15));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initBindings() {
        userName.addCommitListener(v -> {
            getNode().getDataSourceDescriptor().setUserName(v);
            nodeChangeProcessor.run();
        });
        password.addCommitListener(v -> {
            getNode().getDataSourceDescriptor().setPassword(v);
            nodeChangeProcessor.run();
        });
        url.addCommitListener(v -> {
            getNode().getDataSourceDescriptor().setDataSourceUrl(v);
            nodeChangeProcessor.run();
        });
        driver.addCommitListener(v -> {
            getNode().getDataSourceDescriptor().setJdbcDriver(v);
            nodeChangeProcessor.run();
        });
        maxConnections.addCommitListener(v -> {
            if (v != null) {
                try {
                    getNode().getDataSourceDescriptor().setMaxConnections(Integer.parseInt(v));
                    nodeChangeProcessor.run();
                } catch (NumberFormatException ignored) {
                }
            }
        });
        minConnections.addCommitListener(v -> {
            if (v != null) {
                try {
                    getNode().getDataSourceDescriptor().setMinConnections(Integer.parseInt(v));
                    nodeChangeProcessor.run();
                } catch (NumberFormatException ignored) {
                }
            }
        });

        syncWithLocal.addActionListener(e -> syncDataSourceAction());
    }

    @Override
    protected void refreshView() {
        DataSourceDescriptor d = getNode().getDataSourceDescriptor();
        userName.setText(d.getUserName());
        password.setText(d.getPassword());
        url.setText(d.getDataSourceUrl());
        driver.setText(d.getJdbcDriver());
        maxConnections.setText(String.valueOf(d.getMaxConnections()));
        minConnections.setText(String.valueOf(d.getMinConnections()));
    }

    private void syncDataSourceAction() {
        MainFrame frame = app.getFrame();

        if (getNode() == null || getNode().getDataSourceDescriptor() == null) {
            return;
        }

        DataSourceDescriptor projectDataSourceDescriptor = getNode().getDataSourceDescriptor();

        String key = new DataNodePrefs(
                app.getPreferencesRepository(),
                frame.getProjectSession().project(),
                getNode().getName()).getLocalDataSource();
        if (key == null) {
            frame.updateStatus("No Local DataSource selected for node...");
            return;
        }

        DBConnector connector = app.getDbConnectors().get(key);

        if (connector != null) {
            if (connector.copyTo(projectDataSourceDescriptor)) {
                refreshView();
                nodeChangeProcessor.run();
                frame.updateStatus(null);
            } else {
                frame.updateStatus("DataNode is up to date...");
            }
        } else {
            frame.updateStatus("Invalid Local DataSource selected for node...");
        }
    }
}
