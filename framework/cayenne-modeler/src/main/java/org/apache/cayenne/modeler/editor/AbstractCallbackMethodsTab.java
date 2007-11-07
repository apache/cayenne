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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.LifecycleListener;
import org.apache.cayenne.map.*;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.CallbackMethodListener;
import org.apache.cayenne.modeler.event.CallbackMethodEvent;
import org.apache.cayenne.modeler.action.AbstractRemoveCallbackMethodAction;
import org.apache.cayenne.modeler.action.CreateCallbackMethodAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.util.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Base abstract class for all calback methids editing tabs
 * Contains logic for callback methods displaying, creating, removing, esiting, reordering
 *
 * @author Vasil Tarasevich
 * @version 1.0 Oct 23, 2007
 */

public abstract class AbstractCallbackMethodsTab extends JPanel {
    /**
     * mediator instance
     */
    ProjectController mediator;

    /**
     * toolbar for actions
     */
    protected JToolBar toolBar;

    /**
     * table for displaying callback method names
     */
    protected CayenneTable table;

    /**
     * Dropdown for callback type selection.
     * Contains fixed list of 7 callback types.
     */
    protected JComboBox callbackTypeCombo = CayenneWidgetFactory.createComboBox(
            new Object[]{
                    new CallbackType(LifecycleListener.PRE_PERSIST, "pre-persist"),
                    new CallbackType(LifecycleListener.POST_PERSIST, "post-persist"),
                    new CallbackType(LifecycleListener.PRE_UPDATE, "pre-update"),
                    new CallbackType(LifecycleListener.POST_UPDATE, "post-update"),
                    new CallbackType(LifecycleListener.PRE_REMOVE, "pre-remove"),
                    new CallbackType(LifecycleListener.POST_REMOVE, "post-remove"),
                    new CallbackType(LifecycleListener.POST_LOAD, "post-load"),
            }, false);

    /**
     * constructor
     * @param mediator mediator instance
     */
    public AbstractCallbackMethodsTab(ProjectController mediator) {
        this.mediator = mediator;
        init();
        initController();
    }

    /**
     * @return CallbackMap with callback methods
     */
    protected abstract CallbackMap getCallbackMap();

    /**
     * creates filter pane for filtering callback methods list
     * adds callback method type dropdown
     * @param builder forms builder
     */
    protected void buildFilter(DefaultFormBuilder builder) {
        JLabel callbacktypeLabel = new JLabel("Callback type:");
        builder.append(callbacktypeLabel, callbackTypeCombo);
    }

    /**
     * @return create callback method action
     */
    protected CayenneAction getCreateCallbackMethodAction() {
        Application app = Application.getInstance();
        return app.getAction(CreateCallbackMethodAction.ACTION_NAME);
    }

    /**
     * @return remove callback method action
     */
    protected AbstractRemoveCallbackMethodAction getRemoveCallbackMethodAction() {
        Application app = Application.getInstance();
        return (AbstractRemoveCallbackMethodAction)app.getAction(RemoveCallbackMethodAction.ACTION_NAME);
    }

