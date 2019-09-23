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

package org.apache.cayenne.modeler.editor.dbimport.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;

class ColumnNode extends Node<TableNode<?>> {

    ColumnNode(String name, TableNode<?> parent) {
        super(name, parent);
    }

    @Override
    public Status getStatus(ReverseEngineering config) {
        Status parentStatus = getParent().getStatus(config);
        if (parentStatus != Status.INCLUDE) {
            return parentStatus;
        }

        List<FilterContainer> containers = getParent().getContainers(config);
        List<IncludeColumn> includeColumns = new ArrayList<>();
        List<ExcludeColumn> excludeColumns = new ArrayList<>();

        for (FilterContainer container : containers) {
            if(container == null) {
                continue;
            }
            IncludeTable table = getParent().getIncludeTable(container.getIncludeTables());
            if (table != null) {
                includeColumns.addAll(table.getIncludeColumns());
                excludeColumns.addAll(table.getExcludeColumns());
            }
            includeColumns.addAll(container.getIncludeColumns());
            excludeColumns.addAll(container.getExcludeColumns());
        }

        return includesColumn(includeColumns, excludeColumns);
    }

    private Status includesColumn(Collection<IncludeColumn> includeColumns, Collection<ExcludeColumn> excludeColumns) {
        if (includeColumns.isEmpty() && excludeColumns.isEmpty()) {
            return Status.INCLUDE;
        }

        if (!includeColumns.isEmpty()) {
            if (getIncludeColumn(includeColumns) != null) {
                return Status.INCLUDE;
            }
        }

        if (!excludeColumns.isEmpty()) {
            if (getExcludeColumn(excludeColumns) != null) {
                return Status.EXCLUDE_EXPLICIT;
            } else {
                return includeColumns.isEmpty()
                        ? Status.INCLUDE
                        : Status.EXCLUDE_IMPLICIT;
            }
        }

        return Status.EXCLUDE_IMPLICIT;
    }

    IncludeColumn getIncludeColumn(Collection<IncludeColumn> includeColumns) {
        for (IncludeColumn column : includeColumns) {
            if (getName().matches(column.getPattern())) {
                return column;
            }
        }

        return null;
    }

    ExcludeColumn getExcludeColumn(Collection<ExcludeColumn> excludeColumns) {
        for (ExcludeColumn column : excludeColumns) {
            if (getName().matches(column.getPattern())) {
                return column;
            }
        }

        return null;
    }

}
