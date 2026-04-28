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
package org.apache.cayenne.modeler.ui.project.editor.objentity.properties;

import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.modeler.event.model.ObjRelationshipEvent;
import org.apache.cayenne.modeler.event.model.ObjEntityEvent;
import org.apache.cayenne.modeler.event.model.ObjEntityListener;
import org.apache.cayenne.modeler.event.model.ObjRelationshipListener;
import org.apache.cayenne.modeler.toolkit.combobox.CMComboBox;
import org.apache.cayenne.modeler.toolkit.table.CMComboBoxCellEditor;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.ui.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.ui.action.ObjEntityToSuperEntityAction;
import org.apache.cayenne.modeler.ui.action.PasteAction;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.ui.project.editor.objentity.relinfo.ObjRelationshipInfoController;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.display.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.display.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.toolkit.table.CMTable;
import org.apache.cayenne.modeler.toolkit.table.CMTablePanel;
import org.apache.cayenne.modeler.toolkit.Renderers;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Displays ObjRelationships for the edited ObjEntity.
 */
public class ObjRelationshipPanel extends JPanel implements ObjEntityDisplayListener, ObjEntityListener, ObjRelationshipListener {

    private static final ImageIcon INHERITANCE_ICON = IconFactory.buildIcon("icon-inheritance.png");

    private static final Object[] DELETE_RULES = new Object[]{
            DeleteRule.deleteRuleName(DeleteRule.NO_ACTION),
            DeleteRule.deleteRuleName(DeleteRule.NULLIFY),
            DeleteRule.deleteRuleName(DeleteRule.CASCADE),
            DeleteRule.deleteRuleName(DeleteRule.DENY),
    };

    private final ProjectController controller;
    private final CMTable table;
    private final TableColumnPreferences tablePreferences;
    private final ObjEntityPropertiesView parentPanel;
    private final JMenuItem editMenu;

