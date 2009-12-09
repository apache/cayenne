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
import java.awt.CardLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A view for the main DataNode editor tab.
 * 
 */
public class MainDataNodeView extends JPanel {

    protected JTextField dataNodeName;
    protected JComboBox factories;
    protected JPanel dataSourceDetail;
    protected CardLayout dataSourceDetailLayout;
    protected JComboBox localDataSources;
    protected JButton configLocalDataSources;
    protected JComboBox schemaUpdateStrategy;

    public MainDataNodeView(final ProjectController projectController) {

        // create widgets
        this.dataNodeName = CayenneWidgetFactory.createUndoableTextField();

        this.factories = CayenneWidgetFactory.createUndoableComboBox();

        this.localDataSources = CayenneWidgetFactory.createUndoableComboBox();

        this.schemaUpdateStrategy = CayenneWidgetFactory.createUndoableComboBox();
        this.dataSourceDetailLayout = new CardLayout();
        this.dataSourceDetail = new JPanel(dataSourceDetailLayout);

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
        topPanelBuilder.append(
                "Local DataSource (opt.):",
                localDataSources,
                configLocalDataSources);
        topPanelBuilder.append("DataSource Factory:", factories, 3);

        setLayout(new BorderLayout());
        add(topPanelBuilder.getPanel(), BorderLayout.NORTH);
        add(dataSourceDetail, BorderLayout.CENTER);
    }

    public JComboBox getSchemaUpdateStrategy() {
        return schemaUpdateStrategy;
    }

    public JTextField getDataNodeName() {
        return dataNodeName;
    }

    public JPanel getDataSourceDetail() {
        return dataSourceDetail;
    }

    public JComboBox getLocalDataSources() {
        return localDataSources;
    }

    public CardLayout getDataSourceDetailLayout() {
        return dataSourceDetailLayout;
    }

    public JComboBox getFactories() {
        return factories;
    }

    public JButton getConfigLocalDataSources() {
        return configLocalDataSources;
    }
}
