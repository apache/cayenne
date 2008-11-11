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

package org.apache.cayenne.modeler.dialog.db;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.pref.DBConnectionInfoEditor;
import org.apache.cayenne.modeler.util.CayenneController;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 */
public class DataSourceWizardView extends JDialog {

    protected JComboBox dataSources;
    protected JButton configButton;
    protected JButton okButton;
    protected JButton cancelButton;
    protected DBConnectionInfoEditor connectionInfo;

    public DataSourceWizardView(CayenneController controller) {
        super(Application.getFrame());
        
        this.dataSources = new JComboBox();

        this.configButton = new JButton("...");
        this.configButton.setToolTipText("configure local DataSource");
        this.okButton = new JButton("Continue");
        this.cancelButton = new JButton("Cancel");
        this.connectionInfo = new DBConnectionInfoEditor(controller);

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
        getContentPane().add(connectionInfo.getView(), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        setTitle("DB Connection Info");
    }

    public JComboBox getDataSources() {
        return dataSources;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    public JButton getConfigButton() {
        return configButton;
    }

    public JButton getOkButton() {
        return okButton;
    }

    public DBConnectionInfoEditor getConnectionInfo() {
        return connectionInfo;
    }
}
