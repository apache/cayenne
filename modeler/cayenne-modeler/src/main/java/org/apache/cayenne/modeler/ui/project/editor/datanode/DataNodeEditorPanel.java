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
import org.apache.cayenne.access.dbsync.CreateIfNoSchemaStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.ThrowOnPartialOrCreateSchemaStrategy;
import org.apache.cayenne.access.dbsync.ThrowOnPartialSchemaStrategy;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.runtime.XMLPoolingDataSourceFactory;
import org.apache.cayenne.modeler.dbconnector.DBConnector;
import org.apache.cayenne.modeler.event.model.DataNodeEvent;
import org.apache.cayenne.modeler.pref.DataNodePrefs;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.toolkit.combobox.CMUndoableComboBox;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.ui.preferences.PreferenceDialog;
import org.apache.cayenne.modeler.ui.project.editor.datanode.custom.CustomDataSourcePanel;
import org.apache.cayenne.modeler.ui.project.editor.datanode.jdbc.JDBCDataSourcePanel;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Editor panel for a {@link DataNodeDescriptor}. Wires the DataNode header fields
 * (name, schema-update strategy, custom adapter, local DataSource, factory choice) and
 * delegates to a sub-editor selected by the chosen DataSource factory (Cayenne JDBC
 * pooling vs custom).
 */
public class DataNodeEditorPanel extends ProjectPanel {

    static final String NO_LOCAL_DATA_SOURCE = "Select DataSource for Local Work...";
    private static final String XML_POOLING_DATA_SOURCE_FACTORY = XMLPoolingDataSourceFactory.class.getName();

    private static final String[] STANDARD_DATA_SOURCE_FACTORIES = new String[]{
            DataSourceFactoryType.CAYENNE.getLabel(),
            DataSourceFactoryType.CUSTOM.getLabel()
    };

    private static final String[] STANDARD_SCHEMA_UPDATE_STRATEGY = new String[]{
            SkipSchemaUpdateStrategy.class.getName(),
            CreateIfNoSchemaStrategy.class.getName(),
            ThrowOnPartialSchemaStrategy.class.getName(),
            ThrowOnPartialOrCreateSchemaStrategy.class.getName()
    };

    private final Runnable nodeChangeProcessor;
    private final Map<String, DataSourcePanel> datasourceEditors = new HashMap<>();
    private final CustomDataSourcePanel defaultSubeditor;

    private final CMUndoableTextField dataNodeName;
    private final JComboBox<String> factories;
    private final CMUndoableTextField customAdapter;
    private final JComboBox<String> localDataSources;
    private final JButton configLocalDataSources;
    private final JComboBox<String> schemaUpdateStrategy;
    private final CardLayout dataSourceDetailLayout;
    private final JPanel dataSourceDetail;

    private DataNodeDescriptor node;
    private boolean refreshing;

    public DataNodeEditorPanel(ProjectSession session) {
        super(session);

        this.nodeChangeProcessor = () -> session.fireDataNodeEvent(DataNodeEvent.ofChange(this, node));
        this.defaultSubeditor = new CustomDataSourcePanel(app, nodeChangeProcessor);

        this.dataNodeName = new CMUndoableTextField(app.getUndoManager());
        this.factories = new CMUndoableComboBox<>(app.getUndoManager());
        this.localDataSources = new CMUndoableComboBox<>(app.getUndoManager());
        this.schemaUpdateStrategy = new CMUndoableComboBox<>(app.getUndoManager());
        this.dataSourceDetailLayout = new CardLayout();
        this.dataSourceDetail = new JPanel(dataSourceDetailLayout);
        this.customAdapter = new CMUndoableTextField(app.getUndoManager());
        this.configLocalDataSources = new JButton("...");
        this.configLocalDataSources.setToolTipText("configure local DataSource");

        initLayout();
        initBindings();
    }

