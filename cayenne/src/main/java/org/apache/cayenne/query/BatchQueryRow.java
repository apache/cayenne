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

package org.apache.cayenne.query;

import java.util.Map;
import java.util.function.Supplier;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.map.DbAttribute;

/**
 * Represents a single row of values in a BatchQuery. A value may be a deferred {@link Supplier}, e.g. when it comes
 * from a generated key of another row in the same transaction. Deferred values are resolved downstream, when the
 * row is bound to a PreparedStatement.
 *
 * @since 4.0
 */
public abstract class BatchQueryRow {

    protected ObjectId objectId;
    protected Map<String, Object> qualifier;

    public BatchQueryRow(ObjectId objectId, Map<String, Object> qualifier) {
        this.objectId = objectId;
        this.qualifier = qualifier;
    }

    public abstract Object getValue(int i);

    public Map<String, Object> getQualifier() {
        return qualifier;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    protected Object getValue(Map<String, Object> valueMap, DbAttribute attribute) {
        return valueMap.get(attribute.getName());
    }
}
