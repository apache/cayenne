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
package org.apache.cayenne.lifecycle.id;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.ObjectSelect;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fetches objects by their String IDs (see {@link IdCoder}). The IDs do not have to belong to the same entity: a
 * single call may span multiple entities and DB tables, and results in one query per distinct entity.
 *
 * @since 5.0
 */
public final class StringIdFetcher {

    private StringIdFetcher() {
    }

    /**
     * Returns an object matching the String ID, or null if no such object exists.
     */
    public static Persistent fetchOne(ObjectContext context, String stringId) {
        Objects.requireNonNull(stringId, "Null stringId");
        ObjectId id = new IdCoder(context.getEntityResolver()).getObjectId(stringId);
        return ObjectSelect.query(Persistent.class, id.getEntityName()).byId(id).selectOne(context);
    }

    /**
     * Returns a map of fetched objects keyed by their String IDs. IDs with no matching object are absent from
     * the map.
     */
    public static Map<String, Persistent> fetch(ObjectContext context, String... stringIds) {
        Objects.requireNonNull(stringIds, "Null stringIds");
        return fetch(context, Arrays.asList(stringIds));
    }

    /**
     * Returns a map of fetched objects keyed by their String IDs. IDs with no matching object are absent from
     * the map.
     */
    public static Map<String, Persistent> fetch(ObjectContext context, Collection<String> stringIds) {
        Objects.requireNonNull(stringIds, "Null stringIds");

        if (stringIds.isEmpty()) {
            return Map.of();
        }

        IdCoder coder = new IdCoder(context.getEntityResolver());

        // LinkedHashMap keeps entity groups in first-seen order, so the queries run in a predictable sequence
        Map<String, Map<ObjectId, String>> idsByEntity = new LinkedHashMap<>();
        for (String stringId : stringIds) {
            ObjectId id = coder.getObjectId(stringId);
            idsByEntity.computeIfAbsent(id.getEntityName(), n -> new HashMap<>()).put(id, stringId);
        }

        Map<String, Persistent> result = new HashMap<>();
        for (Map.Entry<String, Map<ObjectId, String>> entry : idsByEntity.entrySet()) {
            String entityName = entry.getKey();
            Map<ObjectId, String> ids = entry.getValue();

            List<Persistent> objects = ObjectSelect.query(Persistent.class, entityName)
                    .byIds(ids.keySet())
                    .select(context);

            for (Persistent object : objects) {
                ObjectId id = object.getObjectId();
                String stringId = ids.get(id);
                if (stringId == null) {
                    // a fetched object may be an instance of a subentity, in which case its ObjectId is not among
                    // the keys and must be looked up under the entity that was requested
                    stringId = ids.get(ObjectId.of(entityName, id.getIdSnapshot()));
                }
                result.put(stringId, object);
            }
        }

        return result;
    }
}
