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

package org.apache.cayenne.access.flush;

import java.util.Objects;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.graph.ArcId;

/**
 * Value object describing exact arc between two objects.
 * Implements {@link #equals(Object)} and {@link #hashCode()} methods.
 *
 * @since 4.2
 */
class ArcTarget {

    private final ObjectId sourceId;
    private final ObjectId targetId;
    private final ArcId arcId;
    private final boolean delete;

    ArcTarget(ObjectId sourceId, ObjectId targetId, ArcId arcId, boolean delete) {
        this.sourceId = Objects.requireNonNull(sourceId);
        this.targetId = Objects.requireNonNull(targetId);
        this.arcId = Objects.requireNonNull(arcId);
        this.delete = delete;
    }

    ArcTarget getReversed() {
        return new ArcTarget(targetId, sourceId, arcId.getReverseId(), delete);
    }

    ArcId getArcId() {
        return arcId;
    }

    ObjectId getSourceId() {
        return sourceId;
    }

    ObjectId getTargetId() {
        return targetId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArcTarget arcTarget = (ArcTarget) o;
        if(delete != arcTarget.delete) {
            return false;
        }
        if (!sourceId.equals(arcTarget.sourceId)) {
            return false;
        }
        if (!targetId.equals(arcTarget.targetId)) {
            return false;
        }
        return arcId.equals(arcTarget.arcId);
    }

    @Override
    public int hashCode() {
        int result = sourceId.hashCode();
        result = 31 * result + targetId.hashCode();
        result = 31 * result + arcId.hashCode();
        result = 31 * result + (delete ? 1 : 0);
        return result;
    }
}
