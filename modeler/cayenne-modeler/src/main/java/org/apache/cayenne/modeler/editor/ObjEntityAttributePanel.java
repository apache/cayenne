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
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeRelationshipAction;
import org.apache.cayenne.modeler.dialog.objentity.ObjAttributeInfoDialog;
import org.apache.cayenne.modeler.editor.wrapper.ObjAttributeWrapper;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.ObjEntityDisplayListener;
import org.apache.cayenne.modeler.event.ProjectOnSaveEvent;
import org.apache.cayenne.modeler.event.ProjectOnSaveListener;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.DbAttributePathComboBoxRenderer;
import org.apache.cayenne.modeler.util.DbAttributePathComboBoxEditor;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Detail view of the ObjEntity attributes.
 */
public class ObjEntityAttributePanel extends JPanel implements ObjEntityDisplayListener,
        ObjEntityListener, ObjAttributeListener, ProjectOnSaveListener {

    private static final int INHERITANCE_COLUMN_WIDTH = 35;

    private ProjectController mediator;
    private CayenneTable table;
    private TableColumnPreferences tablePreferences;
    private ObjEntityAttributeRelationshipTab parentPanel;
    private boolean enabledResolve;//for JBottom "resolve" in ObjEntityAttrRelationshipTab

    private ActionListener resolver;

    /**
     * By now popup menu item is made similar to toolbar button. (i.e. all functionality
     * is here) This should be probably refactored as Action.
     */
    private JMenuItem resolveMenu;

    public ObjEntityAttributePanel(ProjectController mediator, ObjEntityAttributeRelationshipTab parentPanel) {
        this.mediator = mediator;
        this.parentPanel = parentPanel;

        initView();
        initController();
    }

    public CayenneTable getTable() {
        return table;
    }

    public void setTable(CayenneTable table) {
        this.table = table;
    }

    private void initView() {
        this.setLayout(new BorderLayout());

        ActionManager actionManager = Application.getInstance().getActionManager();

        table = new CayenneTable();
        table.setDefaultRenderer(String.class, new CellRenderer());
        tablePreferences = new TableColumnPreferences(
                ObjAttributeTableModel.class,
                "objEntity/attributeTable");

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
        mediator.addObjEntityDisplayListener(this);
        mediator.addObjEntityListener(this);
        mediator.addObjAttributeListener(this);

        resolver = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int row = table.getSelectedRow();
                if (row < 0) {
                    return;
                }

                ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

                // ... show dialog...
                new ObjAttributeInfoDialog(mediator, row, model).startupAction();

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

        table.getSelectionModel().addListSelectionListener(new ObjAttributeListSelectionListener());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        mediator.getApplication().getActionManager().setupCutCopyPaste(
                table,
                CutAttributeRelationshipAction.class,
                CopyAttributeRelationshipAction.class);
    }

    public void initComboBoxes() {
        List<String> embeddableNames = new ArrayList<String>();
        List<String> typeNames = new ArrayList<String>();

        Iterator it = ((DataChannelDescriptor) mediator.getProject().getRootNode())
                .getDataMaps()
                .iterator();
        while (it.hasNext()) {
            DataMap dataMap = (DataMap) it.next();
            for (Embeddable emb : dataMap.getEmbeddables()) {
                embeddableNames.add(emb.getClassName());
            }
        }

        String[] registeredTypes = ModelerUtil.getRegisteredTypeNames();
        Collections.addAll(typeNames, registeredTypes);
        typeNames.addAll(embeddableNames);

        TableColumn typeColumn = table.getColumnModel().getColumn(
                ObjAttributeTableModel.OBJ_ATTRIBUTE_TYPE);

        JComboBox javaTypesCombo = Application.getWidgetFactory().createComboBox(
                typeNames.toArray(),
                false);
        AutoCompletion.enable(javaTypesCombo, false, true);
        typeColumn.setCellEditor(Application.getWidgetFactory().createCellEditor(
                javaTypesCombo));
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
        if (table.getEditingRow() != -1 && table.getEditingColumn() != -1) {
            TableCellEditor cellEditor = table.getCellEditor(table.getEditingRow(), table.getEditingColumn());
            cellEditor.stopCellEditing();
        }
        ObjAttributeTableModel model = new ObjAttributeTableModel(entity, mediator, this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);
        setUpTableStructure();
    }

    protected void setUpTableStructure() {
        Map<Integer, Integer> minSizes = new HashMap<>();
        Map<Integer, Integer> maxSizes = new HashMap<>();

        minSizes.put(ObjAttributeTableModel.INHERITED, INHERITANCE_COLUMN_WIDTH);
        maxSizes.put(ObjAttributeTableModel.INHERITED, INHERITANCE_COLUMN_WIDTH);

        initComboBoxes();

        table.getColumnModel().getColumn(3).setCellRenderer(new DbAttributePathComboBoxRenderer());
        table.getColumnModel().getColumn(3).setCellEditor(new DbAttributePathComboBoxEditor());

        tablePreferences.bind(
                table,
                minSizes,
                maxSizes,
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
    final class CellRenderer extends DefaultTableCellRenderer {

        @Override
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
            column = table.getColumnModel().getColumn(column).getModelIndex();
            ObjAttribute attribute = model.getAttribute(row).getValue();
            if (column != ObjAttributeTableModel.INHERITED) {

                if (!model.isCellEditable(row, column)) {
                    setForeground(Color.GRAY);
                } else {
                    setForeground(isSelected && !hasFocus ? table
                            .getSelectionForeground() : table.getForeground());
                }

                if (attribute.isInherited()) {
                    Font font = getFont();
                    Font newFont = font.deriveFont(Font.ITALIC);
                    setFont(newFont);
                }
                setIcon(null);
            } else {
                if (attribute.isInherited()) {
                    ImageIcon objEntityIcon = ModelerUtil.buildIcon("icon-override.gif");
                    setIcon(objEntityIcon);
                }
                setText("");
            }
            setBorder(BorderFactory.createEmptyBorder(0,5,0,0));

            return this;
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

    private class ObjAttributeListSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            ObjAttribute[] attrs = new ObjAttribute[0];

            if (!e.getValueIsAdjusting() && !((ListSelectionModel) e.getSource()).isSelectionEmpty()) {

                parentPanel.getRelationshipPanel().getTable().getSelectionModel().clearSelection();
                if (parentPanel.getRelationshipPanel().getTable().getCellEditor() != null) {
                    parentPanel.getRelationshipPanel().getTable().getCellEditor().stopCellEditing();
                }
                Application.getInstance().getActionManager().getAction(RemoveAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getAttributePanel());
                Application.getInstance().getActionManager().getAction(CutAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getAttributePanel());
                Application.getInstance().getActionManager().getAction(CopyAttributeRelationshipAction.class).setCurrentSelectedPanel(parentPanel.getAttributePanel());
                parentPanel.getResolve().removeActionListener(parentPanel.getRelationshipPanel().getResolver());
                parentPanel.getResolve().removeActionListener(getResolver());
                parentPanel.getResolve().addActionListener(getResolver());
                parentPanel.getResolve().setToolTipText("Edit Attribute");
                parentPanel.getResolve().setEnabled(true);

                if (table.getSelectedRow() >= 0) {
                    ObjAttributeTableModel model = (ObjAttributeTableModel) table.getModel();

                    int[] sel = table.getSelectedRows();
                    attrs = new ObjAttribute[sel.length];

                    for (int i = 0; i < sel.length; i++) {
                        attrs[i] = model.getAttribute(sel[i]).getValue();
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

            mediator.setCurrentObjAttributes(attrs);
            parentPanel.updateActions(attrs);
        }
    }

    public boolean isEnabledResolve() {
        return enabledResolve;
    }

    public ActionListener getResolver() {
        return resolver;
    }


}