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
package org.apache.cayenne.modeler.ui.project.editor.dbentity;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.DbRelationshipListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.ui.dbrelationship.DbRelationshipDialogController;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.display.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.swing.table.BoardTableCellRenderer;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.swing.combo.AutoCompletion;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Displays DbRelationships for the current DbEntity.
 */
public class DbEntityRelationshipPanel extends JPanel implements DbEntityDisplayListener,
        DbEntityListener, DbRelationshipListener, TableModelListener {

    private final ProjectController controller;
    private final CayenneTable table;
    private final TableColumnPreferences tablePreferences;
    private final DbEntityAttributeRelationshipTab parentPanel;
    private final JMenuItem editMenu;
    private JComboBox<DbEntity> targetCombo;

    public DbEntityRelationshipPanel(ProjectController controller, DbEntityAttributeRelationshipTab parentPanel) {
        this.controller = controller;
        this.parentPanel = parentPanel;

        this.setLayout(new BorderLayout());

        ActionManager actionManager = Application.getInstance().getActionManager();

        table = new CayenneTable();
        table.setDefaultRenderer(DbEntity.class, CellRenderers.entityTableRendererWithIcons(controller));
        table.setDefaultRenderer(String.class, new BoardTableCellRenderer());
        tablePreferences = new TableColumnPreferences(DbRelationshipTableModel.class, "relationshipTable");

        editMenu = new JMenuItem("Edit Relationship", ModelerUtil.buildIcon("icon-edit.png"));

        JPopupMenu popup = new JPopupMenu();
        popup.add(editMenu);
        popup.add(actionManager.getAction(RemoveAttributeRelationshipAction.class).buildMenu());

        popup.addSeparator();
        popup.add(actionManager.getAction(CutAttributeRelationshipAction.class).buildMenu());
        popup.add(actionManager.getAction(CopyAttributeRelationshipAction.class).buildMenu());
        popup.add(actionManager.getAction(PasteAction.class).buildMenu());

        TablePopupHandler.install(table, popup);
        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);

        this.controller.addDbEntityDisplayListener(this);
        this.controller.addDbEntityListener(this);
        this.controller.addDbRelationshipListener(this);

        editMenu.addActionListener(this::edit);

        table.getSelectionModel().addListSelectionListener(this::valueChanged);

        controller.getApplication().getActionManager().setupCutCopyPaste(
                table,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
    }

    public CayenneTable getTable() {
        return table;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (table.getSelectedRow() >= 0) {
            DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
            DbRelationship rel = model.getRelationship(table.getSelectedRow());
            editMenu.setEnabled(rel.getTargetEntity() != null);
        }
    }

    /**
     * Selects a specified relationship in the relationships table.
     */
    public void selectRelationships(DbRelationship[] rels) {
        DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();

        List listRels = model.getObjectList();
        int[] newSel = new int[rels.length];

        ActionManager actionManager = Application.getInstance().getActionManager();
        actionManager.getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
        actionManager.getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
        actionManager.getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(this);

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
    public void dbEntitySelected(EntityDisplayEvent e) {
        DbEntity entity = (DbEntity) e.getEntity();
        if (entity != null) {
            // TODO: this line seems to slow down the Modeler significantly sometimes
            // (esp. noticable if selected entity has no relationships!),
            // even when this tab is not showing...maybe we should simply mark the view as
            // dirty and rebuild it when it becomes visible
            rebuildTable(entity);
        }

        // if an entity was selected on a tree,
        // unselect currently selected row
        if (e.isUnselectAttributes()) {
            table.clearSelection();
        }
    }

    protected void rebuildTable(DbEntity entity) {
        DbRelationshipTableModel model = new DbRelationshipTableModel(
                entity,
                controller,
                this);
        model.addTableModelListener(this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        TableColumn col = table.getColumnModel().getColumn(
                DbRelationshipTableModel.TARGET);

        targetCombo = Application.getWidgetFactory().createComboBox();
        AutoCompletion.enable(targetCombo);

        table.getColumnModel().getColumn(DbRelationshipTableModel.TO_DEPENDENT_KEY)
                .setCellRenderer(new CheckBoxCellRenderer());

        targetCombo.setRenderer(CellRenderers.entityListRendererWithIcons(entity.getDataMap()));
        targetCombo.setModel(createComboModel());
        col.setCellEditor(Application.getWidgetFactory().createCellEditor(targetCombo));

        tablePreferences.bind(
                table,
                null,
                null,
                null,
                DbRelationshipTableModel.NAME,
                true);
    }

    public void dbEntityChanged(EntityEvent e) {
    }

    public void dbEntityAdded(EntityEvent e) {
        reloadEntityList(e);
    }

    public void dbEntityRemoved(EntityEvent e) {
        reloadEntityList(e);
    }

    public void dbRelationshipChanged(RelationshipEvent e) {
        if (e.getSource() != this) {
            if (!(table.getModel() instanceof DbRelationshipTableModel)) {
                rebuildTable((DbEntity) e.getEntity());
            }

            table.select(e.getRelationship());
            DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
            model.fireTableDataChanged();
        }
    }

    public void dbRelationshipAdded(RelationshipEvent e) {
        rebuildTable((DbEntity) e.getEntity());
        table.select(e.getRelationship());
    }

    public void dbRelationshipRemoved(RelationshipEvent e) {
        DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
        DbRelationship relationship = (DbRelationship) e.getRelationship();
        int ind = model.getObjectList().indexOf(relationship);
        model.removeRelationship(relationship);
        table.select(ind);
    }

    /**
     * Refresh the list of db entities (targets). Also refresh the table in case some db
     * relationships were deleted.
     */
    private void reloadEntityList(EntityEvent e) {
        if (e.getSource() == this
                || controller.getSelectedDbEntity() == e.getEntity()  // If current model added/removed, do nothing.
                || controller.getSelectedDbEntity() == null) { // If this is just loading new currentDbEntity, do nothing
            return;
        }

        targetCombo.setModel(createComboModel());

        DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
        model.fireTableDataChanged();
    }

    /**
     * Creates a list of DbEntities.
     */
    private ComboBoxModel<DbEntity> createComboModel() {
        EntityResolver resolver = controller.getEntityResolver();
        DbEntity[] objects = resolver.getDbEntities().toArray(new DbEntity[0]);
        return new DefaultComboBoxModel<>(objects);
    }

    private void edit(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }

        DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
        DbRelationship rel = model.getRelationship(row);
        new DbRelationshipDialogController(controller)
                .modifyRaltionship(rel)
                .startUp();
    }

    private void valueChanged(ListSelectionEvent e) {

        if (e.getValueIsAdjusting()) {
            return;
        }

        DbRelationship[] rels = new DbRelationship[0];

        if (!((ListSelectionModel) e.getSource()).isSelectionEmpty()) {

            parentPanel.getAttributePanel().getTable().getSelectionModel().clearSelection();
            if (parentPanel.getAttributePanel().getTable().getCellEditor() != null) {
                parentPanel.getAttributePanel().getTable().getCellEditor().stopCellEditing();
            }

            ActionManager actionManager = Application.getInstance().getActionManager();
            actionManager.getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
            actionManager.getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
            actionManager.getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(this);

            boolean editEnabled = table.getSelectedRow() >= 0;
            parentPanel.rebindEditButton(editEnabled, "Edit Relationship", this::edit);

            if (editEnabled) {
                DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();

                int[] sel = table.getSelectedRows();
                rels = new DbRelationship[sel.length];

                for (int i = 0; i < sel.length; i++) {
                    rels[i] = model.getRelationship(sel[i]);
                }

                if (sel.length == 1) {
                    UIUtil.scrollToSelectedRow(table);
                }
            }

            editMenu.setEnabled(editEnabled);
        }

        controller.displayDbRelationship(new RelationshipDisplayEvent(
                this,
                rels,
                controller.getSelectedDbEntity(),
                controller.getSelectedDataMap(),
                controller.getSelectedDataDomain()));

        parentPanel.updateActions(rels);
    }

    private static class CheckBoxCellRenderer implements TableCellRenderer {

        private final JCheckBox renderer;

        public CheckBoxCellRenderer() {
            renderer = new JCheckBox();
            renderer.setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Color color = isSelected ? table.getSelectionBackground() : table.getBackground();
            renderer.setBackground(color);
            renderer.setEnabled(table.isCellEditable(row, column));
            renderer.setSelected(value != null && (Boolean) value);
            return renderer;
        }
    }
}