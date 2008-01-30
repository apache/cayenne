/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.Validating;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.validation.ValidationException;
import org.apache.cayenne.validation.ValidationResult;

/**
 * A GraphDiff facade for the ObjectStore changes. Provides a way for the lower layers of
 * the access stack to speed up processing of presorted ObjectStore diffs.
 * 
 * @author Andrus Adamchik
 * @since 1.2
 */
class ObjectStoreGraphDiff implements GraphDiff {

    private ObjectStore objectStore;
    private GraphDiff resolvedDiff;

    ObjectStoreGraphDiff(ObjectStore objectStore) {
        this.objectStore = objectStore;
        preprocess(objectStore);
    }

    Map<Object, ObjectDiff> getChangesByObjectId() {
        return objectStore.getChangesByObjectId();
    }

    /**
     * Requires external synchronization on ObjectStore.
     */
    boolean validateAndCheckNoop() {
        if (getChangesByObjectId().isEmpty()) {
            return true;
        }

        boolean noop = true;

        // build a new collection for validation as validation methods may result in
        // ObjectStore modifications

        Collection objectsToValidate = null;

        for (final ObjectDiff diff : getChangesByObjectId().values()) {

            if (!diff.isNoop()) {

                noop = false;

                if (diff.getObject() instanceof Validating) {
                    if (objectsToValidate == null) {
                        objectsToValidate = new ArrayList();
                    }

                    objectsToValidate.add(diff.getObject());
                }

            }
        }

        if (objectsToValidate != null) {
            ValidationResult result = new ValidationResult();

            Iterator validationIt = objectsToValidate.iterator();
            while (validationIt.hasNext()) {
                Validating object = (Validating) validationIt.next();
                switch (((Persistent) object).getPersistenceState()) {
                    case PersistenceState.NEW:
                        object.validateForInsert(result);
                        break;
                    case PersistenceState.MODIFIED:
                        object.validateForUpdate(result);
                        break;
                    case PersistenceState.DELETED:
                        object.validateForDelete(result);
                        break;
                }
            }

            if (result.hasFailures()) {
                throw new ValidationException(result);
            }
        }

        return noop;
    }

    public boolean isNoop() {
        if (getChangesByObjectId().isEmpty()) {
            return true;
        }

        for (ObjectDiff diff : getChangesByObjectId().values()) {
            if (!diff.isNoop()) {
                return false;
            }
        }

        return true;
    }

    public void apply(GraphChangeHandler handler) {
        resolveDiff();
        resolvedDiff.apply(handler);
    }

    public void undo(GraphChangeHandler handler) {
        resolveDiff();
        resolvedDiff.undo(handler);
    }

    /**
     * Converts diffs organized by ObjectId in a collection of diffs sorted by diffId
     * (same as creation order).
     */
    private void resolveDiff() {
        if (resolvedDiff == null) {

            CompoundDiff diff = new CompoundDiff();
            Map<Object, ObjectDiff> changes = getChangesByObjectId();

            if (!changes.isEmpty()) {
                List allChanges = new ArrayList(changes.size() * 2);

                for (final ObjectDiff objectDiff : changes.values()) {
                    objectDiff.appendDiffs(allChanges);
                }

                Collections.sort(allChanges);
                diff.addAll(allChanges);
            }

            this.resolvedDiff = diff;
        }
    }

    private void preprocess(ObjectStore objectStore) {

        Map changes = getChangesByObjectId();
        if (!changes.isEmpty()) {

            Iterator it = changes.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();

                ObjectId id = (ObjectId) entry.getKey();

                Persistent object = (Persistent) objectStore.getNode(id);

                // address manual id override.
                ObjectId objectId = object.getObjectId();
                if (!id.equals(objectId)) {

                    if (objectId != null) {
                        Map<String, Object> replacement = id.getReplacementIdMap();
                        replacement.clear();
                        replacement.putAll(objectId.getIdSnapshot());
                    }

                    object.setObjectId(id);
                }
            }
        }
    }
}
