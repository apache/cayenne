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

package org.apache.cayenne.modeler.editor;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.event.DataMapEvent;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.swing.components.JCayenneCheckBox;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.LinkDataMapAction;
import org.apache.cayenne.modeler.dialog.datamap.CatalogUpdateController;
import org.apache.cayenne.modeler.dialog.datamap.LockingUpdateController;
import org.apache.cayenne.modeler.dialog.datamap.PackageUpdateController;
import org.apache.cayenne.modeler.dialog.datamap.SchemaUpdateController;
import org.apache.cayenne.modeler.dialog.datamap.SuperclassUpdateController;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataMapDisplayListener;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.project.extension.info.ObjectInfo;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;

/**
 * Panel for editing a DataMap.
 */
public class DataMapView extends JPanel {

    protected ProjectController eventController;

    protected TextAdapter name;
    protected JLabel location;
    protected JComboBox<DataNodeDescriptor> nodeSelector;
    protected JCheckBox defaultLockType;
    protected TextAdapter defaultCatalog;
    protected TextAdapter defaultSchema;
    protected TextAdapter defaultPackage;
    protected TextAdapter defaultSuperclass;
    protected JCheckBox quoteSQLIdentifiers;

    protected TextAdapter comment;

    protected JButton updateDefaultCatalog;
    protected JButton updateDefaultSchema;
    protected JButton updateDefaultPackage;
    protected JButton updateDefaultSuperclass;
    protected JButton updateDefaultLockType;

    // client stuff
    protected JCheckBox clientSupport;

    protected JLabel defaultClientPackageLabel;
    protected JLabel defaultClientSuperclassLabel;
    protected TextAdapter defaultClientPackage;
    protected TextAdapter defaultClientSuperclass;
    protected JButton updateDefaultClientPackage;
    protected JButton updateDefaultClientSuperclass;

    public DataMapView(ProjectController eventController) {
        this.eventController = eventController;

        initView();
        initController();
    }

