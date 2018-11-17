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
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.cayenne.map.CallbackDescriptor;
import org.apache.cayenne.map.CallbackMap;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.CopyCallbackMethodAction;
import org.apache.cayenne.modeler.action.CreateCallbackMethodAction;
import org.apache.cayenne.modeler.action.CutCallbackMethodAction;
import org.apache.cayenne.modeler.action.PasteAction;
import org.apache.cayenne.modeler.action.RemoveCallbackMethodAction;
import org.apache.cayenne.modeler.event.CallbackMethodEvent;
import org.apache.cayenne.modeler.event.CallbackMethodListener;
import org.apache.cayenne.modeler.event.TablePopupHandler;
import org.apache.cayenne.modeler.pref.TableColumnPreferences;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Base abstract class for all callback methods editing tabs Contains logic for callback
 * methods displaying, creating, removing, editing, reordering
 * 
 */
public abstract class AbstractCallbackMethodsTab extends JPanel {

    private static Logger logger = LoggerFactory.getLogger(AbstractCallbackMethodsTab.class);

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
     * list of callback method types
     */
    protected CallbackType[] callbackTypes = {
                            new CallbackType(LifecycleEvent.POST_ADD),
                            new CallbackType(LifecycleEvent.PRE_PERSIST),
                            new CallbackType(LifecycleEvent.POST_PERSIST),
                            new CallbackType(LifecycleEvent.PRE_UPDATE),
                            new CallbackType(LifecycleEvent.POST_UPDATE),
                            new CallbackType(LifecycleEvent.PRE_REMOVE),
                            new CallbackType(LifecycleEvent.POST_REMOVE),
                            new CallbackType(LifecycleEvent.POST_LOAD),
                    };
    
    /**
     * list of tables
     */
    protected CayenneTable[] tables = new CayenneTable[callbackTypes.length];
    
    protected JPopupMenu popupMenu;
    
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
    protected RemoveCallbackMethodAction getRemoveCallbackMethodAction() {
        Application app = Application.getInstance();
        return app.getActionManager().getAction(RemoveCallbackMethodAction.class);
    }

    /**
     * @return copy callback method action
     */
    protected CopyCallbackMethodAction getCopyCallbackMethodAction() {
        Application app = Application.getInstance();
        return app.getActionManager().getAction(CopyCallbackMethodAction.class);
    }

    /**
     * @return cut callback method action
     */
    protected CutCallbackMethodAction getCutCallbackMethodAction() {
        Application app = Application.getInstance();
        return app.getActionManager().getAction(CutCallbackMethodAction.class);
    }

    /**
     * @return paste callback method action
     */
    protected PasteAction getPasteCallbackMethodAction() {
        Application app = Application.getInstance();
        return app.getActionManager().getAction(PasteAction.class);
    }

    /**
     * GUI components initialization
     */
    protected void init() {
        this.setLayout(new BorderLayout());

        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(getRemoveCallbackMethodAction().buildButton());
        toolBar.addSeparator(new Dimension(10, 0));
	    toolBar.add(getCopyCallbackMethodAction().buildButton(1));
	    toolBar.add(getCutCallbackMethodAction().buildButton(2));
        toolBar.add(getPasteCallbackMethodAction().buildButton(3));

        add(toolBar, BorderLayout.NORTH);

        auxPanel = new JPanel();
        auxPanel.setOpaque(false);
        auxPanel.setLayout(new BorderLayout());
        
        popupMenu = createPopup(); 
        
        createTables();

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
            	selectAdded();
            }

