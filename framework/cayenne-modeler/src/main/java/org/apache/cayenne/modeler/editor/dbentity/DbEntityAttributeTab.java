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
import java.util.EventObject;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.DbAttributeListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CopyAttributeAction;
import org.apache.cayenne.modeler.action.CreateAttributeAction;
import org.apache.cayenne.modeler.action.CreateObjEntityAction;
import org.apache.cayenne.modeler.action.CutAttributeAction;
import org.apache.cayenne.modeler.action.DbEntitySyncAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveAttributeAction;
import org.apache.cayenne.modeler.editor.ExistingSelectionProcessor;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.DbEntityDisplayListener;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;

/**
 * Detail view of the DbEntity attributes.
 * 
 */
public class DbEntityAttributeTab extends JPanel implements DbEntityDisplayListener,
        ListSelectionListener, DbAttributeListener, ExistingSelectionProcessor {

    protected ProjectController mediator;
    protected CayenneTable table;

    public DbEntityAttributeTab(ProjectController temp_mediator) {
        super();
        mediator = temp_mediator;
        mediator.addDbEntityDisplayListener(this);
        mediator.addDbAttributeListener(this);

        // Create and layout components
        init();
    }

    private void init() {
        this.setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();
        Application app = Application.getInstance();
        toolBar.add(app.getAction(CreateObjEntityAction.getActionName()).buildButton());
        toolBar.add(app.getAction(CreateAttributeAction.getActionName()).buildButton());
        toolBar.add(app.getAction(DbEntitySyncAction.getActionName()).buildButton());

        toolBar.addSeparator();

        toolBar.addSeparator();
        toolBar.add(app.getAction(RemoveAttributeAction.getActionName()).buildButton());
        
        toolBar.addSeparator();
        toolBar.add(app.getAction(CutAttributeAction.getActionName()).buildButton());
        toolBar.add(app.getAction(CopyAttributeAction.getActionName()).buildButton());
        toolBar.add(app.getAction(PasteAction.getActionName()).buildButton());

        add(toolBar, BorderLayout.NORTH);
        
        // Create table with two columns and no rows.
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
        
        mediator.getApplication().getActionManager().setupCCP(table, 
                CutAttributeAction.getActionName(), CopyAttributeAction.getActionName());
    }

    public void valueChanged(ListSelectionEvent e) {
        processExistingSelection(e);
    }

    /**
     * Selects specified attributes.
     */
    public void selectAttributes(DbAttribute[] attrs) {
        ModelerUtil.updateActions(attrs.length,  
                RemoveAttributeAction.getActionName(),
                CutAttributeAction.getActionName(),
                CopyAttributeAction.getActionName());

        DbAttributeTableModel model = (DbAttributeTableModel) table.getModel();
        
        List<?> listAttrs = model.getObjectList();
        int[] newSel = new int[attrs.length];
        
        for (int i = 0; i < attrs.length; i++) {
            newSel[i] = listAttrs.indexOf(attrs[i]);
        }
        
        table.select(newSel);
    }

    public void processExistingSelection(EventObject e) {
        if (e instanceof ChangeEvent) {
            table.clearSelection();
        }
            
        DbAttribute[] attrs = new DbAttribute[0];
        if (table.getSelectedRow() >= 0) {
           DbAttributeTableModel model = (DbAttributeTableModel) table.getModel();
           
           int[] sel = table.getSelectedRows();
           attrs = new DbAttribute[sel.length];
           
           for (int i = 0; i < sel.length; i++) {
               attrs[i] = model.getAttribute(sel[i]);
           }
    
           if (sel.length == 1) {
               // scroll table
               UIUtil.scrollToSelectedRow(table);
           }
        }
    
        mediator.fireDbAttributeDisplayEvent(new AttributeDisplayEvent(
               this,
               attrs,
               mediator.getCurrentDbEntity(),
               mediator.getCurrentDataMap(),
               mediator.getCurrentDataDomain()));
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
        model.removeRow(e.getAttribute());
        table.select(ind);
    }

    public void currentDbEntityChanged(EntityDisplayEvent e) {
        DbEntity entity = (DbEntity) e.getEntity();
        if (entity != null && e.isEntityChanged()) {
            rebuildTable(entity);
        }

        // if an entity was selected on a tree,
        // unselect currently selected row
        if (e.isUnselectAttributes()) {
            table.clearSelection();
        }
    }

    protected void rebuildTable(DbEntity ent) {

        DbAttributeTableModel model = new DbAttributeTableModel(ent, mediator, this);
        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);
        TableColumn col = table.getColumnModel().getColumn(model.nameColumnInd());
        col.setMinWidth(150);

        col = table.getColumnModel().getColumn(model.typeColumnInd());
        col.setMinWidth(90);

        String[] types = TypesMapping.getDatabaseTypes();
        JComboBox comboBox = CayenneWidgetFactory.createComboBox(types, true);
        
        // Types.NULL makes no sense as a column type
        comboBox.removeItem("NULL");
        
        AutoCompletion.enable(comboBox);
        
        col.setCellEditor(CayenneWidgetFactory.createCellEditor(comboBox));

        table.getSelectionModel().addListSelectionListener(this);
    }
}
