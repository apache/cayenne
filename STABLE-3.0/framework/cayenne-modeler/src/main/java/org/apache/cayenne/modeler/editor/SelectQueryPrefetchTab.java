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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.map.event.QueryEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.undo.AddPrefetchUndoableEdit;
import org.apache.cayenne.modeler.util.EntityTreeFilter;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.query.PrefetchTreeNode;

/**
 * Subclass of the SelectQueryOrderingTab configured to work with prefetches.
 * 
 */
public class SelectQueryPrefetchTab extends SelectQueryOrderingTab {

    public SelectQueryPrefetchTab(ProjectController mediator) {
        super(mediator);
    }

    protected JComponent createToolbar() {

        JButton add = new JButton("Add Prefetch", ModelerUtil
                .buildIcon("icon-move_up.gif"));
        
        add.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	String prefetch = getSelectedPath();
                
            	if (prefetch == null) {
                    return;
                }
            	
                addPrefetch(prefetch);
                
                Application.getInstance().getUndoManager().addEdit(new AddPrefetchUndoableEdit(prefetch, SelectQueryPrefetchTab.this));
            }

        });

        JButton remove = new JButton("Remove Prefetch", ModelerUtil
                .buildIcon("icon-move_down.gif"));
        
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

        JToolBar toolbar = new JToolBar();
        toolbar.add(add);
        toolbar.add(remove);
        return toolbar;
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
        if (selectQuery.getPrefetchTree() != null) {

            PrefetchTreeNode node = selectQuery.getPrefetchTree().getNode(prefetch);
            if (node != null && !node.isPhantom()) {
                return;
            }
        }

        selectQuery.addPrefetch(prefetch);
       
        // reset the model, since it is immutable
        table.setModel(createTableModel());
        
        mediator.fireQueryEvent(new QueryEvent(this, selectQuery));
    }

    public void removePrefetch(String prefetch) {
        selectQuery.removePrefetch(prefetch);

        // reset the model, since it is immutable
        table.setModel(createTableModel());
        mediator.fireQueryEvent(new QueryEvent(this, selectQuery));
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

                if (selectQuery.getPrefetchTree() == null) {
                    prefetches = new String[0];
                }
                else {
                    Collection c = selectQuery.getPrefetchTree().nonPhantomNodes();
                    prefetches = new String[c.size()];

                    Iterator it = c.iterator();
                    for (int i = 0; i < prefetches.length; i++) {
                        prefetches[i] = ((PrefetchTreeNode) it.next()).getPath();
                    }
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
