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

package org.apache.cayenne.modeler.ui.project.editor.datamap.main;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.modeler.pref.adapters.DataMapPrefs;
import org.apache.cayenne.modeler.project.ProjectComparators;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.toolkit.ProjectPanel;
import org.apache.cayenne.modeler.toolkit.Renderers;
import org.apache.cayenne.modeler.toolkit.checkbox.CMCheckBox;
import org.apache.cayenne.modeler.toolkit.combobox.CMUndoableComboBox;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.ui.action.LinkDataMapAction;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.catalog.CatalogUpdateDialog;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.locking.LockingUpdateDialog;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.pkg.PackageUpdateDialog;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.schema.SchemaUpdateDialog;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.superclass.SuperclassUpdateDialog;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import java.util.Objects;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Panel for editing a DataMap.
 */
public class DataMapMainView extends ProjectPanel {

    private final CMUndoableTextField name;
    private final JLabel nodeSelectorLabel;
    private final JComboBox<DataNodeDescriptor> nodeSelector;
    private final JCheckBox defaultLockType;
    private final CMUndoableTextField defaultCatalog;
    private final CMUndoableTextField defaultSchema;
    private final CMUndoableTextField defaultPackage;
    private final CMUndoableTextField defaultSuperclass;
    private final JCheckBox quoteSQLIdentifiers;
    private final CMUndoableTextField comment;
    private final JButton updateDefaultCatalog;
    private final JButton updateDefaultSchema;
    private final JButton updateDefaultPackage;
    private final JButton updateDefaultSuperclass;
    private final JButton updateDefaultLockType;

    public DataMapMainView(ProjectSession session) {
        super(session);
        name = new CMUndoableTextField(app.getUndoManager());
        nodeSelectorLabel = new JLabel("DataNode:");
        nodeSelector = new CMUndoableComboBox<>(app.getUndoManager());
        defaultCatalog = new CMUndoableTextField(app.getUndoManager());
        defaultSchema = new CMUndoableTextField(app.getUndoManager());
        quoteSQLIdentifiers = new CMCheckBox(app.getUndoManager());
        comment = new CMUndoableTextField(app.getUndoManager());
        defaultPackage = new CMUndoableTextField(app.getUndoManager());
        defaultSuperclass = new CMUndoableTextField(app.getUndoManager());
        defaultLockType = new CMCheckBox(app.getUndoManager());
        updateDefaultCatalog = new JButton("Update...");
        updateDefaultSchema = new JButton("Update...");
        updateDefaultPackage = new JButton("Update...");
        updateDefaultSuperclass = new JButton("Update...");
        updateDefaultLockType = new JButton("Update...");
        initLayout();
        initBindings();
    }

