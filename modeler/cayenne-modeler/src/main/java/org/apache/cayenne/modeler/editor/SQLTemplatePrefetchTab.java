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
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;

/**
 * Class configured to work with prefetches.
 */
public class SQLTemplatePrefetchTab extends JPanel implements PropertyChangeListener {

    // property for split pane divider size
    private static final String SPLIT_DIVIDER_LOCATION_PROPERTY = "query.orderings.divider.location";

    private static final Dimension BROWSER_CELL_DIM = new Dimension(150, 100);
    private static final Dimension TABLE_DIM = new Dimension(460, 60);

    private static final String REAL_PANEL = "real";
    private static final String PLACEHOLDER_PANEL = "placeholder";

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
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                UIUtil.scrollToSelectedRow(table);
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
        setUpPrefetchBox(table.getColumnModel().getColumn(2));

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

    protected void setUpPrefetchBox(TableColumn column) {

        JComboBox<String> prefetchBox = new JComboBox<>();
        prefetchBox.addItem(SelectQueryPrefetchTab.JOINT_PREFETCH_SEMANTICS);
        prefetchBox.addItem(SelectQueryPrefetchTab.DISJOINT_BY_ID_PREFETCH_SEMANTICS);

        prefetchBox.addActionListener(e -> Application.getInstance().getFrameController().getEditorView().getEventController().setDirty(true));

        column.setCellEditor(new DefaultCellEditor(prefetchBox));

        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        column.setCellRenderer(renderer);
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

        add.addActionListener(e -> {
            String prefetch = getSelectedPath();

            if (prefetch == null) {
                return;
            }

            addPrefetch(prefetch);

            Application.getInstance().getUndoManager().addEdit(new AddPrefetchUndoableEditForSqlTemplate(prefetch, SQLTemplatePrefetchTab.this));
        });

        JButton remove = new CayenneAction.CayenneToolbarButton(null, 3);
        remove.setText("Remove Prefetch");
        Icon removeIcon = ModelerUtil.buildIcon("icon-trash.png");
        remove.setIcon(removeIcon);
        remove.setDisabledIcon(FilteredIconFactory.createDisabledIcon(removeIcon));
        remove.addActionListener(e -> {
            int selection = table.getSelectedRow();
            if (selection < 0) {
                return;
            }
            String prefetch = (String) table.getModel().getValueAt(selection, 0);
            removePrefetch(prefetch);
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
        return new PrefetchModel(sqlTemplate.getPrefetchesMap(), sqlTemplate.getRoot());
    }

    public void addPrefetch(String prefetch) {

        // check if such prefetch already exists
        if (!sqlTemplate.getPrefetchesMap().isEmpty() && sqlTemplate.getPrefetchesMap().containsKey(prefetch)) {
            return;
        }

        //default value is joint
        sqlTemplate.addPrefetch(prefetch, PrefetchModel.getPrefetchType(SelectQueryPrefetchTab.DISJOINT_BY_ID_PREFETCH_SEMANTICS));

        // reset the model, since it is immutable
        table.setModel(createTableModel());
        setUpPrefetchBox(table.getColumnModel().getColumn(2));

        mediator.fireQueryEvent(new QueryEvent(this, sqlTemplate));
    }

    public void removePrefetch(String prefetch) {
        sqlTemplate.removePrefetch(prefetch);

        // reset the model, since it is immutable
        table.setModel(createTableModel());
        setUpPrefetchBox(table.getColumnModel().getColumn(2));

        mediator.fireQueryEvent(new QueryEvent(this, sqlTemplate));
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
