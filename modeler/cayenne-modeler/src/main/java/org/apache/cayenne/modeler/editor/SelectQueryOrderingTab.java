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
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.Icon;
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

import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.MultiColumnBrowser;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.swing.components.image.FilteredIconFactory;
import org.apache.cayenne.util.CayenneMapEntry;

/**
 * A panel for picking SelectQuery orderings.
 * 
 */
public class SelectQueryOrderingTab extends JPanel implements PropertyChangeListener {

    // property for split pane divider size
    private static final String SPLIT_DIVIDER_LOCATION_PROPERTY = "query.orderings.divider.location";

    static final Dimension BROWSER_CELL_DIM = new Dimension(150, 100);
    static final Dimension TABLE_DIM = new Dimension(460, 60);

    static final String PATH_HEADER = "Path";
    static final String ASCENDING_HEADER = "Ascending";
    static final String IGNORE_CASE_HEADER = "Ignore Case";

    static final String REAL_PANEL = "real";
    static final String PLACEHOLDER_PANEL = "placeholder";

    protected ProjectController mediator;
    protected SelectQueryDescriptor selectQuery;

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

        Preferences detail = Application.getInstance().getPreferencesNode(this.getClass(), "");

        int defLocation = Application.getFrame().getHeight() / 2;
        int location = detail != null ? detail.getInt(
                getDividerLocationProperty(),
                defLocation) : defLocation;

        //As of CAY-888 #3 main pane is now a JSplitPane. Top component is a bit larger.
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
        QueryDescriptor query = mediator.getCurrentQuery();

        if (query == null || !QueryDescriptor.SELECT_QUERY.equals(query.getType())) {
            processInvalidModel("Unknown query.");
            return;
        }

        if (!(query.getRoot() instanceof Entity)) {
            processInvalidModel("SelectQuery has no root set.");
            return;
        }

        this.selectQuery = (SelectQueryDescriptor) query;
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

        // setting minimal size, otherwise scrolling looks awful, because of
        // VERTICAL_SCROLLBAR_NEVER strategy
        panel.setMinimumSize(panel.getPreferredSize());

        return panel;
    }

    protected JComponent createToolbar() {

        JButton add = new CayenneAction.CayenneToolbarButton(null, 1);
        add.setText("Add Ordering");
        Icon addIcon = ModelerUtil.buildIcon("icon-plus.png");
        add.setIcon(addIcon);
        add.setDisabledIcon(FilteredIconFactory.createDisabledIcon(addIcon));
        add.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addOrdering();
            }

        });

        JButton remove = new CayenneAction.CayenneToolbarButton(null, 3);
        remove.setText("Remove Ordering");
        Icon removeIcon = ModelerUtil.buildIcon("icon-trash.png");
        remove.setIcon(removeIcon);
        remove.setDisabledIcon(FilteredIconFactory.createDisabledIcon(removeIcon));
        remove.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeOrdering();
            }

        });

        JToolBar toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        toolBar.add(add);
        toolBar.add(remove);
        return toolBar;
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
        if (path.length < 2) {
            return null;
        }

        StringBuilder buffer = new StringBuilder();

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
                    if (((Boolean) value).booleanValue()) {
                        ordering.setAscending();
                    }
                    else {
                        ordering.setDescending();
                    }
                    break;
                case 2:
                    if (((Boolean) value).booleanValue()) {
                        ordering.setCaseInsensitive();
                    }
                    else {
                        ordering.setCaseSensitive();
                    }
                    break;
                default:
                    throw new IndexOutOfBoundsException("Invalid editable column: "
                            + column);
            }

            mediator.fireQueryEvent(new QueryEvent(
                    SelectQueryOrderingTab.this,
                    selectQuery));
        }
    }

    /**
     * Updates split pane divider location in properties
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (JSplitPane.DIVIDER_LOCATION_PROPERTY.equals(evt.getPropertyName())) {
            int value = (Integer) evt.getNewValue();

            Preferences detail = Application.getInstance().getPreferencesNode(this.getClass(), "");
            detail.putInt(getDividerLocationProperty(), value);
        }
    }

    /**
     * Returns name of a property for divider location.
     */
    protected String getDividerLocationProperty() {
        return SPLIT_DIVIDER_LOCATION_PROPERTY;
    }
}
