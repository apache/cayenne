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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JOptionPane;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataNodeEvent;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.DataNodeDisplayEvent;
import org.apache.cayenne.modeler.event.DataNodeDisplayListener;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.BindingDelegate;
import org.apache.cayenne.swing.ObjectBinding;

public class PasswordEncoderEditor extends CayenneController {

    protected DataNodeDescriptor node;
    protected ObjectBinding[] bindings;
    protected PasswordEncoderView view;
    protected BindingDelegate nodeChangeProcessor;

    public PasswordEncoderEditor(CayenneController parent) {

        super(parent);

        this.view = new PasswordEncoderView();

        this.nodeChangeProcessor = new BindingDelegate() {

            public void modelUpdated(
                    ObjectBinding binding,
                    Object oldValue,
                    Object newValue) {

                DataNodeEvent e = new DataNodeEvent(PasswordEncoderEditor.this, node);
                ((ProjectController) getParent()).fireDataNodeEvent(e);
            }
        };

        initController();
    }

    protected void initController() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.setDelegate(nodeChangeProcessor);

        bindings = new ObjectBinding[4];

        bindings[0] = builder.bindToComboSelection(
                view.getPasswordEncoder(),
                "node.dataSourceDescriptor.passwordEncoderClass");
        bindings[1] = builder.bindToTextField(
                view.getPasswordKey(),
                "node.dataSourceDescriptor.passwordEncoderKey");
        bindings[2] = builder.bindToComboSelection(
                view.getPasswordLocation(),
                "node.dataSourceDescriptor.passwordLocation");
        bindings[3] = builder.bindToTextField(
                view.getPasswordSource(),
                "node.dataSourceDescriptor.passwordSource");

        ((ProjectController) getParent())
                .addDataNodeDisplayListener(new DataNodeDisplayListener() {

                    public void currentDataNodeChanged(DataNodeDisplayEvent e) {
                        refreshView(e.getDataNode());
                    }
                });

        getView().addComponentListener(new ComponentAdapter() {

            public void componentShown(ComponentEvent e) {
                refreshView(node != null ? node : ((ProjectController) getParent())
                        .getCurrentDataNode());
            }
        });

        builder
                .bindToAction(
                        view.getPasswordEncoder(),
                        "validatePasswordEncoderAction()");
        builder.bindToAction(
                view.getPasswordLocation(),
                "passwordLocationChangedAction()");

    }

    protected void refreshView(DataNodeDescriptor dataNodeDescriptor) {
        this.node = dataNodeDescriptor;

        if (dataNodeDescriptor == null || dataNodeDescriptor.getDataSourceDescriptor() == null) {
            getView().setVisible(false);
            return;
        }

        for (ObjectBinding binding : bindings) {
            binding.updateView();
        }
    }

    public void validatePasswordEncoderAction() {
        if (node == null || node.getDataSourceDescriptor() == null)
            return;

        DataSourceInfo dsi = node.getDataSourceDescriptor();

        if (!view.getPasswordEncoder().getSelectedItem().equals(dsi.getPasswordEncoderClass()))
            return;

        if (dsi.getPasswordEncoder() == null) {
            JOptionPane
                    .showMessageDialog(
                            getView(),
                            "A valid Password Encoder should be specified (check your CLASSPATH).",
                            "Invalid Password Encoder",
                            JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updatePasswordElements(
            boolean isPasswordFieldEnabled,
            boolean isPasswordLocationEnabled,
            String passwordText,
            String passwordLocationLabel,
            String passwordLocationText) {
        view.getPasswordSource().setEnabled(isPasswordLocationEnabled);
        view.getPasswordSourceLabel().setText(passwordLocationLabel);
        view.getPasswordSource().setText(passwordLocationText);

    }

    public void passwordLocationChangedAction() {
        if (node == null || node.getDataSourceDescriptor() == null)
            return;

        DataSourceInfo dsi = node.getDataSourceDescriptor();

        String selectedItem = (String) view.getPasswordLocation().getSelectedItem();

        if (selectedItem.equals(DataSourceInfo.PASSWORD_LOCATION_CLASSPATH))
            updatePasswordElements(
                    true,
                    true,
                    dsi.getPassword(),
                    "Password Filename:",
                    dsi.getPasswordSourceFilename());
        else if (selectedItem.equals(DataSourceInfo.PASSWORD_LOCATION_EXECUTABLE))
            updatePasswordElements(false, true, null, "Password Executable:", dsi
                    .getPasswordSourceExecutable());
        else if (selectedItem.equals(DataSourceInfo.PASSWORD_LOCATION_MODEL))
            updatePasswordElements(
                    true,
                    false,
                    dsi.getPassword(),
                    "Password Source:",
                    dsi.getPasswordSourceModel());
        else if (selectedItem.equals(DataSourceInfo.PASSWORD_LOCATION_URL))
            updatePasswordElements(false, true, null, "Password URL:", dsi
                    .getPasswordSourceUrl());
    }

    public Component getView() {
        return view;
    }

    public DataNodeDescriptor getNode() {
        return node;
    }

    public void setNode(DataNodeDescriptor node) {
        this.node = node;
    }

}
