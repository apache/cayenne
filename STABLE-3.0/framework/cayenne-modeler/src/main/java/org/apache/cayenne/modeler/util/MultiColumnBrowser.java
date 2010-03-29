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

package org.apache.cayenne.modeler.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


/**
 * A simple non-editable tree browser with multiple columns 
 * for display and navigation of a tree structure. This type of
 * browser is ideal for showing deeply (or infinitely) nested 
 * trees/graphs. The most famous example of its use is Mac OS X 
 * Finder column view. 
 * 
 * <p>
 * MultiColumnBrowser starts at the root of the tree
 * and automatically expands to the right as navigation goes deeper. 
 * MultiColumnBrowser uses the same TreeModel as a regular JTree 
 * for its navigation model.
 * </p>
 * 
 * <p>
 * Users are notified of selection changes via a TreeSelectionEvents.
 * </p>
 * 
 * @since 1.1
 */
public class MultiColumnBrowser extends JPanel {

    protected static final ImageIcon rightArrow =
        ModelerUtil.buildIcon("scroll_right.gif");

    public static final int DEFAULT_MIN_COLUMNS_COUNT = 3;

    protected int minColumns;
    protected ListCellRenderer renderer;
    protected TreeModel model;
    protected Object[] selectionPath;
    protected Dimension preferredColumnSize;

    protected List<BrowserPanel> columns;
    protected ListSelectionListener browserSelector;
    protected List<TreeSelectionListener> treeSelectionListeners;
    
    /**
     * Whether firing of TreeSelectionListeners is disabled now
     */
    private boolean fireDisabled;

    public MultiColumnBrowser() {
        this(DEFAULT_MIN_COLUMNS_COUNT);
    }

    public MultiColumnBrowser(int minColumns) {
        if (minColumns < DEFAULT_MIN_COLUMNS_COUNT) {
            throw new IllegalArgumentException(
                "Expected "
                    + DEFAULT_MIN_COLUMNS_COUNT
                    + " or more columns, got: "
                    + minColumns);
        }

        this.minColumns = minColumns;
        this.browserSelector = new PanelController();
        this.treeSelectionListeners = Collections.synchronizedList(new ArrayList<TreeSelectionListener>());
        initView();
    }

    public void addTreeSelectionListener(TreeSelectionListener listener) {
        synchronized (treeSelectionListeners) {
            if (listener != null && !treeSelectionListeners.contains(listener)) {
                treeSelectionListeners.add(listener);
            }
        }
    }

    public void removeTreeSelectionListener(TreeSelectionListener listener) {
        synchronized (treeSelectionListeners) {
            treeSelectionListeners.remove(listener);
        }
    }

    /**
     * Notifies listeners of a tree selection change.
     */
    protected void fireTreeSelectionEvent(Object[] selectionPath) {
        if (fireDisabled) {
            return;
        }
        
        TreeSelectionEvent e =
            new TreeSelectionEvent(this, new TreePath(selectionPath), false, null, null);
        synchronized (treeSelectionListeners) {
            for (TreeSelectionListener listener : treeSelectionListeners)
                listener.valueChanged(e);
        }
    }

    /**
     * Returns current selection path or null if no selection is made.
     */
    public TreePath getSelectionPath() {
        return new TreePath(selectionPath);
    }
    
    /**
     * Sets new selection path and fires an event
     */
    public void setSelectionPath(TreePath path) {
        try {
            fireDisabled = true;
            
            for (int i = 0; i < path.getPathCount(); i++) {
                selectRow(path.getPathComponent(i), i, path);
            }
        }
        finally {
            fireDisabled = false;
        }
    }
    
    /**
     * Selects one path component
     */
    protected void selectRow(Object row, int index, TreePath path) {
        if (index > 0 && columns.get(index - 1).getSelectedValue() != row) {
            columns.get(index - 1).setSelectedValue(row, true);
        }
        else { //update
            updateFromModel(row, index - 1);
        }
    }

    /**
     * Returns the minumum number of displayed columns.
     */
    public int getMinColumns() {
        return minColumns;
    }

    /**
     * Sets the minumum number of displayed columns.
     */
    public void setMinColumns(int minColumns) {
        this.minColumns = minColumns;
    }

    /**
     * Returns prefrred size of a single browser column.
     */
    public Dimension getPreferredColumnSize() {
        return preferredColumnSize;
    }

    public void setPreferredColumnSize(Dimension preferredColumnSize) {
        this.preferredColumnSize = preferredColumnSize;
        refreshPreferredSize();
    }

    /**
     * Resets currently used renderer to default one that will
     * use the "name" property of objects and display a small
     * arrow to the right of all non-leaf nodes.
     */
    public void setDefaultRenderer() {
        if (!(renderer instanceof MultiColumnBrowserRenderer)) {
            setRenderer(new MultiColumnBrowserRenderer());
        }
    }

