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
package org.apache.cayenne.modeler.editor.dbentity;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.DbRelationshipListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.dialog.ResolveDbRelationshipDialog;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Displays DbRelationships for the current DbEntity.
 */
public class DbEntityRelationshipPanel extends JPanel implements DbEntityDisplayListener,
        DbEntityListener, DbRelationshipListener, TableModelListener {

    protected ProjectController mediator;
    protected CayenneTable table;
    private TableColumnPreferences tablePreferences;
    private ActionListener resolver;
    private boolean enabledResolve;
    private DbEntityAttributeRelationshipTab parentPanel;

    /**
     * By now popup menu item is made similiar to toolbar button. (i.e. all functionality
     * is here) This should be probably refactored as Action.
     */
    protected JMenuItem resolveMenu;

    /**
     * Combo to edit 'target' field
     */
    protected JComboBox targetCombo;

    public DbEntityRelationshipPanel(ProjectController mediator, DbEntityAttributeRelationshipTab parentPanel) {
        this.mediator = mediator;
        this.parentPanel = parentPanel;

        init();
        initController();
    }

    protected void init() {
        this.setLayout(new BorderLayout());

        ActionManager actionManager = Application.getInstance().getActionManager();

        table = new CayenneTable();
        table.setDefaultRenderer(DbEntity.class, CellRenderers
                .entityTableRendererWithIcons(mediator));
        tablePreferences = new TableColumnPreferences(
                DbRelationshipTableModel.class,
                "relationshipTable");

        /**
         * Create and install a popup
         */
        Icon ico = ModelerUtil.buildIcon("icon-info.gif");
        resolveMenu = new JMenuItem("Database Mapping", ico);

        JPopupMenu popup = new JPopupMenu();
        popup.add(resolveMenu);
        popup.add(actionManager.getAction(RemoveAttributeRelationshipAction.class).buildMenu());

        popup.addSeparator();
        popup.add(actionManager.getAction(CutAttributeRelationshipAction.class).buildMenu());
        popup.add(actionManager.getAction(CopyAttributeRelationshipAction.class).buildMenu());
        popup.add(actionManager.getAction(PasteAction.class).buildMenu());

        TablePopupHandler.install(table, popup);
        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);
    }

    private void initController() {
        this.mediator.addDbEntityDisplayListener(this);
        this.mediator.addDbEntityListener(this);
        this.mediator.addDbRelationshipListener(this);

        resolver = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    return;
                }

                // Get DbRelationship
                DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
                DbRelationship rel = model.getRelationship(row);
                ResolveDbRelationshipDialog dialog = new ResolveDbRelationshipDialog(rel);
                dialog.setVisible(true);
                dialog.dispose();
            }
        };
        resolveMenu.addActionListener(resolver);

        table.getSelectionModel().addListSelectionListener(new DbRelationshipListSelectionListener());

        mediator.getApplication().getActionManager().setupCutCopyPaste(
                table,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
    }

    public void tableChanged(TableModelEvent e) {
        DbRelationship rel = null;
        if (table.getSelectedRow() >= 0) {
            DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
            rel = model.getRelationship(table.getSelectedRow());
            enabledResolve = (rel.getTargetEntity() != null);
            resolveMenu.setEnabled(enabledResolve);
        }
    }

    /**
     * Selects a specified relationship in the relationships table.
     */
    public void selectRelationships(DbRelationship[] rels) {
        DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();

        List listRels = model.getObjectList();
        int[] newSel = new int[rels.length];

        parentPanel.updateActions(rels);

        for (int i = 0; i < rels.length; i++) {
            newSel[i] = listRels.indexOf(rels[i]);
        }

        table.select(newSel);
    }

    /**
     * Loads obj relationships into table.
     */
    public void currentDbEntityChanged(EntityDisplayEvent e) {
        DbEntity entity = (DbEntity) e.getEntity();
        if (entity != null && e.isEntityChanged()) {
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
                mediator,
                this);
        model.addTableModelListener(this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        TableColumn col = table.getColumnModel().getColumn(
                DbRelationshipTableModel.TARGET);

        targetCombo = Application.getWidgetFactory().createComboBox();
        AutoCompletion.enable(targetCombo);

        targetCombo.setRenderer(CellRenderers.entityListRendererWithIcons(entity
                .getDataMap()));
        targetCombo.setModel(createComboModel(entity));
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
        int ind = model.getObjectList().indexOf(e.getRelationship());
        model.removeRelationship(e.getRelationship());
        table.select(ind);
    }

    /**
     * Refresh the list of db entities (targets). Also refresh the table in case some db
     * relationships were deleted.
     */
    private void reloadEntityList(EntityEvent e) {
        if (e.getSource() == this)
            return;
        // If current model added/removed, do nothing.
        if (mediator.getCurrentDbEntity() == e.getEntity())
            return;
        // If this is just loading new currentDbEntity, do nothing
        if (mediator.getCurrentDbEntity() == null)
            return;
        targetCombo.setModel(createComboModel(e.getEntity()));

        DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
        model.fireTableDataChanged();
    }

    /**
     * Creates a list of DbEntities.
     */
    private ComboBoxModel createComboModel(Entity entity) {
        EntityResolver resolver = mediator.getEntityResolver();
        Object[] objects = resolver.getDbEntities().toArray();
        return new DefaultComboBoxModel(objects);
    }

    public boolean isEnabledResolve() {
        return enabledResolve;
    }

    public ActionListener getResolver() {
        return resolver;
    }

    private class DbRelationshipListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            DbRelationship[] rels = new DbRelationship[0];

            if (!e.getValueIsAdjusting() && !((ListSelectionModel) e.getSource()).isSelectionEmpty()) {

                parentPanel.getAttributePanel().table.getSelectionModel().clearSelection();
                if (parentPanel.getAttributePanel().table.getCellEditor() != null)
                    parentPanel.getAttributePanel().table.getCellEditor().stopCellEditing();
                Application.getInstance().getActionManager().getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getRelationshipPanel());
                Application.getInstance().getActionManager().getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getRelationshipPanel());
                Application.getInstance().getActionManager().getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getRelationshipPanel());
                parentPanel.getResolve().removeActionListener(getResolver());
                parentPanel.getResolve().addActionListener(getResolver());

                if (table.getSelectedRow() >= 0) {
                    DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();

                    int[] sel = table.getSelectedRows();
                    rels = new DbRelationship[sel.length];

                    for (int i = 0; i < sel.length; i++) {
                        rels[i] = model.getRelationship(sel[i]);
                    }

                    if (sel.length == 1) {
                        UIUtil.scrollToSelectedRow(table);
                    }

                    enabledResolve = true;
                } else {
                    enabledResolve = false;
                }
                resolveMenu.setEnabled(enabledResolve);
            }

            mediator.setCurrentDbRelationships(rels);
            parentPanel.updateActions(rels);
        }
    }
}