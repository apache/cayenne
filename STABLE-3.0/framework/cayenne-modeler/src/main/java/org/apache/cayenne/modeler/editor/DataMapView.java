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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.DataMapEvent;
import org.apache.cayenne.map.event.DataNodeEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.datamap.LockingUpdateController;
import org.apache.cayenne.modeler.dialog.datamap.PackageUpdateController;
import org.apache.cayenne.modeler.dialog.datamap.SchemaUpdateController;
import org.apache.cayenne.modeler.dialog.datamap.SuperclassUpdateController;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.event.DataMapDisplayListener;
import org.apache.cayenne.modeler.pref.DataMapDefaults;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.project.ApplicationProject;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Panel for editing a DataMap.
 */
public class DataMapView extends JPanel {

    protected ProjectController eventController;

    protected TextAdapter name;
    protected JLabel location;
    protected JComboBox nodeSelector;
    protected JCheckBox defaultLockType;
    protected TextAdapter defaultSchema;
    protected TextAdapter defaultPackage;
    protected TextAdapter defaultSuperclass;
    protected JCheckBox quoteSQLIdentifiers;

    protected JButton updateDefaultSchema;
    protected JButton updateDefaultPackage;
    protected JButton updateDefaultSuperclass;
    protected JButton updateDefaultLockType;

    // client stuff
    protected JCheckBox clientSupport;

    protected JLabel defaultClientPackageLabel;
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
        nodeSelector = CayenneWidgetFactory.createUndoableComboBox();
        nodeSelector.setRenderer(CellRenderers.listRendererWithIcons());

        updateDefaultSchema = new JButton("Update...");
        defaultSchema = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDefaultSchema(text);
            }
        };
        
        quoteSQLIdentifiers = new JCheckBox();

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
        defaultLockType = new JCheckBox();

        clientSupport = new JCheckBox();
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
                "right:70dlu, 3dlu, fill:110dlu, 3dlu, fill:100",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("DataMap Configuration");
        builder.append("DataMap Name:", name.getComponent(), 3);
        builder.append("File:", location, 3);
        builder.append("DataNode:", nodeSelector, 3);
        builder.append("Quote SQL Identifiers:", quoteSQLIdentifiers, 3);

        builder.appendSeparator("Entity Defaults");
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
        builder.append(
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
        
        quoteSQLIdentifiers.addActionListener(new ActionListener(){

            public void actionPerformed(ActionEvent e) {
                setQuoteSQLIdentifiers(quoteSQLIdentifiers.isSelected());
            }
        });

        defaultLockType.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setDefaultLockType(defaultLockType.isSelected()
                        ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                        : ObjEntity.LOCK_TYPE_NONE);
            }
        });

        clientSupport.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
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
        String locationText = map.getLocation();
        location.setText((locationText != null) ? locationText : "(no file)");

        quoteSQLIdentifiers.setSelected(map.isQuotingSQLIdentifiers());
        // rebuild data node list
        Object nodes[] = eventController.getCurrentDataDomain().getDataNodes().toArray();

        // add an empty item to the front
        Object[] objects = new Object[nodes.length + 1];
        // objects[0] = null;

        // now add the entities
        if (nodes.length > 0) {
            Arrays.sort(nodes, Comparators.getNamedObjectComparator());
            System.arraycopy(nodes, 0, objects, 1, nodes.length);
        }

        DefaultComboBoxModel model = new DefaultComboBoxModel(objects);

        // find selected node
        for (int i = 0; i < nodes.length; i++) {
            DataNode node = (DataNode) nodes[i];
            if (node.getDataMaps().contains(map)) {
                model.setSelectedItem(node);
                break;
            }
        }

        nodeSelector.setModel(model);

        // init default fields
        defaultLockType.setSelected(map.getDefaultLockType() != ObjEntity.LOCK_TYPE_NONE);
        defaultPackage.setText(map.getDefaultPackage());
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
        Configuration config = ((ApplicationProject) Application.getProject())
                .getConfiguration();

        DataMap matchingMap = null;

        for (DataDomain domain : config.getDomains()) {
            DataMap nextMap = domain.getMap(newName);

            if (nextMap == map) {
                continue;
            }

            if (nextMap != null) {
                matchingMap = nextMap;
                break;
            }
        }

        if (matchingMap != null) {

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
        ProjectUtil.setDataMapName(eventController.getCurrentDataDomain(), map, newName);
        pref.rename(newName);
        eventController.fireDataMapEvent(e);
    }

    void setDataNode() {
        DataNode node = (DataNode) nodeSelector.getSelectedItem();
        DataMap map = eventController.getCurrentDataMap();

        // no change?
        if (node != null && node.getDataMaps().contains(map)) {
            return;
        }

        boolean hasChanges = false;

        // unlink map from any nodes

        for (DataNode nextNode : eventController.getCurrentDataDomain().getDataNodes()) {

            // Theoretically only one node may contain a datamap at each given time.
            // Being paranoid, we will still scan through all.
            if (nextNode != node && nextNode.getDataMaps().contains(map)) {
                nextNode.removeDataMap(map.getName());

                // announce DataNode change
                eventController.fireDataNodeEvent(new DataNodeEvent(this, nextNode));

                hasChanges = true;
            }
        }

        // link to a selected node
        if (node != null) {
            node.addDataMap(map);
            hasChanges = true;

            // announce DataNode change
            eventController.fireDataNodeEvent(new DataNodeEvent(this, node));
        }

        if (hasChanges) {
            // TODO: maybe reindexing is an overkill in the modeler?
            eventController.getCurrentDataDomain().reindexNodes();
        }
    }

    void updateDefaultSchema() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getDbEntities().size() > 0 || dataMap.getProcedures().size() > 0) {
            new SchemaUpdateController(eventController, dataMap).startup();
        }
    }

    void updateDefaultSuperclass() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getObjEntities().size() > 0) {
            new SuperclassUpdateController(eventController, dataMap, false).startup();
        }
    }

    void updateDefaultPackage() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getObjEntities().size() > 0 || dataMap.getEmbeddables().size() > 0) {
            new PackageUpdateController(eventController, dataMap, false).startup();
        }
    }

    void updateDefaultClientPackage() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getObjEntities().size() > 0) {
            new PackageUpdateController(eventController, dataMap, true).startup();
        }
    }

    void updateDefaultClientSuperclass() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getObjEntities().size() > 0) {
            new SuperclassUpdateController(eventController, dataMap, true).startup();
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
}
