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

import javax.swing.JOptionPane;

import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.modeler.CayenneModelerController;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.project.ProjectDataSource;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.BindingDelegate;
import org.apache.cayenne.swing.ObjectBinding;

/**
 * @author Andrus Adamchik
 */
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

        fieldAdapters = new ObjectBinding[10];
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
        fieldAdapters[6] =
          builder.bindToComboSelection(view.getPasswordEncoder(), "node.dataSource.dataSourceInfo.passwordEncoderClass");
        fieldAdapters[7] =
          builder.bindToTextField(view.getPasswordKey(), "node.dataSource.dataSourceInfo.passwordEncoderKey");
        fieldAdapters[8] =
          builder.bindToComboSelection(view.getPasswordLocation(), "node.dataSource.dataSourceInfo.passwordLocation");
        fieldAdapters[9] =
          builder.bindToTextField(view.getPasswordSource(), "node.dataSource.dataSourceInfo.passwordSource");
        
        // one way binding
        builder.bindToAction(view.getPasswordEncoder(),  "validatePasswordEncoderAction()");
        builder.bindToAction(view.getPasswordLocation(), "passwordLocationChangedAction()");
        builder.bindToAction(view.getSyncWithLocal(),    "syncDataSourceAction()");
    }

    /**
     * This action is called when a new password encoder is specified.  It
     * warns the user if the encoder class is not available and advises them
     * to check their classpath.
     */
    public void validatePasswordEncoderAction()
    {
      if (getNode() == null || getNode().getDataSource() == null)
        return;

      DataSourceInfo dsi = ((ProjectDataSource) getNode().getDataSource()).getDataSourceInfo();

      if (view.getPasswordEncoder().getSelectedItem().equals(dsi.getPasswordEncoderClass()) == false)
        return;

      if (dsi.getPasswordEncoder() == null)
      {
        JOptionPane.showMessageDialog(getView(),
                                      "A valid Password Encoder should be specified (check your CLASSPATH).",
                                      "Invalid Password Encoder",
                                      JOptionPane.ERROR_MESSAGE);
      }
    }

    /**
     * Updates labels and editability of the password related fields.
     * Called by the passwordLocationChangedAction method.
     * 
     * @param isPasswordFieldEnabled True if password field is editable, false if not.
     * @param isPasswordLocationEnabled True if password location field is editable, false if not.
     * @param passwordText The password (which is obscured to the user, of course).
     * @param passwordLocationLabel Label for the password location.
     * @param passwordLocationText Text of the password location field.
     */
    private void updatePasswordElements(boolean isPasswordFieldEnabled,
                                        boolean isPasswordLocationEnabled,
                                        String  passwordText,
                                        String  passwordLocationLabel,
                                        String  passwordLocationText)
    {
      view.getPassword().setEnabled(isPasswordFieldEnabled);
      view.getPassword().setText(passwordText);
      view.getPasswordSource().setEnabled(isPasswordLocationEnabled);
      view.getPasswordSourceLabel().setText(passwordLocationLabel);
      view.getPasswordSource().setText(passwordLocationText);
    }

    /**
     * This action is called whenever the password location is changed
     * in the GUI pulldown.  It changes labels and editability of the
     * password fields depending on the option that was selected.
     */
    public void passwordLocationChangedAction()
    {
      if (getNode() == null || getNode().getDataSource() == null)
        return;

      DataSourceInfo dsi = ((ProjectDataSource) getNode().getDataSource()).getDataSourceInfo();

      String selectedItem = (String) view.getPasswordLocation().getSelectedItem();

      if (selectedItem.equals(DataSourceInfo.PASSWORD_LOCATION_CLASSPATH))
        updatePasswordElements(true, true, dsi.getPassword(), "Password Filename:", dsi.getPasswordSourceFilename());
      else if (selectedItem.equals(DataSourceInfo.PASSWORD_LOCATION_EXECUTABLE))
        updatePasswordElements(false, true, null, "Password Executable:", dsi.getPasswordSourceExecutable());
      else if (selectedItem.equals(DataSourceInfo.PASSWORD_LOCATION_MODEL))
        updatePasswordElements(true, false, dsi.getPassword(), "Password Source:", dsi.getPasswordSourceModel());
      else if (selectedItem.equals(DataSourceInfo.PASSWORD_LOCATION_URL))
        updatePasswordElements(false, true, null, "Password URL:", dsi.getPasswordSourceUrl());
    }


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
