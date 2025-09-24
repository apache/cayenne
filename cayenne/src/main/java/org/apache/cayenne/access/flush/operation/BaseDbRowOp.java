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

import java.util.Objects;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public abstract class BaseDbRowOp implements DbRowOp {

    protected final Persistent object;
    protected final DbEntity entity;
    // Can be ObjEntity id or a DB row id for flattened rows
    protected ObjectId changeId;
    protected int hashCode;

    protected BaseDbRowOp(Persistent object, DbEntity entity, ObjectId id) {
        this.object = Objects.requireNonNull(object);
        this.entity = Objects.requireNonNull(entity);
        this.changeId = Objects.requireNonNull(id);
        this.hashCode = changeId.hashCode();
    }

    @Override
    public DbEntity getEntity() {
        return entity;
    }

    @Override
    public ObjectId getChangeId() {
        return changeId;
    }

    @Override
    public Persistent getObject() {
        return object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DbRowOp)) return false;

        DbRowOp other = (DbRowOp) o;
        return changeId.equals(other.getChangeId());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return entity.getName() + " " + changeId;
    }

    /**
     * Internal check used in the batching ops logic
     * @param rowOp to compare with
     * @return true if another op has the same entity name and datamap
     */
    protected boolean entitiesHaveSameNameAndDataMap(DbRowOp rowOp) {
        return getEntity().getName().equals(rowOp.getEntity().getName())
                && getEntity().getDataMap().getName().equals(rowOp.getEntity().getDataMap().getName());
    }
}
