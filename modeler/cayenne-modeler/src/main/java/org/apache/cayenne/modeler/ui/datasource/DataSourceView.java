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

package org.apache.cayenne.modeler.ui.datasource;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import java.awt.*;


public class DataSourceView extends JDialog {

    private final JComboBox<String> dataSources;

    public DataSourceView(DataSourceController controller, Frame parent, Component editorView) {
        super(parent);

        this.dataSources = new JComboBox<>();
        JButton configButton = new JButton("...");
        configButton.setToolTipText("configure local DataSource");
        JButton okButton = new JButton("Continue");
        JButton cancelButton = new JButton("Cancel");

        getRootPane().setDefaultButton(okButton);

        dataSources.addActionListener(e -> {
            Object sel = dataSources.getSelectedItem();
            controller.setSelectedDataSource(sel != null ? sel.toString() : null);
        });
        cancelButton.addActionListener(e -> controller.cancelClicked());
        okButton.addActionListener(e -> controller.okClicked());
        configButton.addActionListener(e -> controller.dataSourceConfigClicked());

        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "20dlu:grow, pref, 3dlu, fill:max(150dlu;pref), 3dlu, fill:20dlu",
                "p"));
        builder.setDefaultDialogBorder();

        builder.addLabel("Saved DataSources:", cc.xy(2, 1));
        builder.add(dataSources, cc.xy(4, 1));
        builder.add(configButton, cc.xy(6, 1));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(okButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(builder.getPanel(), BorderLayout.NORTH);
        getContentPane().add(editorView, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        setTitle("DB Connection Info");
    }

    public void setDataSources(String[] keys) {
        dataSources.setModel(new DefaultComboBoxModel<>(keys));
    }

    public void selectDataSource(String key) {
        dataSources.setSelectedItem(key);
    }
}
