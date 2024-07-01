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

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;

import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.dialog.db.load.TransferableNode;

public class PrintTablesBiFunction implements BiFunction<FilterContainer, DbImportTreeNode, Void> {

    private DbImportTree dbImportTree;

    public PrintTablesBiFunction(DbImportTree dbImportTree) {
        this.dbImportTree = dbImportTree;
    }

    @Override
    public Void apply(FilterContainer filterContainer, DbImportTreeNode root) {
        boolean isTransferable = dbImportTree.isTransferable();
        if (root.getChildCount() != 0) {
            root.removeAllChildren();
        }
        Collection<IncludeTable> includeTables = filterContainer.getIncludeTables();
        for (IncludeTable includeTable : includeTables) {
            DbImportTreeNode node = !isTransferable ?
                    new DbImportTreeNode(includeTable) :
                    new TransferableNode(includeTable);
            if (isTransferable &&
                    includeTable.getIncludeColumns().isEmpty() &&
                    includeTable.getExcludeColumns().isEmpty()) {
                dbImportTree.printParams(Collections.singletonList(
                        new IncludeColumn("Loading...")), node);
            }
            root.add(node);
            dbImportTree.packColumns(includeTable, node);
        }
        dbImportTree.reloadModelKeepingExpanded(root);

        return null;
    }
}