    /**
     * Returns ListCellRenderer for individual elements of each column.
     */
    public ListCellRenderer getRenderer() {
        return renderer;
    }

    /**
     * Initializes the renderer of column cells.
     */
    public synchronized void setRenderer(ListCellRenderer renderer) {
        if (this.renderer != renderer) {
            this.renderer = renderer;

            // update existing browser
            if (columns != null && columns.size() > 0) {
                for (JList column : columns)
                    column.setCellRenderer(renderer);
            }
        }
    }

    /**
     * Initializes browser model.
     */
    public synchronized void setModel(TreeModel model) {
        if (model == null) {
            throw new NullPointerException("Null tree model.");
        }

        if (this.model != model) {
            this.model = model;

            // display first column
            updateFromModel(model.getRoot(), -1);
        }
    }

    /**
     * Returns browser model.
     */
    public TreeModel getModel() {
        return model;
    }

    /**
     * Returns a current number of columns.
     */
    public int getColumnsCount() {
        return columns.size();
    }

    // ====================================================
    // Internal private methods
    // ====================================================

    private void initView() {
        columns = Collections.synchronizedList(new ArrayList<BrowserPanel>(minColumns));
        adjustViewColumns(minColumns);
    }

    /**
     * Expands or contracts the view by <code>delta</code> columns.
     * Never contracts the view below <code>minColumns</code>.
     */
    private void adjustViewColumns(int delta) {
        if (delta == 0) {
            return;
        }

        setLayout(new GridLayout(1, columns.size() + delta, 3, 3));
        if (delta > 0) {
            for (int i = 0; i < delta; i++) {
                appendColumn();
            }
        }
        else {
            for (int i = -delta; i > 0 && columns.size() > minColumns; i--) {
                removeLastColumn();
            }
        }

        refreshPreferredSize();
        revalidate();
    }

