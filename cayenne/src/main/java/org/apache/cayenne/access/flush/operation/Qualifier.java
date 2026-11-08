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

package org.apache.cayenne.access.flush.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.cayenne.map.compiled.CompiledColumn;

/**
 * Qualifier of DB row. It uses PK and optimistic lock qualifier if any.
 *
 * @since 4.2
 */
public class Qualifier {

    protected final DbRowOp row;
    // additional qualifier for optimistic lock
    protected Map<CompiledColumn, Object> additionalQualifier;
    protected List<String> nullNames;
    protected boolean optimisticLock;

    protected Qualifier(DbRowOp row) {
        this.row = row;
    }

    public Map<String, Object> getSnapshot() {
        Map<String, Object> idSnapshot = row.getChangeId().getIdSnapshot();
        if(additionalQualifier == null || additionalQualifier.isEmpty()) {
            return new HashMap<>(idSnapshot);
        }

        Map<String, Object> qualifier = HashMap.newHashMap(additionalQualifier.size() + idSnapshot.size());
        AtomicBoolean hasPK = new AtomicBoolean(!idSnapshot.isEmpty());
        idSnapshot.forEach((attr, value) -> {
            if(value != null) {
                qualifier.put(attr, value);
            } else {
                hasPK.set(false);
            }
        });

        if(!hasPK.get() || optimisticLock) {
            additionalQualifier.forEach((attr, value) ->
                    qualifier.put(attr.name(), value)
            );
        }

        return qualifier;
    }

    public List<CompiledColumn> getQualifierAttributes() {
        List<CompiledColumn> primaryKeys = row.getEntity().primaryKey();
        if(additionalQualifier == null || additionalQualifier.isEmpty()) {
            return primaryKeys;
        }

        List<CompiledColumn> columns = new ArrayList<>();
        Map<String, Object> idSnapshot = row.getChangeId().getIdSnapshot();
        AtomicBoolean hasPK = new AtomicBoolean(!idSnapshot.isEmpty());
        primaryKeys.forEach(pk -> {
            if(idSnapshot.get(pk.name()) != null) {
                columns.add(pk);
            } else {
                hasPK.set(false);
            }
        });

        if(!hasPK.get() || optimisticLock) {
            columns.addAll(additionalQualifier.keySet());
        }
        return columns;
    }

    public Collection<String> getNullQualifierNames() {
        if(nullNames == null || nullNames.isEmpty()) {
            return Collections.emptyList();
        }
        return nullNames;
    }

    public void addAdditionalQualifier(CompiledColumn column, Object value) {
        addAdditionalQualifier(column, value, false);
    }

    public void addAdditionalQualifier(CompiledColumn column, Object value, boolean optimisticLock) {
        if(additionalQualifier == null) {
            additionalQualifier = new HashMap<>();
        }

        additionalQualifier.put(column, value);
        if(value == null) {
            if(nullNames == null) {
                nullNames = new ArrayList<>();
            }
            nullNames.add(column.name());
        }

        if(optimisticLock) {
            this.optimisticLock = true;
        }
    }

    public boolean isUsingOptimisticLocking() {
        return optimisticLock;
    }

    public boolean isSameBatch(Qualifier other) {
        if(additionalQualifier == null) {
            return other.additionalQualifier == null;
        }
        if(optimisticLock != other.optimisticLock) {
            return false;
        }
        if(other.additionalQualifier == null) {
            return false;
        }
        if(!additionalQualifier.keySet().equals(other.additionalQualifier.keySet())) {
            return false;
        }
        return Objects.equals(nullNames, other.nullNames);
    }

}
