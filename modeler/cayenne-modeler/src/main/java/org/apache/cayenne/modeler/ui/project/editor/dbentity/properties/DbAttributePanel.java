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

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.modeler.event.display.DbAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.display.TablePopupHandler;
import org.apache.cayenne.modeler.event.model.DbAttributeEvent;
import org.apache.cayenne.modeler.event.model.DbAttributeListener;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.toolkit.combobox.AutoCompletion;
import org.apache.cayenne.modeler.toolkit.combobox.CMComboBox;
import org.apache.cayenne.modeler.toolkit.table.BoardTableCellRenderer;
import org.apache.cayenne.modeler.toolkit.table.CMComboBoxCellEditor;
import org.apache.cayenne.modeler.toolkit.table.CMTable;
import org.apache.cayenne.modeler.toolkit.table.CMTablePanel;
import org.apache.cayenne.modeler.toolkit.table.CMTablePrefs;
import org.apache.cayenne.modeler.ui.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.ui.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.ui.action.PasteAction;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeRelationshipAction;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Detail view of the DbEntity attributes.
 */
public class DbAttributePanel extends JPanel implements DbEntityDisplayListener, DbAttributeListener {

    private final ProjectSession session;
    private final CMTable table;
    private final DbEntityPropertiesView parentPanel;

    public DbAttributePanel(ProjectSession session, DbEntityPropertiesView parentPanel) {
        this.session = session;
        this.parentPanel = parentPanel;
        this.table = new CMTable();
        initLayout();
        initBindings();
    }

    private void initLayout() {
        setLayout(new BorderLayout());

        GlobalActions globalActions = session.app().getActionManager();

        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setDefaultRenderer(String.class, new BoardTableCellRenderer());

        JPopupMenu popup = new JPopupMenu();
        popup.add(globalActions.getAction(RemoveAttributeRelationshipAction.class).buildMenu());
        popup.addSeparator();
        popup.add(globalActions.getAction(CutAttributeRelationshipAction.class).buildMenu());
        popup.add(globalActions.getAction(CopyAttributeRelationshipAction.class).buildMenu());
        popup.add(globalActions.getAction(PasteAction.class).buildMenu());

        TablePopupHandler.install(table, popup);
        add(new CMTablePanel(table), BorderLayout.CENTER);
    }

    private void initBindings() {
        session.addDbEntityDisplayListener(this);
        session.addDbAttributeListener(this);

        table.getSelectionModel().addListSelectionListener(this::valueChanged);

        session.app().getActionManager().setupCutCopyPaste(
                table,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
    }

    public CMTable getTable() {
        return table;
    }

    public void selectAttributes(DbAttribute[] attrs) {
        DbAttributeTableModel model = (DbAttributeTableModel) table.getModel();

        List<?> listAttrs = model.getObjectList();
        int[] newSel = new int[attrs.length];

        session.app().getActionManager()
                .getAction(RemoveAttributeRelationshipAction.class)
                .setCurrentSelectedPanel(parentPanel.getAttributePanel());
        session.app().getActionManager()
                .getAction(CutAttributeRelationshipAction.class)
                .setCurrentSelectedPanel(parentPanel.getAttributePanel());
        session.app().getActionManager()
                .getAction(CopyAttributeRelationshipAction.class)
                .setCurrentSelectedPanel(parentPanel.getAttributePanel());

        parentPanel.updateActions(attrs);

        for (int i = 0; i < attrs.length; i++) {
            newSel[i] = listAttrs.indexOf(attrs[i]);
        }

        table.select(newSel);


    }

    @Override
    public void dbAttributeChanged(DbAttributeEvent e) {
        table.select(e.getAttribute());
    }

    @Override
    public void dbAttributeAdded(DbAttributeEvent e) {
        rebuildTable(e.getEntity());
        table.select(e.getAttribute());
    }

    @Override
    public void dbAttributeRemoved(DbAttributeEvent e) {
        DbAttributeTableModel model = (DbAttributeTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getAttribute());
        model.removeRow(e.getAttribute());
        table.select(ind);
    }

    @Override
    public void dbEntitySelected(DbEntityDisplayEvent e) {

        DbEntity entity = e.getEntity();
        if (entity != null) {
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

        DbAttributeTableModel model = new DbAttributeTableModel(ent, session, this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        String[] types = TypesMapping.getDatabaseTypes();
        Arrays.sort(types);
        JComboBox<String> comboBox = new CMComboBox<>(types);

        // Types.NULL makes no sense as a column type
        comboBox.removeItem("NULL");
        AutoCompletion.enable(comboBox, session::getSelectedDataMap);

        TableColumn typeColumn = table.getColumnModel().getColumn(DbAttributeTableModel.DB_ATTRIBUTE_TYPE);
        typeColumn.setCellEditor(new CMComboBoxCellEditor(comboBox));

        CMTablePrefs.of(session.app().getPreferencesRepository(), "dbEntity/attributeTable")
                .bind(table, null, DbAttributeTableModel.DB_ATTRIBUTE_NAME);
    }

    private void valueChanged(ListSelectionEvent e) {

        if (e.getValueIsAdjusting()) {
            return;
        }

        DbAttribute[] attrs = new DbAttribute[0];

        if (!((ListSelectionModel) e.getSource()).isSelectionEmpty()) {

            parentPanel.getRelationshipPanel().getTable().getSelectionModel().clearSelection();
            if (parentPanel.getRelationshipPanel().getTable().getCellEditor() != null) {
                parentPanel.getRelationshipPanel().getTable().getCellEditor().stopCellEditing();
            }

            GlobalActions globalActions = session.app().getActionManager();

            globalActions.getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
            globalActions.getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
            globalActions.getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(this);

            if (table.getSelectedRow() >= 0) {
                DbAttributeTableModel model = (DbAttributeTableModel) table.getModel();

                int[] sel = table.getSelectedRows();
                attrs = new DbAttribute[sel.length];

                for (int i = 0; i < sel.length; i++) {
                    attrs[i] = model.getAttribute(sel[i]);
                }

                if (sel.length == 1) {
                    table.scrollToSelectedRow();
                }
            }
        }

        session.displayDbAttribute(new DbAttributeDisplayEvent(
                this,
                session.getSelectedDataDomain(),
                session.getSelectedDataMap(),
                session.getSelectedDbEntity(),
                attrs));

        parentPanel.updateActions(attrs);
    }
}