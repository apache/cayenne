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

package org.apache.cayenne.modeler.dialog.pref;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.cayenne.modeler.util.CayenneController;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 */
public class DataSourcePreferencesView extends JPanel {

    protected JButton addDataSource;
    protected JButton duplicateDataSource;
    protected JButton removeDataSource;
    protected JButton testDataSource;
    protected JComboBox dataSources;
    protected DBConnectionInfoEditor dataSourceEditor;

    public DataSourcePreferencesView(CayenneController controller) {
        this.addDataSource = new JButton("New...");
        this.duplicateDataSource = new JButton("Duplicate...");
        this.removeDataSource = new JButton("Delete");
        this.testDataSource = new JButton("Test...");
        this.dataSources = new JComboBox();
        this.dataSourceEditor = new DBConnectionInfoEditor(controller);

        // assemble
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(new FormLayout(
                "fill:min(150dlu;pref)",
                "p, 3dlu, p, 10dlu, p, 3dlu, p, 3dlu, p, 10dlu, p"));
        builder.setDefaultDialogBorder();

        builder.add(new JLabel("Select DataSource"), cc.xy(1, 1));
        builder.add(dataSources, cc.xy(1, 3));
        builder.add(addDataSource, cc.xy(1, 5));
        builder.add(duplicateDataSource, cc.xy(1, 7));
        builder.add(removeDataSource, cc.xy(1, 9));
        builder.add(testDataSource, cc.xy(1, 11));

        setLayout(new BorderLayout());
        add(dataSourceEditor.getView(), BorderLayout.CENTER);
        add(builder.getPanel(), BorderLayout.EAST);
    }

    public DBConnectionInfoEditor getDataSourceEditor() {
        return dataSourceEditor;
    }

    public JComboBox getDataSources() {
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
