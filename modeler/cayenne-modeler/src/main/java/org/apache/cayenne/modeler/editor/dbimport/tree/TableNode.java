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

import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;

abstract class TableNode<T extends Node> extends Node<T> {

    TableNode(String name, T parent) {
        super(name, parent);
    }
    
    @Override
    public Status getStatus(ReverseEngineering config) {
        T parent = getParent();
        if(parent != null) {
            Status parentStatus = parent.getStatus(config);
            if (parentStatus != Status.INCLUDE) {
                return parentStatus;
            }
        }

        List<IncludeTable> includeTables = new ArrayList<>();
        List<ExcludeTable> excludeTables = new ArrayList<>();
        for(FilterContainer container : getContainers(config)) {
            if(container == null) {
                continue;
            }
            includeTables.addAll(container.getIncludeTables());
            excludeTables.addAll(container.getExcludeTables());
        }

        return includesTable(includeTables, excludeTables);
    }
    
    abstract List<FilterContainer> getContainers(ReverseEngineering config);

    Status includesTable(Collection<IncludeTable> includeTables, Collection<ExcludeTable> excludeTables) {
        if(includeTables.isEmpty() && excludeTables.isEmpty()) {
            return Status.INCLUDE;
        }

        if(!includeTables.isEmpty()) {
            if(getIncludeTable(includeTables) != null) {
                return Status.INCLUDE;
            }
        }

        if(!excludeTables.isEmpty()) {
            if(getExcludeTable(excludeTables) != null) {
                return Status.EXCLUDE_EXPLICIT;
            } else {
                return includeTables.isEmpty()
                        ? Status.INCLUDE
                        : Status.EXCLUDE_IMPLICIT;
            }
        }

        return Status.EXCLUDE_IMPLICIT;
    }

    IncludeTable getIncludeTable(Collection<IncludeTable> includeTables) {
        for(IncludeTable table : includeTables) {
            if(getName().matches(table.getPattern())) {
                return table;
            }
        }
        return null;
    }

    ExcludeTable getExcludeTable(Collection<ExcludeTable> excludeTables) {
        for(ExcludeTable table : excludeTables) {
            if(getName().matches(table.getPattern())) {
                return table;
            }
        }
        return null;
    }
}