    private void initLayout() {
        DefaultFormBuilder topPanelBuilder = new DefaultFormBuilder(new FormLayout(
                "right:80dlu, 3dlu, fill:177dlu, 3dlu, fill:20dlu",
                ""));
        topPanelBuilder.setDefaultDialogBorder();

        topPanelBuilder.appendSeparator("DataNode Configuration");
        topPanelBuilder.append("DataNode Name:", dataNodeName, 3);
        topPanelBuilder.append("Schema Update Strategy:", schemaUpdateStrategy, 3);

        DefaultFormBuilder builderForLabel = new DefaultFormBuilder(new FormLayout("right:199dlu"));
        JLabel label = new JLabel("You can enter custom class implementing SchemaUpdateStrategy");
        Font font = new Font(getFont().getName(), Font.PLAIN, getFont().getSize() - 2);
        label.setFont(font);
        builderForLabel.append(label);
        topPanelBuilder.append("", builderForLabel.getPanel(), 3);

        topPanelBuilder.append("Custom Adapter (optional):", customAdapter, 3);
        topPanelBuilder.append("Local DataSource (opt.):", localDataSources, configLocalDataSources);
        topPanelBuilder.append("DataSource Factory:", factories, 3);

        setLayout(new BorderLayout());
        add(topPanelBuilder.getPanel(), BorderLayout.NORTH);
        add(dataSourceDetail, BorderLayout.CENTER);

        dataSourceDetail.add(defaultSubeditor, "default");
        factories.setEditable(false);
        factories.setModel(new DefaultComboBoxModel<>(STANDARD_DATA_SOURCE_FACTORIES));
        schemaUpdateStrategy.setEditable(true);
        schemaUpdateStrategy.setModel(new DefaultComboBoxModel<>(STANDARD_SCHEMA_UPDATE_STRATEGY));
    }

