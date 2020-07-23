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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.util.IDUtil;

/**
 * Tmp {@link ObjectId}
 * @since 4.2
 */
class ObjectIdTmp implements ObjectId {

    private static final long serialVersionUID = 6566399722364067372L;

    private final String entityName;
    private final byte[] id;

    private Map<String, Object> replacementId;

    // exists for deserialization with Hessian and similar
    @SuppressWarnings("unused")
    private ObjectIdTmp() {
        entityName = null;
        id = null;
    }

    ObjectIdTmp(String entityName, byte[] id) {
        this.id = id;
        this.entityName = entityName;
    }

    ObjectIdTmp(String entityName) {
        this(entityName, IDUtil.pseudoUniqueByteSequence8());
    }

    @Override
    public boolean isTemporary() {
        return true;
    }

    @Override
    public String getEntityName() {
        return entityName;
    }

    @Override
    public byte[] getKey() {
        return id;
    }

    @Override
    public Map<String, Object> getIdSnapshot() {
        if(replacementId != null) {
            return Collections.unmodifiableMap(replacementId);
        }
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getReplacementIdMap() {
        if(replacementId == null) {
            replacementId = new HashMap<>();
        }
        return replacementId;
    }

    @Override
    public ObjectId createReplacementId() {
        return ObjectId.of(entityName, replacementId);
    }

    @Override
    public boolean isReplacementIdAttached() {
        return replacementId != null && !replacementId.isEmpty();
    }

    @Override
    public String toString() {
        return "<ObjectId:" + entityName + ",TEMP:" + hashCode() + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectIdTmp that = (ObjectIdTmp) o;
        if (id != that.id && !Arrays.equals(id, that.id)) {
            return false;
        }
        return entityName.equals(that.entityName);
    }

    @Override
    public int hashCode() {
        return 31 * entityName.hashCode() + Arrays.hashCode(id);
    }
}
