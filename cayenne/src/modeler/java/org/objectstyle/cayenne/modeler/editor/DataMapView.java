/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.DataMapEvent;
import org.objectstyle.cayenne.map.event.DataNodeEvent;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.dialog.datamap.PackageUpdateController;
import org.objectstyle.cayenne.modeler.dialog.datamap.SchemaUpdateController;
import org.objectstyle.cayenne.modeler.dialog.datamap.SuperclassUpdateController;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayListener;
import org.objectstyle.cayenne.modeler.pref.DataMapDefaults;
import org.objectstyle.cayenne.modeler.util.CayenneWidgetFactory;
import org.objectstyle.cayenne.modeler.util.CellRenderers;
import org.objectstyle.cayenne.modeler.util.Comparators;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.modeler.util.TextAdapter;
import org.objectstyle.cayenne.project.ApplicationProject;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.validation.ValidationException;

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
    protected TextAdapter defaultSchema;
    protected TextAdapter defaultPackage;
    protected TextAdapter defaultSuperclass;
    protected JCheckBox defaultLockType;
    protected JButton updateDefaultSchema;
    protected JButton updateDefaultPackage;
    protected JButton updateDefaultSuperclass;
    protected JButton updateDefaultLockType;

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

        location = CayenneWidgetFactory.createLabel("");
        nodeSelector = CayenneWidgetFactory.createComboBox();
        nodeSelector.setRenderer(CellRenderers.listRendererWithIcons());

        updateDefaultSchema = new JButton("Update...");
        defaultSchema = new TextAdapter(new JTextField()) {

            protected void updateModel(String text) {
                setDefaultSchema(text);
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

        updateDefaultLockType = new JButton("Update");
        defaultLockType = new JCheckBox();

        // assemble
        FormLayout layout = new FormLayout(
                "right:max(50dlu;pref), 3dlu, fill:max(110dlu;pref), 3dlu, fill:90",
                "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();

        builder.appendSeparator("DataMap Configuration");
        builder.append("DataMap Name:", name.getComponent(), 3);
        builder.append("File:", location, 3);
        builder.append("DataNode:", nodeSelector, 3);

        builder.appendSeparator("Entity Defaults");
        builder.append(
                "DB Schema:",
                defaultSchema.getComponent(),
                updateDefaultSchema);
        builder.append(
                "Java Package:",
                defaultPackage.getComponent(),
                updateDefaultPackage);
        builder.append(
                "DataObject Superclass:",
                defaultSuperclass.getComponent(),
                updateDefaultSuperclass);
        builder.append("Optimistic Locking:", defaultLockType, updateDefaultLockType);

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

        defaultLockType.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setDefaultLockType(defaultLockType.isSelected()
                        ? ObjEntity.LOCK_TYPE_OPTIMISTIC
                        : ObjEntity.LOCK_TYPE_NONE);
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

        Iterator it = config.getDomains().iterator();
        while (it.hasNext()) {
            DataDomain domain = (DataDomain) it.next();
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

        // completely new name, set new name for domain
        DataMapDefaults pref = eventController.getDataMapPreferences();
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
        Iterator nodes = eventController.getCurrentDataDomain().getDataNodes().iterator();

        while (nodes.hasNext()) {
            DataNode nextNode = (DataNode) nodes.next();

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
            new SuperclassUpdateController(eventController, dataMap).startup();
        }
    }

    void updateDefaultPackage() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getObjEntities().size() > 0) {
            new PackageUpdateController(eventController, dataMap).startup();
        }
    }

    void updateDefaultLockType() {
        DataMap dataMap = eventController.getCurrentDataMap();

        if (dataMap == null) {
            return;
        }

        if (dataMap.getObjEntities().size() > 0) {
            int defaultLockType = dataMap.getDefaultLockType();

            Iterator it = dataMap.getObjEntities().iterator();
            while (it.hasNext()) {
                ObjEntity entity = (ObjEntity) it.next();
                if (defaultLockType != entity.getDeclaredLockType()) {
                    entity.setDeclaredLockType(defaultLockType);

                    // any way to batch events, a big change will flood the app with
                    // entity events..?
                    eventController.fireDbEntityEvent(new EntityEvent(this, entity));
                }
            }
        }
    }
}