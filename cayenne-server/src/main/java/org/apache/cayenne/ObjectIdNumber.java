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

package org.apache.cayenne;

import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.util.SingleEntryMap;

/**
 * Optimized implementation of {@link ObjectId} for single numeric PK
 * @since 4.2
 */
class ObjectIdNumber implements ObjectId {

    private static final long serialVersionUID = 3968183354758914938L;

    // this two fields can be kept somewhere else as ID index shared by all IDs
    private final String entityName;
    private final String keyName;

    private final Number value;

    private SingleEntryMap<String, Object> replacementId;

    // exists for deserialization with Hessian and similar
    @SuppressWarnings("unused")
    private ObjectIdNumber() {
        this.entityName = "";
        this.keyName = "";
        this.value = 0L;
    }

    ObjectIdNumber(String entityName, String keyName, Number value) {
        this.entityName = entityName;
        this.keyName = keyName;
        this.value = value;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String getEntityName() {
        return entityName;
    }

    @Override
    public byte[] getKey() {
        return null;
    }

    @Override
    public Map<String, Object> getIdSnapshot() {
        return Collections.singletonMap(keyName, value);
    }

    @Override
    public Map<String, Object> getReplacementIdMap() {
        if(replacementId == null) {
            replacementId = new SingleEntryMap<>(keyName);
        }
        return replacementId;
    }

    @Override
    public ObjectId createReplacementId() {
        Object newValue = replacementId == null ? null : replacementId.getValue();
        return newValue == null ? this : ObjectId.of(entityName, keyName, newValue);
    }

    @Override
    public boolean isReplacementIdAttached() {
        return replacementId != null && !replacementId.isEmpty();
    }

    @Override
    public String toString() {
        return "<ObjectId:" + entityName + ", " + keyName + "=" + value + ">";
    }

    String getKeyName() {
        return keyName;
    }

    Number getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ObjectIdNumber that = (ObjectIdNumber) o;
        return value.longValue() == that.value.longValue() && entityName.equals(that.entityName);
    }

    @Override
    public int hashCode() {
        long longValue = value.longValue();
        return 31 * entityName.hashCode() + (int) (longValue ^ (longValue >>> 32));
    }
}
