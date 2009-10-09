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
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;

import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.MultiColumnBrowser;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.pref.Domain;
import org.apache.cayenne.pref.PreferenceDetail;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * A panel for picking SelectQuery orderings.
 * 
 */
public class SelectQueryOrderingTab extends JPanel implements PropertyChangeListener {

    //property for split pane divider size
    private static final String SPLIT_DIVIDER_LOCATION_PROPERTY = "query.orderings.divider.location"; 
    
    static final Dimension BROWSER_CELL_DIM = new Dimension(150, 100);
    static final Dimension TABLE_DIM = new Dimension(460, 60);

    static final String PATH_HEADER = "Path";
    static final String ASCENDING_HEADER = "Ascending";
    static final String IGNORE_CASE_HEADER = "Ignore Case";

    static final String REAL_PANEL = "real";
    static final String PLACEHOLDER_PANEL = "placeholder";

    protected ProjectController mediator;
    protected SelectQuery selectQuery;

    protected MultiColumnBrowser browser;
    protected JTable table;

    protected CardLayout cardLayout;
    protected JPanel messagePanel;

    public SelectQueryOrderingTab(ProjectController mediator) {
        this.mediator = mediator;

        initView();
        initController();
    }

    protected void initView() {

        messagePanel = new JPanel(new BorderLayout());
        cardLayout = new CardLayout();
        
        PreferenceDetail detail = getDomain().
            getDetail(getDividerLocationProperty(), false);
        
        int defLocation = Application.getFrame().getHeight() / 2;
        int location = detail != null ? 
                detail.getIntProperty(getDividerLocationProperty(), defLocation) : defLocation; 

        /**
         * As of CAY-888 #3 main pane is now a JSplitPane.
         * Top component is a bit larger.
         */
        JSplitPane mainPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainPanel.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, this);
        mainPanel.setDividerLocation(location);
        
        mainPanel.setTopComponent(createEditorPanel());
        mainPanel.setBottomComponent(createSelectorPanel());

