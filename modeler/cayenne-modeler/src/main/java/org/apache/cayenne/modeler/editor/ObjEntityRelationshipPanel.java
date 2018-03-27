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
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.dialog.objentity.ObjRelationshipInfo;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CellRenderers;
import org.apache.cayenne.modeler.util.Comparators;
import org.apache.cayenne.modeler.util.DbRelationshipPathComboBoxEditor;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.swing.components.image.FilteredIconFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Displays ObjRelationships for the edited ObjEntity.
 */
public class ObjEntityRelationshipPanel extends JPanel implements ObjEntityDisplayListener,
        ObjEntityListener, ObjRelationshipListener {

    private static Logger logObj = LoggerFactory.getLogger(ObjEntityRelationshipPanel.class);

    private static final Object[] DELETE_RULES = new Object[]{
            DeleteRule.deleteRuleName(DeleteRule.NO_ACTION),
            DeleteRule.deleteRuleName(DeleteRule.NULLIFY),
            DeleteRule.deleteRuleName(DeleteRule.CASCADE),
            DeleteRule.deleteRuleName(DeleteRule.DENY),
    };

    private ProjectController mediator;
    private CayenneTable table;
    private TableColumnPreferences tablePreferences;
    private ActionListener resolver;
    private ObjEntityAttributeRelationshipTab parentPanel;
    private boolean enabledResolve;//for JBottom "resolve" in ObjEntityAttrRelationshipTab

    /**
     * By now popup menu item is made similar to toolbar button. (i.e. all functionality
     * is here) This should be probably refactored as Action.
     */
    private JMenuItem resolveMenu;

    public ObjEntityRelationshipPanel(ProjectController mediator, ObjEntityAttributeRelationshipTab parentPanel) {
        this.mediator = mediator;
        this.parentPanel = parentPanel;

        init();
        initController();
    }

    public CayenneTable getTable() {
        return table;
    }

    public void setTable(CayenneTable table) {
        this.table = table;
    }

    private void init() {
        this.setLayout(new BorderLayout());

        ActionManager actionManager = Application.getInstance().getActionManager();

        table = new CayenneTable();
        table.setDefaultRenderer(String.class, new StringRenderer());
        table.setDefaultRenderer(ObjEntity.class, new EntityRenderer());
        tablePreferences = new TableColumnPreferences(
                ObjRelationshipTableModel.class,
                "objEntity/relationshipTable");

        // Create and install a popup
        Icon ico = ModelerUtil.buildIcon("icon-edit.png");
        resolveMenu = new CayenneAction.CayenneMenuItem("Database Mapping", ico);

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
        mediator.addObjEntityDisplayListener(this);
        mediator.addObjEntityListener(this);
        mediator.addObjRelationshipListener(this);

        resolver = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    return;
                }

                ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                        .getModel();
                new ObjRelationshipInfo(mediator, model.getRelationship(row)).startupAction();

                /**
                 * This is required for a table to be updated properly
                 */
                table.cancelEditing();

                // need to refresh selected row... do this by unselecting/selecting the
                // row
                table.getSelectionModel().clearSelection();
                table.select(row);
                enabledResolve = false;
            }
        };
        resolveMenu.addActionListener(resolver);

        table.getSelectionModel().addListSelectionListener(new ObjRelationshipListSelectionListener());

        mediator.getApplication().getActionManager().setupCutCopyPaste(
                table,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
    }

    /**
     * Selects a specified relationship in the relationships table.
     */
    public void selectRelationships(ObjRelationship[] rels) {
        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();

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

        ObjEntity objEntity = (ObjEntity) e.getEntity();
        if (objEntity.getSuperEntity() != null) {
            parentPanel.getToolBar().getComponentAtIndex(2).setEnabled(false);
        } else {
            parentPanel.getToolBar().getComponentAtIndex(2).setEnabled(true);
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

        return map.getNamespace().getObjEntities().stream()
                .sorted(Comparators.getDataMapChildrenComparator())
                .toArray();
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
        model.removeRow((ObjRelationship) e.getRelationship());
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

        ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();
        model.fireTableDataChanged();
    }

    protected void rebuildTable(ObjEntity entity) {
        final ObjRelationshipTableModel model = new ObjRelationshipTableModel(
                entity,
                mediator,
                this);

        model.addTableModelListener(e -> {
            if (table.getSelectedRow() >= 0) {
                ObjRelationship rel = model.getRelationship(table.getSelectedRow());
                enabledResolve = rel.getSourceEntity().getDbEntity() != null;
                resolveMenu.setEnabled(enabledResolve);
            }
        });

        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        TableColumn col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_TARGET_PATH);
        col.setCellEditor(new DbRelationshipPathComboBoxEditor());
        col.setCellRenderer(new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
                setToolTipText("To choose relationship press enter two times.To choose next relationship press dot.");
                return this;
            }
        });

        col = table.getColumnModel().getColumn(ObjRelationshipTableModel.REL_DELETE_RULE);
        JComboBox deleteRulesCombo = Application.getWidgetFactory().createComboBox(
                DELETE_RULES,
                false);
        deleteRulesCombo.setFocusable(false);
        deleteRulesCombo.setEditable(true);
        ((JComponent) deleteRulesCombo.getEditor().getEditorComponent()).setBorder(null);
        deleteRulesCombo.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
        deleteRulesCombo.setSelectedIndex(0); // Default to the first value
        col.setCellEditor(Application.getWidgetFactory().createCellEditor(
                deleteRulesCombo));

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
            value = CellRenderers.asString(value);

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            Icon icon = CellRenderers.iconForObject(oldValue);
            if(isSelected) {
                icon = FilteredIconFactory.createIcon(icon, FilteredIconFactory.FilterType.SELECTION);
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

            if (relationship != null
                    && relationship.getSourceEntity() != model.getEntity()) {
                setForeground(Color.GRAY);
            } else {
                setForeground(isSelected && !hasFocus
                        ? table.getSelectionForeground()
                        : table.getForeground());
            }
            setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
            setFont(UIManager.getFont("Label.font"));

            return this;
        }
    }

    private class ObjRelationshipListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            ObjRelationship[] rels = new ObjRelationship[0];

            if (!e.getValueIsAdjusting() && !((ListSelectionModel) e.getSource()).isSelectionEmpty()) {

                parentPanel.getAttributePanel().getTable().getSelectionModel().clearSelection();
                if (parentPanel.getAttributePanel().getTable().getCellEditor() != null) {
                    parentPanel.getAttributePanel().getTable().getCellEditor().stopCellEditing();
                }
                Application.getInstance().getActionManager().getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getRelationshipPanel());
                Application.getInstance().getActionManager().getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getRelationshipPanel());
                Application.getInstance().getActionManager().getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getRelationshipPanel());
                parentPanel.getResolve().removeActionListener(parentPanel.getAttributePanel().getResolver());
                parentPanel.getResolve().removeActionListener(getResolver());
                parentPanel.getResolve().addActionListener(getResolver());
                parentPanel.getResolve().setToolTipText("Edit Relationship");
                parentPanel.getResolve().setEnabled(true);

                if (table.getSelectedRow() >= 0) {
                    ObjRelationshipTableModel model = (ObjRelationshipTableModel) table.getModel();

                    int[] sel = table.getSelectedRows();
                    rels = new ObjRelationship[sel.length];

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

            mediator.setCurrentObjRelationships(rels);
            parentPanel.updateActions(rels);
        }
    }

    public boolean isEnabledResolve() {
        return enabledResolve;
    }

    public ActionListener getResolver() {
        return resolver;
    }

}