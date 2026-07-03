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

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * A "soft" delete op that is executed as an UPDATE setting the soft-delete flag column to {@code true} instead of a
 * SQL DELETE, leaving the row physically in the table. For all other purposes — sorting, merging, snapshot cache
 * eviction and the rest of the flush postprocessing — it behaves exactly like a {@link DeleteDbRowOp}.
 *
 * @since 5.0
 */
public class SoftDeleteDbRowOp extends DeleteDbRowOp implements DbRowOpWithValues {

    protected final Values values;

    public SoftDeleteDbRowOp(Persistent object, DbEntity entity, ObjectId id, DbAttribute deletedAttribute) {
        super(object, entity, id);
        this.values = new Values(this, false);
        values.addValue(deletedAttribute, true, false);
    }

    @Override
    public <T> T accept(DbRowOpVisitor<T> visitor) {
        return visitor.visitSoftDelete(this);
    }

    @Override
    public Values getValues() {
        return values;
    }

    @Override
    public boolean isSameBatch(DbRowOp rowOp) {
        if (!(rowOp instanceof SoftDeleteDbRowOp other)) {
            return false;
        }
        if (!super.isSameBatch(rowOp)) {
            return false;
        }
        return values.isSameBatch(other.values);
    }

    @Override
    public String toString() {
        return "soft " + super.toString();
    }
}
