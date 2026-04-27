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
package org.apache.cayenne.modeler.ui.project.editor.dbentity.properties;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.modeler.event.model.DbRelationshipEvent;
import org.apache.cayenne.modeler.event.model.DbEntityEvent;
import org.apache.cayenne.modeler.event.model.DbEntityListener;
import org.apache.cayenne.modeler.event.model.DbRelationshipListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.icon.IconFactory;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.ui.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.ui.action.PasteAction;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.ui.dbrelationship.DbRelationshipDialogController;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.display.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.toolkit.WidgetFactory;
import org.apache.cayenne.modeler.toolkit.table.BoardTableCellRenderer;
import org.apache.cayenne.modeler.toolkit.table.CayenneTable;
import org.apache.cayenne.modeler.toolkit.Renderers;
import org.apache.cayenne.modeler.toolkit.combo.AutoCompletion;

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
public class DbRelationshipPanel extends JPanel implements DbEntityDisplayListener,
        DbEntityListener, DbRelationshipListener, TableModelListener {

    private final ProjectController controller;
    private final CayenneTable table;
    private final TableColumnPreferences tablePreferences;
    private final DbEntityPropertiesView parentPanel;
    private final JMenuItem editMenu;
    private JComboBox<DbEntity> targetCombo;

    public DbRelationshipPanel(ProjectController controller, DbEntityPropertiesView parentPanel) {
        this.controller = controller;
        this.parentPanel = parentPanel;

        this.setLayout(new BorderLayout());

        GlobalActions globalActions = Application.getInstance().getActionManager();

        table = new CayenneTable();
        table.setDefaultRenderer(DbEntity.class, Renderers.entityTableRendererWithIcons(controller));
        table.setDefaultRenderer(String.class, new BoardTableCellRenderer());
        tablePreferences = new TableColumnPreferences(DbRelationshipTableModel.class, "relationshipTable");

        editMenu = new JMenuItem("Edit Relationship", IconFactory.buildIcon("icon-edit.png"));

        JPopupMenu popup = new JPopupMenu();
        popup.add(editMenu);
        popup.add(globalActions.getAction(RemoveAttributeRelationshipAction.class).buildMenu());

        popup.addSeparator();
        popup.add(globalActions.getAction(CutAttributeRelationshipAction.class).buildMenu());
        popup.add(globalActions.getAction(CopyAttributeRelationshipAction.class).buildMenu());
        popup.add(globalActions.getAction(PasteAction.class).buildMenu());

        TablePopupHandler.install(table, popup);
        add(WidgetFactory.createTablePanel(table, null), BorderLayout.CENTER);

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

        GlobalActions globalActions = Application.getInstance().getActionManager();
        globalActions.getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
        globalActions.getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
        globalActions.getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(this);

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

        targetCombo = WidgetFactory.createComboBox();
        AutoCompletion.enable(targetCombo);

        targetCombo.setRenderer(Renderers.entityListRendererWithIcons(entity.getDataMap()));
        targetCombo.setModel(createComboModel());

        TableColumn targetColumn = table.getColumnModel().getColumn(DbRelationshipTableModel.TARGET);
        targetColumn.setCellEditor(WidgetFactory.createCellEditor(targetCombo));

        TableColumn toDepPkColumn = table.getColumnModel().getColumn(DbRelationshipTableModel.TO_DEPENDENT_KEY);
        toDepPkColumn.setCellRenderer(new CheckBoxCellRenderer());

        tablePreferences.bind(
                table,
                null,
                null,
                null,
                DbRelationshipTableModel.NAME,
                true);
    }

    public void dbEntityChanged(DbEntityEvent e) {
    }

    public void dbEntityAdded(DbEntityEvent e) {
        reloadEntityList(e);
    }

    public void dbEntityRemoved(DbEntityEvent e) {
        reloadEntityList(e);
    }

    public void dbRelationshipChanged(DbRelationshipEvent e) {
        if (e.getSource() != this) {
            if (!(table.getModel() instanceof DbRelationshipTableModel)) {
                rebuildTable((DbEntity) e.getEntity());
            }

            table.select(e.getRelationship());
            DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
            model.fireTableDataChanged();
        }
    }

    public void dbRelationshipAdded(DbRelationshipEvent e) {
        rebuildTable((DbEntity) e.getEntity());
        table.select(e.getRelationship());
    }

    public void dbRelationshipRemoved(DbRelationshipEvent e) {
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
    private void reloadEntityList(DbEntityEvent e) {
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

            GlobalActions globalActions = Application.getInstance().getActionManager();
            globalActions.getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
            globalActions.getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
            globalActions.getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(this);

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
                    table.scrollToSelectedRow();
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