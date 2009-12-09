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

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class JDBCDataSourceView extends JPanel {

    protected TextAdapter driver;
    protected TextAdapter url;
    protected TextAdapter userName;

    protected JPasswordField password;

    protected TextAdapter minConnections;
    protected TextAdapter maxConnections;

    protected JButton syncWithLocal;

    public JDBCDataSourceView() {

        driver = new TextAdapter(new JTextField()) {

            @Override
            protected void updateModel(String text) throws ValidationException {
            }
        };

        url = new TextAdapter(new JTextField()) {

            @Override
            protected void updateModel(String text) throws ValidationException {
            }
        };

        userName = new TextAdapter(new JTextField()) {

            @Override
            protected void updateModel(String text) throws ValidationException {
            }
        };

        password = new JPasswordField();
        minConnections = new TextAdapter(new JTextField(6)) {

            @Override
            protected void updateModel(String text) throws ValidationException {
            }
        };

        maxConnections = new TextAdapter(new JTextField(6)) {

            @Override
            protected void updateModel(String text) throws ValidationException {
            }
        };

        syncWithLocal = new JButton("Sync with Local");
        syncWithLocal.setToolTipText("Update from local DataSource");

        // assemble
        CellConstraints cc = new CellConstraints();
        // FormLayout layout = new FormLayout(
        // "right:80dlu, 3dlu, fill:50dlu, 3dlu, fill:74dlu, 3dlu, fill:70dlu",
        // "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p");
        FormLayout layout = new FormLayout(
                "right:80dlu, 3dlu, fill:50dlu, 3dlu, fill:74dlu, 3dlu, fill:70dlu", // Columns
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p"); // Rows

        PanelBuilder builder = new PanelBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.addSeparator("JDBC Configuration", cc.xywh(1, 1, 7, 1));
        builder.addLabel("JDBC Driver:", cc.xy(1, 3));
        builder.add(getDriver(), cc.xywh(3, 3, 5, 1));
        builder.addLabel("DB URL:", cc.xy(1, 5));
        builder.add(getUrl(), cc.xywh(3, 5, 5, 1));
        builder.addLabel("Username:", cc.xy(1, 7));
        builder.add(getUserName(), cc.xywh(3, 7, 5, 1));
        builder.addLabel("Password:", cc.xy(1, 9));
        builder.add(password, cc.xywh(3, 9, 5, 1));
        builder.addLabel("Min Connections:", cc.xy(1, 11));
        builder.add(getMinConnections(), cc.xy(3, 11));
        builder.addLabel("Max Connections:", cc.xy(1, 13));
        builder.add(getMaxConnections(), cc.xy(3, 13));
        builder.add(syncWithLocal, cc.xy(7, 15));

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public JTextField getDriver() {
        return (JTextField) driver.getComponent();
    }

    public JPasswordField getPassword() {
        return password;
    }

    public JTextField getUrl() {
        return (JTextField) url.getComponent();
    }

    public JTextField getUserName() {
        return (JTextField) userName.getComponent();
    }

    public JTextField getMaxConnections() {
        return (JTextField) maxConnections.getComponent();
    }

    public JTextField getMinConnections() {
        return (JTextField) minConnections.getComponent();
    }

    public JButton getSyncWithLocal() {
        return syncWithLocal;
    }
}
