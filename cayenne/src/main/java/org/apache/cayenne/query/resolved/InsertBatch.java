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

package org.apache.cayenne.query.resolved;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A batch of inserts of the rows of a single DbEntity. All attributes of the entity are bound.
 *
 * @since 6.0
 */
public record InsertBatch(DbEntity dbEntity, List<DbAttribute> dbAttributes, List<BatchRow> rows)
        implements ResolvedBatch {

    public InsertBatch(DbEntity dbEntity, List<BatchRow> rows) {
        this(dbEntity, new ArrayList<>(dbEntity.getAttributes()), rows);
    }

    @Override
    public boolean usingOptimisticLocking() {
        return false;
    }

    @Override
    public Object value(BatchRow row, int attributeIndex) {
        return row.values().get(dbAttributes.get(attributeIndex).getName());
    }
}
