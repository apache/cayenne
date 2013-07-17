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
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ComponentColorModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.JTableHeader;
import org.apache.cayenne.map.CallbackDescriptor;
import org.apache.cayenne.map.CallbackMap;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.AbstractRemoveCallbackMethodAction;
import org.apache.cayenne.modeler.action.CreateCallbackMethodAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.event.CallbackMethodEvent;
import org.apache.cayenne.modeler.event.CallbackMethodListener;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Base abstract class for all calback methids editing tabs Contains logic for callback
 * methods displaying, creating, removing, esiting, reordering
 * 
 */
public abstract class AbstractCallbackMethodsTab extends JPanel {

    private static Log logger = LogFactory.getLog(AbstractCallbackMethodsTab.class);

    /**
     * mediator instance
     */
    ProjectController mediator;

    /**
     * toolbar for actions
     */
    protected JToolBar toolBar;
    
    /**
     * panel for displaying callback method tables
     */
    protected JPanel auxPanel;

    /**
     * preferences for the callback methods table
     */
    protected TableColumnPreferences tablePreferences;

    /**
     * Dropdown for callback type selection. Contains fixed list of 7 callback types.
     */
    protected CallbackType[] callbackTypes = {
                            new CallbackType(LifecycleEvent.POST_ADD, "post-add"),
                            new CallbackType(LifecycleEvent.PRE_PERSIST, "pre-persist"),
                            new CallbackType(LifecycleEvent.POST_PERSIST, "post-persist"),
                            new CallbackType(LifecycleEvent.PRE_UPDATE, "pre-update"),
                            new CallbackType(LifecycleEvent.POST_UPDATE, "post-update"),
                            new CallbackType(LifecycleEvent.PRE_REMOVE, "pre-remove"),
                            new CallbackType(LifecycleEvent.POST_REMOVE, "post-remove"),
                            new CallbackType(LifecycleEvent.POST_LOAD, "post-load"),
                    };

    /**
     * constructor
     * 
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
     * @return create callback method action
     */
    protected CayenneAction getCreateCallbackMethodAction() {
        Application app = Application.getInstance();
        return app.getActionManager().getAction(CreateCallbackMethodAction.class);
    }

    /**
     * @return remove callback method action
     */
    protected AbstractRemoveCallbackMethodAction getRemoveCallbackMethodAction() {
        Application app = Application.getInstance();
        return app.getActionManager().getAction(RemoveCallbackMethodAction.class);
    }

    /**
     * GUI components initialization
     */
    protected void init() {
        this.setLayout(new BorderLayout());

        toolBar = new JToolBar();
        toolBar.add(getRemoveCallbackMethodAction().buildButton());

        add(toolBar, BorderLayout.NORTH);

        auxPanel = new JPanel();
        auxPanel.setOpaque(false);
        auxPanel.setLayout(new BorderLayout());

        initTablePreferences();

        add(new JScrollPane(auxPanel), BorderLayout.CENTER);
    }

    /**
     * Inits the {@link TableColumnPreferences} object according to callback table name.
     */
    protected abstract void initTablePreferences();

    /**
     * listeners initialization
     */
    protected void initController() {
        mediator.addCallbackMethodListener(new CallbackMethodListener() {

            public void callbackMethodChanged(CallbackMethodEvent e) {
                rebuildTables();
            }

            public void callbackMethodAdded(CallbackMethodEvent e) {
                rebuildTables();
            }

            public void callbackMethodRemoved(CallbackMethodEvent e) {
                rebuildTables();
            }
        });
    }

    /**
     * rebuilds table content
     */
    protected void rebuildTables() {
    	FormLayout formLayout = new FormLayout("left:" + auxPanel.getWidth() + "px");
        DefaultFormBuilder builder = new DefaultFormBuilder(formLayout);

    	auxPanel.removeAll();

    	CallbackMap callbackMap = getCallbackMap();
        
        if (callbackMap != null) {
        	for(CallbackType callbackType : callbackTypes) {
        		builder.append(createTable(callbackType));
            }
        }

        auxPanel.add(builder.getPanel(), BorderLayout.CENTER);
        validate();
    }

    private JPanel createTable(final CallbackType callbackType)
    {
   	
    	final CayenneTable cayenneTable = new CayenneTable();

        // drag-and-drop initialization
    	cayenneTable.setDragEnabled(true);
    	
        List<String> methods = new ArrayList<String>();
        CallbackDescriptor descriptor = null;
        CallbackMap callbackMap = getCallbackMap();

        descriptor = callbackMap.getCallbackDescriptor(callbackType.getType());
        for (String callbackMethod : descriptor.getCallbackMethods()) {
            methods.add(callbackMethod);
        }

        final CallbackDescriptorTableModel model = new CallbackDescriptorTableModel(
                mediator,
                this,
                methods,
                descriptor);

        cayenneTable.setModel(model);
        cayenneTable.setRowHeight(25);
        cayenneTable.setRowMargin(3);
        cayenneTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        cayenneTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        cayenneTable.setTransferHandler(new TransferHandler() {

            @Override
            protected Transferable createTransferable(JComponent c) {
                int rowIndex = cayenneTable.getSelectedRow();

                String result = null;
                if (rowIndex >= 0 && rowIndex < cayenneTable.getModel().getRowCount()) {
                    result = String.valueOf(cayenneTable.getModel().getValueAt(
                            rowIndex,
                            CallbackDescriptorTableModel.METHOD_NAME));
                }

                return new StringSelection(result);
            }

            @Override
            public int getSourceActions(JComponent c) {
                return COPY_OR_MOVE;
            }

            @Override
            public boolean importData(JComponent comp, Transferable t) {
                if (canImport(comp, t.getTransferDataFlavors())) {
                    String callbackMethod;
                    try {
                        callbackMethod = (String) t
                                .getTransferData(DataFlavor.stringFlavor);
                    }
                    catch (Exception e) {
                        logger.warn("Error transferring", e);
                        return false;
                    }

                    int rowIndex = cayenneTable.getSelectedRow();

                    CallbackDescriptor callbackDescriptor = ((CallbackDescriptorTableModel)cayenneTable.getCayenneModel()).getCallbackDescriptor();
                    mediator.setDirty(callbackDescriptor.moveMethod(
                            callbackMethod,
                            rowIndex));
                    rebuildTables();
                    return true;
                }

                return false;
            }

            @Override
            public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
                for (DataFlavor flavor : transferFlavors) {
                    if (DataFlavor.stringFlavor.equals(flavor)) {
                        return true;
                    }
                }
                return false;
            }
        });
        
        cayenneTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    String[] methods = new String[0];

                    if (cayenneTable.getSelectedRow() != -1) {
                        int[] sel = cayenneTable.getSelectedRows();
                        methods = new String[sel.length];

                        for (int i = 0; i < sel.length; i++) {
                            methods[i] = (String) cayenneTable
                                    .getValueAt(
                                            sel[i],
                                            cayenneTable
                                                    .convertColumnIndexToView(CallbackDescriptorTableModel.METHOD_NAME));
                        }
                    }

                    LifecycleEvent currentType = ((CallbackDescriptorTableModel)cayenneTable.getCayenneModel()).getCallbackDescriptor().getCallbackType();
                    for(CallbackType callbackType : callbackTypes) {
                    	if(callbackType.getType() == currentType) {
                    		mediator.setCurrentCallbackType(callbackType);
                    		break;
                    	}
                    }

                    mediator.setCurrentCallbackMethods(methods);
                    getRemoveCallbackMethodAction().setEnabled(methods.length > 0);
                    getRemoveCallbackMethodAction().setName(
                            getRemoveCallbackMethodAction().getActionName(
                                    methods.length > 1));
                }
            }
        });
        
        cayenneTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			
        	public void columnMarginChanged(ChangeEvent e) {
        		if(!cayenneTable.getColumnWidthChanged()) {
	        		if(cayenneTable.getTableHeader().getResizingColumn() != null) {
		        		tablePreferences.bind(cayenneTable, null, null, null);
	      				cayenneTable.setColumnWidthChanged(true);
	                }
        		}
            }

			public void columnSelectionChanged(ListSelectionEvent e) {
				
			}
			
			public void columnRemoved(TableColumnModelEvent e) {
				
			}
			
			public void columnMoved(TableColumnModelEvent e) {
				
			}
		
			public void columnAdded(TableColumnModelEvent e) {
				
			}
		});

        cayenneTable.getTableHeader().addMouseListener(new MouseAdapter()
		{
		    public void mouseReleased(MouseEvent e)
		    {
		        if(cayenneTable.getColumnWidthChanged())
		        {
		        	if(cayenneTable.getWidth() <= 60)
		        		cayenneTable.setSize(50, cayenneTable.getHeight());
		        	rebuildTables();
		        	cayenneTable.setColumnWidthChanged(false);
		        }
		    }
		});
        
        tablePreferences.bind(cayenneTable, null, null, null);

        // Create and install a popup
        JPopupMenu popup = new JPopupMenu();
        popup.add(getRemoveCallbackMethodAction().buildMenu());

        TablePopupHandler.install(cayenneTable, popup);
        
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        addButtonAtHeader(cayenneTable, getCreateCallbackMethodAction().buildButton(), new ButtonListener(callbackType), ModelerUtil.buildIcon("icon-create-method.gif"));
       
        panel.add(cayenneTable.getTableHeader(), BorderLayout.NORTH);
        panel.add(cayenneTable, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void addButtonAtHeader(JTable table, JButton button, ActionListener buttonListener, ImageIcon buttonIcon){
        PanelBuilder builder = new PanelBuilder(new FormLayout("left:10dlu, 2dlu", "center:10dlu"));
        CellConstraints cc = new CellConstraints();
        
        button.setIcon(ModelerUtil.buildIcon("icon-create-method.gif"));
        button.setOpaque(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.addActionListener(buttonListener);
        
        builder.add(button, cc.xy(1, 1));
        
        JPanel buttonPanel = builder.getPanel();
        buttonPanel.setOpaque(false);
       
        JTableHeader header = table.getTableHeader();
        //header.setMinimumSize(new Dimension(50, header.getHeight()));
        header.setLayout(new BorderLayout());
        header.add(buttonPanel, BorderLayout.EAST);
    }

    class ButtonListener implements ActionListener  
    {  
    	private CallbackType callbackType;
    	
    	public ButtonListener(CallbackType callbackType){
    		this.callbackType = callbackType;
    	}
    	
        public void actionPerformed(ActionEvent e) {
        	mediator.setCurrentCallbackType(callbackType);
        }
    } 

    protected final CallbackType getSelectedCallbackType() {
    	return mediator.getCurrentCallbackType();
    }    
}
