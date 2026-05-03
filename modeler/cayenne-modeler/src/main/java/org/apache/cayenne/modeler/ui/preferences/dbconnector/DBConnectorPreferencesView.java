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

package org.apache.cayenne.modeler.ui.preferences.dbconnector;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.apache.cayenne.modeler.mvc.RootController;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class DBConnectorPreferencesView extends JPanel {

    protected JButton addConnector;
    protected JButton duplicateConnector;
    protected JButton removeConnector;
    protected JButton testConnector;
    protected JComboBox<Object> connectors;
    protected DBConnectionInfoEditorController connectorEditor;

    public DBConnectorPreferencesView(RootController controller) {
        this.addConnector = new JButton("New...");
        this.duplicateConnector = new JButton("Duplicate...");
        this.removeConnector = new JButton("Delete");
        this.testConnector = new JButton("Test...");
        this.connectors = new JComboBox<>();
        this.connectorEditor = new DBConnectionInfoEditorController(controller);

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder sidebar = new PanelBuilder(new FormLayout(
                "fill:min(150dlu;pref)",
                "p, 10dlu, p, 3dlu, p, 3dlu, p, 10dlu, p"));
        sidebar.setDefaultDialogBorder();

        sidebar.add(connectors, cc.xy(1, 1));
        sidebar.add(addConnector, cc.xy(1, 3));
        sidebar.add(duplicateConnector, cc.xy(1, 5));
        sidebar.add(removeConnector, cc.xy(1, 7));
        sidebar.add(testConnector, cc.xy(1, 9));

        PanelBuilder editor = new PanelBuilder(new FormLayout(
                "fill:default:grow",
                "p, 3dlu, fill:default:grow"));
        editor.setDefaultDialogBorder();
        editor.addSeparator("Edit DB Connector", cc.xy(1, 1));
        editor.add(connectorEditor.getView(), cc.xy(1, 3));

        setLayout(new BorderLayout());
        add(editor.getPanel(), BorderLayout.CENTER);
        add(sidebar.getPanel(), BorderLayout.EAST);
    }

    public DBConnectionInfoEditorController getConnectorEditor() {
        return connectorEditor;
    }

    public JComboBox<Object> getConnectors() {
        return connectors;
    }

    public JButton getAddConnector() {
        return addConnector;
    }

    public JButton getRemoveConnector() {
        return removeConnector;
    }

    public JButton getTestConnector() {
        return testConnector;
    }

    public JButton getDuplicateConnector() {
        return duplicateConnector;
    }
}
