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
import org.apache.cayenne.modeler.toolkit.text.CMPasswordField;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.undo.CayenneUndoManager;

import javax.swing.*;
import java.awt.*;

public class JDBCDataSourceView extends JPanel {

    protected CMUndoableTextField driver;
    protected CMUndoableTextField url;
    protected CMUndoableTextField userName;
    protected CMPasswordField password;

    protected CMUndoableTextField minConnections;
    protected CMUndoableTextField maxConnections;
    protected JButton syncWithLocal;

    public JDBCDataSourceView(CayenneUndoManager undoManager) {

        driver = new CMUndoableTextField(undoManager);
        driver.setTrim(true);
        url = new CMUndoableTextField(undoManager);
        url.setTrim(true);
        userName = new CMUndoableTextField(undoManager);
        password = new CMPasswordField();
        minConnections = new CMUndoableTextField(undoManager, 6);
        maxConnections = new CMUndoableTextField(undoManager, 6);
        syncWithLocal = new JButton("Sync with Local");
        syncWithLocal.setToolTipText("Update from local DataSource");

        // assemble
        CellConstraints cc = new CellConstraints();
        FormLayout layout = new FormLayout(
                "right:80dlu, 3dlu, fill:50dlu, 3dlu, fill:74dlu, 3dlu, fill:70dlu", // Columns
                "p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p"); // Rows

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

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.CENTER);
    }

    public CMUndoableTextField getDriver() {
        return driver;
    }

    public CMPasswordField getPassword() {
        return password;
    }

    public CMUndoableTextField getUrl() {
        return url;
    }

    public CMUndoableTextField getUserName() {
        return userName;
    }

    public CMUndoableTextField getMaxConnections() {
        return maxConnections;
    }

    public CMUndoableTextField getMinConnections() {
        return minConnections;
    }

    public JButton getSyncWithLocal() {
        return syncWithLocal;
    }
}
