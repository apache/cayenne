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

package org.apache.cayenne.graph;

import java.io.Serializable;
import java.util.Objects;

import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.reflect.ArcProperty;

/**
 * Object that represents Arc identifier.
 * Used in graph change operations.
 *
 * @since 4.2
 */
public class ArcId implements Serializable {

    private static final long serialVersionUID = -3712846298213425259L;

    private final String forwardArc;
    private final String reverseArc;

    public ArcId(ArcProperty property) {
        this.forwardArc = property.getName();
        this.reverseArc = property.getComplimentaryReverseArc() == null
                ? ASTDbPath.DB_PREFIX + property.getComplimentaryReverseDbRelationshipPath()
                : property.getComplimentaryReverseArc().getName();
    }

    public ArcId(String forwardArc, String reverseArc) {
        this.forwardArc = Objects.requireNonNull(forwardArc);
        this.reverseArc = Objects.requireNonNull(reverseArc);
    }

    public String getForwardArc() {
        return forwardArc;
    }

    public String getReverseArc() {
        return reverseArc;
    }

    public ArcId getReverseId() {
        return new ArcId(reverseArc, forwardArc);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArcId arcId = (ArcId) o;

        if (!forwardArc.equals(arcId.forwardArc)) return false;
        return reverseArc.equals(arcId.reverseArc);
    }

    @Override
    public int hashCode() {
        int result = forwardArc.hashCode();
        result = 31 * result + reverseArc.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return forwardArc;
    }
}
