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

import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;

abstract class ProcedureNode<T extends Node> extends Node<T> {

    ProcedureNode(String name, T parent) {
        super(name, parent);
    }

    @Override
    public Status getStatus(ReverseEngineering config) {
        Status parentStatus = getParent().getStatus(config);
        if(parentStatus != Status.INCLUDE) {
            return parentStatus;
        }

        List<IncludeProcedure> includeProcedures = new ArrayList<>();
        List<ExcludeProcedure> excludeProcedures = new ArrayList<>();
        for(FilterContainer container : getContainers(config)) {
            if(container == null) {
                continue;
            }
            includeProcedures.addAll(container.getIncludeProcedures());
            excludeProcedures.addAll(container.getExcludeProcedures());
        }

        return includesProcedure(includeProcedures, excludeProcedures);
    }

    abstract List<FilterContainer> getContainers(ReverseEngineering config);

    private Status includesProcedure(Collection<IncludeProcedure> includeProcedures, Collection<ExcludeProcedure> excludeProcedures) {
        if(includeProcedures.isEmpty() && excludeProcedures.isEmpty()) {
            return Status.INCLUDE;
        }

        if(!includeProcedures.isEmpty()) {
            if(includesProcedure(includeProcedures)) {
                return Status.INCLUDE;
            }
        }

        if(!excludeProcedures.isEmpty()) {
            if(excludesProcedure(excludeProcedures)) {
                return Status.EXCLUDE_EXPLICIT;
            } else {
                return includeProcedures.isEmpty()
                        ? Status.INCLUDE
                        : Status.EXCLUDE_IMPLICIT;
            }
        }

        return Status.EXCLUDE_IMPLICIT;
    }

    private boolean includesProcedure(Collection<IncludeProcedure> includeProcedures) {
        for(IncludeProcedure procedure : includeProcedures) {
            if(getName().matches(procedure.getPattern())) {
                return true;
            }
        }
        return false;
    }

    private boolean excludesProcedure(Collection<ExcludeProcedure> excludeProcedures) {
        for(ExcludeProcedure procedure : excludeProcedures) {
            if(getName().matches(procedure.getPattern())) {
                return true;
            }
        }
        return false;
    }
}
