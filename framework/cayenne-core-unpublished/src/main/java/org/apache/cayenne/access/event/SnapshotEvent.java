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

package org.apache.cayenne.access.event;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.event.CayenneEvent;

/**
 * Event sent on modification of the DataRowStore.
 * 
 * @since 1.1
 */
public class SnapshotEvent extends CayenneEvent {

    protected long timestamp;
    protected Collection deletedIds;
    protected Collection invalidatedIds;
    protected Map modifiedDiffs;
    protected Collection indirectlyModifiedIds;

    public SnapshotEvent(Object source, Object postedBy, Map modifiedDiffs,
            Collection deletedIds, Collection invalidatedIds,
            Collection indirectlyModifiedIds) {

        super(source, postedBy, null);

        this.timestamp = System.currentTimeMillis();
        this.modifiedDiffs = modifiedDiffs;
        this.deletedIds = deletedIds;
        this.invalidatedIds = invalidatedIds;
        this.indirectlyModifiedIds = indirectlyModifiedIds;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map getModifiedDiffs() {
        return (modifiedDiffs != null) ? modifiedDiffs : Collections.EMPTY_MAP;
    }

    public Collection getDeletedIds() {
        return (deletedIds != null) ? deletedIds : Collections.EMPTY_LIST;
    }

    public Collection getInvalidatedIds() {
        return (invalidatedIds != null) ? invalidatedIds : Collections.EMPTY_LIST;
    }

    public Collection getIndirectlyModifiedIds() {
        return (indirectlyModifiedIds != null)
                ? indirectlyModifiedIds
                : Collections.EMPTY_LIST;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("[SnapshotEvent] source: ").append(getSource());

        Map modified = getModifiedDiffs();
        if (!modified.isEmpty()) {
            buffer.append(", modified ").append(modified.size()).append(" id(s)");
        }

        Collection deleted = getDeletedIds();
        if (!deleted.isEmpty()) {
            buffer.append(", deleted ").append(deleted.size()).append(" id(s)");
        }

        Collection invalidated = getInvalidatedIds();
        if (!invalidated.isEmpty()) {
            buffer.append(", invalidated ").append(invalidated.size()).append(" id(s)");
        }

        Collection related = getIndirectlyModifiedIds();
        if (!related.isEmpty()) {
            buffer.append(", indirectly modified ").append(related.size()).append(
                    " id(s)");
        }

        return buffer.toString();
    }
}