    public ObjRelationshipPanel(ProjectController controller, ObjEntityPropertiesView parentPanel) {
        this.controller = controller;
        this.parentPanel = parentPanel;

        this.setLayout(new BorderLayout());

        GlobalActions globalActions = controller.getApplication().getActionManager();

        table = new CMTable();
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setDefaultRenderer(String.class, new StringRenderer());
        table.setDefaultRenderer(ObjEntity.class, new EntityRenderer());
        tablePreferences = new TableColumnPreferences(ObjRelationshipTableModel.class, "objEntity/relationshipTable");

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                ObjRelationshipTableModel tableModel = ((ObjRelationshipTableModel) table.getModel());
                ObjRelationship relationship = tableModel.getRelationship(row);
                int columnFromModel = table.getColumnModel().getColumn(col).getModelIndex();
                if (row >= 0 && columnFromModel == ObjRelationshipTableModel.REL_NAME) {
                    if (relationship.getSourceEntity() != tableModel.getEntity()) {
                        TableCellRenderer renderer = table.getCellRenderer(row, col);
                        Rectangle rectangle = table.getCellRect(row, col, false);
                        ((StringRenderer) renderer).mouseClicked(e, rectangle.x);
                    }
                }
            }
        });

        // Create and install a popup
        Icon ico = IconFactory.buildIcon("icon-edit.png");
        editMenu = new JMenuItem("Edit Relationship", ico);

        JPopupMenu popup = new JPopupMenu();
        popup.add(editMenu);
        popup.add(globalActions.getAction(RemoveAttributeRelationshipAction.class).buildMenu());

        popup.addSeparator();
        popup.add(globalActions.getAction(CutAttributeRelationshipAction.class).buildMenu());
        popup.add(globalActions.getAction(CopyAttributeRelationshipAction.class).buildMenu());
        popup.add(globalActions.getAction(PasteAction.class).buildMenu());

        TablePopupHandler.install(table, popup);
        add(new CMTablePanel(table), BorderLayout.CENTER);

        controller.addObjEntityDisplayListener(this);
        controller.addObjEntityListener(this);
        controller.addObjRelationshipListener(this);

        editMenu.addActionListener(this::edit);

        table.getSelectionModel().addListSelectionListener(this::valueChanged);

        controller.getApplication().getActionManager().setupCutCopyPaste(
                table,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
    }

    public CMTable getTable() {
        return table;
    }

    /**
     * Selects a specified relationship in the relationships table.
     */
    public void selectRelationships(ObjRelationship[] rels) {
        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();

        List<ObjRelationship> listRels = model.getObjectList();
        int[] newSel = new int[rels.length];

        parentPanel.updateActions(rels);

        for (int i = 0; i < rels.length; i++) {
            newSel[i] = listRels.indexOf(rels[i]);
        }

        table.select(newSel);
        parentPanel.rebindEditButton(rels.length > 0, "Edit Relationship", this::edit);
    }

    /**
     * Loads obj relationships into table.
     */
    public void objEntitySelected(EntityDisplayEvent e) {
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

        ObjEntity objEntity = (ObjEntity) e.getEntity();
        parentPanel.getToolBar().getComponentAtIndex(2).setEnabled(objEntity.getSuperEntity() == null);
    }

    public void objEntityChanged(ObjEntityEvent e) {
    }

    public void objEntityAdded(ObjEntityEvent e) {
        reloadEntityList(e);
    }

    public void objEntityRemoved(ObjEntityEvent e) {
        reloadEntityList(e);
    }

    public void objRelationshipChanged(ObjRelationshipEvent e) {
        table.select(e.getRelationship());
    }

    public void objRelationshipAdded(ObjRelationshipEvent e) {
        rebuildTable(e.getEntity());
        table.select(e.getRelationship());
    }

    public void objRelationshipRemoved(ObjRelationshipEvent e) {
        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getRelationship());
        model.removeRow(e.getRelationship());
        table.select(ind);
    }

    /**
     * Refresh the list of ObjEntity targets. Also refresh the table in case some
     * ObjRelationships were deleted.
     */
    private void reloadEntityList(ObjEntityEvent e) {
        if (e.getSource() != this) {
            return;
        }

        // If current model added/removed, do nothing.
        ObjEntity entity = controller.getSelectedObjEntity();
        if (entity == e.getEntity() || entity == null) {
            return;
        }

        TableColumn col = table.getColumnModel().getColumn(
                ObjRelationshipTableModel.REL_TARGET);
        DefaultCellEditor editor = (DefaultCellEditor) col.getCellEditor();

        JComboBox combo = (JComboBox) editor.getComponent();
        combo.setRenderer(Renderers.entityListRendererWithIcons(entity.getDataMap()));

        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        model.fireTableDataChanged();
    }

    protected void rebuildTable(ObjEntity entity) {
        final ObjRelationshipTableModel model = new ObjRelationshipTableModel(
                entity,
                controller,
                this);

        model.addTableModelListener(e -> {
            if (table.getSelectedRow() >= 0) {
                editMenu.setEnabled(model.getRelationship(table.getSelectedRow()).getSourceEntity().getDbEntity() != null);
            }
        });

        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        TableColumn col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_TARGET_PATH);
        col.setCellEditor(new DbRelationshipPathComboBoxEditor(controller.getApplication()));
        col.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
                setToolTipText("To choose relationship press enter two times.To choose next relationship press dot.");
                return this;
            }
        });

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_DELETE_RULE);
        JComboBox deleteRulesCombo = new CMComboBox<>(DELETE_RULES);
        deleteRulesCombo.setFocusable(false);
        deleteRulesCombo.setEditable(true);
        ((JComponent) deleteRulesCombo.getEditor().getEditorComponent()).setBorder(null);
        deleteRulesCombo.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        deleteRulesCombo.setSelectedIndex(0); // Default to the first value
        col.setCellEditor(new CMComboBoxCellEditor(deleteRulesCombo));

        tablePreferences.bind(
                table,
                null,
                null,
                null,
                ObjRelationshipTableModel.REL_NAME,
                true);
    }

    class EntityRenderer extends StringRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            Object oldValue = value;
            value = Renderers.asString(value, controller.getSelectedDataMap());

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            Icon icon = IconFactory.iconForObject(oldValue);
            if (isSelected) {
                setForeground(UIManager.getColor("Table.selectionForeground"));
            }
            setIcon(icon);
            return this;
        }
    }

    class StringRenderer extends DefaultTableCellRenderer {

        @Override
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

            setIcon(null);

            column = table.getColumnModel().getColumn(column).getModelIndex();
            if (relationship != null
                    && relationship.getSourceEntity() != model.getEntity()) {
                setForeground(isSelected ? new Color(0xEEEEEE) : Color.GRAY);
                if (column == ObjRelationshipTableModel.REL_NAME) {
                    setIcon(INHERITANCE_ICON);
                }
            } else {
                setForeground(isSelected && !hasFocus
                        ? table.getSelectionForeground()
                        : table.getForeground());
            }

            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            setFont(UIManager.getFont("Label.font"));

            return this;
        }

        public void mouseClicked(MouseEvent event, int x) {
            Point point = event.getPoint();
            if (point.x - x <= INHERITANCE_ICON.getIconWidth()) {
                GlobalActions globalActions = controller.getApplication().getActionManager();
                globalActions.getAction(ObjEntityToSuperEntityAction.class).performAction(null);
            }
        }
    }

    private void edit(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }

        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        new ObjRelationshipInfoController(controller)
                .modifyRelationship(model.getRelationship(row))
                .startupAction();

        // This is required for a table to be updated properly
        table.cancelEditing();

        // need to refresh selected row... do this by unselecting/selecting the row
        table.getSelectionModel().clearSelection();
        table.select(row);
    }

    private void valueChanged(ListSelectionEvent e) {

        if (e.getValueIsAdjusting()) {
            return;
        }

        ObjRelationship[] rels = new ObjRelationship[0];

        if (!((ListSelectionModel) e.getSource()).isSelectionEmpty()) {

            parentPanel.getAttributePanel().getTable().getSelectionModel().clearSelection();
            if (parentPanel.getAttributePanel().getTable().getCellEditor() != null) {
                parentPanel.getAttributePanel().getTable().getCellEditor().stopCellEditing();
            }

            GlobalActions globalActions = controller.getApplication().getActionManager();
            globalActions.getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
            globalActions.getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
            globalActions.getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(this);

            boolean editEnabled = table.getSelectedRow() >= 0;
            parentPanel.rebindEditButton(editEnabled, "Edit Relationship", this::edit);

            if (editEnabled) {
                ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();

                int[] sel = table.getSelectedRows();
                rels = new ObjRelationship[sel.length];

                for (int i = 0; i < sel.length; i++) {
                    rels[i] = model.getRelationship(sel[i]);
                }

                if (sel.length == 1) {
                    table.scrollToSelectedRow();
                }
            }

            editMenu.setEnabled(editEnabled);
        }

        controller.displayObjRelationship(new RelationshipDisplayEvent(
                this,
                rels,
                controller.getSelectedObjEntity(),
                controller.getSelectedDataMap(),
                controller.getSelectedDataDomain()));

        parentPanel.updateActions(rels);
    }
}