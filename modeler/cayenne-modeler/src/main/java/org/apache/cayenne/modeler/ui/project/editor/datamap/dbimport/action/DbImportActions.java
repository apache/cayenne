/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.action;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.action.ModelerAbstractAction;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportTree;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportView;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-view container of action instances used by the DataMap DB Import view.
 * Owns one instance of each action; consumers (toolbar, popup menus, cell editor,
 * tree) receive the same instances by typed accessor.
 */
public class DbImportActions {

    private final AddCatalogAction addCatalog;
    private final AddSchemaAction addSchema;
    private final AddIncludeTableAction addIncludeTable;
    private final AddExcludeTableAction addExcludeTable;
    private final AddIncludeColumnAction addIncludeColumn;
    private final AddExcludeColumnAction addExcludeColumn;
    private final AddIncludeProcedureAction addIncludeProcedure;
    private final AddExcludeProcedureAction addExcludeProcedure;
    private final EditNodeAction editNode;
    private final DeleteNodeAction deleteNode;
    private final SortNodesAction sortNodes;
    private final GetDbConnectionAction getDbConnection;
    private final LoadDbSchemaAction loadDbSchema;
    private final MoveImportNodeAction moveImportNode;
    private final MoveInvertNodeAction moveInvertNode;
    private final DragAndDropNodeAction dragAndDropNode;

    private final Map<Class<?>, TreeManipulationAction> actionsByNodeType;

    public DbImportActions(Application application, DbImportView view, DbImportTree targetTree, DbImportTree sourceTree) {
        // ModelerAbstractAction starts disabled by default; setAlwaysOn(true) latches each
        // action enabled so the toolbar and popup menus aren't greyed out. The view exists
        // only when a DataMap is open, so the global "no project => disable" rule doesn't apply.
        this.addCatalog = enabled(new AddCatalogAction(application, targetTree));
        this.addSchema = enabled(new AddSchemaAction(application, targetTree));
        this.addIncludeTable = enabled(new AddIncludeTableAction(application, targetTree));
        this.addExcludeTable = enabled(new AddExcludeTableAction(application, targetTree));
        this.addIncludeColumn = enabled(new AddIncludeColumnAction(application, targetTree));
        this.addExcludeColumn = enabled(new AddExcludeColumnAction(application, targetTree));
        this.addIncludeProcedure = enabled(new AddIncludeProcedureAction(application, targetTree));
        this.addExcludeProcedure = enabled(new AddExcludeProcedureAction(application, targetTree));
        this.editNode = enabled(new EditNodeAction(application, targetTree));
        this.deleteNode = enabled(new DeleteNodeAction(application, view, targetTree));
        this.sortNodes = enabled(new SortNodesAction(application, targetTree));
        this.getDbConnection = enabled(new GetDbConnectionAction(application));
        this.loadDbSchema = enabled(new LoadDbSchemaAction(application, view));
        this.moveImportNode = enabled(new MoveImportNodeAction(application, view, sourceTree, targetTree));
        this.moveInvertNode = enabled(new MoveInvertNodeAction(application, view, sourceTree, targetTree));
        this.dragAndDropNode = enabled(new DragAndDropNodeAction(application, targetTree));

        this.actionsByNodeType = new HashMap<>();
        actionsByNodeType.put(Catalog.class, addCatalog);
        actionsByNodeType.put(Schema.class, addSchema);
        actionsByNodeType.put(IncludeTable.class, addIncludeTable);
        actionsByNodeType.put(ExcludeTable.class, addExcludeTable);
        actionsByNodeType.put(IncludeColumn.class, addIncludeColumn);
        actionsByNodeType.put(ExcludeColumn.class, addExcludeColumn);
        actionsByNodeType.put(IncludeProcedure.class, addIncludeProcedure);
        actionsByNodeType.put(ExcludeProcedure.class, addExcludeProcedure);
    }

    private static <T extends ModelerAbstractAction> T enabled(T action) {
        action.setAlwaysOn(true);
        return action;
    }

    public TreeManipulationAction getAction(Class<?> nodeType) {
        return actionsByNodeType.get(nodeType);
    }

    public AddCatalogAction getAddCatalogAction() {
        return addCatalog;
    }

    public AddSchemaAction getAddSchemaAction() {
        return addSchema;
    }

    public AddIncludeTableAction getAddIncludeTableAction() {
        return addIncludeTable;
    }

    public AddExcludeTableAction getAddExcludeTableAction() {
        return addExcludeTable;
    }

    public AddIncludeColumnAction getAddIncludeColumnAction() {
        return addIncludeColumn;
    }

    public AddExcludeColumnAction getAddExcludeColumnAction() {
        return addExcludeColumn;
    }

    public AddIncludeProcedureAction getAddIncludeProcedureAction() {
        return addIncludeProcedure;
    }

    public AddExcludeProcedureAction getAddExcludeProcedureAction() {
        return addExcludeProcedure;
    }

    public EditNodeAction getEditNodeAction() {
        return editNode;
    }

    public DeleteNodeAction getDeleteNodeAction() {
        return deleteNode;
    }

    public SortNodesAction getSortNodesAction() {
        return sortNodes;
    }

    public GetDbConnectionAction getGetDbConnectionAction() {
        return getDbConnection;
    }

    public LoadDbSchemaAction getLoadDbSchemaAction() {
        return loadDbSchema;
    }

    public MoveImportNodeAction getMoveImportNodeAction() {
        return moveImportNode;
    }

    public MoveInvertNodeAction getMoveInvertNodeAction() {
        return moveInvertNode;
    }

    public DragAndDropNodeAction getDragAndDropNodeAction() {
        return dragAndDropNode;
    }
}
