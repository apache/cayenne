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
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.undo.AddPrefetchUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.apache.cayenne.modeler.util.EntityTreeFilter;
import org.apache.cayenne.modeler.util.EntityTreeModel;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.swing.components.image.FilteredIconFactory;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JToolBar;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeModel;

/**
 * Subclass of the SelectQueryOrderingTab configured to work with prefetches.
 * 
 */
public class SelectQueryPrefetchTab extends SelectQueryOrderingTab {

    static final String JOINT_PREFETCH_SEMANTICS = "Joint";
    static final String DISJOINT_PREFETCH_SEMANTICS = "Disjoint";
    static final String DISJOINT_BY_ID_PREFETCH_SEMANTICS = "Disjoint by id";
    static final String UNDEFINED_SEMANTICS = "Undefined semantics";

    public SelectQueryPrefetchTab(ProjectController mediator) {
        super(mediator);
    }

    protected void initFromModel(){
        super.initFromModel();
        if(table.getColumnModel().getColumns().hasMoreElements()) {
            setUpPrefetchBox(table.getColumnModel().getColumn(2));
        }
    }

    protected void setUpPrefetchBox(TableColumn column) {

        JComboBox<String> prefetchBox = new JComboBox<>();
        prefetchBox.addItem(JOINT_PREFETCH_SEMANTICS);
        prefetchBox.addItem(DISJOINT_PREFETCH_SEMANTICS);
        prefetchBox.addItem(DISJOINT_BY_ID_PREFETCH_SEMANTICS);

        prefetchBox.addActionListener(e -> Application.getInstance().getFrameController().getEditorView().getEventController().setDirty(true));

        column.setCellEditor(new DefaultCellEditor(prefetchBox));

        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Click for combo box");
        column.setCellRenderer(renderer);
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

            Application.getInstance().getUndoManager().addEdit(new AddPrefetchUndoableEdit(prefetch, SelectQueryPrefetchTab.this));
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
        return new PrefetchModel(selectQuery.getPrefetchesMap(), selectQuery.getRoot());
    }

    public void addPrefetch(String prefetch) {
        
        // check if such prefetch already exists
        if (!selectQuery.getPrefetchesMap().isEmpty() && selectQuery.getPrefetchesMap().containsKey(prefetch)) {
            return;
        }

        //default value id disjoint
        selectQuery.addPrefetch(prefetch, PrefetchModel.getPrefetchType(DISJOINT_PREFETCH_SEMANTICS));
       
        // reset the model, since it is immutable
        table.setModel(createTableModel());
        setUpPrefetchBox(table.getColumnModel().getColumn(2));
        
        mediator.fireQueryEvent(new QueryEvent(this, selectQuery));
    }

    public void removePrefetch(String prefetch) {
        selectQuery.removePrefetch(prefetch);

        // reset the model, since it is immutable
        table.setModel(createTableModel());
        setUpPrefetchBox(table.getColumnModel().getColumn(2));

        mediator.fireQueryEvent(new QueryEvent(this, selectQuery));
    }

}
