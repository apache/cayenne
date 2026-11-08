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

package org.apache.cayenne.access.flush;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.ObjectDiff;
import org.apache.cayenne.access.flush.operation.DbRowOpWithQualifier;
import org.apache.cayenne.map.compiled.CompiledColumnPair;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledAttribute;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;
import org.apache.cayenne.map.compiled.CompiledToOneRelationship;

/**
 * Builds optimistic lock qualifier for given db change out of the locking attributes and to-one relationships of an
 * entity.
 *
 * @since 4.2
 */
class OptimisticLockQualifierBuilder {
    private final DbRowOpWithQualifier dbRow;
    private final ObjectDiff diff;

    OptimisticLockQualifierBuilder(DbRowOpWithQualifier dbRow, ObjectDiff diff) {
        this.dbRow = dbRow;
        this.diff = diff;
    }

    void build(CompiledEntity entity) {
        for (CompiledAttribute attribute : entity.allAttributes()) {
            addAttribute(attribute);
        }
        for (CompiledRelationship relationship : entity.allRelationships()) {
            if (relationship instanceof CompiledToOneRelationship toOne) {
                addToOne(toOne);
            }
        }
    }

    private void addAttribute(CompiledAttribute attribute) {
        CompiledColumn column = attribute.column();
        if (attribute.isUsedForLocking() && column.table() == dbRow.getEntity()) {
            dbRow.getQualifier()
                    .addAdditionalQualifier(column, diff.getSnapshotValue(attribute.name()), true);

        } else {
            // unimplemented case, see CAY-2560 for details.
            // we can't grab sub entity row here as no good accessor for this implemented.
        }
    }

    private void addToOne(CompiledToOneRelationship toOne) {
        if(toOne.isUsedForLocking()) {
            ObjectId value = diff.getArcSnapshotValue(toOne.name());
            CompiledJoin dbRelationship = toOne.joins().get(0);
            for(CompiledColumnPair join : dbRelationship.columnPairs()) {
                CompiledColumn source = join.source();
                if(!source.isPrimaryKey()) {
                    Object valueObjectId = value != null 
                        ? ObjectIdValueSupplier.getFor(value, join.target().name()) 
                        : null;
                    dbRow.getQualifier()
                            .addAdditionalQualifier(source, valueObjectId, true);
                }
            }
        }
    }
}
