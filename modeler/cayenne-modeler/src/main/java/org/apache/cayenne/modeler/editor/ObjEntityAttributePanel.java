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
package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjAttributeListener;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.CopyAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.CutAttributeRelationshipAction;
import org.apache.cayenne.modeler.action.ObjEntityToSuperEntityAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.dialog.objentity.ObjAttributeInfoDialog;
import org.apache.cayenne.modeler.editor.wrapper.ObjAttributeWrapper;
import org.apache.cayenne.modeler.event.display.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.display.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.model.ProjectOnSaveEvent;
import org.apache.cayenne.modeler.event.model.ProjectOnSaveListener;
import org.apache.cayenne.modeler.event.display.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.DbAttributePathComboBoxEditor;
import org.apache.cayenne.modeler.util.DbAttributePathComboBoxRenderer;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Detail view of the ObjEntity attributes.
 */
public class ObjEntityAttributePanel extends JPanel implements ObjEntityDisplayListener, ObjEntityListener, ObjAttributeListener, ProjectOnSaveListener {

    private static final ImageIcon INHERITANCE_ICON = ModelerUtil.buildIcon("icon-inheritance.png");

    private final ProjectController controller;
    private final ObjEntityAttributeRelationshipTab parentPanel;

    private final CayenneTable table;
    private final TableColumnPreferences tablePreferences;
    private final JMenuItem editMenu;