    private void initLayout() {
        nodeSelector.setRenderer(Renderers.listRendererWithIcons());

        FormLayout layout = new FormLayout(
                "right:70dlu, 3dlu, fill:180dlu, 3dlu, fill:120",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("DataMap Configuration");
        builder.append("DataMap Name:", name, 2);
        builder.append("Quote SQL Identifiers:", quoteSQLIdentifiers, 3);
        builder.append("Comment:", comment, 2);

        builder.appendSeparator("Entity Defaults");
        builder.append("DB Catalog:", defaultCatalog, updateDefaultCatalog);
        builder.append("DB Schema:", defaultSchema, updateDefaultSchema);
        builder.append("Java Package:", defaultPackage, updateDefaultPackage);
        builder.append("Custom Superclass:", defaultSuperclass, updateDefaultSuperclass);
        builder.append("Optimistic Locking:", defaultLockType, updateDefaultLockType);

        builder.appendSeparator("Linked DataNode");
        builder.append(nodeSelectorLabel);
        builder.append(nodeSelector, 2);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initBindings() {
        name.addCommitListener(this::setDataMapName);
        defaultCatalog.addCommitListener(this::setDefaultCatalog);
        defaultSchema.addCommitListener(this::setDefaultSchema);
        comment.addCommitListener(this::updateComment);
        defaultPackage.addCommitListener(this::setDefaultPackage);
        defaultSuperclass.addCommitListener(this::setDefaultSuperclass);
        nodeSelector.addActionListener(e -> setDataNode());
        quoteSQLIdentifiers.addItemListener(e -> setQuoteSQLIdentifiers(quoteSQLIdentifiers.isSelected()));
        defaultLockType.addItemListener(e -> setDefaultLockType(defaultLockType.isSelected()
                ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                : ObjEntity.LOCK_TYPE_NONE));
        updateDefaultCatalog.addActionListener(e -> updateDefaultCatalog());
        updateDefaultSchema.addActionListener(e -> updateDefaultSchema());
        updateDefaultPackage.addActionListener(e -> updateDefaultPackage());
        updateDefaultSuperclass.addActionListener(e -> updateDefaultSuperclass());
        updateDefaultLockType.addActionListener(e -> updateDefaultLockType());
        session.addDataMapDisplayListener(e -> {
            DataMap map = e.getDataMap();
            if (map != null) {
                initFromModel(map);
            }
        });
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * ObjEntity is changed.
     */
    private void initFromModel(DataMap map) {
        name.setText(map.getName());
        quoteSQLIdentifiers.setSelected(map.isQuotingSQLIdentifiers());
        comment.setText(getComment(map));

        // rebuild data node list

        DataNodeDescriptor[] nodes = ((DataChannelDescriptor) session.project().getRootNode())
                .getNodeDescriptors().toArray(new DataNodeDescriptor[0]);

        // add an empty item to the front
        DataNodeDescriptor[] objects = new DataNodeDescriptor[nodes.length + 1];

        // now add the entities
        if (nodes.length > 0) {
            Arrays.sort(nodes, ProjectComparators.forNamedObjects());
            System.arraycopy(nodes, 0, objects, 1, nodes.length);
        }

        DefaultComboBoxModel<DataNodeDescriptor> model = new DefaultComboBoxModel<>(objects);

        // find selected node
        for (DataNodeDescriptor node : nodes) {
            if (node.getDataMapNames().contains(map.getName())) {
                model.setSelectedItem(node);
                break;
            }
        }

        nodeSelector.setModel(model);
        boolean hasNodes = nodes.length > 0;
        nodeSelector.setEnabled(hasNodes);
        nodeSelectorLabel.setEnabled(hasNodes);

        // init default fields
        defaultLockType.setSelected(map.getDefaultLockType() != ObjEntity.LOCK_TYPE_NONE);
        defaultPackage.setText(map.getDefaultPackage());
        defaultCatalog.setText(map.getDefaultCatalog());
        defaultSchema.setText(map.getDefaultSchema());
        defaultSuperclass.setText(map.getDefaultSuperclass());
    }

    void setDefaultLockType(int lockType) {
        DataMap dataMap = session.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        int oldType = dataMap.getDefaultLockType();
        if (oldType == lockType) {
            return;
        }

        dataMap.setDefaultLockType(lockType);
        session.fireDataMapEvent(DataMapEvent.ofChange(this, dataMap));
    }

    void setQuoteSQLIdentifiers(boolean flag) {
        DataMap dataMap = session.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.isQuotingSQLIdentifiers() != flag) {
            dataMap.setQuotingSQLIdentifiers(flag);

            session.fireDataMapEvent(DataMapEvent.ofChange(this, dataMap));
        }
    }

    void setDefaultPackage(String newDefaultPackage) {
        DataMap dataMap = session.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (newDefaultPackage != null && newDefaultPackage.trim().isEmpty()) {
            newDefaultPackage = null;
        }

        String oldPackage = dataMap.getDefaultPackage();
        if (Objects.equals(newDefaultPackage, oldPackage)) {
            return;
        }

        dataMap.setDefaultPackage(newDefaultPackage);

        // update class generation preferences
        new DataMapPrefs(app.getPrefsManager().dataMapPref(dataMap, null))
                .setSuperclassPackage(newDefaultPackage, DataMapPrefs.DEFAULT_SUPERCLASS_PACKAGE_SUFFIX);

        session.fireDataMapEvent(DataMapEvent.ofChange(this, dataMap));
    }

    void setDefaultCatalog(String newCatalog) {
        DataMap dataMap = session.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (newCatalog != null && newCatalog.trim().isEmpty()) {
            newCatalog = null;
        }

        String oldCatalog = dataMap.getDefaultCatalog();
        if (Objects.equals(newCatalog, oldCatalog)) {
            return;
        }

        dataMap.setDefaultCatalog(newCatalog);
        session.fireDataMapEvent(DataMapEvent.ofChange(this, dataMap));
    }

    void setDefaultSchema(String newSchema) {
        DataMap dataMap = session.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (newSchema != null && newSchema.trim().isEmpty()) {
            newSchema = null;
        }

        String oldSchema = dataMap.getDefaultSchema();
        if (Objects.equals(newSchema, oldSchema)) {
            return;
        }

        dataMap.setDefaultSchema(newSchema);
        session.fireDataMapEvent(DataMapEvent.ofChange(this, dataMap));
    }

    void setDefaultSuperclass(String newSuperclass) {
        DataMap dataMap = session.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (newSuperclass != null && newSuperclass.trim().isEmpty()) {
            newSuperclass = null;
        }

        String oldSuperclass = dataMap.getDefaultSuperclass();
        if (Objects.equals(newSuperclass, oldSuperclass)) {
            return;
        }

        dataMap.setDefaultSuperclass(newSuperclass);
        session.fireDataMapEvent(DataMapEvent.ofChange(this, dataMap));
    }

    void setDataMapName(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new ValidationException("Enter name for DataMap");
        }

        DataMap map = session.getSelectedDataMap();

        // search for matching map name across domains, as currently they have to be
        // unique globally
        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) session.project().getRootNode();

        DataMap matchingMap = dataChannelDescriptor.getDataMap(newName);

        if (matchingMap != null && !matchingMap.equals(map)) {

            // there is an entity with the same name
            throw new ValidationException("There is another DataMap named '"
                    + newName
                    + "'. Use a different name.");
        }
        String oldName = map.getName();
        if (Objects.equals(newName, oldName)) {
            return;
        }

        // completely new name, set new name for domain
        DataMapEvent e = DataMapEvent.ofChange(this, map, oldName);
        DataChannelDescriptor domain = (DataChannelDescriptor) session.project().getRootNode();

        // must fully relink renamed map across node descriptors
        List<DataNodeDescriptor> nodesUsingMap = new ArrayList<>();
        for (DataNodeDescriptor node : domain.getNodeDescriptors()) {
            if (node.getDataMapNames().contains(oldName)) {
                nodesUsingMap.add(node);
            }
        }
        app.getPrefsManager().stageDataMapRename(map, newName);
        map.setName(newName);
        for (DataNodeDescriptor node : nodesUsingMap) {
            node.getDataMapNames().remove(oldName);
            node.getDataMapNames().add(newName);
        }

        session.fireDataMapEvent(e);
    }

