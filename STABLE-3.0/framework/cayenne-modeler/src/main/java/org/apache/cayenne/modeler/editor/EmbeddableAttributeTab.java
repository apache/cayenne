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
import java.util.EventObject;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.event.EmbeddableAttributeEvent;
import org.apache.cayenne.map.event.EmbeddableAttributeListener;
import org.apache.cayenne.map.event.EmbeddableEvent;
import org.apache.cayenne.map.event.EmbeddableListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CopyAttributeAction;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CutAttributeAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.event.EmbeddableAttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayEvent;
import org.apache.cayenne.modeler.event.EmbeddableDisplayListener;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;

public class EmbeddableAttributeTab extends JPanel implements
        EmbeddableAttributeListener, EmbeddableDisplayListener, EmbeddableListener,
        ExistingSelectionProcessor {

    protected ProjectController mediator;
    protected CayenneTable table;

    JButton resolve;

    public EmbeddableAttributeTab(ProjectController mediator) {
        this.mediator = mediator;
        init();
        initController();
    }

    private void init() {
        this.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        Application app = Application.getInstance();

        toolBar.add(app.getAction(CreateAttributeAction.getActionName()).buildButton());
        toolBar.addSeparator();

        toolBar.add(app.getAction(RemoveAttributeAction.getActionName()).buildButton());
        toolBar.addSeparator();

        toolBar.add(app.getAction(CutAttributeAction.getActionName()).buildButton());
        toolBar.add(app.getAction(CopyAttributeAction.getActionName()).buildButton());
        toolBar.add(app.getAction(PasteAction.getActionName()).buildButton());

        add(toolBar, BorderLayout.NORTH);

        table = new CayenneTable();

        /**
         * Create and install a popup
         */
        JPopupMenu popup = new JPopupMenu();
        popup.add(app.getAction(RemoveAttributeAction.getActionName()).buildMenu());

        popup.addSeparator();

        popup.add(app.getAction(CutAttributeAction.getActionName()).buildMenu());
        popup.add(app.getAction(CopyAttributeAction.getActionName()).buildMenu());
        popup.add(app.getAction(PasteAction.getActionName()).buildMenu());

        TablePopupHandler.install(table, popup);
        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);
    }

    private void initController() {
        mediator.addEmbeddableAttributeListener(this);
        mediator.addEmbeddableDisplayListener(this);
        mediator.addEmbeddableListener(this);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                processExistingSelection(e);
            }
        });

        mediator.getApplication().getActionManager().setupCCP(
                table,
                CutAttributeAction.getActionName(),
                CopyAttributeAction.getActionName());
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
                mediator.getCurrentEmbeddable(),
                attrs,
                mediator.getCurrentDataMap(),
                mediator.getCurrentDataDomain());

        mediator.fireEmbeddableAttributeDisplayEvent(ev);
    }

    private void rebuildTable(Embeddable emb) {
        EmbeddableAttributeTableModel model = new EmbeddableAttributeTableModel(
                emb,
                mediator,
                this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);
        setUpTableStructure(model);
    }

    private void setUpTableStructure(EmbeddableAttributeTableModel model) {

        TableColumn nameColumn = table.getColumnModel().getColumn(
                EmbeddableAttributeTableModel.OBJ_ATTRIBUTE);
        nameColumn.setMinWidth(150);

        TableColumn typeColumn = table.getColumnModel().getColumn(
                EmbeddableAttributeTableModel.OBJ_ATTRIBUTE_TYPE);
        typeColumn.setMinWidth(150);

        JComboBox javaTypesCombo = CayenneWidgetFactory.createComboBox(ModelerUtil
                .getRegisteredTypeNames(), false);
        AutoCompletion.enable(javaTypesCombo, false, true);
        typeColumn.setCellEditor(CayenneWidgetFactory.createCellEditor(javaTypesCombo));

        TableColumn dbAttrColumn = table.getColumnModel().getColumn(
                EmbeddableAttributeTableModel.DB_ATTRIBUTE);
        dbAttrColumn.setMinWidth(150);

    }

    /**
     * Selects a specified attribute.
     */
    public void selectAttributes(EmbeddableAttribute[] embAttrs) {
        ModelerUtil.updateActions(
                embAttrs.length,
                RemoveAttributeAction.getActionName(),
                CopyAttributeAction.getActionName(),
                CutAttributeAction.getActionName());

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
        rebuildTable((Embeddable) e.getEmbeddable());
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

    public void currentEmbeddableChanged(EmbeddableDisplayEvent e) {
        if (e.getSource() == this) {
            return;
        }

        Embeddable embeddable = (Embeddable) e.getEmbeddable();
        if (embeddable != null) {
            rebuildTable(embeddable);
        }

    }

    public void embeddableAdded(EmbeddableEvent e, DataMap map) {
    }

    public void embeddableChanged(EmbeddableEvent e, DataMap map) {
        if (e.getOldName() != null) {
            ((Embeddable)map.getEmbeddable(e.getOldName())).setClassName(e.getEmbeddable().getClassName());
            if(map.getEmbeddableMap().containsKey(e.getOldName())){
                map.removeEmbeddable(e.getOldName());
                map.addEmbeddable(e.getEmbeddable());
            }
        }
    }

    public void embeddableRemoved(EmbeddableEvent e, DataMap map) {
    }
}
