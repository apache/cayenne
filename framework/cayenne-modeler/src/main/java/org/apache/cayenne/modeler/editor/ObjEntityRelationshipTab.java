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
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.map.event.ObjRelationshipListener;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CopyRelationshipAction;
import org.apache.cayenne.modeler.action.CreateRelationshipAction;
import org.apache.cayenne.modeler.action.CutRelationshipAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveRelationshipAction;
import org.apache.cayenne.modeler.dialog.objentity.ObjRelationshipInfoController;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.RelationshipDisplayEvent;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Displays ObjRelationships for the edited ObjEntity.
 * 
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
    
    /**
     * By now popup menu item is made similiar to toolbar button. 
     * (i.e. all functionality is here)
     * This should be probably refactored as Action.
     */
    protected JMenuItem resolveMenu;

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
        
        Icon ico = ModelerUtil.buildIcon("icon-info.gif");

        resolve = new JButton();
        resolve.setIcon(ico);
        resolve.setToolTipText("Edit Relationship");
        toolBar.add(resolve);

        toolBar.addSeparator();

        toolBar.add(app.getAction(RemoveRelationshipAction.getActionName()).buildButton());
        
        toolBar.addSeparator();
        toolBar.add(app.getAction(CutRelationshipAction.getActionName()).buildButton());
        toolBar.add(app.getAction(CopyRelationshipAction.getActionName()).buildButton());
        toolBar.add(app.getAction(PasteAction.getActionName()).buildButton());
        
        add(toolBar, BorderLayout.NORTH);

        table = new CayenneTable();
        table.setDefaultRenderer(String.class, new StringRenderer());
        table.setDefaultRenderer(ObjEntity.class, new EntityRenderer());
        
        /**
         * Create and install a popup
         */
        resolveMenu = new JMenuItem("Database Mapping", ico);
        
        JPopupMenu popup = new JPopupMenu();
        popup.add(resolveMenu);
        popup.add(app.getAction(RemoveRelationshipAction.getActionName()).buildMenu());
        
        popup.addSeparator();
        popup.add(app.getAction(CutRelationshipAction.getActionName()).buildMenu());
        popup.add(app.getAction(CopyRelationshipAction.getActionName()).buildMenu());
        popup.add(app.getAction(PasteAction.getActionName()).buildMenu());
        
        TablePopupHandler.install(table, popup);

        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);
    }

    private void initController() {
        mediator.addObjEntityDisplayListener(this);
        mediator.addObjEntityListener(this);
        mediator.addObjRelationshipListener(this);

        ActionListener resolver = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    return;
                }

                ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                        .getModel();
                new ObjRelationshipInfoController(mediator, model.getRelationship(row))
                        .startup();
                
                /**
                 * This is required for a table to be updated properly
                 */
                table.cancelEditing();

                // need to refresh selected row... do this by unselecting/selecting the
                // row
                table.getSelectionModel().clearSelection();
                table.select(row);
            }
        };
        
        resolve.addActionListener(resolver);
        resolveMenu.addActionListener(resolver);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                processExistingSelection(e);
            }
        });
        
        mediator.getApplication().getActionManager().setupCCP(table, 
                CutRelationshipAction.getActionName(), CopyRelationshipAction.getActionName());
    }

    /**
     * Selects a specified relationship in the relationships table.
     */
    public void selectRelationships(ObjRelationship[] rels) {
        ModelerUtil.updateActions(rels.length,  
                RemoveRelationshipAction.getActionName(),
                CutRelationshipAction.getActionName(),
                CopyRelationshipAction.getActionName());

        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();

        List listAttrs = model.getObjectList();
        int[] newSel = new int[rels.length];
        
        for (int i = 0; i < rels.length; i++) {
            newSel[i] = listAttrs.indexOf(rels[i]);
        }
        
        table.select(newSel);
    }

    public void processExistingSelection(EventObject e) {
        if (e instanceof ChangeEvent) {
            table.clearSelection();
        }
        ObjRelationship[] rels = new ObjRelationship[0];
        if (table.getSelectedRow() >= 0) {
            ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                    .getModel();
            
            int[] sel = table.getSelectedRows();
            rels = new ObjRelationship[sel.length];
            
            for (int i = 0; i < sel.length; i++) {
                rels[i] = model.getRelationship(sel[i]);
            }
            
            resolve.setEnabled(true);

            // scroll table
            UIUtil.scrollToSelectedRow(table);
        }
        else {
            resolve.setEnabled(false);
        }
        
        resolveMenu.setEnabled(resolve.isEnabled());

        RelationshipDisplayEvent ev = new RelationshipDisplayEvent(this, rels, mediator
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
                    if (((ObjEntity) rel.getSourceEntity()).getDbEntity() != null) {
                        resolve.setEnabled(true);
                    }
                    else
                        resolve.setEnabled(false);
                    
                    resolveMenu.setEnabled(resolve.isEnabled());
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
        AutoCompletion.enable(targetCombo);        
        
        targetCombo.setRenderer(CellRenderers.entityListRendererWithIcons(entity
                .getDataMap()));
        targetCombo.setSelectedIndex(-1);
        col.setCellEditor(CayenneWidgetFactory.createCellEditor(targetCombo));

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_SEMANTICS);
        col.setMinWidth(150);

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_DELETERULE);
        col.setMinWidth(60);
        JComboBox deleteRulesCombo = CayenneWidgetFactory.createComboBox(
                deleteRules,
                false);
        deleteRulesCombo.setEditable(false);
        deleteRulesCombo.setSelectedIndex(0); // Default to the first value
        col.setCellEditor(CayenneWidgetFactory.createCellEditor(deleteRulesCombo));
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
            value = CellRenderers.asString(value);

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);
            
            setIcon(CellRenderers.iconForObject(oldValue));
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
