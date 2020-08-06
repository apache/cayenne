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
package org.apache.cayenne.modeler.editor.dbentity;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.util.List;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.DbAttributeListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.util.BoardTableCellRenderer;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.swing.components.LimitedTextField;

/**
 * Detail view of the DbEntity attributes.
 */
public class DbEntityAttributePanel extends JPanel implements DbEntityDisplayListener, DbAttributeListener {

    protected ProjectController mediator;
    protected CayenneTable table;
    private TableColumnPreferences tablePreferences;
    private DbEntityAttributeRelationshipTab parentPanel;


    public DbEntityAttributePanel(ProjectController mediator, DbEntityAttributeRelationshipTab parentPanel) {
        super();
        this.mediator = mediator;
        this.parentPanel = parentPanel;

        // Create and layout components
        initView();
        initController();
    }

    private void initView() {
        this.setLayout(new BorderLayout());

        ActionManager actionManager = Application.getInstance().getActionManager();

        // Create table with two columns and no rows.
        table = new CayenneTable();
        tablePreferences = new TableColumnPreferences(
                DbAttributeTableModel.class,
                "attributeTable");
        table.setDefaultRenderer(String.class, new BoardTableCellRenderer());

        // Create and install a popup
        JPopupMenu popup = new JPopupMenu();
        popup.add(actionManager.getAction(RemoveAttributeRelationshipAction.class).buildMenu());

        popup.addSeparator();
        popup.add(actionManager.getAction(CutAttributeRelationshipAction.class).buildMenu());
        popup.add(actionManager.getAction(CopyAttributeRelationshipAction.class).buildMenu());
        popup.add(actionManager.getAction(PasteAction.class).buildMenu());

        TablePopupHandler.install(table, popup);
        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);

    }

    private void initController() {
        mediator.addDbEntityDisplayListener(this);
        mediator.addDbAttributeListener(this);

        table.getSelectionModel().addListSelectionListener(new DbAttributeListSelectionListener());

        mediator.getApplication().getActionManager().setupCutCopyPaste(
                table,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
    }

    /**
     * Selects specified attributes.
     */
    public void selectAttributes(DbAttribute[] attrs) {
        DbAttributeTableModel model = (DbAttributeTableModel) table.getModel();

        List<?> listAttrs = model.getObjectList();
        int[] newSel = new int[attrs.length];

        Application.getInstance().getActionManager()
                .getAction(RemoveAttributeRelationshipAction.class)
                .setCurrentSelectedPanel(parentPanel.getAttributePanel());
        Application.getInstance().getActionManager()
                .getAction(CutAttributeRelationshipAction.class)
                .setCurrentSelectedPanel(parentPanel.getAttributePanel());
        Application.getInstance().getActionManager()
                .getAction(CopyAttributeRelationshipAction.class)
                .setCurrentSelectedPanel(parentPanel.getAttributePanel());

        parentPanel.updateActions(attrs);

        for (int i = 0; i < attrs.length; i++) {
            newSel[i] = listAttrs.indexOf(attrs[i]);
        }

        table.select(newSel);
    }

    public void dbAttributeChanged(AttributeEvent e) {
        table.select(e.getAttribute());
    }

    public void dbAttributeAdded(AttributeEvent e) {
        rebuildTable((DbEntity) e.getEntity());
        table.select(e.getAttribute());
    }

    public void dbAttributeRemoved(AttributeEvent e) {
        DbAttributeTableModel model = (DbAttributeTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getAttribute());
        model.removeRow((DbAttribute) e.getAttribute());
        table.select(ind);
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {

        DbEntity entity = (DbEntity) e.getEntity();
        if (entity != null && e.isEntityChanged()) {
            rebuildTable(entity);
        }

        // if an entity was selected on a tree, unselect currently selected row
        if (e.isUnselectAttributes()) {
            table.clearSelection();
        }
    }

    protected void rebuildTable(DbEntity ent) {
        if (table.getEditingRow() != -1 && table.getEditingColumn() != -1) {
            TableCellEditor cellEditor = table.getCellEditor(table.getEditingRow(), table.getEditingColumn());
            cellEditor.stopCellEditing();
        }

        DbAttributeTableModel model = new DbAttributeTableModel(ent, mediator, this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        TableColumn col = table.getColumnModel().getColumn(model.typeColumnInd());

        String[] types = TypesMapping.getDatabaseTypes();
        JComboBox comboBox = Application.getWidgetFactory().createComboBox(types, true);

        // Types.NULL makes no sense as a column type
        comboBox.removeItem("NULL");

        AutoCompletion.enable(comboBox);

        col.setCellEditor(Application.getWidgetFactory().createCellEditor(comboBox));

        TableColumn lengthColumn = table.getColumnModel().getColumn(model.lengthColumnId());
        LimitedTextField limitedLengthField = new LimitedTextField(10);
        lengthColumn.setCellEditor(Application.getWidgetFactory().createCellEditor(limitedLengthField));

        TableColumn scaleColumn = table.getColumnModel().getColumn(model.scaleColumnId());
        LimitedTextField limitedScaleField = new LimitedTextField(10);
        scaleColumn.setCellEditor(Application.getWidgetFactory().createCellEditor(limitedScaleField));

        tablePreferences.bind(table, null, null, null, model.nameColumnInd(), true);
    }

    private class DbAttributeListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            DbAttribute[] attrs = new DbAttribute[0];

            if (!e.getValueIsAdjusting() && !((ListSelectionModel) e.getSource()).isSelectionEmpty()) {

                parentPanel.getRelationshipPanel().table.getSelectionModel().clearSelection();
                if (parentPanel.getRelationshipPanel().table.getCellEditor() != null)
                    parentPanel.getRelationshipPanel().table.getCellEditor().stopCellEditing();
                Application.getInstance().getActionManager().getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getAttributePanel());
                Application.getInstance().getActionManager().getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getAttributePanel());
                Application.getInstance().getActionManager().getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getAttributePanel());
                parentPanel.getResolve().setEnabled(false);

                if (table.getSelectedRow() >= 0) {
                    DbAttributeTableModel model = (DbAttributeTableModel) table.getModel();

                    int[] sel = table.getSelectedRows();
                    attrs = new DbAttribute[sel.length];

                    for (int i = 0; i < sel.length; i++) {
                        attrs[i] = model.getAttribute(sel[i]);
                    }

                    if (sel.length == 1) {
                        UIUtil.scrollToSelectedRow(table);
                    }
                }
            }

            mediator.setCurrentDbAttributes(attrs);
            parentPanel.updateActions(attrs);
        }
    }
}