    private void initBindings() {
        session.addDataNodeDisplayListener(e -> refreshView(e.getDataNode()));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                refreshView(node != null ? node : session.getSelectedDataNode());
            }
        });

        localDataSources.addActionListener(e -> {
            if (refreshing) return;
            Object sel = localDataSources.getSelectedItem();
            String key = (sel == null || NO_LOCAL_DATA_SOURCE.equals(sel)) ? null : sel.toString();
            nodePrefs().setLocalDataSource(key);
        });

        dataNodeName.addCommitListener(v -> {
            if (node == null) return;
            String oldName = node.getName();
            try {
                setNodeName(v);
            } catch (ValidationException ignored) {
                return;
            }
            session.fireDataNodeEvent(DataNodeEvent.ofChange(this, node, oldName));
        });

        customAdapter.addCommitListener(v -> {
            if (node == null) return;
            node.setAdapterType(v);
            session.fireDataNodeEvent(DataNodeEvent.ofChange(this, node));
        });

        factories.addActionListener(e -> {
            if (refreshing) return;
            setFactoryName((String) factories.getSelectedItem());
            session.fireDataNodeEvent(DataNodeEvent.ofChange(this, node));
        });

        schemaUpdateStrategy.addActionListener(e -> {
            if (refreshing) return;
            if (node != null) {
                node.setSchemaUpdateStrategyType((String) schemaUpdateStrategy.getSelectedItem());
            }
            session.fireDataNodeEvent(DataNodeEvent.ofChange(this, node));
        });

        configLocalDataSources.addActionListener(e -> dataSourceConfigAction());
    }

    private String getFactoryName() {
        return XML_POOLING_DATA_SOURCE_FACTORY.equals(node.getDataSourceFactoryType())
                ? DataSourceFactoryType.CAYENNE.getLabel()
                : DataSourceFactoryType.CUSTOM.getLabel();
    }

    private void setFactoryName(String factoryName) {
        if (node != null) {
            if (DataSourceFactoryType.CAYENNE.getLabel().equals(factoryName)) {
                node.setDataSourceFactoryType(XML_POOLING_DATA_SOURCE_FACTORY);
            } else {
                node.setDataSourceFactoryType(defaultSubeditor.getFactoryName());
            }
            showDataSourceSubview(factoryName);
        }
    }

    private void setNodeName(String newName) {
        if (node == null) {
            return;
        }

        if (newName == null) {
            throw new ValidationException("Empty DataNode Name");
        }

        DataChannelDescriptor dataChannelDescriptor =
                (DataChannelDescriptor) app.getFrame().getProjectSession().project().getRootNode();

        Collection<DataNodeDescriptor> matchingNode = dataChannelDescriptor.getNodeDescriptors();
        for (DataNodeDescriptor n : matchingNode) {
            if (n.getName().equals(newName)) {
                throw new ValidationException("There is another DataNode named '" + newName
                        + "'. Use a different name.");
            }
        }

        // passed validation, set value, then move prefs subtree to the new name...
        String oldName = node.getName();
        node.setName(newName);

        new DataNodePrefs(app.getPrefsManager(), session.project(), oldName).rename(newName);
    }

    private DataNodePrefs nodePrefs() {
        DataNodeDescriptor selected = session.getSelectedDataNode();
        if (selected == null) {
            throw new IllegalStateException("No DataNode selected");
        }
        return new DataNodePrefs(app.getPrefsManager(), session.project(), selected.getName());
    }

    private void dataSourceConfigAction() {
        new PreferenceDialog(app, app.getFrame())
                .showDBConnectorEditorAction(localDataSources.getSelectedItem());
        refreshLocalDataSources();
    }

    private void refreshLocalDataSources() {
        Map<String, DBConnector> sources = app.getDbConnectors().getAll();

        int len = sources.size();
        String[] keys = new String[len + 1];

        // a slight chance that a real datasource is called NO_LOCAL_DATA_SOURCE...
        keys[0] = NO_LOCAL_DATA_SOURCE;
        String[] dataSources = sources.keySet().toArray(new String[0]);
        System.arraycopy(dataSources, 0, keys, 1, dataSources.length);

        refreshing = true;
        try {
            localDataSources.setModel(new DefaultComboBoxModel<>(keys));
            String localDs = nodePrefs().getLocalDataSource();
            localDataSources.setSelectedItem(localDs != null ? localDs : NO_LOCAL_DATA_SOURCE);
        } finally {
            refreshing = false;
        }
    }

    private void refreshView(DataNodeDescriptor node) {
        this.node = node;

        if (node == null) {
            setVisible(false);
            return;
        }

        refreshLocalDataSources();

        dataNodeName.setText(node.getName());
        customAdapter.setText(node.getAdapterType());
        refreshing = true;
        try {
            factories.setSelectedItem(getFactoryName());
            schemaUpdateStrategy.setSelectedItem(node.getSchemaUpdateStrategyType());
        } finally {
            refreshing = false;
        }

        showDataSourceSubview(getFactoryName());
    }

    private void showDataSourceSubview(String factoryName) {
        DataSourcePanel c = datasourceEditors.get(factoryName);
        // create subview dynamically...
        if (c == null) {
            if (DataSourceFactoryType.CAYENNE.getLabel().equals(factoryName)) {
                c = new JDBCDataSourcePanel(app, nodeChangeProcessor);
            } else {
                // special case - no detail view, just show it and bail..
                defaultSubeditor.setNode(node);
                dataSourceDetailLayout.show(dataSourceDetail, "default");
                return;
            }

            datasourceEditors.put(factoryName, c);
            dataSourceDetail.add(c, factoryName);
            // needed to display freshly added panel
            dataSourceDetail.getParent().validate();
        }

        c.setNode(node);
        dataSourceDetailLayout.show(dataSourceDetail, factoryName);
    }

    enum DataSourceFactoryType {
        CAYENNE("Cayenne Data Source Factory"),
        CUSTOM("Custom Data Source Factory");
        private final String label;

        DataSourceFactoryType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
