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
package org.apache.cayenne.lifecycle.changeset;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;

/**
 * A {@link ChangeSet} implemented as a wrapper on top of {@link GraphDiff} of unspecified
 * nature.
 * <p>
 * Synchronization note: While this class is thread safe, but is not generally intended
 * for use in multi-threaded manner. It is common to use it within a single transaction
 * thread.
 * 
 * @since 3.1
 */
public class GenericChangeSet implements ChangeSet {

    private GraphDiff diff;
    private Map<ObjectId, Map<String, PropertyChange>> changes;

    public GenericChangeSet(GraphDiff diff) {
        this.diff = diff;
    }

    public Map<String, PropertyChange> getChanges(Persistent object) {
        Map<String, PropertyChange> changes = getChanges().get(object.getObjectId());
        return changes != null ? changes : Collections.EMPTY_MAP;
    }

    private Map<ObjectId, Map<String, PropertyChange>> getChanges() {
        if (changes == null) {
            changes = parseDiff();
        }

        return changes;
    }

    private Map<ObjectId, Map<String, PropertyChange>> parseDiff() {

        final Map<ObjectId, Map<String, PropertyChange>> changes = new HashMap<ObjectId, Map<String, PropertyChange>>();

        diff.apply(new GraphChangeHandler() {

            private Map<String, PropertyChange> getChangeMap(Object id) {
                Map<String, PropertyChange> map = changes.get(id);

                if (map == null) {
                    map = new HashMap<String, PropertyChange>();
                    changes.put((ObjectId) id, map);
                }

                return map;
            }

            PropertyChange getChange(Object id, String property, Object oldValue) {
                Map<String, PropertyChange> map = getChangeMap(id);

                PropertyChange change = map.get(property);
                if (change == null) {
                    change = new PropertyChange(property, oldValue);
                    map.put(property, change);
                }

                return change;
            }

            public void nodeRemoved(Object nodeId) {
                // noop, don't care, we'll still track the changes for deleted objects.
            }

            public void nodeCreated(Object nodeId) {
                // noop (??)
            }

            public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
                // record the fact of relationship change... TODO: analyze relationship
                // semantics and record changset values
                getChange(nodeId, (String) arcId, null);
            }

            public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
                // record the fact of relationship change... TODO: analyze relationship
                // semantics and record changset values
                getChange(nodeId, (String) arcId, null);
            }

            public void nodePropertyChanged(
                    Object nodeId,
                    String property,
                    Object oldValue,
                    Object newValue) {
                getChange(nodeId, property, oldValue).setNewValue(newValue);
            }

            public void nodeIdChanged(Object nodeId, Object newId) {

                // store the same change set under old and new ids to allow lookup before
                // and after the commit
                Map<String, PropertyChange> map = getChangeMap(nodeId);
                changes.put((ObjectId) newId, map);

                // record a change for a special ID "property"
                getChange(nodeId, OBJECT_ID_PROPERTY_NAME, nodeId).setNewValue(newId);
            }

        });

        return changes;
    }
}
