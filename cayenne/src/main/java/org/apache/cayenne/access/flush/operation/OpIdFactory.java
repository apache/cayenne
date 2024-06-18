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

package org.apache.cayenne.access.flush.operation;

import org.apache.cayenne.ObjectId;

import java.util.Map;
import java.util.Objects;

/**
 * Factory that wraps provided ID to be suitable for the better processing in the flush operation.
 *
 * @since 4.2
 */
public class OpIdFactory {

    private static final String DB_PREFIX = "db:";

    static public ObjectId idForOperation(ObjectId originalId) {
        if(originalId.isReplacementIdAttached() && originalId.getEntityName().startsWith(DB_PREFIX)) {
            return new ReplacementAwareObjectId(originalId);
        } else {
            return originalId;
        }
    }

    /**
     * Special wrapper for the ObjectId, that uses entity name + replacement map for hashCode() and equals()
     */
    static class ReplacementAwareObjectId implements ObjectId {

        private final ObjectId originalId;

        ReplacementAwareObjectId(ObjectId originalId) {
            this.originalId = Objects.requireNonNull(originalId);
        }

        @Override
        public boolean isTemporary() {
            return originalId.isTemporary();
        }

        @Override
        public String getEntityName() {
            return originalId.getEntityName();
        }

        @Override
        public byte[] getKey() {
            return originalId.getKey();
        }

        @Override
        public Map<String, Object> getIdSnapshot() {
            return originalId.getIdSnapshot();
        }

        @Override
        public Map<String, Object> getReplacementIdMap() {
            return originalId.getReplacementIdMap();
        }

        @Override
        public ObjectId createReplacementId() {
            return originalId.createReplacementId();
        }

        @Override
        public boolean isReplacementIdAttached() {
            return originalId.isReplacementIdAttached();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if(!(obj instanceof ObjectId)) {
                return false;
            }

            ObjectId other = (ObjectId) obj;
            if(!other.isReplacementIdAttached()) {
                return false;
            }
            if(!Objects.equals(originalId.getEntityName(), other.getEntityName())) {
                return false;
            }
            return originalId.getReplacementIdMap().equals(other.getReplacementIdMap());
        }

        @Override
        public int hashCode() {
            return 31 * getEntityName().hashCode() + originalId.getReplacementIdMap().hashCode();
        }

        @Override
        public String toString() {
            return "OpId: " + originalId;
        }
    }

}
