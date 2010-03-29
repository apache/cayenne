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

package org.apache.cayenne.modeler.editor.datanode;

import java.awt.Component;

import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.project.ProjectDataSource;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.BindingDelegate;
import org.apache.cayenne.swing.ObjectBinding;

public class JDBCDataSourceEditor extends DataSourceEditor {

    protected JDBCDataSourceView view;

    public JDBCDataSourceEditor(ProjectController parent,
            BindingDelegate nodeChangeProcessor) {
        super(parent, nodeChangeProcessor);

    }

    public Component getView() {
        return view;
    }

    protected void prepareBindings(BindingBuilder builder) {
        this.view = new JDBCDataSourceView();

        
        fieldAdapters = new ObjectBinding[6];
        fieldAdapters[0] =
          builder.bindToTextField(view.getUserName(), "node.dataSource.dataSourceInfo.userName");
        fieldAdapters[1] =
          builder.bindToTextField(view.getPassword(), "node.dataSource.dataSourceInfo.password");
        fieldAdapters[2] =
          builder.bindToTextField(view.getUrl(), "node.dataSource.dataSourceInfo.dataSourceUrl");
        fieldAdapters[3] =
          builder.bindToTextField(view.getDriver(), "node.dataSource.dataSourceInfo.jdbcDriver");
        fieldAdapters[4] =
          builder.bindToTextField(view.getMaxConnections(), "node.dataSource.dataSourceInfo.maxConnections");
        fieldAdapters[5] =
          builder.bindToTextField(view.getMinConnections(), "node.dataSource.dataSourceInfo.minConnections");
        

        builder.bindToAction(view.getSyncWithLocal(),    "syncDataSourceAction()");
    }


    /**
     * This action is called whenever the password location is changed
     * in the GUI pulldown.  It changes labels and editability of the
     * password fields depending on the option that was selected.
     */


    public void syncDataSourceAction() {
        CayenneModelerController mainController = getApplication().getFrameController();

        if (getNode() == null || getNode().getDataSource() == null) {
            return;
        }

        ProjectDataSource projectDS = (ProjectDataSource) getNode().getDataSource();

        ProjectController parent = (ProjectController) getParent();
        String key = parent.getDataNodePreferences().getLocalDataSource();
        if (key == null) {
            mainController.updateStatus("No Local DataSource selected for node...");
            return;
        }

        DBConnectionInfo dataSource = (DBConnectionInfo) parent
                .getApplicationPreferenceDomain()
                .getDetail(key, DBConnectionInfo.class, false);

        if (dataSource != null) {
            if (dataSource.copyTo(projectDS.getDataSourceInfo())) {
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