    private BrowserPanel appendColumn() {
        BrowserPanel panel = new BrowserPanel();
        installColumn(panel);

        columns.add(panel);
        JScrollPane scroller =
            new JScrollPane(
                panel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // note - it is important to set prefrred size on scroller,
        // not on the component itself... otherwise resizing
        // will be very ugly...
        if (preferredColumnSize != null) {
            scroller.setPreferredSize(preferredColumnSize);
        }
        add(scroller);
        return panel;
    }
    
    /**
     * Installs all needed columns and renderers to a new column
     */
    protected void installColumn(BrowserPanel panel) {
        panel.addListSelectionListener(browserSelector);
        panel.setCellRenderer(renderer);
    }

    private BrowserPanel removeLastColumn() {
        if (columns.size() == 0) {
            return null;
        }

        int index = columns.size() - 1;

        BrowserPanel panel = columns.remove(index);
        uninstallColumn(panel);

        // remove ansestor of the column (JScrollPane)
        remove(index);
        return panel;
    }
    
    /**
     * Clears selection in browser and removes all unnessesary columns
     */
    public void clearSelection() {
        updateFromModel(model.getRoot(), -1);
    }
    
    /**
     * Removes all local listeners from the column
     */
    protected void uninstallColumn(BrowserPanel panel) {
        panel.removeListSelectionListener(browserSelector);
    }

    /**
     * Refreshes preferred size of the browser to
     * reflect current number of columns.
     */
    private void refreshPreferredSize() {
        if (preferredColumnSize != null) {
            int w = getColumnsCount() * (preferredColumnSize.width + 3) + 3;
            int h = preferredColumnSize.height + 6;
            setPreferredSize(new Dimension(w, h));
        }
    }

    /**
     * Makes numbered column visible if the browser parent allows scrolling.
     */
    private void scrollToColumn(int column) {
        if (getParent() instanceof JViewport) {

            JViewport viewport = (JViewport) getParent();

            // find a rectangle in the middle of the browser
            // and scroll it...
            double x = getWidth() * column / ((double) getMinColumns());
            double y = getHeight() / 2;

            if (preferredColumnSize != null) {
                x -= preferredColumnSize.width / 2;
                if (x < 0) {
                    x = 0;
                }
            }

            Rectangle rectangle = new Rectangle((int) x, (int) y, 1, 1);

            // Scroll the area into view.
            viewport.scrollRectToVisible(rectangle);
        }
    }
    
    /**
     * Rebuilds view for the new object selection.
     */
    protected synchronized void updateFromModel(Object selectedNode, int panelIndex) {
        updateFromModel(selectedNode, panelIndex, true);
    }

    /**
     * Rebuilds view for the new object selection.
     * @param load Whether children are loaded automatically
     */
    protected synchronized void updateFromModel(Object selectedNode, int panelIndex, boolean load) {
        if(selectionPath == null) {
            selectionPath = new Object[0];
        }
        
        // clean up extra columns
        int lastIndex = selectionPath.length;

        // check array range to handle race conditions 
        for (int i = panelIndex + 1;
            i <= lastIndex && i >= 0 && i < columns.size();
            i++) {
            BrowserPanel column = columns.get(i);
            column.getSelectionModel().clearSelection();
            column.setRootNode(null);
        }

        // build path to selected node
        this.selectionPath = rebuildPath(selectionPath, selectedNode, panelIndex);

        if (load) {
            // a selectedNode is contained in "panelIndex" column, 
            // but its children are in the next column.
            panelIndex++;
        }
        
        // expand/contract columns as needed
        adjustViewColumns(panelIndex + 1 - columns.size());

        if (load) {
            // selectedNode becomes the root of columns[panelIndex]
            if (!model.isLeaf(selectedNode)) {
                BrowserPanel lastPanel = columns.get(panelIndex);
                lastPanel.setRootNode(selectedNode);
                scrollToColumn(panelIndex);
            }
        }

        fireTreeSelectionEvent(selectionPath);
    }

    /** 
     * Builds a TreePath to the new node, that is known to be a peer or a child 
     * of one of the path components. As the method walks the current path backwards,
     * it cleans columns that are not common with the new path.
     */
    private Object[] rebuildPath(Object[] path, Object node, int panelIndex) {
        Object[] newPath = new Object[panelIndex + 2];
        System.arraycopy(path, 0, newPath, 0, panelIndex + 1);
        newPath[panelIndex + 1] = node;
        return newPath;
    }

    // ====================================================
    // Helper classes
    // ====================================================

    // ====================================================
    // Represents a browser column list model. This is an
    // adapter on top of the TreeModel node, showing the branch
    // containing node children
    // ====================================================
    final class ColumnListModel extends AbstractListModel {
        Object treeNode;
        int children;

        void setTreeNode(Object treeNode) {
            int oldChildren = children;
            this.treeNode = treeNode;
            this.children = (treeNode != null) ? model.getChildCount(treeNode) : 0;

            // must fire an event to refresh the view
            super.fireContentsChanged(
                MultiColumnBrowser.this,
                0,
                Math.max(oldChildren, children));
        }

        public Object getElementAt(int index) {
            return model.getChild(treeNode, index);
        }

        public int getSize() {
            return children;
        }
    }

    // ====================================================
    // Represents a single browser column
    // ====================================================
    protected final class BrowserPanel extends JList {
        BrowserPanel() {
            BrowserPanel.this.setModel(new ColumnListModel());
            BrowserPanel.this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

        void setRootNode(Object node) {
            ((ColumnListModel) BrowserPanel.this.getModel()).setTreeNode(node);
        }

        Object getTreeNode() {
            return ((ColumnListModel) BrowserPanel.this.getModel()).treeNode;
        }
    }

    // ====================================================
    // Processes selection events in each column
    // ====================================================
    final class PanelController implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {

            // ignore "adjusting"
            if (!e.getValueIsAdjusting()) {
                BrowserPanel panel = (BrowserPanel) e.getSource();
                Object selectedNode = panel.getSelectedValue();

                // ignore unselected
                if (selectedNode != null) {
                    updateFromModel(selectedNode, columns.indexOf(panel));
                }
            }
        }
    }

    // ====================================================
    // Default renderer that shows non-leaf nodes with a 
    // small right arrow. Unfortunately we can't subclass
    // DefaultListCellRenerer since it extends JLabel that
    // does not allow the layout that we need.
    // ====================================================
    final class MultiColumnBrowserRenderer implements ListCellRenderer, Serializable {

        ListCellRenderer leafRenderer;
        JPanel nonLeafPanel;
        ListCellRenderer nonLeafTextRenderer;

        MultiColumnBrowserRenderer() {

            leafRenderer = CellRenderers.listRenderer();

            nonLeafTextRenderer = new DefaultListCellRenderer() {
                @Override
                public Border getBorder() {
                    return null;
                }
            };

            nonLeafPanel = new JPanel();
            nonLeafPanel.setLayout(new BorderLayout());
            nonLeafPanel.add(new JLabel(rightArrow), BorderLayout.EAST);
            nonLeafPanel.add((Component) nonLeafTextRenderer, BorderLayout.WEST);
        }

        public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

            if (getModel().isLeaf(value)) {
                return leafRenderer.getListCellRendererComponent(
                    list,
                    value,
                    index,
                    isSelected,
                    cellHasFocus);
            }

            Object renderedValue = ModelerUtil.getObjectName(value);
            if (renderedValue == null) {
                // render NULL as empty string
                renderedValue = " ";
            }

            Component text =
                nonLeafTextRenderer.getListCellRendererComponent(
                    list,
                    renderedValue,
                    index,
                    isSelected,
                    cellHasFocus);

            nonLeafPanel.setComponentOrientation(text.getComponentOrientation());
            nonLeafPanel.setBackground(text.getBackground());
            nonLeafPanel.setForeground(text.getForeground());
            nonLeafPanel.setEnabled(text.isEnabled());
            return nonLeafPanel;
        }
    }

}
