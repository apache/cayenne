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
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.map.event.ObjRelationshipListener;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.dialog.objentity.ObjRelationshipInfoController;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Displays ObjRelationships for the edited ObjEntity.
 * 
 * @author Michael Misha Shengaout
 * @author Andrus Adamchik
 */
public class ObjEntityRelationshipTab extends JPanel implements ObjEntityDisplayListener,
        ObjEntityListener, ObjRelationshipListener, ExistingSelectionProcessor {

    private static Log logObj = LogFactory.getLog(ObjEntityRelationshipTab.class);

    private static final Object[] deleteRules = new Object[] {
            DeleteRule.deleteRuleName(DeleteRule.NO_ACTION),
            DeleteRule.deleteRuleName(DeleteRule.NULLIFY),
            DeleteRule.deleteRuleName(DeleteRule.CASCADE),
            DeleteRule.deleteRuleName(DeleteRule.DENY),
    };

    ProjectController mediator;

    CayenneTable table;
    JButton resolve;

    public ObjEntityRelationshipTab(ProjectController mediator) {
        this.mediator = mediator;

        init();
        initController();
    }

    private void init() {
        this.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        Application app = Application.getInstance();
        toolBar
                .add(app
                        .getAction(CreateRelationshipAction.getActionName())
                        .buildButton());
        toolBar.add(app.getAction(ObjEntitySyncAction.getActionName()).buildButton());

        toolBar.addSeparator();

        resolve = new JButton();
        resolve.setIcon(ModelerUtil.buildIcon("icon-info.gif"));
        resolve.setToolTipText("Edit Relationship");
        toolBar.add(resolve);

        toolBar.addSeparator();

        toolBar
                .add(app
                        .getAction(RemoveRelationshipAction.getActionName())
                        .buildButton());
        add(toolBar, BorderLayout.NORTH);

        table = new CayenneTable();
        table.setDefaultRenderer(String.class, new StringRenderer());
        table.setDefaultRenderer(ObjEntity.class, new EntityRenderer());

        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);
    }

    private void initController() {
        mediator.addObjEntityDisplayListener(this);
        mediator.addObjEntityListener(this);
        mediator.addObjRelationshipListener(this);

        resolve.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    return;
                }

                ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                        .getModel();
                new ObjRelationshipInfoController(mediator, model.getRelationship(row))
                        .startup();

                // need to refresh selected row... do this by unselecting/selecting the
                // row
                table.getSelectionModel().clearSelection();
                table.select(row);
            }
        });

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                processExistingSelection(e);
            }
        });
    }

    /**
     * Selects a specified relationship in the relationships table.
     */
    public void selectRelationship(ObjRelationship rel) {
        if (rel == null) {
            Application
                    .getInstance()
                    .getAction(RemoveRelationshipAction.getActionName())
                    .setEnabled(false);
            return;
        }
        // enable the remove button
        Application
                .getInstance()
                .getAction(RemoveRelationshipAction.getActionName())
                .setEnabled(true);

        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        java.util.List rels = model.getObjectList();
        int relPos = rels.indexOf(rel);
        if (relPos >= 0) {
            table.select(relPos);
        }
    }

    public void processExistingSelection(EventObject e) {
        if (e instanceof ChangeEvent) {
            table.clearSelection();
        }
        ObjRelationship rel = null;
        if (table.getSelectedRow() >= 0) {
            ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                    .getModel();
            rel = model.getRelationship(table.getSelectedRow());
            if (rel.getTargetEntity() != null
                    && ((ObjEntity) rel.getSourceEntity()).getDbEntity() != null
                    && ((ObjEntity) rel.getTargetEntity()).getDbEntity() != null) {
                resolve.setEnabled(true);
            }
            else
                resolve.setEnabled(false);

            // scroll table
            UIUtil.scrollToSelectedRow(table);
        }
        else
            resolve.setEnabled(false);

        RelationshipDisplayEvent ev = new RelationshipDisplayEvent(this, rel, mediator
                .getCurrentObjEntity(), mediator.getCurrentDataMap(), mediator
                .getCurrentDataDomain());

        mediator.fireObjRelationshipDisplayEvent(ev);
    }

    /** Loads obj relationships into table. */
    public void currentObjEntityChanged(EntityDisplayEvent e) {
        if (e.getSource() == this) {
            return;
        }

        ObjEntity entity = (ObjEntity) e.getEntity();
        // Important: process event even if this is the same entity,
        // since the inheritance structure might have changed
        if (entity != null) {
            rebuildTable(entity);
        }

        // if an entity was selected on a tree,
        // unselect currently selected row
        if (e.isUnselectAttributes()) {
            table.clearSelection();
        }
    }

    /**
     * Creates a list of ObjEntity names.
     */
    private Object[] createObjEntityComboModel() {
        DataMap map = mediator.getCurrentDataMap();

        // this actually happens per CAY-221... can't reproduce though
        if (map == null) {
            logObj.warn("createObjEntityComboModel:: Null DataMap.");
            return new Object[0];
        }

        if (map.getNamespace() == null) {
            logObj.warn("createObjEntityComboModel:: Null DataMap namespace - " + map);
            return new Object[0];
        }

        Collection objEntities = map.getNamespace().getObjEntities();
        return objEntities.toArray();
    }

    public void objEntityChanged(EntityEvent e) {
    }

    public void objEntityAdded(EntityEvent e) {
        reloadEntityList(e);
    }

    public void objEntityRemoved(EntityEvent e) {
        reloadEntityList(e);
    }

    public void objRelationshipChanged(RelationshipEvent e) {
        table.select(e.getRelationship());
    }

    public void objRelationshipAdded(RelationshipEvent e) {
        rebuildTable((ObjEntity) e.getEntity());
        table.select(e.getRelationship());
    }

    public void objRelationshipRemoved(RelationshipEvent e) {
        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getRelationship());
        model.removeRow(e.getRelationship());
        table.select(ind);
    }

    /**
     * Refresh the list of ObjEntity targets. Also refresh the table in case some
     * ObjRelationships were deleted.
     */
    private void reloadEntityList(EntityEvent e) {
        if (e.getSource() != this) {
            return;
        }

        // If current model added/removed, do nothing.
        ObjEntity entity = mediator.getCurrentObjEntity();
        if (entity == e.getEntity() || entity == null) {
            return;
        }

        TableColumn col = table.getColumnModel().getColumn(
                ObjRelationshipTableModel.REL_TARGET);
        DefaultCellEditor editor = (DefaultCellEditor) col.getCellEditor();

        JComboBox combo = (JComboBox) editor.getComponent();
        combo.setRenderer(CellRenderers.entityListRendererWithIcons(entity.getDataMap()));
        combo.setModel(new DefaultComboBoxModel(createObjEntityComboModel()));

        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        model.fireTableDataChanged();
    }

    protected void rebuildTable(ObjEntity entity) {
        final ObjRelationshipTableModel model = new ObjRelationshipTableModel(
                entity,
                mediator,
                this);

        model.addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                if (table.getSelectedRow() >= 0) {
                    ObjRelationship rel = model.getRelationship(table.getSelectedRow());
                    if (rel.getTargetEntity() != null
                            && ((ObjEntity) rel.getSourceEntity()).getDbEntity() != null
                            && ((ObjEntity) rel.getTargetEntity()).getDbEntity() != null) {
                        resolve.setEnabled(true);
                    }
                    else
                        resolve.setEnabled(false);
                }
            }
        });

        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        TableColumn lockColumn = table.getColumnModel().getColumn(
                ObjRelationshipTableModel.REL_LOCKING);
        lockColumn.setMinWidth(100);

        TableColumn col = table.getColumnModel().getColumn(
                ObjRelationshipTableModel.REL_NAME);
        col.setMinWidth(150);

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_TARGET);
        col.setMinWidth(150);
        JComboBox targetCombo = CayenneWidgetFactory.createComboBox(
                createObjEntityComboModel(),
                false);
        targetCombo.setRenderer(CellRenderers.entityListRendererWithIcons(entity
                .getDataMap()));
        targetCombo.setEditable(false);
        targetCombo.setSelectedIndex(-1);
        DefaultCellEditor editor = new DefaultCellEditor(targetCombo);
        editor.setClickCountToStart(1);
        col.setCellEditor(editor);

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_SEMANTICS);
        col.setMinWidth(150);

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_DELETERULE);
        col.setMinWidth(60);
        JComboBox deleteRulesCombo = CayenneWidgetFactory.createComboBox(
                deleteRules,
                false);
        deleteRulesCombo.setEditable(false);
        deleteRulesCombo.setSelectedIndex(0); // Default to the first value
        editor = new DefaultCellEditor(deleteRulesCombo);
        editor.setClickCountToStart(1);
        col.setCellEditor(editor);
    }

    class EntityRenderer extends StringRenderer {

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            if (value instanceof CayenneMapEntry) {
                CayenneMapEntry mapObject = (CayenneMapEntry) value;
                String label = mapObject.getName();

                if (mapObject instanceof Entity) {
                    Entity entity = (Entity) mapObject;

                    // for different namespace display its name
                    DataMap dataMap = entity.getDataMap();
                    if (dataMap != null && dataMap != mediator.getCurrentDataMap()) {
                        label += " (" + dataMap.getName() + ")";
                    }
                }

                value = label;
            }

            return super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);
        }
    }

    class StringRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            // center cardinality column
            int align = column == ObjRelationshipTableModel.REL_SEMANTICS
                    ? JLabel.CENTER
                    : JLabel.LEFT;
            super.setHorizontalAlignment(align);

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                    .getModel();
            ObjRelationship relationship = model.getRelationship(row);

            if (relationship != null
                    && relationship.getSourceEntity() != model.getEntity()) {
                setForeground(Color.GRAY);
            }
            else {
                setForeground(isSelected && !hasFocus
                        ? table.getSelectionForeground()
                        : table.getForeground());
            }

            return this;
        }
    }
}
