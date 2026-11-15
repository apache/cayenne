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

import java.util.List;


import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.compiled.CompiledTable;

/**
 * @since 4.2
 */
public class DeleteDbRowOp extends BaseDbRowOp implements DbRowOpWithQualifier {

    protected final Qualifier qualifier;

    public DeleteDbRowOp(Persistent object, CompiledTable table, ObjectId id) {
        super(object, table, id);
        qualifier = new Qualifier(this);
    }

    @Override
    public <T> T accept(DbRowOpVisitor<T> visitor) {
        return visitor.visitDelete(this);
    }

    @Override
    public DbRowOpType getType() {
        return DbRowOpType.DELETE;
    }

    public void setChangeId(ObjectId changeId) {
        this.changeId = changeId;
        this.hashCode = changeId.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof DbRowOpWithQualifier)) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    protected BatchKey createBatchKey() {
        return new BatchKey(
                BatchKey.Kind.DELETE,
                table,
                List.of(),
                qualifier.additionalQualifier(),
                qualifier.isUsingOptimisticLocking());
    }

    @Override
    public Qualifier getQualifier() {
        return qualifier;
    }

    @Override
    public String toString() {
        return "delete " + super.toString();
    }
}