    private void initView() {
        // create widgets
        name = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDataMapName(text);
            }
        };

        location = new JLabel();
        nodeSelector = Application.getWidgetFactory().createUndoableComboBox();
        nodeSelector.setRenderer(CellRenderers.listRendererWithIcons());

        updateDefaultCatalog = new JButton("Update...");
        defaultCatalog = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDefaultCatalog(text);
            }
        };
        
        updateDefaultSchema = new JButton("Update...");
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

        updateDefaultPackage = new JButton("Update...");
        defaultPackage = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDefaultPackage(text);
            }
        };

        updateDefaultSuperclass = new JButton("Update...");
        defaultSuperclass = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDefaultSuperclass(text);
            }
        };

        updateDefaultLockType = new JButton("Update...");
        defaultLockType = new JCayenneCheckBox();

        clientSupport = new JCayenneCheckBox();
        updateDefaultClientPackage = new JButton("Update...");
        defaultClientPackage = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDefaultClientPackage(text);
            }
        };

        updateDefaultClientSuperclass = new JButton("Update...");
        defaultClientSuperclass = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDefaultClientSuperclass(text);
            }
        };

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

        builder.appendSeparator("Client Class Defaults");
        builder.append("Allow Client Entities:", clientSupport, new JPanel());
        defaultClientPackageLabel = builder.append(
                "Client Java Package:",
                defaultClientPackage.getComponent(),
                updateDefaultClientPackage);
        defaultClientSuperclassLabel = builder.append(
                "Custom Superclass:",
                defaultClientSuperclass.getComponent(),
                updateDefaultClientSuperclass);

        this.setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    private void initController() {
        eventController.addDataMapDisplayListener(new DataMapDisplayListener() {

            public void currentDataMapChanged(DataMapDisplayEvent e) {
                DataMap map = e.getDataMap();
                if (map != null) {
                    initFromModel(map);
                }
            }
        });

        nodeSelector.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setDataNode();
            }
        });

        quoteSQLIdentifiers.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                setQuoteSQLIdentifiers(quoteSQLIdentifiers.isSelected());
            }
        });

        defaultLockType.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                setDefaultLockType(defaultLockType.isSelected()
                        ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                        : ObjEntity.LOCK_TYPE_NONE);
            }
        });

        clientSupport.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                setClientSupport(clientSupport.isSelected());
            }
        });

        updateDefaultClientPackage.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateDefaultClientPackage();
            }
        });

        updateDefaultClientSuperclass.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateDefaultClientSuperclass();
            }
        });
        
        updateDefaultCatalog.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateDefaultCatalog();
            }
        });

        updateDefaultSchema.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateDefaultSchema();
            }
        });

        updateDefaultPackage.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateDefaultPackage();
            }
        });

        updateDefaultSuperclass.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateDefaultSuperclass();
            }
        });

        updateDefaultLockType.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateDefaultLockType();
            }
        });
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

        DataNodeDescriptor nodes[] = ((DataChannelDescriptor) eventController.getProject().getRootNode())
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

        // client defaults
        clientSupport.setSelected(map.isClientSupported());
        defaultClientPackage.setText(map.getDefaultClientPackage());
        defaultClientSuperclass.setText(map.getDefaultClientSuperclass());
        toggleClientProperties(map.isClientSupported());
    }

    private void toggleClientProperties(boolean enabled) {
        defaultClientPackage.getComponent().setEnabled(enabled);
        updateDefaultClientPackage.setEnabled(enabled);
        defaultClientPackageLabel.setEnabled(enabled);

        defaultClientSuperclassLabel.setEnabled(enabled);
        defaultClientSuperclass.getComponent().setEnabled(enabled);
        updateDefaultClientSuperclass.setEnabled(enabled);
    }

    void setDefaultLockType(int lockType) {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        int oldType = dataMap.getDefaultLockType();
        if (oldType == lockType) {
            return;
        }

        dataMap.setDefaultLockType(lockType);
        eventController.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    void setClientSupport(boolean flag) {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.isClientSupported() != flag) {
            dataMap.setClientSupported(flag);

            toggleClientProperties(flag);
            eventController.fireDataMapEvent(new DataMapEvent(this, dataMap));
        }
    }

    void setQuoteSQLIdentifiers(boolean flag) {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.isQuotingSQLIdentifiers() != flag) {
            dataMap.setQuotingSQLIdentifiers(flag);

            eventController.fireDataMapEvent(new DataMapEvent(this, dataMap));
        }
    }

    void setDefaultPackage(String newDefaultPackage) {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (newDefaultPackage != null && newDefaultPackage.trim().length() == 0) {
            newDefaultPackage = null;
        }

        String oldPackage = dataMap.getDefaultPackage();
        if (Util.nullSafeEquals(newDefaultPackage, oldPackage)) {
            return;
        }

        dataMap.setDefaultPackage(newDefaultPackage);

        // update class generation preferences
        eventController.getDataMapPreferences("").setSuperclassPackage(
                newDefaultPackage,
                DataMapDefaults.DEFAULT_SUPERCLASS_PACKAGE_SUFFIX);

        eventController.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    void setDefaultClientPackage(String newDefaultPackage) {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (newDefaultPackage != null && newDefaultPackage.trim().length() == 0) {
            newDefaultPackage = null;
        }

        String oldPackage = dataMap.getDefaultClientPackage();
        if (Util.nullSafeEquals(newDefaultPackage, oldPackage)) {
            return;
        }

        dataMap.setDefaultClientPackage(newDefaultPackage);

        // TODO: (andrus, 09/10/2005) - add the same logic for the client package
        // update class generation preferences
        // eventController.getDataMapPreferences().setSuperclassPackage(
        // newDefaultPackage,
        // DataMapDefaults.DEFAULT_SUPERCLASS_PACKAGE);

        eventController.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    void setDefaultClientSuperclass(String newSuperclass) {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (newSuperclass != null && newSuperclass.trim().length() == 0) {
            newSuperclass = null;
        }

        String oldSuperclass = dataMap.getDefaultClientSuperclass();
        if (Util.nullSafeEquals(newSuperclass, oldSuperclass)) {
            return;
        }

        dataMap.setDefaultClientSuperclass(newSuperclass);
        eventController.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }
    
    void setDefaultCatalog(String newCatalog) {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (newCatalog != null && newCatalog.trim().length() == 0) {
            newCatalog = null;
        }

        String oldCatalog = dataMap.getDefaultCatalog();
        if (Util.nullSafeEquals(newCatalog, oldCatalog)) {
            return;
        }

        dataMap.setDefaultCatalog(newCatalog);
        eventController.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    void setDefaultSchema(String newSchema) {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (newSchema != null && newSchema.trim().length() == 0) {
            newSchema = null;
        }

        String oldSchema = dataMap.getDefaultSchema();
        if (Util.nullSafeEquals(newSchema, oldSchema)) {
            return;
        }

        dataMap.setDefaultSchema(newSchema);
        eventController.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    void setDefaultSuperclass(String newSuperclass) {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (newSuperclass != null && newSuperclass.trim().length() == 0) {
            newSuperclass = null;
        }

        String oldSuperclass = dataMap.getDefaultSuperclass();
        if (Util.nullSafeEquals(newSuperclass, oldSuperclass)) {
            return;
        }

        dataMap.setDefaultSuperclass(newSuperclass);
        eventController.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    void setDataMapName(String newName) {
        if (newName == null || newName.trim().length() == 0) {
            throw new ValidationException("Enter name for DataMap");
        }

        DataMap map = eventController.getCurrentDataMap();

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
        DataMapDefaults pref = eventController.getDataMapPreferences("");
        DataMapEvent e = new DataMapEvent(this, map, map.getName());
        ProjectUtil.setDataMapName((DataChannelDescriptor) eventController
                .getProject()
                .getRootNode(), map, newName);
        pref.copyPreferences(newName);
        eventController.fireDataMapEvent(e);
    }

    void setDataNode() {
        DataNodeDescriptor node = (DataNodeDescriptor) nodeSelector.getSelectedItem();
        DataMap map = eventController.getCurrentDataMap();
        LinkDataMapAction action = eventController.getApplication().getActionManager().getAction(LinkDataMapAction.class);
        action.linkDataMap(map, node);
    }
    
    void updateDefaultCatalog() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getDbEntities().size() > 0 || dataMap.getProcedures().size() > 0) {
            new CatalogUpdateController(eventController, dataMap).startupAction();
        }
    }

    void updateDefaultSchema() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getDbEntities().size() > 0 || dataMap.getProcedures().size() > 0) {
            new SchemaUpdateController(eventController, dataMap).startupAction();
        }
    }

    void updateDefaultSuperclass() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getObjEntities().size() > 0) {
            new SuperclassUpdateController(eventController, dataMap, false).startupAction();
        }
    }

    void updateDefaultPackage() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getObjEntities().size() > 0 || dataMap.getEmbeddables().size() > 0) {
            new PackageUpdateController(eventController, dataMap, false).startupAction();
        }
    }

    void updateDefaultClientPackage() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getObjEntities().size() > 0) {
            new PackageUpdateController(eventController, dataMap, true).startupAction();
        }
    }

    void updateDefaultClientSuperclass() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getObjEntities().size() > 0) {
            new SuperclassUpdateController(eventController, dataMap, true).startupAction();
        }
    }

    void updateDefaultLockType() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getObjEntities().size() > 0) {
            new LockingUpdateController(eventController, dataMap).startup();
        }
    }

    void updateComment(String comment) {
        DataMap dataMap = eventController.getCurrentDataMap();
        if (dataMap == null) {
            return;
        }

        ObjectInfo.putToMetaData(eventController.getApplication().getMetaData(), dataMap, ObjectInfo.COMMENT, comment);
        eventController.fireDataMapEvent(new DataMapEvent(this, dataMap));
    }

    private String getComment(DataMap dataMap) {
        return ObjectInfo.getFromMetaData(eventController.getApplication().getMetaData(), dataMap, ObjectInfo.COMMENT);
    }
}