        setLayout(cardLayout);
        add(mainPanel, REAL_PANEL);
        add(messagePanel, PLACEHOLDER_PANEL);
    }

    protected void initController() {

        // scroll to selected row whenever a selection even occurs
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    UIUtil.scrollToSelectedRow(table);
                }
            }
        });
    }

    protected void initFromModel() {
        Query query = mediator.getCurrentQuery();

        if (!(query instanceof SelectQuery)) {
            processInvalidModel("Unknown query.");
            return;
        }

        if (!(((SelectQuery)query).getRoot() instanceof Entity)) {
            processInvalidModel("SelectQuery has no root set.");
            return;
        }

        this.selectQuery = (SelectQuery) query;
        browser.setModel(createBrowserModel((Entity) selectQuery.getRoot()));
        table.setModel(createTableModel());

        // init column sizes
        table.getColumnModel().getColumn(0).setPreferredWidth(250);

        cardLayout.show(this, REAL_PANEL);
    }

    protected void processInvalidModel(String message) {
        JLabel messageLabel = new JLabel(message, JLabel.CENTER);

        messagePanel.removeAll();
        messagePanel.add(messageLabel, BorderLayout.CENTER);
        cardLayout.show(this, PLACEHOLDER_PANEL);
    }

    protected JPanel createEditorPanel() {

        table = new JTable();
        table.setRowHeight(25);
        table.setRowMargin(3);
        table.setPreferredScrollableViewportSize(TABLE_DIM);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        return panel;
    }

    protected JPanel createSelectorPanel() {
        browser = new MultiColumnBrowser();
        browser.setPreferredColumnSize(BROWSER_CELL_DIM);
        browser.setDefaultRenderer();
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(createToolbar(), BorderLayout.NORTH);
        panel.add(new JScrollPane(
                browser,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);

        //setting minimal size, otherwise scrolling looks awful, because of 
        //VERTICAL_SCROLLBAR_NEVER strategy
        panel.setMinimumSize(panel.getPreferredSize());
        
        return panel;
    }

    protected JComponent createToolbar() {

        JButton add = new JButton("Add Ordering", ModelerUtil
                .buildIcon("icon-move_up.gif"));
        add.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addOrdering();
            }

        });

        JButton remove = new JButton("Remove Ordering", ModelerUtil
                .buildIcon("icon-move_down.gif"));
        remove.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeOrdering();
            }

        });

        JToolBar toolbar = new JToolBar();
        toolbar.add(add);
        toolbar.add(remove);
        return toolbar;
    }

    protected TreeModel createBrowserModel(Entity entity) {
        return new EntityTreeModel(entity);
    }

    protected TableModel createTableModel() {
        return new OrderingModel();
    }

    protected String getSelectedPath() {
        Object[] path = browser.getSelectionPath().getPath();

        // first item in the path is Entity, so we must have
        // at least two elements to constitute a valid ordering path
        if (path != null && path.length < 2) {
            return null;
        }

        StringBuffer buffer = new StringBuffer();

        // attribute or relationships
        CayenneMapEntry first = (CayenneMapEntry) path[1];
        buffer.append(first.getName());

        for (int i = 2; i < path.length; i++) {
            CayenneMapEntry pathEntry = (CayenneMapEntry) path[i];
            buffer.append(".").append(pathEntry.getName());
        }

        return buffer.toString();
    }

    void addOrdering() {
        String orderingPath = getSelectedPath();
        if (orderingPath == null) {
            return;
        }

        // check if such ordering already exists
        for (Ordering ord : selectQuery.getOrderings()) {
            if (orderingPath.equals(ord.getSortSpecString())) {
                return;
            }
        }

        selectQuery.addOrdering(new Ordering(orderingPath, SortOrder.ASCENDING));
        int index = selectQuery.getOrderings().size() - 1;

        OrderingModel model = (OrderingModel) table.getModel();
        model.fireTableRowsInserted(index, index);
        mediator.fireQueryEvent(new QueryEvent(SelectQueryOrderingTab.this, selectQuery));
    }

    void removeOrdering() {
        int selection = table.getSelectedRow();
        if (selection < 0) {
            return;
        }

        OrderingModel model = (OrderingModel) table.getModel();
        Ordering ordering = model.getOrdering(selection);
        selectQuery.removeOrdering(ordering);

        model.fireTableRowsDeleted(selection, selection);
        mediator.fireQueryEvent(new QueryEvent(SelectQueryOrderingTab.this, selectQuery));
    }

    /**
     * A table model for the Ordering editing table.
     */
    final class OrderingModel extends AbstractTableModel {

        Ordering getOrdering(int row) {
            return selectQuery.getOrderings().get(row);
        }

        public int getColumnCount() {
            return 3;
        }

        public int getRowCount() {
            return (selectQuery != null) ? selectQuery.getOrderings().size() : 0;
        }

        public Object getValueAt(int row, int column) {
            Ordering ordering = getOrdering(row);

            switch (column) {
                case 0:
                    return ordering.getSortSpecString();
                case 1:
                    return ordering.isAscending() ? Boolean.TRUE : Boolean.FALSE;
                case 2:
                    return ordering.isCaseInsensitive() ? Boolean.TRUE : Boolean.FALSE;
                default:
                    throw new IndexOutOfBoundsException("Invalid column: " + column);
            }
        }

        @Override
        public Class getColumnClass(int column) {
            switch (column) {
                case 0:
                    return String.class;
                case 1:
                case 2:
                    return Boolean.class;
                default:
                    throw new IndexOutOfBoundsException("Invalid column: " + column);
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return PATH_HEADER;
                case 1:
                    return ASCENDING_HEADER;
                case 2:
                    return IGNORE_CASE_HEADER;
                default:
                    throw new IndexOutOfBoundsException("Invalid column: " + column);
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 1 || column == 2;
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            Ordering ordering = getOrdering(row);

            switch (column) {
                case 1:
                    ordering.setAscending(((Boolean) value).booleanValue());
                    mediator.fireQueryEvent(new QueryEvent(
                            SelectQueryOrderingTab.this,
                            selectQuery));
                    break;
                case 2:
                    ordering.setCaseInsensitive(((Boolean) value).booleanValue());
                    mediator.fireQueryEvent(new QueryEvent(
                            SelectQueryOrderingTab.this,
                            selectQuery));
                    break;
                default:
                    throw new IndexOutOfBoundsException("Invalid editable column: "
                            + column);
            }

        }
    }

    /**
     * Updates split pane divider location in properties 
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(evt.getPropertyName())) {
            int value = (Integer) evt.getNewValue();
            
            PreferenceDetail detail = getDomain().
                getDetail(getDividerLocationProperty(), true);
            detail.setIntProperty(getDividerLocationProperty(), value);
        }
    }
    
    /**
     * Returns name of a property for divider location. 
     */
    protected String getDividerLocationProperty() {
        return SPLIT_DIVIDER_LOCATION_PROPERTY;
    }
    
    protected Domain getDomain() {
        //note: getClass() returns different values for Orderings and Prefetches tabs
        return Application.getInstance().getPreferenceDomain().getSubdomain(getClass());
    }
}
