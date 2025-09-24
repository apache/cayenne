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
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class UpdateDbRowOp extends BaseDbRowOp implements DbRowOpWithValues, DbRowOpWithQualifier {

    protected final Values values;
    protected final Qualifier qualifier;

    public UpdateDbRowOp(Persistent object, DbEntity entity, ObjectId id) {
        super(object, entity, id);
        values = new Values(this, false);
        qualifier = new Qualifier(this);
    }

    @Override
    public <T> T accept(DbRowOpVisitor<T> visitor) {
        return visitor.visitUpdate(this);
    }

    @Override
    public Qualifier getQualifier() {
        return qualifier;
    }

    @Override
    public Values getValues() {
        return values;
    }

    @Override
    public boolean isSameBatch(DbRowOp rowOp) {
        if(!(rowOp instanceof UpdateDbRowOp)) {
            return false;
        }
        if(!entitiesHaveSameNameAndDataMap(rowOp)) {
            return false;
        }
        UpdateDbRowOp other = (UpdateDbRowOp) rowOp;
        if(!values.isSameBatch(other.values)) {
            return false;
        }
        return qualifier.isSameBatch(other.qualifier);
    }

    @Override
    public String toString() {
        return "update " + super.toString();
    }
}
