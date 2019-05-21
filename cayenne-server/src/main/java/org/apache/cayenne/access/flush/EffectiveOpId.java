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

import java.util.Map;

import org.apache.cayenne.ObjectId;

/**
 * Helper value-object class that used to compare operations by "effective" id (i.e. by id snapshot,
 * that will include replacement id if any).
 * This class is not used directly by Cayenne, it's designed to ease custom implementations.
 * @since 4.2
 */
@SuppressWarnings("unused")
public class EffectiveOpId {
    private final String entityName;
    private final Map<String, Object> snapshot;

    public EffectiveOpId(ObjectId id) {
        this.entityName = id.getEntityName();
        this.snapshot = id.getIdSnapshot();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        if(snapshot.isEmpty()) {
            return false;
        }

        EffectiveOpId that = (EffectiveOpId) o;

        if (!entityName.equals(that.entityName)) return false;
        return snapshot.equals(that.snapshot);

    }

    @Override
    public int hashCode() {
        int result = entityName.hashCode();
        result = 31 * result + snapshot.hashCode();
        return result;
    }
}
