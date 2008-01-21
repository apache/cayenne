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

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
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
import org.apache.cayenne.modeler.action.CreateProcedureParameterAction;
import org.apache.cayenne.modeler.action.RemoveProcedureParameterAction;
import org.apache.cayenne.modeler.event.ProcedureDisplayEvent;
import org.apache.cayenne.modeler.event.ProcedureDisplayListener;
import org.apache.cayenne.modeler.event.ProcedureParameterDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.PanelFactory;
import org.apache.cayenne.modeler.util.UIUtil;

/**
 * @author Andrus Adamchik
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
    }

    protected void init() {
        setLayout(new BorderLayout());

        JToolBar toolBar = new JToolBar();

        Application app = Application.getInstance();
        toolBar.add(app.getAction(CreateProcedureParameterAction.getActionName()).buildButton());
        removeParameterButton = app.getAction(RemoveProcedureParameterAction.getActionName()).buildButton();
        toolBar.add(removeParameterButton);
        toolBar.addSeparator();

        moveUp = new JButton();
        moveUp.setIcon(ModelerUtil.buildIcon("icon-move_up.gif"));
        moveUp.setToolTipText("Move Parameter Up");
        toolBar.add(moveUp);
        
        moveDown = new JButton();
        moveDown.setIcon(ModelerUtil.buildIcon("icon-move_down.gif"));
        moveDown.setToolTipText("Move Parameter Down");
        toolBar.add(moveDown);
        
        add(toolBar, BorderLayout.NORTH);

        // Create table with two columns and no rows.
        table = new CayenneTable();
        add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);
    }
    
    public void processExistingSelection(EventObject e) {
        if (e instanceof ChangeEvent){
            table.clearSelection();
        }

        ProcedureParameter parameter = null;
        boolean enableUp = false;
        boolean enableDown = false;
        boolean enableRemoveButton = false;

        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            enableRemoveButton = true;
            ProcedureParameterTableModel model =
                (ProcedureParameterTableModel) table.getModel();
            parameter = model.getParameter(table.getSelectedRow());

            // scroll table
            UIUtil.scrollToSelectedRow(table);

            int rowCount = table.getRowCount();
            if (rowCount > 1){
                if (selectedRow >0) enableUp = true;
                if (selectedRow < (rowCount - 1)) enableDown = true;
            }
        }

        removeParameterButton.setEnabled(enableRemoveButton);
        moveUp.setEnabled(enableUp);
        moveDown.setEnabled(enableDown);

        ProcedureParameterDisplayEvent ppde =
            new ProcedureParameterDisplayEvent(
                this,
                parameter,
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
     * Selects a specified parameter.
     */
    public void selectParameter(ProcedureParameter parameter) {
        if (parameter == null) {
            return;
        }

        ProcedureParameterTableModel model =
            (ProcedureParameterTableModel) table.getModel();
        java.util.List parameters = model.getObjectList();
        int pos = parameters.indexOf(parameter);
        if (pos >= 0) {
            table.select(pos);
        }
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
        typesEditor.setEditable(true);
        typesColumn.setCellEditor(new DefaultCellEditor(typesEditor));

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
        directionColumn.setCellEditor(new DefaultCellEditor(directionEditor));

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

        if (e.getSource() == moveUp) {
            index = model.moveRowUp(parameter);
        }
        else if (e.getSource() == moveDown) {
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
}
