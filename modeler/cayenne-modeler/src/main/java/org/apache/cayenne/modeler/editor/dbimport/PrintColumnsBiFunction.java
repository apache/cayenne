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

package org.apache.cayenne.modeler.editor.dbimport;

import java.util.function.BiFunction;

import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;

public class PrintColumnsBiFunction implements BiFunction<FilterContainer, DbImportTreeNode, Void> {

    private final DbImportTree dbImportTree;

    public PrintColumnsBiFunction(DbImportTree dbImportTree) {
        this.dbImportTree = dbImportTree;
    }

    @Override
    public Void apply(FilterContainer filterContainer, DbImportTreeNode root) {
        if (filterContainer != null) {
            filterContainer.getIncludeTables().forEach(tableFilter -> processTable(tableFilter, root));
        }
        return null;
    }

    private void processTable(IncludeTable tableFilter, DbImportTreeNode root) {
        DbImportModel model = (DbImportModel) dbImportTree.getModel();
        DbImportTreeNode container = dbImportTree
                .findNodeInParent(root, tableFilter);
        if (container == null) {
            return;
        }
        if (container.getChildCount() != 0) {
            container.removeAllChildren();
        }

        dbImportTree.packColumns(tableFilter, container);

        container.setLoaded(true);
        dbImportTree.reloadModelKeepingExpanded(container);
    }
}
