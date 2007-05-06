/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.editor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;

import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.EntityTreeModel;
import org.objectstyle.cayenne.modeler.util.ModelerUtil;
import org.objectstyle.cayenne.modeler.util.MultiColumnBrowser;
import org.objectstyle.cayenne.modeler.util.UIUtil;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.CayenneMapEntry;

/**
 * A panel for picking SelectQuery orderings.
 * 
 * @author Andrei Adamchik
 */
public class SelectQueryOrderingTab extends JPanel {

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

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(createEditorPanel(), BorderLayout.CENTER);
        mainPanel.add(createSelectorPanel(), BorderLayout.SOUTH);

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
        Iterator it = selectQuery.getOrderings().iterator();
        while (it.hasNext()) {
            Ordering ord = (Ordering) it.next();
            if (orderingPath.equals(ord.getSortSpecString())) {
                return;
            }
        }

        selectQuery.addOrdering(new Ordering(orderingPath, Ordering.ASC));
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
            return (Ordering) selectQuery.getOrderings().get(row);
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

        public boolean isCellEditable(int row, int column) {
            return column == 1 || column == 2;
        }

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
}