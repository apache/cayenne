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

package org.apache.cayenne.modeler.ui.preferences.datasource;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.apache.cayenne.modeler.mvc.RootController;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;


public class DataSourcePreferencesView extends JPanel {

    protected JButton addDataSource;
    protected JButton duplicateDataSource;
    protected JButton removeDataSource;
    protected JButton testDataSource;
    protected JComboBox<Object> dataSources;
    protected DBConnectionInfoEditorController dataSourceEditor;

    public DataSourcePreferencesView(RootController controller) {
        this.addDataSource = new JButton("New...");
        this.duplicateDataSource = new JButton("Duplicate...");
        this.removeDataSource = new JButton("Delete");
        this.testDataSource = new JButton("Test...");
        this.dataSources = new JComboBox<>();
        this.dataSourceEditor = new DBConnectionInfoEditorController(controller);

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder sidebar = new PanelBuilder(new FormLayout(
                "fill:min(150dlu;pref)",
                "p, 10dlu, p, 3dlu, p, 3dlu, p, 10dlu, p"));
        sidebar.setDefaultDialogBorder();

        sidebar.add(dataSources, cc.xy(1, 1));
        sidebar.add(addDataSource, cc.xy(1, 3));
        sidebar.add(duplicateDataSource, cc.xy(1, 5));
        sidebar.add(removeDataSource, cc.xy(1, 7));
        sidebar.add(testDataSource, cc.xy(1, 9));

        PanelBuilder editor = new PanelBuilder(new FormLayout(
                "fill:default:grow",
                "p, 3dlu, fill:default:grow"));
        editor.setDefaultDialogBorder();
        editor.addSeparator("Edit DB Connector", cc.xy(1, 1));
        editor.add(dataSourceEditor.getView(), cc.xy(1, 3));

        setLayout(new BorderLayout());
        add(editor.getPanel(), BorderLayout.CENTER);
        add(sidebar.getPanel(), BorderLayout.EAST);
    }

    public DBConnectionInfoEditorController getDataSourceEditor() {
        return dataSourceEditor;
    }

    public JComboBox<Object> getDataSources() {
        return dataSources;
    }

    public JButton getAddDataSource() {
        return addDataSource;
    }

    public JButton getRemoveDataSource() {
        return removeDataSource;
    }

    public JButton getTestDataSource() {
        return testDataSource;
    }

    public JButton getDuplicateDataSource() {
        return duplicateDataSource;
    }
}