    /**
     * GUI components initialization
     */
    protected void init() {
        this.setLayout(new BorderLayout());

        toolBar = new JToolBar();
        toolBar.add(getCreateCallbackMethodAction().buildButton());
        toolBar.add(getRemoveCallbackMethodAction().buildButton());
        add(toolBar, BorderLayout.NORTH);

        JPanel auxPanel = new JPanel();
        auxPanel.setOpaque(false);
        auxPanel.setLayout(new BorderLayout());

        FormLayout formLayout = new FormLayout("right:70dlu, 3dlu, fill:150dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(formLayout);
        buildFilter(builder);
        auxPanel.add(builder.getPanel(), BorderLayout.NORTH);

        table = new CayenneTable();
        table.setDefaultRenderer(String.class, new StringRenderer());
        table.getTableHeader().setReorderingAllowed(false);

        //drag-and-drop initialization
        table.setDragEnabled(true);
        table.setTransferHandler(new StringTransferHandler() {
            protected String exportString(JComponent c) {
                JTable table = (JTable)c;
                int rowIndex = table.getSelectedRow();

                String result = null;
                if (rowIndex >= 0 && rowIndex < table.getModel().getRowCount()) {
                    result = String.valueOf(table.getModel().getValueAt(rowIndex, 0));
                }

                return result;
            }

            protected void importString(JComponent c, String callbackMethod) {
                JTable table = (JTable)c;
                int rowIndex = table.getSelectedRow();

                //move callback method inside of model
                CallbackDescriptor callbackDescriptor =
                        getCallbackMap().getCallbackDescriptor(((CallbackType)callbackTypeCombo.getSelectedItem()).getType());
                mediator.setDirty(callbackDescriptor.moveMethod(callbackMethod, rowIndex));
                rebuildTable();
            }

            protected void cleanup(JComponent c, boolean remove) {
                //System.out.println("c");
            }
        });
        auxPanel.add(PanelFactory.createTablePanel(table, null), BorderLayout.CENTER);

        add(auxPanel, BorderLayout.CENTER);
    }

    /**
     * listeners initialization
     */
    protected void initController() {
        mediator.addCallbackMethodListener(
                new CallbackMethodListener() {
                    public void callbackMethodChanged(CallbackMethodEvent e) {
                        if (isVisible()) {
                            rebuildTable();
                        }
                    }

                    public void callbackMethodAdded(CallbackMethodEvent e) {
                        if (isVisible()) {
                            updateCallbackTypeCounters();
                            rebuildTable();
                        }
                    }

                    public void callbackMethodRemoved(CallbackMethodEvent e) {
                        if (isVisible()) {
                            updateCallbackTypeCounters();
                            rebuildTable();
                        }
                    }
                }
        );

        callbackTypeCombo.addItemListener(
                new ItemListener() {
                    public void itemStateChanged(ItemEvent e) {
                        if (e.getStateChange() == ItemEvent.SELECTED) {
                            mediator.setCurrentCallbackType((CallbackType)callbackTypeCombo.getSelectedItem());
                            updateCallbackTypeCounters();
                            rebuildTable();
                        }
                    }
                }
        );

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    mediator.setCurrentCallbackMethod(
                            table.getSelectedRow() >= 0 ?
                                (String)table.getModel().getValueAt(table.getSelectedRow(), 0) : null);
                    getRemoveCallbackMethodAction().setEnabled(table.getSelectedRow() >= 0);
                }
            }
        });
    }

    protected void updateCallbackTypeCounters() {
        CallbackMap map = getCallbackMap();
        
        for (int i = 0; i < callbackTypeCombo.getItemCount(); i++) {
            CallbackType type = (CallbackType) callbackTypeCombo.getItemAt(i);

            if (map == null) {
                type.setCounter(0);
            }
            else {
                CallbackDescriptor callbackDescriptor = map.getCallbackDescriptor(type
                        .getType());
                type.setCounter(callbackDescriptor.getCallbackMethods().size());
            }
        }
        callbackTypeCombo.repaint();
    }


    /**
     * rebuilds table content
     */
    protected void rebuildTable() {
        CallbackType callbackType = (CallbackType)callbackTypeCombo.getSelectedItem();
        List methods = new ArrayList();
        CallbackDescriptor descriptor = null;
        CallbackMap callbackMap = getCallbackMap();
        if (callbackMap != null && callbackType != null) {
            descriptor = callbackMap.getCallbackDescriptor(callbackType.getType());
            for (Iterator j = descriptor.getCallbackMethods().iterator(); j.hasNext();) {
                methods.add(j.next());
            }
        }

        final CallbackDescriptorTableModel model = new CallbackDescriptorTableModel(
                mediator,
                this,
                methods,
                descriptor);

        table.setModel(model);
        table.setRowHeight(25);
        table.setRowMargin(3);

        TableColumn methodNameColumn =
                table.getColumnModel().getColumn(CallbackDescriptorTableModel.METHOD_NAME);
        methodNameColumn.setMinWidth(424);

        mediator.setCurrentCallbackMethod(null);
    }

    /**
     * class for renderig string values
     */
    class StringRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            // center cardinality column
            int align = column == ObjRelationshipTableModel.REL_SEMANTICS
                        ? JLabel.CENTER
                        : JLabel.LEFT;
            super.setHorizontalAlignment(align);

            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column);

            ObjRelationshipTableModel model = (ObjRelationshipTableModel) table
                    .getModel();
            ObjRelationship relationship = model.getRelationship(row);

            if (relationship != null
                && relationship.getSourceEntity() != model.getEntity()) {
                setForeground(Color.GRAY);
            } else {
                setForeground(isSelected && !hasFocus
                              ? table.getSelectionForeground()
                              : table.getForeground());
            }

            return this;
        }
    }

    protected final CallbackType getSelectedCallbackType() {
        CallbackType selectedType = (CallbackType)callbackTypeCombo.getSelectedItem();
        if (selectedType == null) {
                selectedType = (CallbackType)callbackTypeCombo.getItemAt(0);
        }
        return selectedType;
    }
}


