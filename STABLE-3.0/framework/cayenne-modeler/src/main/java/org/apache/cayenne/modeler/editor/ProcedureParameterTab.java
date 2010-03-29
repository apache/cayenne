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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.map.event.ProcedureEvent;
import org.apache.cayenne.map.event.ProcedureParameterEvent;
import org.apache.cayenne.map.event.ProcedureParameterListener;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CopyProcedureParameterAction;
import org.apache.cayenne.modeler.action.CreateProcedureParameterAction;
import org.apache.cayenne.modeler.action.CutProcedureParameterAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveProcedureParameterAction;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayListener;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.util.CayenneCellEditor;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;

/**
 */
public class ProcedureParameterTab
    extends JPanel
    implements
        ProcedureParameterListener,
        ProcedureDisplayListener,
        ExistingSelectionProcessor,
        ActionListener {
    
    protected ProjectController eventController;

    protected CayenneTable table;
    protected JButton removeParameterButton;
    protected JButton moveUp;
    protected JButton moveDown;
    
    /**
     * By now popup menu items are made similiar to toolbar button. 
     * (i.e. all functionality is here)
     * This should be probably refactored as Action.
     */
    protected JMenuItem removeParameterMenu;
    protected JMenuItem moveUpMenu;
    protected JMenuItem moveDownMenu;

    public ProcedureParameterTab(ProjectController eventController) {
        this.eventController = eventController;

        init();

        eventController.addProcedureDisplayListener(this);
        eventController.addProcedureParameterListener(this);

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                processExistingSelection(e);
            }
        });
        
        moveDown.addActionListener(this);
        moveUp.addActionListener(this);
        
        moveDownMenu.addActionListener(this);
        moveUpMenu.addActionListener(this);
    }

    protected void init() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();

        Application app = Application.getInstance();
        toolBar.add(app.getAction(CreateProcedureParameterAction.getActionName()).buildButton());
        removeParameterButton = app.getAction(RemoveProcedureParameterAction.getActionName()).buildButton();
        toolBar.add(removeParameterButton);
        toolBar.addSeparator();
        
        Icon up = ModelerUtil.buildIcon("icon-move_up.gif");
        Icon down = ModelerUtil.buildIcon("icon-move_down.gif");

        moveUp = new JButton();
        moveUp.setIcon(up);
        moveUp.setToolTipText("Move Parameter Up");
        toolBar.add(moveUp);
        
        moveDown = new JButton();
        moveDown.setIcon(down);
        moveDown.setToolTipText("Move Parameter Down");
        toolBar.add(moveDown);
        
        toolBar.addSeparator();
        toolBar.add(app.getAction(CutProcedureParameterAction.getActionName()).buildButton());
        toolBar.add(app.getAction(CopyProcedureParameterAction.getActionName()).buildButton());
        toolBar.add(app.getAction(PasteAction.getActionName()).buildButton());
        
        add(toolBar, BorderLayout.NORTH);

        // Create table with two columns and no rows.
        table = new CayenneTable();
        
        /**
         * Create and install a popup
         */
        JPopupMenu popup = new JPopupMenu();
        
        removeParameterMenu = app.getAction(RemoveProcedureParameterAction.getActionName()).buildMenu(); 
        
        popup.add(removeParameterMenu);
        popup.addSeparator();
        
        moveUpMenu = new JMenuItem("Move Parameter Up", up);
        moveDownMenu = new JMenuItem("Move Parameter Down", down);
        
        popup.add(moveUpMenu);
        popup.add(moveDownMenu);
        
        popup.addSeparator();
        popup.add(app.getAction(CutProcedureParameterAction.getActionName()).buildMenu());
        popup.add(app.getAction(CopyProcedureParameterAction.getActionName()).buildMenu());
        popup.add(app.getAction(PasteAction.getActionName()).buildMenu());
        
        TablePopupHandler.install(table, popup);
        
        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);
        
        eventController.getApplication().getActionManager().setupCCP(table, 
                CutProcedureParameterAction.getActionName(), 
                CopyProcedureParameterAction
                
                .getActionName());
    }
    
    public void processExistingSelection(EventObject e) {
        if (e instanceof ChangeEvent){
            table.clearSelection();
        }

        ProcedureParameter[] parameters = new ProcedureParameter[0];
        boolean enableUp = false;
        boolean enableDown = false;
        boolean enableRemoveButton = false;

        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            enableRemoveButton = true;
            ProcedureParameterTableModel model =
                (ProcedureParameterTableModel) table.getModel();
            
            int[] sel = table.getSelectedRows();
            parameters = new ProcedureParameter[sel.length];
            
            for (int i = 0; i < sel.length; i++) {
                parameters[i] = model.getParameter(sel[i]);
            }

            if (sel.length == 1) {
                // scroll table
                UIUtil.scrollToSelectedRow(table);

                int rowCount = table.getRowCount();
                if (rowCount > 1) {
                    if (selectedRow >0) {
                        enableUp = true;
                    }
                    if (selectedRow < (rowCount - 1)) {
                        enableDown = true;
                    }
                }
            }
        }

        removeParameterButton.setEnabled(enableRemoveButton);
        moveUp.setEnabled(enableUp);
        moveDown.setEnabled(enableDown);
        
        syncButtons();

        ProcedureParameterDisplayEvent ppde =
            new ProcedureParameterDisplayEvent(
                this,
                parameters,
                eventController.getCurrentProcedure(),
                eventController.getCurrentDataMap(),
                eventController.getCurrentDataDomain());
        eventController.fireProcedureParameterDisplayEvent(ppde);
    }

    /**
      * Invoked when currently selected Procedure object is changed.
      */
    public void currentProcedureChanged(ProcedureDisplayEvent e) {
        Procedure procedure = e.getProcedure();
        if (procedure != null && e.isProcedureChanged()) {
            rebuildTable(procedure);
        }
    }

    /**
     * Selects a specified parameters.
     */
    public void selectParameters(ProcedureParameter[] parameters) {
        ModelerUtil.updateActions(parameters.length,  
                RemoveProcedureParameterAction.getActionName(),
                CutProcedureParameterAction.getActionName(),
                CopyProcedureParameterAction.getActionName());

        ProcedureParameterTableModel model =
            (ProcedureParameterTableModel) table.getModel();
        
        List listAttrs = model.getObjectList();
        int[] newSel = new int[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            newSel[i] = listAttrs.indexOf(parameters[i]);
        }
        
        table.select(newSel);
    }

    protected void rebuildTable(Procedure procedure) {
        ProcedureParameterTableModel model =
            new ProcedureParameterTableModel(procedure, eventController, this);

        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        // number column tweaking
        TableColumn numberColumn =
            table.getColumnModel().getColumn(
                ProcedureParameterTableModel.PARAMETER_NUMBER);
        numberColumn.setPreferredWidth(35);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        numberColumn.setCellRenderer(renderer);

        // name column tweaking
        TableColumn nameColumn =
            table.getColumnModel().getColumn(ProcedureParameterTableModel.PARAMETER_NAME);
        nameColumn.setMinWidth(150);

        // types column tweaking
        TableColumn typesColumn =
            table.getColumnModel().getColumn(ProcedureParameterTableModel.PARAMETER_TYPE);
        typesColumn.setMinWidth(90);

        JComboBox typesEditor =
            CayenneWidgetFactory.createComboBox(TypesMapping.getDatabaseTypes(), true);
        AutoCompletion.enable(typesEditor);
        typesColumn.setCellEditor(CayenneWidgetFactory.createCellEditor(typesEditor));

        // direction column tweaking
        TableColumn directionColumn =
            table.getColumnModel().getColumn(
                ProcedureParameterTableModel.PARAMETER_DIRECTION);
        directionColumn.setMinWidth(90);

        JComboBox directionEditor =
            CayenneWidgetFactory.createComboBox(
                ProcedureParameterTableModel.PARAMETER_DIRECTION_NAMES,
                false);
        directionEditor.setEditable(false);
        directionColumn.setCellEditor(new CayenneCellEditor(directionEditor));

        moveUp.setEnabled(false);
        moveDown.setEnabled(false);
    }

    public void procedureParameterAdded(ProcedureParameterEvent e) {
        rebuildTable(e.getParameter().getProcedure());
        table.select(e.getParameter());
    }

    public void procedureParameterChanged(ProcedureParameterEvent e) {
        table.select(e.getParameter());
    }

    public void procedureParameterRemoved(ProcedureParameterEvent e) {
        ProcedureParameterTableModel model =
            (ProcedureParameterTableModel) table.getModel();
        int ind = model.getObjectList().indexOf(e.getParameter());
        model.removeRow(e.getParameter());
        table.select(ind);
    }

    public void actionPerformed(ActionEvent e) {
        ProcedureParameterTableModel model =
            (ProcedureParameterTableModel) table.getModel();
        ProcedureParameter parameter = model.getParameter(table.getSelectedRow());
        
        int index = -1;

        if (e.getSource() == moveUp || e.getSource() == moveUpMenu) {
            index = model.moveRowUp(parameter);
        }
        else if (e.getSource() == moveDown || e.getSource() == moveDownMenu) {
            index = model.moveRowDown(parameter);
        }

        if (index >= 0) {
            table.select(index);
            
            // note that 'setCallParameters' is donw by copy internally
            parameter.getProcedure().setCallParameters(model.getObjectList());
            eventController.fireProcedureEvent(
                new ProcedureEvent(this, parameter.getProcedure(), MapEvent.CHANGE));
        }
    }
    
    /**
     * Synchronizes state of toolbar and popup menu buttons
     */
    private void syncButtons() {
        removeParameterMenu.setEnabled(removeParameterButton.isEnabled());
        moveUpMenu.setEnabled(moveUp.isEnabled());
        moveDownMenu.setEnabled(moveDown.isEnabled());   
    }
}
