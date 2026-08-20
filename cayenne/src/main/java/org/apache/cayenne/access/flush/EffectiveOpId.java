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
import org.apache.cayenne.access.DeferredValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper value-object class that used to compare operations by "effective" id (i.e. by id snapshot,
 * that will include replacement id if any).
 *
 * @since 4.2
 */
public class EffectiveOpId {

    private final String entityName;
    private final Map<String, Object> snapshot;
    private final ObjectId id;

    public EffectiveOpId(ObjectId id) {
        this(id, id.getEntityName(), id.getIdSnapshot());
    }

    public EffectiveOpId(String entityName, ObjectId id) {
        this(id, entityName, id.getIdSnapshot());
    }

    public EffectiveOpId(String entityName, Map<String, Object> idSnapshot) {
        this(null, entityName, idSnapshot);
    }

    private EffectiveOpId(ObjectId id, String entityName, Map<String, Object> idSnapshot) {
        this.entityName = entityName;
        if(idSnapshot.size() == 1 && !(idSnapshot.values().iterator().next() instanceof DeferredValue)) {
            this.snapshot = idSnapshot;
        } else {
            this.snapshot = new HashMap<>(idSnapshot.size());
            idSnapshot.forEach((key, value) -> {
                Object resolved = DeferredValue.resolve(value);
                // keep the unresolved value if it resolves to null, preserving the original snapshot entry
                this.snapshot.put(key, resolved != null ? resolved : value);
            });
        }
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EffectiveOpId that = (EffectiveOpId) o;

        if(id == that.id) return true;

        if(snapshot.isEmpty()) {
            return false;
        }

        if (!entityName.equals(that.entityName)) return false;
        return snapshot.equals(that.snapshot);

    }

    @Override
    public int hashCode() {
        int result = entityName.hashCode();
        result = 31 * result + snapshot.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "EffectiveOpId{" + entityName + ": " + snapshot + '}';
    }
}
