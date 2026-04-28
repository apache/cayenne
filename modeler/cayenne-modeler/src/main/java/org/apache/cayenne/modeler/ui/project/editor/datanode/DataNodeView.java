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

package org.apache.cayenne.modeler.ui.project.editor.datanode;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.modeler.toolkit.WidgetFactory;
import org.apache.cayenne.modeler.toolkit.text.CayenneUndoableTextField;
import org.apache.cayenne.modeler.undo.CayenneUndoManager;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;

public class DataNodeView extends JPanel {

    protected CayenneUndoableTextField dataNodeName;
    protected JComboBox<String> factories;
    protected JPanel dataSourceDetail;
    protected CardLayout dataSourceDetailLayout;
    protected CayenneUndoableTextField customAdapter;
    protected JComboBox<String> localDataSources;
    protected JButton configLocalDataSources;
    protected JComboBox<String> schemaUpdateStrategy;

    public DataNodeView(CayenneUndoManager undoManager) {

        // create widgets
        this.dataNodeName = new CayenneUndoableTextField(undoManager);

        this.factories = WidgetFactory.createUndoableComboBox(undoManager);

        this.localDataSources = WidgetFactory.createUndoableComboBox(undoManager);

        this.schemaUpdateStrategy = WidgetFactory.createUndoableComboBox(undoManager);
        this.dataSourceDetailLayout = new CardLayout();
        this.dataSourceDetail = new JPanel(dataSourceDetailLayout);

        this.customAdapter = new CayenneUndoableTextField(undoManager);

        this.configLocalDataSources = new JButton("...");
        this.configLocalDataSources.setToolTipText("configure local DataSource");

        // assemble

        DefaultFormBuilder topPanelBuilder = new DefaultFormBuilder(new FormLayout(
                "right:80dlu, 3dlu, fill:177dlu, 3dlu, fill:20dlu",
                ""));
        topPanelBuilder.setDefaultDialogBorder();

        topPanelBuilder.appendSeparator("DataNode Configuration");
        topPanelBuilder.append("DataNode Name:", getDataNodeName(), 3);
        topPanelBuilder.append("Schema Update Strategy:", schemaUpdateStrategy, 3);

        DefaultFormBuilder builderForLabel = new DefaultFormBuilder(new FormLayout(
                "right:199dlu"));
        JLabel label = new JLabel(
                "You can enter custom class implementing SchemaUpdateStrategy");
        Font font = new Font(getFont().getName(), Font.PLAIN, getFont().getSize() - 2);
        label.setFont(font);
        builderForLabel.append(label);
        topPanelBuilder.append("", builderForLabel.getPanel(), 3);

        topPanelBuilder.append("Custom Adapter (optional):", customAdapter, 3);

        topPanelBuilder.append(
                "Local DataSource (opt.):",
                localDataSources,
                configLocalDataSources);
        topPanelBuilder.append("DataSource Factory:", factories, 3);

        setLayout(new BorderLayout());
        add(topPanelBuilder.getPanel(), BorderLayout.NORTH);
        add(dataSourceDetail, BorderLayout.CENTER);
    }

    public JComboBox<String> getSchemaUpdateStrategy() {
        return schemaUpdateStrategy;
    }

    public CayenneUndoableTextField getDataNodeName() {
        return dataNodeName;
    }

    public JPanel getDataSourceDetail() {
        return dataSourceDetail;
    }

    public JComboBox<String> getLocalDataSources() {
        return localDataSources;
    }

    public CardLayout getDataSourceDetailLayout() {
        return dataSourceDetailLayout;
    }

    public JComboBox<String> getFactories() {
        return factories;
    }

    public JButton getConfigLocalDataSources() {
        return configLocalDataSources;
    }

    public CayenneUndoableTextField getCustomAdapter() {
        return customAdapter;
    }
}
