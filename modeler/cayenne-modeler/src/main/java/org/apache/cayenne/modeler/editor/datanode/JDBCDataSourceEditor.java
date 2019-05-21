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

package org.apache.cayenne.modeler.editor.datanode;

import java.awt.Component;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.BindingDelegate;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.util.Util;

public class JDBCDataSourceEditor extends DataSourceEditor {

    protected JDBCDataSourceView view;

    public JDBCDataSourceEditor(ProjectController parent,
            BindingDelegate nodeChangeProcessor) {
        super(parent, nodeChangeProcessor);

    }

    public Component getView() {
        return view;
    }
    
    @Override
    public void setNode(DataNodeDescriptor node) {
        if (!Util.nullSafeEquals(getNode(), node)) {
            if (node.getDataSourceDescriptor() == null) {
                node.setDataSourceDescriptor(new DataSourceInfo());
            }
            super.setNode(node);
        }
    }

    protected void prepareBindings(BindingBuilder builder) {
        this.view = new JDBCDataSourceView();
        
        fieldAdapters = new ObjectBinding[6];
        fieldAdapters[0] =
          builder.bindToTextField(view.getUserName(), "node.dataSourceDescriptor.userName");
        fieldAdapters[1] =
          builder.bindToTextField(view.getPassword(), "node.dataSourceDescriptor.password");
        fieldAdapters[2] =
          builder.bindToTextField(view.getUrl(), "node.dataSourceDescriptor.dataSourceUrl");
        fieldAdapters[3] =
          builder.bindToTextField(view.getDriver(), "node.dataSourceDescriptor.jdbcDriver");
        fieldAdapters[4] =
          builder.bindToTextField(view.getMaxConnections(), "node.dataSourceDescriptor.maxConnections");
        fieldAdapters[5] =
          builder.bindToTextField(view.getMinConnections(), "node.dataSourceDescriptor.minConnections");
        

        builder.bindToAction(view.getSyncWithLocal(), "syncDataSourceAction()");
    }


    /**
     * This action is called whenever the password location is changed
     * in the GUI pulldown.  It changes labels and editability of the
     * password fields depending on the option that was selected.
     */


    public void syncDataSourceAction() {
        CayenneModelerController mainController = getApplication().getFrameController();

        if (getNode() == null || getNode().getDataSourceDescriptor() == null) {
            return;
        }

        DataSourceInfo projectDSI = getNode().getDataSourceDescriptor();

        ProjectController parent = (ProjectController) getParent();
        String key = parent.getDataNodePreferences().getLocalDataSource();
        if (key == null) {
            mainController.updateStatus("No Local DataSource selected for node...");
            return;
        }

        DBConnectionInfo dataSource = (DBConnectionInfo) getApplication()
            .getCayenneProjectPreferences()
            .getDetailObject(DBConnectionInfo.class)
            .getObject(key);

        if (dataSource != null) {
            if (dataSource.copyTo(projectDSI)) {
                refreshView();
                super.nodeChangeProcessor.modelUpdated(null, null, null);
                mainController.updateStatus(null);
            }
            else {
                mainController.updateStatus("DataNode is up to date...");
            }
        }
        else {
            mainController.updateStatus("Invalid Local DataSource selected for node...");
        }
    }
}
