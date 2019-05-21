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
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.Validating;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.NodeDiff;
import org.apache.cayenne.validation.ValidationException;
import org.apache.cayenne.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A GraphDiff facade for the ObjectStore changes. Provides a way for the lower
 * layers of the access stack to speed up processing of presorted ObjectStore
 * diffs.
 * 
 * @since 1.2
 */
public class ObjectStoreGraphDiff implements GraphDiff {

    private ObjectStore objectStore;
    private GraphDiff resolvedDiff;
    private int lastSeenDiffId;

    ObjectStoreGraphDiff(ObjectStore objectStore) {
        this.objectStore = objectStore;
        preprocess(objectStore);
    }

    public Map<Object, ObjectDiff> getChangesByObjectId() {
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

        // build a new collection for validation as validation methods may
        // result in
        // ObjectStore modifications

        Collection<Validating> objectsToValidate = null;

        for (final ObjectDiff diff : getChangesByObjectId().values()) {

            if (!diff.isNoop()) {

                noop = false;

                if (diff.getObject() instanceof Validating) {
                    if (objectsToValidate == null) {
                        objectsToValidate = new ArrayList<>();
                    }

                    objectsToValidate.add((Validating) diff.getObject());
                }

            }
        }

        if (objectsToValidate != null) {
            ValidationResult result = new ValidationResult();

            for (Validating object : objectsToValidate) {
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

    @Override
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

    @Override
    public void apply(GraphChangeHandler handler) {
        resolveDiff();
        resolvedDiff.apply(handler);
    }

    @Override
    public void undo(GraphChangeHandler handler) {
        resolveDiff();
        resolvedDiff.undo(handler);
    }

    /**
     * Converts diffs organized by ObjectId in a collection of diffs sorted by
     * diffId (same as creation order).
     */
	private void resolveDiff() {

		// refresh the diff on first access or if the underlying ObjectStore has
		// changed the the last time we cached the changes.
		if (resolvedDiff == null || lastSeenDiffId < objectStore.currentDiffId) {

			CompoundDiff diff = new CompoundDiff();
			Map<Object, ObjectDiff> changes = getChangesByObjectId();

			if (!changes.isEmpty()) {
				List<NodeDiff> allChanges = new ArrayList<>(changes.size() * 2);

				for (final ObjectDiff objectDiff : changes.values()) {
					objectDiff.appendDiffs(allChanges);
				}

				Collections.sort(allChanges);
				diff.addAll(allChanges);

			}

			this.lastSeenDiffId = objectStore.currentDiffId;
			this.resolvedDiff = diff;
		}
	}

    private void preprocess(ObjectStore objectStore) {

        Map<Object, ObjectDiff> changes = getChangesByObjectId();
        if (!changes.isEmpty()) {

            for (Entry<Object, ObjectDiff> entry : changes.entrySet()) {

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