    void setDataNode() {
        DataNodeDescriptor node = (DataNodeDescriptor) nodeSelector.getSelectedItem();
        DataMap map = session.getSelectedDataMap();
        LinkDataMapAction action = app.getActionManager().getAction(LinkDataMapAction.class);
        action.linkDataMap(map, node);
    }

    void updateDefaultCatalog() {
        DataMap dataMap = session.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (!dataMap.getDbEntities().isEmpty() || !dataMap.getProcedures().isEmpty()) {
            new CatalogUpdateDialog(session, app.getFrame(), dataMap).open();
        }
    }

    void updateDefaultSchema() {
        DataMap dataMap = session.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (!dataMap.getDbEntities().isEmpty() || !dataMap.getProcedures().isEmpty()) {
            new SchemaUpdateDialog(session, app.getFrame(), dataMap).open();
        }
    }

    void updateDefaultSuperclass() {
        DataMap dataMap = session.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (!dataMap.getObjEntities().isEmpty()) {
            new SuperclassUpdateDialog(session, app.getFrame(), dataMap).open();
        }
    }

    void updateDefaultPackage() {
        DataMap dataMap = session.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (!dataMap.getObjEntities().isEmpty() || !dataMap.getEmbeddables().isEmpty()) {
            new PackageUpdateDialog(session, app.getFrame(), dataMap).open();
        }
    }

    void updateDefaultLockType() {
        DataMap dataMap = session.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (!dataMap.getObjEntities().isEmpty()) {
            new LockingUpdateDialog(session, app.getFrame(), dataMap).open();
        }
    }

    void updateComment(String comment) {
        DataMap dataMap = session.getSelectedDataMap();
        if (dataMap == null) {
            return;
        }
        String currentComment = getComment(dataMap);
        if (currentComment == null) {
            currentComment = "";
        }
        if (!currentComment.equals(comment)) {
            ObjectInfo.putToMetaData(app.getMetaData(), dataMap, ObjectInfo.COMMENT, comment);
            session.fireDataMapEvent(DataMapEvent.ofChange(this, dataMap));
        }
    }

    private String getComment(DataMap dataMap) {
        return ObjectInfo.getFromMetaData(app.getMetaData(), dataMap, ObjectInfo.COMMENT);
    }
}