            public void callbackMethodRemoved(CallbackMethodEvent e) {
            	int row = -1, i;
            	
            	for(i = 0; i < callbackTypes.length; i++) {
            		if(callbackTypes[i] == getSelectedCallbackType()) {
                        row = tables[i].getSelectedRow();
                        break;
            		}
            	}
                
            	rebuildTables();
                
            	if(row == tables[i].getRowCount()) {
                	row--;
                }
                
            	if (row < 0) {
                    return;
                }

            	tables[i].changeSelection(row, 0, false, false);
            }
        });

        for (CayenneTable table : tables) {
            mediator.getApplication().getActionManager().setupCutCopyPaste(
                    table,
                    CutCallbackMethodAction.class,
                    CopyCallbackMethodAction.class);
        }
    }

    /**
     * rebuilds table content
     */
    protected void rebuildTables() {
                
        CallbackMap callbackMap = getCallbackMap();

       	for(int i = 0; i < callbackTypes.length; i++) {
       		
       		CallbackType callbackType = callbackTypes[i];
  
       		List<String> methods = new ArrayList<>();
       		CallbackDescriptor descriptor = null;
       		
       		if (callbackMap != null && callbackType != null) {
       			descriptor = callbackMap.getCallbackDescriptor(callbackType.getType());
                methods.addAll(descriptor.getCallbackMethods());
       	    }
        		
            CallbackDescriptorTableModel model = new CallbackDescriptorTableModel(
                    mediator,
                    this,
                    methods,
                    descriptor,
                    callbackType);
            
            tables[i].setModel(model);
        }

        for (CayenneTable table : tables) {
            tablePreferences.bind(table, null, null, null);
        }
    }
    
    private void createTables() {
    	FormLayout formLayout = new FormLayout("left:pref");
        DefaultFormBuilder builder = new DefaultFormBuilder(formLayout);
    	
    	int index = 0;
      	for(CallbackType callbackType : callbackTypes) {
       		tables[index] = createTable(callbackType);
       		builder.append(createTablePanel(tables[index++]));
    	}
      	
      	initTablePreferences();

        auxPanel.add(builder.getPanel(), BorderLayout.CENTER);
        validate();
    }

    private CayenneTable createTable(CallbackType callbackType) {
    	final CayenneTable cayenneTable = new CayenneTable();

        // drag-and-drop initialization
    	cayenneTable.setDragEnabled(true);

    	cayenneTable.setSortable(false);
	    cayenneTable.setRowHeight(25);
	    cayenneTable.setRowMargin(3);
	    cayenneTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    cayenneTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	    
	    cayenneTable.setTransferHandler(new CallbackImportableHandler(cayenneTable));
	    cayenneTable.getSelectionModel().addListSelectionListener(new CallbackListSelectionListener(cayenneTable));
	    cayenneTable.getColumnModel().addColumnModelListener(new CallbackTableColumnModelListener(cayenneTable));
	    cayenneTable.getTableHeader().addMouseListener(new CallbackMouseAdapter(cayenneTable));
	    cayenneTable.getTableHeader().addMouseMotionListener(new CallbackMouseMotionListener(cayenneTable));
	      
	    TablePopupHandler.install(cayenneTable, popupMenu);

        addButtonAtHeader(
        		cayenneTable, 
        		getCreateCallbackMethodAction().buildButton(),
        		new ButtonListener(callbackType), 
        		ModelerUtil.buildIcon("icon-create-method.png"));
	    
	    return cayenneTable;
    }
    
    private JPopupMenu createPopup() {
	    JPopupMenu popup = new JPopupMenu();

	    popup.add(getRemoveCallbackMethodAction().buildMenu());
	    popup.addSeparator();
	    popup.add(getCopyCallbackMethodAction().buildMenu());
	    popup.add(getCutCallbackMethodAction().buildMenu());
	    popup.add(getPasteCallbackMethodAction().buildMenu());
	    
		return popup;
	}
    
    private JPanel createTablePanel(final CayenneTable cayenneTable) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        
        panel.add(cayenneTable.getTableHeader(), BorderLayout.NORTH);
        panel.add(cayenneTable, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void addButtonAtHeader(JTable table, JButton button, ActionListener buttonListener, ImageIcon buttonIcon){
        PanelBuilder builder = new PanelBuilder(new FormLayout("left:10dlu, 2dlu", "center:14dlu"));
        CellConstraints cc = new CellConstraints();
        
        button.setIcon(buttonIcon);
        button.setOpaque(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.addActionListener(buttonListener);
        
        builder.add(button, cc.xy(1, 1));
        
        JPanel buttonPanel = builder.getPanel();
        buttonPanel.setOpaque(false);
       
        JTableHeader header = table.getTableHeader();
        header.setLayout(new BorderLayout());
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(150, 22));
        header.add(buttonPanel, BorderLayout.EAST);
    }

    class ButtonListener implements ActionListener {
    	private CallbackType callbackType;
    	
    	public ButtonListener(CallbackType callbackType) {
    		this.callbackType = callbackType;
    	}
    	
        public void actionPerformed(ActionEvent e) {
        	mediator.setCurrentCallbackType(callbackType);
        }
    } 

    protected final CallbackType getSelectedCallbackType() {
    	return mediator.getCurrentCallbackType();
    }
    
    private void selectAdded() {
    	for(int i = 0; i < callbackTypes.length; i++) {
    		if(callbackTypes[i] == getSelectedCallbackType()) {
                if (tables[i].editCellAt(
                		tables[i].getRowCount() - 1,
                        CallbackDescriptorTableModel.METHOD_NAME)
                        && tables[i].getEditorComponent() != null) {
                	tables[i].changeSelection(tables[i].getRowCount() - 1, 0, false, false);
                	tables[i].editCellAt(tables[i].getRowCount() - 1, 0);
                	tables[i].getCellEditor().stopCellEditing();
                	return;
                }
    		}
    	}
    }
    

	private void unselectAll() {
		for(int i = 0; i < callbackTypes.length; i++) {
          	if(tables[i].getCellEditor() != null) {
    			tables[i].getCellEditor().stopCellEditing();
    		}
          	tables[i].clearSelection();
    	}
	}
    
    private class CallbackImportableHandler extends TransferHandler {
    	
    	private CayenneTable table;
    	
    	public CallbackImportableHandler(CayenneTable table) {
    		this.table = table;
    	}
    	
        protected Transferable createTransferable(JComponent c) {
            int rowIndex = table.getSelectedRow();

            String result = null;
            if (rowIndex >= 0 && rowIndex < table.getModel().getRowCount()) {
                result = String.valueOf(table.getModel().getValueAt(
                        rowIndex,
                        CallbackDescriptorTableModel.METHOD_NAME));
            }

            return new StringSelection(result);
        }

        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        public boolean importData(JComponent comp, Transferable t) {
            if (canImport(comp, t.getTransferDataFlavors())) {
                String callbackMethod;
                try {
                    callbackMethod = (String) t.getTransferData(DataFlavor.stringFlavor);
                } catch (Exception e) {
                    logger.warn("Error transferring", e);
                    return false;
                }

                int rowIndex = table.getSelectedRow();

                CallbackDescriptor callbackDescriptor =
                        ((CallbackDescriptorTableModel)table.getCayenneModel()).getCallbackDescriptor();
                mediator.setDirty(callbackDescriptor.moveMethod(callbackMethod, rowIndex));
                rebuildTables();
                return true;
            }

            return false;
        }

        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            for (DataFlavor flavor : transferFlavors) {
                if (DataFlavor.stringFlavor.equals(flavor)) {
                    return true;
                }
            }
            return false;
        }
    }

    private class CallbackListSelectionListener implements ListSelectionListener {    	
        
    	private CayenneTable table;
    	
    	public CallbackListSelectionListener(CayenneTable table){
    		this.table = table;
    	}
    	
    	public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
            	ObjCallbackMethod[] methods = new ObjCallbackMethod[0];

                if(!((ListSelectionModel)e.getSource()).isSelectionEmpty()) {
                    for (CayenneTable nextTable : tables) {
                        if (!nextTable.equals(table)) {
                            nextTable.clearSelection();
                            if (nextTable.getCellEditor() != null) {
                                nextTable.getCellEditor().stopCellEditing();
                            }
                        }
                    }

	                mediator.setCurrentCallbackType(((CallbackDescriptorTableModel)table.getCayenneModel()).getCallbackType());
                }

                if (table.getSelectedRow() != -1) {
                    int[] sel = table.getSelectedRows();
                    CallbackType callbackType = mediator.getCurrentCallbackType();
                    
                    methods = new ObjCallbackMethod[sel.length];
                                        
                    for (int i = 0; i < sel.length; i++) {
                    	String methodName = (String) table.getValueAt(
                    	        sel[i],
                                table.convertColumnIndexToView(CallbackDescriptorTableModel.METHOD_NAME));
                    	methods[i] = new ObjCallbackMethod(methodName, callbackType);
                    }
                }

                mediator.setCurrentCallbackMethods(methods);
                boolean enabled = methods.length > 0;
                boolean multiple = methods.length > 1;

                getRemoveCallbackMethodAction().setEnabled(enabled);
                getRemoveCallbackMethodAction().setName(getRemoveCallbackMethodAction().getActionName(multiple));
                getCopyCallbackMethodAction().setEnabled(enabled);
                getCopyCallbackMethodAction().setName(getCopyCallbackMethodAction().getActionName(multiple));
                getCutCallbackMethodAction().setEnabled(enabled);
                getCutCallbackMethodAction().setName(getCutCallbackMethodAction().getActionName(multiple));
            }
        }
    }

    private class CallbackTableColumnModelListener implements TableColumnModelListener {
        
    	private CayenneTable table;
    	
    	public CallbackTableColumnModelListener(CayenneTable table){
    		this.table = table;
    	}
    	
	    public void columnMarginChanged(ChangeEvent e) {
	        if(!table.getColumnWidthChanged() && table.getTableHeader().getResizingColumn() != null) {
                table.setColumnWidthChanged(true);
	        }
	    }
	
	    public void columnMoved(TableColumnModelEvent e) { 
	    	
	    }
	
	    public void columnAdded(TableColumnModelEvent e) { 
	    	
	    }
	
	    public void columnRemoved(TableColumnModelEvent e) { 
	    	
	    }
	
	    public void columnSelectionChanged(ListSelectionEvent e) { 
	    	
	    }
	}

    private class CallbackMouseAdapter extends MouseAdapter {
    	
    	CayenneTable table;
    	
    	public CallbackMouseAdapter(CayenneTable table) {
    		this.table = table;
    	}
    	
    	@Override
	    public void mouseReleased(MouseEvent e) {
	    	if(table.getColumnWidthChanged()) {
                for (CayenneTable nextTable : tables) {
                    nextTable.getColumnModel().getColumn(0).setPreferredWidth(table.getWidth());
                }
		    	
		    	initTablePreferences();
	        	table.setColumnWidthChanged(false);
	        }
	    }
	    
    	@Override
	    public void mousePressed(MouseEvent e) {
    		if (e.isPopupTrigger() && e.getComponent() instanceof JTableHeader ) {

    			unselectAll();
            	
    			mediator.setCurrentCallbackType(((CallbackDescriptorTableModel)table.getCayenneModel()).getCallbackType());
            	popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
	    }
	}
    
    private class CallbackMouseMotionListener implements MouseMotionListener {
    	
    	CayenneTable table;
    	
    	public CallbackMouseMotionListener(CayenneTable table) {
    		this.table = table;
    	}
    	
		public void mouseMoved(MouseEvent e) {
		}
		
		public void mouseDragged(MouseEvent e) {
	    	if(table.getColumnWidthChanged()) {
	    		tablePreferences.bind(table, null, null, null);
                for (CayenneTable nextTable : tables) {
                    if (!table.equals(nextTable)) {
                        nextTable.getColumnModel().getColumn(0).setPreferredWidth(table.getWidth());
                    }
                }
	        }
		}
	}
}
