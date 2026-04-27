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
package org.apache.cayenne.modeler.ui.project.editor.embeddable.attributes;

import org.apache.cayenne.modeler.ui.project.editor.query.ExistingSelectionProcessor;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.modeler.event.model.EmbeddableAttributeEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableAttributeListener;
import org.apache.cayenne.modeler.event.model.EmbeddableEvent;
import org.apache.cayenne.modeler.event.model.EmbeddableListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.modeler.service.action.GlobalActions;
import org.apache.cayenne.modeler.ui.action.CopyAttributeAction;
import org.apache.cayenne.modeler.ui.action.CreateAttributeAction;
import org.apache.cayenne.modeler.ui.action.CutAttributeAction;
import org.apache.cayenne.modeler.ui.action.PasteAction;
import org.apache.cayenne.modeler.ui.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.event.display.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.display.EmbeddableDisplayListener;
import org.apache.cayenne.modeler.event.display.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.toolkit.table.CayenneTable;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.toolkit.WidgetFactory;
import org.apache.cayenne.modeler.toolkit.combo.AutoCompletion;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.EventObject;
import java.util.List;

public class EmbeddableAttributesView extends JPanel implements
        EmbeddableAttributeListener, EmbeddableDisplayListener, EmbeddableListener,
        ExistingSelectionProcessor {

    private final ProjectController controller;
    private CayenneTable table;
    private TableColumnPreferences tablePreferences;

    public EmbeddableAttributesView(ProjectController controller) {
        this.controller = controller;
        init();
        initController();
    }

    private void init() {
        this.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        GlobalActions globalActions = Application.getInstance().getActionManager();

        toolBar.add(globalActions.getAction(CreateAttributeAction.class).buildButton());
        toolBar.addSeparator();

        toolBar.add(globalActions.getAction(RemoveAttributeAction.class).buildButton());
        toolBar.addSeparator();

        toolBar.add(globalActions.getAction(CutAttributeAction.class).buildButton(1));
        toolBar.add(globalActions.getAction(CopyAttributeAction.class).buildButton(2));
        toolBar.add(globalActions.getAction(PasteAction.class).buildButton(3));

        add(toolBar, BorderLayout.NORTH);

        table = new CayenneTable();

        tablePreferences = new TableColumnPreferences(
                this.getClass(),
                "embeddable/attributeTable");

        // Create and install a popup
        JPopupMenu popup = new JPopupMenu();
        popup.add(globalActions.getAction(RemoveAttributeAction.class).buildMenu());

        popup.addSeparator();

        popup.add(globalActions.getAction(CutAttributeAction.class).buildMenu());
        popup.add(globalActions.getAction(CopyAttributeAction.class).buildMenu());
        popup.add(globalActions.getAction(PasteAction.class).buildMenu());

        TablePopupHandler.install(table, popup);
        add(WidgetFactory.createTablePanel(table, null), BorderLayout.CENTER);
    }

    private void initController() {
        controller.addEmbeddableAttributeListener(this);
        controller.addEmbeddableDisplayListener(this);
        controller.addEmbeddableListener(this);

        table.getSelectionModel().addListSelectionListener(this::processExistingSelection);

        controller.getApplication().getActionManager().setupCutCopyPaste(
                table,
                CutAttributeAction.class,
                CopyAttributeAction.class);
    }

    public void processExistingSelection(EventObject e) {

        if (e instanceof ChangeEvent) {
            table.clearSelection();
        }

        EmbeddableAttribute[] attrs = new EmbeddableAttribute[0];
        if (table.getSelectedRow() >= 0) {
            EmbeddableAttributeTableModel model = (EmbeddableAttributeTableModel) table
                    .getModel();

            int[] sel = table.getSelectedRows();
            attrs = new EmbeddableAttribute[sel.length];

            for (int i = 0; i < sel.length; i++) {
                attrs[i] = model.getEmbeddableAttribute(sel[i]);
            }

            if (sel.length == 1) {
                UIUtil.scrollToSelectedRow(table);
            }
        }

        EmbeddableAttributeDisplayEvent ev = new EmbeddableAttributeDisplayEvent(
                this,
                controller.getSelectedEmbeddable(),
                attrs,
                controller.getSelectedDataMap(),
                (DataChannelDescriptor) controller.getProject().getRootNode());

        controller.displayEmbeddableAttribute(ev);
    }

    private void rebuildTable(Embeddable emb) {
        EmbeddableAttributeTableModel model = new EmbeddableAttributeTableModel(
                emb,
                controller,
                this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);
        setUpTableStructure();
    }

    private void setUpTableStructure() {

        TableColumn typeColumn = table.getColumnModel().getColumn(EmbeddableAttributeTableModel.OBJ_ATTRIBUTE_TYPE);
        JComboBox javaTypesCombo = WidgetFactory.createComboBox(
                ModelerUtil.getRegisteredTypeNames(),
                false);
        AutoCompletion.enable(javaTypesCombo, false, true);
        typeColumn.setCellEditor(WidgetFactory.createCellEditor(
                javaTypesCombo));

        tablePreferences.bind(
                table,
                null,
                null,
                null,
                EmbeddableAttributeTableModel.OBJ_ATTRIBUTE,
                true);

    }

    /**
     * Selects a specified attribute.
     */
    public void selectAttributes(EmbeddableAttribute[] embAttrs) {
        ModelerUtil.updateActions(
                embAttrs.length,
                RemoveAttributeAction.class,
                CopyAttributeAction.class,
                CutAttributeAction.class);

        EmbeddableAttributeTableModel model = (EmbeddableAttributeTableModel) table
                .getModel();

        List listAttrs = model.getObjectList();
        int[] newSel = new int[embAttrs.length];

        for (int i = 0; i < embAttrs.length; i++) {
            newSel[i] = listAttrs.indexOf(embAttrs[i]);
        }

        table.select(newSel);
    }

    public void embeddableAttributeAdded(EmbeddableAttributeEvent e) {
        rebuildTable(e.getEmbeddable());
        table.select(e.getEmbeddableAttribute());
    }

    public void embeddableAttributeChanged(EmbeddableAttributeEvent e) {
        table.select(e.getEmbeddableAttribute());
    }

    public void embeddableAttributeRemoved(EmbeddableAttributeEvent e) {

        EmbeddableAttributeTableModel model = (EmbeddableAttributeTableModel) table
                .getModel();
        int ind = model.getObjectList().indexOf(e.getEmbeddableAttribute());
        model.removeRow(e.getEmbeddableAttribute());
        table.select(ind);
    }

    public void embeddableSelected(EmbeddableDisplayEvent e) {
        if (e.getSource() == this) {
            return;
        }

        Embeddable embeddable = e.getEmbeddable();
        if (embeddable != null) {
            rebuildTable(embeddable);
        }
    }

    public void embeddableAdded(EmbeddableEvent e, DataMap map) {
    }

    public void embeddableRemoved(EmbeddableEvent e, DataMap map) {
    }

    public void embeddableChanged(EmbeddableEvent e, DataMap map) {
        if (e.getOldName() != null) {
            map.getEmbeddable(e.getOldName()).setClassName(e
                    .getEmbeddable()
                    .getClassName());
            if (map.getEmbeddableMap().containsKey(e.getOldName())) {
                map.removeEmbeddable(e.getOldName());
                map.addEmbeddable(e.getEmbeddable());
            }
        }
    }
}
