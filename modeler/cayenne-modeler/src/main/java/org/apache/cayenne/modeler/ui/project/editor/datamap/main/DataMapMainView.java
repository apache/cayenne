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
import org.apache.cayenne.modeler.event.model.DataMapEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.action.LinkDataMapAction;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.catalog.CatalogUpdateController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.locking.LockingUpdateController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.pkg.PackageUpdateController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.schema.SchemaUpdateController;
import org.apache.cayenne.modeler.ui.project.editor.datamap.main.superclass.SuperclassUpdateController;
import org.apache.cayenne.modeler.event.model.ProjectSavedEvent;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.modeler.swing.JCayenneCheckBox;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Panel for editing a DataMap.
 */
public class DataMapMainView extends JPanel {

    private final ProjectController controller;

    private final TextAdapter name;
    private final JLabel location;
    private final JComboBox<DataNodeDescriptor> nodeSelector;
    private final JCheckBox defaultLockType;
    private final TextAdapter defaultCatalog;
    private final TextAdapter defaultSchema;
    private final TextAdapter defaultPackage;
    private final TextAdapter defaultSuperclass;
    private final JCheckBox quoteSQLIdentifiers;

    private final TextAdapter comment;

    public DataMapMainView(ProjectController controller) {
        this.controller = controller;

        // create widgets
        name = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDataMapName(text);
            }
        };

        location = new JLabel();
        nodeSelector = Application.getWidgetFactory().createUndoableComboBox();
        nodeSelector.setRenderer(CellRenderers.listRendererWithIcons());

        JButton updateDefaultCatalog = new JButton("Update...");
        defaultCatalog = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDefaultCatalog(text);
            }
        };

        JButton updateDefaultSchema = new JButton("Update...");
        defaultSchema = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDefaultSchema(text);
            }
        };

        quoteSQLIdentifiers = new JCayenneCheckBox();

        comment = new TextAdapter(new JTextField()) {
            @Override
            protected void updateModel(String text) throws ValidationException {
                updateComment(text);
            }
        };

        JButton updateDefaultPackage = new JButton("Update...");
        defaultPackage = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDefaultPackage(text);
            }
        };

        JButton updateDefaultSuperclass = new JButton("Update...");
        defaultSuperclass = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDefaultSuperclass(text);
            }
        };

        JButton updateDefaultLockType = new JButton("Update...");
        defaultLockType = new JCayenneCheckBox();

        // assemble
        FormLayout layout = new FormLayout(
                "right:70dlu, 3dlu, fill:180dlu, 3dlu, fill:120",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("DataMap Configuration");
        builder.append("DataMap Name:", name.getComponent(), 2);
        builder.append("File:", location, 3);
        builder.append("DataNode:", nodeSelector, 2);
        builder.append("Quote SQL Identifiers:", quoteSQLIdentifiers, 3);
        builder.append("Comment:", comment.getComponent(), 2);

        builder.appendSeparator("Entity Defaults");
        builder.append("DB Catalog:", defaultCatalog.getComponent(), updateDefaultCatalog);
        builder.append("DB Schema:", defaultSchema.getComponent(), updateDefaultSchema);
        builder.append(
                "Java Package:",
                defaultPackage.getComponent(),
                updateDefaultPackage);
        builder.append(
                "Custom Superclass:",
                defaultSuperclass.getComponent(),
                updateDefaultSuperclass);
        builder.append("Optimistic Locking:", defaultLockType, updateDefaultLockType);

        this.setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        controller.addDataMapDisplayListener(e -> {
            DataMap map = e.getDataMap();
            if (map != null) {
                initFromModel(map);
            }
        });

        controller.addProjectSavedListener(this::updateNamesAfterSaving);

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
    }

    /**
     * Updates the view from the current model state. Invoked when a currently displayed
     * ObjEntity is changed.
     */
    private void initFromModel(DataMap map) {
        name.setText(map.getName());
        location.setText((map.getLocation() != null) ? map.getLocation() : "(no file)");
        quoteSQLIdentifiers.setSelected(map.isQuotingSQLIdentifiers());
        comment.setText(getComment(map));

        // rebuild data node list

        DataNodeDescriptor[] nodes = ((DataChannelDescriptor) controller.getProject().getRootNode())
                .getNodeDescriptors().toArray(new DataNodeDescriptor[0]);

        // add an empty item to the front
        DataNodeDescriptor[] objects = new DataNodeDescriptor[nodes.length + 1];

        // now add the entities
        if (nodes.length > 0) {
            Arrays.sort(nodes, Comparators.getNamedObjectComparator());
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

        // init default fields
        defaultLockType.setSelected(map.getDefaultLockType() != ObjEntity.LOCK_TYPE_NONE);
        defaultPackage.setText(map.getDefaultPackage());
        defaultCatalog.setText(map.getDefaultCatalog());
        defaultSchema.setText(map.getDefaultSchema());
        defaultSuperclass.setText(map.getDefaultSuperclass());
    }

    void setDefaultLockType(int lockType) {
        DataMap dataMap = controller.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        int oldType = dataMap.getDefaultLockType();
        if (oldType == lockType) {
            return;
        }

        dataMap.setDefaultLockType(lockType);
        controller.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    void setQuoteSQLIdentifiers(boolean flag) {
        DataMap dataMap = controller.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.isQuotingSQLIdentifiers() != flag) {
            dataMap.setQuotingSQLIdentifiers(flag);

            controller.fireDataMapEvent(new DataMapEvent(this, dataMap));
        }
    }

    void setDefaultPackage(String newDefaultPackage) {
        DataMap dataMap = controller.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (newDefaultPackage != null && newDefaultPackage.trim().isEmpty()) {
            newDefaultPackage = null;
        }

        String oldPackage = dataMap.getDefaultPackage();
        if (Util.nullSafeEquals(newDefaultPackage, oldPackage)) {
            return;
        }

        dataMap.setDefaultPackage(newDefaultPackage);

        // update class generation preferences
        controller.getSelectedDataMapPreferences("").setSuperclassPackage(
                newDefaultPackage,
                DataMapDefaults.DEFAULT_SUPERCLASS_PACKAGE_SUFFIX);

        controller.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    void setDefaultCatalog(String newCatalog) {
        DataMap dataMap = controller.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (newCatalog != null && newCatalog.trim().isEmpty()) {
            newCatalog = null;
        }

        String oldCatalog = dataMap.getDefaultCatalog();
        if (Util.nullSafeEquals(newCatalog, oldCatalog)) {
            return;
        }

        dataMap.setDefaultCatalog(newCatalog);
        controller.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    void setDefaultSchema(String newSchema) {
        DataMap dataMap = controller.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (newSchema != null && newSchema.trim().isEmpty()) {
            newSchema = null;
        }

        String oldSchema = dataMap.getDefaultSchema();
        if (Util.nullSafeEquals(newSchema, oldSchema)) {
            return;
        }

        dataMap.setDefaultSchema(newSchema);
        controller.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    void setDefaultSuperclass(String newSuperclass) {
        DataMap dataMap = controller.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (newSuperclass != null && newSuperclass.trim().isEmpty()) {
            newSuperclass = null;
        }

        String oldSuperclass = dataMap.getDefaultSuperclass();
        if (Util.nullSafeEquals(newSuperclass, oldSuperclass)) {
            return;
        }

        dataMap.setDefaultSuperclass(newSuperclass);
        controller.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    void setDataMapName(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new ValidationException("Enter name for DataMap");
        }

        DataMap map = controller.getSelectedDataMap();

        // search for matching map name across domains, as currently they have to be
        // unique globally
        DataChannelDescriptor dataChannelDescriptor = (DataChannelDescriptor) Application
                .getInstance()
                .getProject()
                .getRootNode();

        DataMap matchingMap = dataChannelDescriptor.getDataMap(newName);

        if (matchingMap != null && !matchingMap.equals(map)) {

            // there is an entity with the same name
            throw new ValidationException("There is another DataMap named '"
                    + newName
                    + "'. Use a different name.");
        }
        String oldName = map.getName();
        if (Util.nullSafeEquals(newName, oldName)) {
            return;
        }
        // completely new name, set new name for domain
        DataMapDefaults pref = controller.getSelectedDataMapPreferences("");
        DataMapEvent e = new DataMapEvent(this, map, oldName);
        DataChannelDescriptor domain = (DataChannelDescriptor) controller.getProject().getRootNode();

        // must fully relink renamed map across node descriptors
        List<DataNodeDescriptor> nodesUsingMap = new ArrayList<>();
        for (DataNodeDescriptor node : domain.getNodeDescriptors()) {
            if (node.getDataMapNames().contains(oldName)) {
                nodesUsingMap.add(node);
            }
        }
        map.setName(newName);
        for (DataNodeDescriptor node : nodesUsingMap) {
            node.getDataMapNames().remove(oldName);
            node.getDataMapNames().add(newName);
        }

        pref.copyPreferences(newName);
        controller.fireDataMapEvent(e);
    }

    void setDataNode() {
        DataNodeDescriptor node = (DataNodeDescriptor) nodeSelector.getSelectedItem();
        DataMap map = controller.getSelectedDataMap();
        LinkDataMapAction action = controller.getApplication().getActionManager().getAction(LinkDataMapAction.class);
        action.linkDataMap(map, node);
    }

    void updateDefaultCatalog() {
        DataMap dataMap = controller.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (!dataMap.getDbEntities().isEmpty() || !dataMap.getProcedures().isEmpty()) {
            new CatalogUpdateController(controller, dataMap).startupAction();
        }
    }

    void updateDefaultSchema() {
        DataMap dataMap = controller.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (!dataMap.getDbEntities().isEmpty() || !dataMap.getProcedures().isEmpty()) {
            new SchemaUpdateController(controller, dataMap).startupAction();
        }
    }

    void updateDefaultSuperclass() {
        DataMap dataMap = controller.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (!dataMap.getObjEntities().isEmpty()) {
            new SuperclassUpdateController(controller, dataMap).startupAction();
        }
    }

    void updateDefaultPackage() {
        DataMap dataMap = controller.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (!dataMap.getObjEntities().isEmpty() || !dataMap.getEmbeddables().isEmpty()) {
            new PackageUpdateController(controller, dataMap).startupAction();
        }
    }

    void updateDefaultLockType() {
        DataMap dataMap = controller.getSelectedDataMap();

        if (dataMap == null) {
            return;
        }

        if (!dataMap.getObjEntities().isEmpty()) {
            new LockingUpdateController(controller, dataMap).startup();
        }
    }

    void updateComment(String comment) {
        DataMap dataMap = controller.getSelectedDataMap();
        if (dataMap == null) {
            return;
        }
        String currentComment = getComment(dataMap);
        if (currentComment == null) {
            currentComment = "";
        }
        if (!currentComment.equals(comment)) {
            ObjectInfo.putToMetaData(controller.getApplication().getMetaData(), dataMap, ObjectInfo.COMMENT, comment);
            controller.fireDataMapEvent(new DataMapEvent(this, dataMap));
        }
    }

    private String getComment(DataMap dataMap) {
        return ObjectInfo.getFromMetaData(controller.getApplication().getMetaData(), dataMap, ObjectInfo.COMMENT);
    }

    public void updateNamesAfterSaving(ProjectSavedEvent e) {
        DataMap currentDataMap = controller.getSelectedDataMap();
        if (currentDataMap != null && !currentDataMap.getLocation().equals(location.getText())) {
            location.setText(currentDataMap.getLocation());
        }
    }
}
