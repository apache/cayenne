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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.event.DbEntityListener;
import org.apache.cayenne.map.event.DbRelationshipListener;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.dialog.ResolveDbRelationshipDialog;
import org.apache.cayenne.modeler.editor.ExistingSelectionProcessor;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * Displays DbRelationships for the current DbEntity.
 * 
 * @author Michael Misha Shengaout
 * @author Andrus Adamchik
 */
public class DbEntityRelationshipTab extends JPanel implements DbEntityDisplayListener,
        DbEntityListener, DbRelationshipListener, ExistingSelectionProcessor,
        ListSelectionListener, TableModelListener {

    protected ProjectController mediator;
    protected CayenneTable table;
    protected JButton resolve;

    public DbEntityRelationshipTab(ProjectController mediator) {

        this.mediator = mediator;
        this.mediator.addDbEntityDisplayListener(this);
        this.mediator.addDbEntityListener(this);
        this.mediator.addDbRelationshipListener(this);

        init();
        resolve.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                resolveRelationship();
            }
        });
    }

    protected void init() {
        this.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        Application app = Application.getInstance();
        toolBar.add(app.getAction(CreateObjEntityAction.getActionName()).buildButton());
        toolBar
                .add(app
                        .getAction(CreateRelationshipAction.getActionName())
                        .buildButton());
        toolBar.add(app.getAction(DbEntitySyncAction.getActionName()).buildButton());

        toolBar.addSeparator();

        resolve = new JButton();
        resolve.setIcon(ModelerUtil.buildIcon("icon-info.gif"));
        resolve.setToolTipText("Database Mapping");
        toolBar.add(resolve);

        toolBar.addSeparator();

        toolBar
                .add(app
                        .getAction(RemoveRelationshipAction.getActionName())
                        .buildButton());
        add(toolBar, BorderLayout.NORTH);

        table = new CayenneTable();
        table.setDefaultRenderer(DbEntity.class, new EntityRenderer());

        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);
    }

    public void valueChanged(ListSelectionEvent e) {
        processExistingSelection(e);
    }

    public void tableChanged(TableModelEvent e) {
        DbRelationship rel = null;
        if (table.getSelectedRow() >= 0) {
            DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
            rel = model.getRelationship(table.getSelectedRow());
            resolve.setEnabled(rel.getTargetEntity() != null);
        }
    }

    /**
     * Selects a specified relationship in the relationships table.
     */
    public void selectRelationship(DbRelationship rel) {
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

        DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
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
        DbRelationship rel = null;
        if (table.getSelectedRow() >= 0) {
            DbRelationshipTableModel model;
            model = (DbRelationshipTableModel) table.getModel();
            rel = model.getRelationship(table.getSelectedRow());
            resolve.setEnabled(rel.getTargetEntity() != null);

            // scroll table
            UIUtil.scrollToSelectedRow(table);
        }
        else
            resolve.setEnabled(false);

        RelationshipDisplayEvent ev = new RelationshipDisplayEvent(this, rel, mediator
                .getCurrentDbEntity(), mediator.getCurrentDataMap(), mediator
                .getCurrentDataDomain());

        mediator.fireDbRelationshipDisplayEvent(ev);
    }

    private void resolveRelationship() {
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

    /** Loads obj relationships into table. */
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
        TableColumn col = table.getColumnModel().getColumn(DbRelationshipTableModel.NAME);
        col.setMinWidth(150);
        col = table.getColumnModel().getColumn(DbRelationshipTableModel.TARGET);
        col.setMinWidth(150);

        JComboBox combo = CayenneWidgetFactory.createComboBox();
        combo.setRenderer(CellRenderers.entityListRendererWithIcons(entity.getDataMap()));
        combo.setModel(createComboModel());
        combo.setEditable(false);
        col.setCellEditor(new DefaultCellEditor(combo));
        table.getSelectionModel().addListSelectionListener(this);
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
        TableColumn col = table.getColumnModel().getColumn(
                DbRelationshipTableModel.TARGET);
        DefaultCellEditor editor = (DefaultCellEditor) col.getCellEditor();
        JComboBox combo = (JComboBox) editor.getComponent();
        combo.setModel(createComboModel());

        DbRelationshipTableModel model = (DbRelationshipTableModel) table.getModel();
        model.fireTableDataChanged();
        table.getSelectionModel().addListSelectionListener(this);
    }

    /**
     * Creates a list of DbEntities.
     */
    private ComboBoxModel createComboModel() {
        DataMap map = mediator.getCurrentDataMap();
        Object[] objects = map.getNamespace().getDbEntities().toArray();
        return new DefaultComboBoxModel(objects);
    }

    class EntityRenderer extends DefaultTableCellRenderer {

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

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            return this;
        }
    }

}
