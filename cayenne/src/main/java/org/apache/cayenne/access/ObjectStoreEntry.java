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

package org.apache.cayenne.access;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.path.CayennePath;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 5.0
 */
public class ObjectStoreEntry implements Serializable {

    final protected Persistent persistent;
    protected Map<CayennePath, ObjectId> trackedFlattenedPaths;

    public ObjectStoreEntry(Persistent persistent) {
        this.persistent = persistent;
    }

    public Persistent persistent() {
        return persistent;
    }

    public boolean hasObject() {
        return persistent != null;
    }

    public void markFlattenedPath(CayennePath path, ObjectId objectId) {
        if (trackedFlattenedPaths == null) {
            trackedFlattenedPaths = new ConcurrentHashMap<>();
        }
        trackedFlattenedPaths.put(path, objectId);
    }

    public ObjectId getFlattenedId(CayennePath path) {
        return trackedFlattenedPaths == null ? null : trackedFlattenedPaths.get(path);
    }

    public Collection<ObjectId> getFlattenedIds() {
        return trackedFlattenedPaths == null ? Collections.emptyList() : trackedFlattenedPaths.values();
    }

    public Map<CayennePath, ObjectId> getFlattenedPathIdMap() {
        return trackedFlattenedPaths == null ? Collections.emptyMap() : trackedFlattenedPaths;
    }
}
