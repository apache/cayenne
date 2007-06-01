/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionException;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.map.event.QueryEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.EntityTreeModel;

/**
 * Subclass of the SelectQueryOrderingTab configured to work with prefetches.
 * 
 * @author Andrei Adamchik
 */
public class SelectQueryPrefetchTab extends SelectQueryOrderingTab {

    public SelectQueryPrefetchTab(ProjectController mediator) {
        super(mediator);
    }

    protected JButton createAddPathButton() {
        JButton button = new JButton("Add Prefetch");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                addPrefetch();
            }
        });

        return button;
    }

    protected JButton createRemovePathButton() {
        JButton button = new JButton("Remove Prefetch");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                removePrefetch();
            }
        });

        return button;
    }

    protected TreeModel createBrowserModel(Entity entity) {
        EntityTreeModel treeModel = new EntityTreeModel(entity);
        treeModel.setHideAttributes(true);
        return treeModel;
    }

    protected TableModel createTableModel() {
        return new PrefetchModel();
    }

    void addPrefetch() {
        String prefetch = getSelectedPath();
        if (prefetch == null) {
            return;
        }

        // check if such prefetch already exists
        if (selectQuery.getPrefetches().contains(prefetch)) {
            return;
        }

        selectQuery.addPrefetch(prefetch);

        // reset the model, since it is immutable
        table.setModel(createTableModel());
        mediator.fireQueryEvent(new QueryEvent(SelectQueryPrefetchTab.this, selectQuery));
    }

    void removePrefetch() {
        int selection = table.getSelectedRow();
        if (selection < 0) {
            return;
        }

        String prefetch = (String) table.getModel().getValueAt(selection, 0);
        selectQuery.removePrefetch(prefetch);

        // reset the model, since it is immutable
        table.setModel(createTableModel());
        mediator.fireQueryEvent(new QueryEvent(SelectQueryPrefetchTab.this, selectQuery));
    }

    boolean isToMany(String prefetch) {
        if (selectQuery == null) {
            return false;
        }

        Object root = selectQuery.getRoot();

        // totally invalid path would result in ExpressionException
        try {
            Expression exp = Expression.fromString(prefetch);
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
     * A table model for the Ordering editing table.
     */
    final class PrefetchModel extends AbstractTableModel {

        String[] prefetches;

        PrefetchModel() {
            if (selectQuery != null) {
                prefetches = new String[selectQuery.getPrefetches().size()];
                selectQuery.getPrefetches().toArray(prefetches);
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