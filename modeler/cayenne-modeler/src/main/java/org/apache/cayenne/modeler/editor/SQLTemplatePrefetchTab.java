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

import org.apache.cayenne.configuration.event.QueryEvent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.undo.AddPrefetchUndoableEditForSqlTemplate;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.EntityTreeFilter;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.MultiColumnBrowser;
import org.apache.cayenne.modeler.util.UIUtil;
import org.apache.cayenne.swing.components.image.FilteredIconFactory;
import org.apache.cayenne.util.CayenneMapEntry;

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
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;

public class SQLTemplatePrefetchTab extends JPanel implements PropertyChangeListener {

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
    protected SQLTemplateDescriptor sqlTemplate;

    protected MultiColumnBrowser browser;
    protected JTable table;

    protected CardLayout cardLayout;
    protected JPanel messagePanel;

    public SQLTemplatePrefetchTab(ProjectController mediator) {
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

        if (query == null || !QueryDescriptor.SQL_TEMPLATE.equals(query.getType())) {
            processInvalidModel("Unknown query.");
            return;
        }

        if (!(query.getRoot() instanceof Entity)) {
            processInvalidModel("SQLTemplate has no root set.");
            return;
        }

        this.sqlTemplate = (SQLTemplateDescriptor) query;
        browser.setModel(createBrowserModel((Entity) sqlTemplate.getRoot()));
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
        add.setText("Add Prefetch");
        Icon addIcon = ModelerUtil.buildIcon("icon-plus.png");
        add.setIcon(addIcon);
        add.setDisabledIcon(FilteredIconFactory.createDisabledIcon(addIcon));

        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String prefetch = getSelectedPath();

                if (prefetch == null) {
                    return;
                }

                addPrefetch(prefetch);

                Application.getInstance().getUndoManager().addEdit(new AddPrefetchUndoableEditForSqlTemplate(prefetch, SQLTemplatePrefetchTab.this));
            }

        });

        JButton remove = new CayenneAction.CayenneToolbarButton(null, 3);
        remove.setText("Remove Prefetch");
        Icon removeIcon = ModelerUtil.buildIcon("icon-trash.png");
        remove.setIcon(removeIcon);
        remove.setDisabledIcon(FilteredIconFactory.createDisabledIcon(removeIcon));

        remove.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int selection = table.getSelectedRow();

                if (selection < 0) {
                    return;
                }

                String prefetch = (String) table.getModel().getValueAt(selection, 0);

                removePrefetch(prefetch);
            }

        });

        JToolBar toolBar = new JToolBar();
        toolBar.setBorder(BorderFactory.createEmptyBorder());
        toolBar.setFloatable(false);
        toolBar.add(add);
        toolBar.add(remove);
        return toolBar;
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

    protected TreeModel createBrowserModel(Entity entity) {
        EntityTreeModel treeModel = new EntityTreeModel(entity);
        treeModel.setFilter(
                new EntityTreeFilter() {
                    public boolean attributeMatch(Object node, Attribute attr) {
                        return false;
                    }

                    public boolean relationshipMatch(Object node, Relationship rel) {
                        return true;
                    }
                });
        return treeModel;
    }

    protected TableModel createTableModel() {
        return new PrefetchModel();
    }

    public void addPrefetch(String prefetch) {

        // check if such prefetch already exists
        if (!sqlTemplate.getPrefetches().isEmpty() && sqlTemplate.getPrefetches().contains(prefetch)) {
            return;
        }

        sqlTemplate.addPrefetch(prefetch);

        // reset the model, since it is immutable
        table.setModel(createTableModel());

        mediator.fireQueryEvent(new QueryEvent(this, sqlTemplate));
    }

    public void removePrefetch(String prefetch) {
        sqlTemplate.removePrefetch(prefetch);

        // reset the model, since it is immutable
        table.setModel(createTableModel());
        mediator.fireQueryEvent(new QueryEvent(this, sqlTemplate));
    }

    boolean isToMany(String prefetch) {
        if (sqlTemplate == null) {
            return false;
        }

        Object root = sqlTemplate.getRoot();

        // totally invalid path would result in ExpressionException
        try {
            Expression exp = ExpressionFactory.exp(prefetch);
            Object object = exp.evaluate(root);
            if (object instanceof Relationship) {
                return ((Relationship) object).isToMany();
            }
            else {
                return false;
            }
        }
        catch (ExpressionException e) {
            return false;
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

    /**
     * A table model for the Ordering editing table.
     */
    final class PrefetchModel extends AbstractTableModel {

        String[] prefetches;

        PrefetchModel() {
            if (sqlTemplate != null) {
                prefetches = new String[sqlTemplate.getPrefetches().size()];

                for (int i = 0; i < prefetches.length; i++) {
                    prefetches[i] = sqlTemplate.getPrefetches().get(i);
                }
            }
        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            return (prefetches != null) ? prefetches.length : 0;
        }

        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return prefetches[row];
                case 1:
                    return isToMany(prefetches[row]) ? Boolean.TRUE : Boolean.FALSE;
                default:
                    throw new IndexOutOfBoundsException("Invalid column: " + column);
            }
        }

        public Class getColumnClass(int column) {
            switch (column) {
                case 0:
                    return String.class;
                case 1:
                    return Boolean.class;
                default:
                    throw new IndexOutOfBoundsException("Invalid column: " + column);
            }
        }

        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Prefetch Path";
                case 1:
                    return "To Many";
                default:
                    throw new IndexOutOfBoundsException("Invalid column: " + column);
            }
        }

        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
