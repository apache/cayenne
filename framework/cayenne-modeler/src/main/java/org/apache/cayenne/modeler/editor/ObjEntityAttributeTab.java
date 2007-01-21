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
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjAttributeListener;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.ObjEntitySyncAction;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;

/**
 * Detail view of the ObjEntity attributes.
 * 
 * @author Michael Misha Shengaout
 * @author Andrus Adamchik
 */
public class ObjEntityAttributeTab extends JPanel implements ObjEntityDisplayListener,
        ObjEntityListener, ObjAttributeListener, ExistingSelectionProcessor {

    protected ProjectController mediator;
    protected CayenneTable table;

    public ObjEntityAttributeTab(ProjectController mediator) {
        this.mediator = mediator;

        init();
        initController();
    }

    private void init() {
        this.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        Application app = Application.getInstance();
        toolBar.add(app.getAction(CreateAttributeAction.getActionName()).buildButton());
        toolBar.add(app.getAction(ObjEntitySyncAction.getActionName()).buildButton());
        toolBar.addSeparator();
        toolBar.add(app.getAction(RemoveAttributeAction.getActionName()).buildButton());
        add(toolBar, BorderLayout.NORTH);

        table = new CayenneTable();
        table.setDefaultRenderer(String.class, new CellRenderer());

        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);
    }

    private void initController() {
        mediator.addObjEntityDisplayListener(this);
        mediator.addObjEntityListener(this);
        mediator.addObjAttributeListener(this);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                processExistingSelection(e);
            }
        });
    }

    /**
     * Selects a specified attribute.
     */
    public void selectAttribute(ObjAttribute attr) {
        if (attr == null) {
            Application
                    .getInstance()
                    .getAction(RemoveAttributeAction.getActionName())
                    .setEnabled(false);
            return;
        }
        // enable the remove button
        Application
                .getInstance()
                .getAction(RemoveAttributeAction.getActionName())
                .setEnabled(true);

        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
        java.util.List attrs = model.getObjectList();
        int attrPos = attrs.indexOf(attr);
        if (attrPos >= 0) {
            table.select(attrPos);
        }
    }

    public void processExistingSelection(EventObject e) {
        if (e instanceof ChangeEvent) {
            table.clearSelection();
        }
        ObjAttribute attribute = null;
        if (table.getSelectedRow() >= 0) {
            ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
            attribute = model.getAttribute(table.getSelectedRow());

            // scroll table
            UIUtil.scrollToSelectedRow(table);
        }

        AttributeDisplayEvent ev = new AttributeDisplayEvent(this, attribute, mediator
                .getCurrentObjEntity(), mediator.getCurrentDataMap(), mediator
                .getCurrentDataDomain());

        mediator.fireObjAttributeDisplayEvent(ev);
    }

    public void objAttributeChanged(AttributeEvent e) {
        table.select(e.getAttribute());
    }

    public void objAttributeAdded(AttributeEvent e) {
        rebuildTable((ObjEntity) e.getEntity());
        table.select(e.getAttribute());
    }

    public void objAttributeRemoved(AttributeEvent e) {
        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getAttribute());
        model.removeRow(e.getAttribute());
        table.select(ind);
    }

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

    protected void rebuildTable(ObjEntity entity) {
        ObjAttributeTableModel model = new ObjAttributeTableModel(entity, mediator, this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);
        setUpTableStructure(model);
    }

    protected void setUpTableStructure(ObjAttributeTableModel model) {
        TableColumn nameColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.OBJ_ATTRIBUTE);
        nameColumn.setMinWidth(150);

        TableColumn typeColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.OBJ_ATTRIBUTE_TYPE);
        typeColumn.setMinWidth(150);

        JComboBox javaTypesCombo = CayenneWidgetFactory.createComboBox(ModelerUtil
                .getRegisteredTypeNames(), false);
        javaTypesCombo.setEditable(true);
        typeColumn.setCellEditor(new DefaultCellEditor(javaTypesCombo));

        TableColumn lockColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.LOCKING);
        lockColumn.setMinWidth(100);

        TableColumn dbTypeColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.DB_ATTRIBUTE_TYPE);
        dbTypeColumn.setMinWidth(120);

        TableColumn dbNameColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.DB_ATTRIBUTE);
        dbNameColumn.setMinWidth(150);

        if (model.getEntity().getDbEntity() != null) {
            JComboBox dbAttributesCombo = CayenneWidgetFactory
                    .createComboBox(ModelerUtil.getDbAttributeNames(mediator, model
                            .getEntity()
                            .getDbEntity()), true);

            dbAttributesCombo.setEditable(false);
            dbNameColumn.setCellEditor(new DefaultCellEditor(dbAttributesCombo));
        }
    }

    /**
     * Refreshes attributes view for the updated entity
     */
    public void objEntityChanged(EntityEvent e) {
        if (e.getSource() == this) {
            return;
        }

        if (!(table.getModel() instanceof ObjAttributeTableModel)) {
            // probably means this panel hasn't been loaded yet...
            return;
        }

        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
        if (model.getDbEntity() != ((ObjEntity) e.getEntity()).getDbEntity()) {
            model.resetDbEntity();
            setUpTableStructure(model);
        }
    }

    public void objEntityAdded(EntityEvent e) {
    }

    public void objEntityRemoved(EntityEvent e) {
    }

    // custom renderer used for inherited attributes highlighting
    final class CellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
            ObjAttribute attribute = model.getAttribute(row);

            if (attribute != null && attribute.getEntity() != model.getEntity()) {
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
