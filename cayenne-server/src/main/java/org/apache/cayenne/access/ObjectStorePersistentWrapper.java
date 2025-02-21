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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper for a {@link Persistent} object used by the {@link ObjectStore} to keep additional info about persistent.
 * <p>
 * Right now the only additional information it keeps is flattened path linked to the object.
 * @since 4.2.2
 */
public class ObjectStorePersistentWrapper implements Persistent {
    protected final Persistent dataObject;
    protected Map<String, ObjectId> trackedFlattenedPaths;

    public ObjectStorePersistentWrapper(Persistent dataObject) {
        this.dataObject = dataObject;
    }

    public boolean hasObject() {
        return dataObject != null;
    }

    public Persistent dataObject() {
        return dataObject;
    }

    public void markFlattenedPath(String path, ObjectId objectId) {
        if (trackedFlattenedPaths == null) {
            trackedFlattenedPaths = new ConcurrentHashMap<>();
        }
        trackedFlattenedPaths.put(path, objectId);
    }

    public boolean hasFlattenedPath(String path) {
        return trackedFlattenedPaths != null && trackedFlattenedPaths.containsKey(path);
    }

    public ObjectId getFlattenedId(String path) {
        return trackedFlattenedPaths == null ? null : trackedFlattenedPaths.get(path);
    }

    public Collection<ObjectId> getFlattenedIds() {
        return trackedFlattenedPaths == null ? Collections.emptyList() : trackedFlattenedPaths.values();
    }

    public Map<String, ObjectId> getFlattenedPathIdMap() {
        return trackedFlattenedPaths == null ? Collections.emptyMap() : trackedFlattenedPaths;
    }

    @Override
    public ObjectId getObjectId() {
        return dataObject.getObjectId();
    }

    @Override
    public void setObjectId(ObjectId id) {
        dataObject.setObjectId(id);
    }

    @Override
    public int getPersistenceState() {
        return dataObject.getPersistenceState();
    }

    @Override
    public void setPersistenceState(int state) {
        dataObject.setPersistenceState(state);
    }

    @Override
    public ObjectContext getObjectContext() {
        return dataObject.getObjectContext();
    }

    @Override
    public void setObjectContext(ObjectContext objectContext) {
        dataObject.setObjectContext(objectContext);
    }
}
