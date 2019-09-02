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

package org.apache.cayenne.exp.property;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.query.PrefetchTreeNode;

/**
 * Interface (or "Trait") that provides basic functionality for all types of relationships.
 * <p>
 * Provides "dot", prefetch and "outer" functionality.
 *
 * @see org.apache.cayenne.exp.property
 * @since 4.2
 */
public interface RelationshipProperty<E> extends PathProperty<E> {

    /**
     * Returns a version of this property that represents an OUTER join. It is
     * up to caller to ensure that the property corresponds to a relationship,
     * as "outer" attributes make no sense.
     */
    BaseProperty<E> outer();

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with "joint"
     * prefetch semantics.
     */
    default PrefetchTreeNode joint() {
        if(!getExpression().getPathAliases().isEmpty()) {
            throw new CayenneRuntimeException("Can't use aliases with prefetch");
        }
        return PrefetchTreeNode.withPath(getName(), PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS);
    }

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with
     * "disjoint" prefetch semantics.
     */
    default PrefetchTreeNode disjoint() {
        if(!getExpression().getPathAliases().isEmpty()) {
            throw new CayenneRuntimeException("Can't use aliases with prefetch");
        }
        return PrefetchTreeNode.withPath(getName(), PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
    }

    /**
     * Returns a prefetch tree that follows this property path, potentially
     * spanning a number of phantom nodes, and having a single leaf with
     * "disjoint by id" prefetch semantics.
     */
    default PrefetchTreeNode disjointById() {
        if(!getExpression().getPathAliases().isEmpty()) {
            throw new CayenneRuntimeException("Can't use aliases with prefetch");
        }
        return PrefetchTreeNode.withPath(getName(), PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS);
    }

}