    public ObjEntityAttributePanel(ProjectController controller, ObjEntityAttributeRelationshipTab parentPanel) {
        this.controller = controller;
        this.parentPanel = parentPanel;

        this.setLayout(new BorderLayout());

        ActionManager actionManager = Application.getInstance().getActionManager();

        table = new CayenneTable();
        table.setDefaultRenderer(String.class, new CellRenderer());
        tablePreferences = new TableColumnPreferences(
                ObjAttributeTableModel.class,
                "objEntity/attributeTable");

        // go to SuperEntity from ObjEntity by inheritance icon
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                ObjAttribute objAttribute = ((ObjAttributeTableModel) table.getModel()).getAttribute(row).getValue();
                int columnFromModel = table.getColumnModel().getColumn(col).getModelIndex();
                if (row >= 0 && columnFromModel == ObjAttributeTableModel.OBJ_ATTRIBUTE) {
                    if (objAttribute.isInherited()) {
                        TableCellRenderer renderer = table.getCellRenderer(row, col);
                        Rectangle rectangle = table.getCellRect(row, col, false);
                        ((CellRenderer) renderer).mouseClicked(e, rectangle.x);
                    }
                }
            }
        });

        this.editMenu = new JMenuItem("Edit Attribute", ModelerUtil.buildIcon("icon-edit.png"));
        editMenu.addActionListener(this::edit);

        JPopupMenu popup = new JPopupMenu();
        popup.add(editMenu);
        popup.add(actionManager.getAction(RemoveAttributeRelationshipAction.class).buildMenu());

        popup.addSeparator();
        popup.add(actionManager.getAction(CutAttributeRelationshipAction.class).buildMenu());
        popup.add(actionManager.getAction(CopyAttributeRelationshipAction.class).buildMenu());
        popup.add(actionManager.getAction(PasteAction.class).buildMenu());

        TablePopupHandler.install(table, popup);
        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);

        controller.addObjEntityDisplayListener(this);
        controller.addObjEntityListener(this);
        controller.addObjAttributeListener(this);

        table.getSelectionModel().addListSelectionListener(this::valueChanged);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        actionManager.setupCutCopyPaste(
                table,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
    }

    public CayenneTable getTable() {
        return table;
    }

    public void initComboBoxes() {
        List<String> embeddableNames = new ArrayList<>();
        List<String> typeNames = new ArrayList<>();

        for (DataMap dataMap : ((DataChannelDescriptor) controller.getProject().getRootNode()).getDataMaps()) {
            for (Embeddable emb : dataMap.getEmbeddables()) {
                embeddableNames.add(emb.getClassName());
            }
        }

        String[] registeredTypes = ModelerUtil.getRegisteredTypeNames();
        Collections.addAll(typeNames, registeredTypes);
        typeNames.addAll(embeddableNames);

        TableColumn typeColumn = table.getColumnModel().getColumn(ObjAttributeTableModel.OBJ_ATTRIBUTE_TYPE);

        JComboBox<String> javaTypesCombo = Application.getWidgetFactory().createComboBox(typeNames.toArray(new String[0]), false);
        AutoCompletion.enable(javaTypesCombo, false, true);
        typeColumn.setCellEditor(Application.getWidgetFactory().createCellEditor(javaTypesCombo));
    }

    /**
     * Selects a specified attribute.
     */
    public void selectAttributes(ObjAttribute[] attrs) {
        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

        List<ObjAttributeWrapper> listAttrs = model.getObjectList();
        int[] newSel = new int[attrs.length];

        parentPanel.updateActions(attrs);

        // search for attributes to select from attributes that model has
        for (int i = 0; i < attrs.length; i++) {
            for (int j = 0; j < listAttrs.size(); j++) {
                if (listAttrs.get(j).getValue() == attrs[i]) {
                    newSel[i] = j;
                    break;
                }
            }
        }

        table.select(newSel);

        parentPanel.rebindEditButton(attrs.length > 0, "Edit Attribute", this::edit);
    }

    public void objAttributeChanged(AttributeEvent e) {
        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

        if (!model.isValid()) {
            model.resetModel();
        }

        model.fireTableDataChanged();

        int ind = -1;
        List<ObjAttributeWrapper> list = model.getObjectList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue() == e.getAttribute()) {
                ind = i;
            }
        }

        table.select(ind);
        if (e.getOldName() != null) {
            removeDuplicateAttribute(e);
        }
    }

    public void objAttributeAdded(AttributeEvent e) {
        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

        if (!model.isValid()) {
            model.resetModel();
        }

        model.addRow(new ObjAttributeWrapper((ObjAttribute) e.getAttribute()));
        model.fireTableDataChanged();

        int ind = -1;
        List<ObjAttributeWrapper> list = model.getObjectList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue() == e.getAttribute()) {
                ind = i;
            }
        }

        table.select(ind);
    }

    public void objAttributeRemoved(AttributeEvent e) {
        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
        int ind = -1;
        List<ObjAttributeWrapper> list = model.getObjectList();

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue() == e.getAttribute()) {
                ind = i;
            }
        }

        if (!model.isValid()) {
            model.resetModel();
        }

        if (ind >= 0) {
            model.removeRow(list.get(ind));
            model.fireTableDataChanged();
            table.select(ind);
        }
    }

    public void removeDuplicateAttribute(AttributeEvent e) {
        Collection<ObjEntity> objEntities = ProjectUtil.getCollectionOfChildren((ObjEntity) e.getEntity());


        for (ObjEntity objEntity : objEntities) {
            if (objEntity.getDeclaredAttribute(e.getAttribute().getName()) != null) {

                JOptionPane pane = new JOptionPane(
                        String.format("'%s' and '%s' can't both have attribute '%s'. " +
                                        "Would you like to delete this attribute from the '%s'?",
                                objEntity.getName(), e.getEntity().getName(), e.getAttribute().getName(), objEntity.getName()),
                        JOptionPane.QUESTION_MESSAGE,
                        JOptionPane.YES_NO_OPTION);

                JDialog dialog = pane.createDialog(Application.getFrame(), "Confirm Remove");
                dialog.setVisible(true);

                boolean shouldDelete;
                Object selectedValue = pane.getValue();
                shouldDelete = selectedValue != null && selectedValue.equals(JOptionPane.YES_OPTION);
                if (shouldDelete) {
                    objEntity.removeAttribute(e.getAttribute().getName());
                    objEntity.removeAttributeOverride(e.getAttribute().getName());
                }
            }
        }
    }

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
    }

    protected void rebuildTable(ObjEntity entity) {
        if (table.getEditingRow() != -1 && table.getEditingColumn() != -1) {
            TableCellEditor cellEditor = table.getCellEditor(table.getEditingRow(), table.getEditingColumn());
            cellEditor.stopCellEditing();
        }
        ObjAttributeTableModel model = new ObjAttributeTableModel(entity, controller, this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);
        setUpTableStructure();
    }

    private void setUpTableStructure() {
        Map<Integer, Integer> minSizes = new HashMap<>();
        minSizes.put(ObjAttributeTableModel.OBJ_ATTRIBUTE, 150);

        initComboBoxes();

        table.getColumnModel().getColumn(ObjAttributeTableModel.DB_ATTRIBUTE).setCellRenderer(new DbAttributePathComboBoxRenderer());
        table.getColumnModel().getColumn(ObjAttributeTableModel.DB_ATTRIBUTE).setCellEditor(new DbAttributePathComboBoxEditor());

        tablePreferences.bind(
                table,
                minSizes,
                null,
                null,
                ObjAttributeTableModel.OBJ_ATTRIBUTE,
                true);
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
            setUpTableStructure();
        }
    }

    public void objEntityAdded(EntityEvent e) {
        if (e.getSource() == this) {
            return;
        }

        this.rebuildTable((ObjEntity) e.getEntity());
    }

    public void objEntityRemoved(EntityEvent e) {
    }

    // custom renderer used for inherited attributes highlighting
    static final class CellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();
            column = table.getColumnModel().getColumn(column).getModelIndex();
            ObjAttribute attribute = model.getAttribute(row).getValue();

            if (!model.isCellEditable(row, column)) {
                setForeground(isSelected ? new Color(0xEEEEEE) : Color.GRAY);
            } else {
                setForeground(isSelected && !hasFocus ? table.getSelectionForeground() : table.getForeground());
            }

            setIcon(null);

            if (attribute.isInherited()) {
                Font font = getFont();
                Font newFont = font.deriveFont(Font.ITALIC);
                setFont(newFont);
                if (column == ObjAttributeTableModel.OBJ_ATTRIBUTE) {
                    setIcon(INHERITANCE_ICON);
                }
            }

            setFont(UIManager.getFont("Label.font"));
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

            return this;
        }

        public void mouseClicked(MouseEvent event, int x) {
            Point point = event.getPoint();
            if (point.x - x <= INHERITANCE_ICON.getIconWidth()) {
                ActionManager actionManager = Application.getInstance().getActionManager();
                actionManager.getAction(ObjEntityToSuperEntityAction.class).performAction(null);
            }
        }
    }

    private void resetTableModel() {
        CayenneTableModel model = table.getCayenneModel();
        if (model != null && !model.isValid()) {
            model.resetModel();
            model.fireTableDataChanged();
        }
    }

    @Override
    public void beforeSaveChanges(ProjectOnSaveEvent e) {
        resetTableModel();
    }

    private void edit(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }

        ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

        // ... show dialog...
        new ObjAttributeInfoDialog(controller, row, model).startupAction();

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

        ObjAttribute[] attrs = new ObjAttribute[0];

        if (!((ListSelectionModel) e.getSource()).isSelectionEmpty()) {

            parentPanel.getRelationshipPanel().getTable().getSelectionModel().clearSelection();
            if (parentPanel.getRelationshipPanel().getTable().getCellEditor() != null) {
                parentPanel.getRelationshipPanel().getTable().getCellEditor().stopCellEditing();
            }

            ActionManager actionManager = Application.getInstance().getActionManager();
            actionManager.getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
            actionManager.getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(this);
            actionManager.getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(this);

            boolean editEnabled = table.getSelectedRow() >= 0;
            parentPanel.rebindEditButton(editEnabled, "Edit Attribute", this::edit);

            if (editEnabled) {
                ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

                int[] sel = table.getSelectedRows();
                attrs = new ObjAttribute[sel.length];

                for (int i = 0; i < sel.length; i++) {
                    attrs[i] = model.getAttribute(sel[i]).getValue();
                }

                if (sel.length == 1) {
                    UIUtil.scrollToSelectedRow(table);
                }
            }

            editMenu.setEnabled(editEnabled);
        }

        controller.displayObjAttribute(new AttributeDisplayEvent(
                this,
                attrs,
                controller.getSelectedObjEntity(),
                controller.getSelectedDataMap(),
                controller.getSelectedDataDomain()));

        parentPanel.updateActions(attrs);
    }
}