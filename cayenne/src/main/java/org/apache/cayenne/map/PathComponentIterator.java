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
package org.apache.cayenne.map;

import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.path.CayennePathSegment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

/**
 * @since 3.0
 */
class PathComponentIterator<E extends Entity<E, A, R>, A extends Attribute<E, A, R>, R extends Relationship<E, A, R>>
        implements Iterator<PathComponent<A, R>> {

    private final CayennePath path;

    private final Iterator<CayennePathSegment> iterator;
    private final Map<String, String> aliasMap;

    private EmbeddedAttribute embeddedAttribute;
    private Entity<E, A, R> currentEntity;

    PathComponentIterator(Entity<E, A, R> root, CayennePath path, Map<String, String> aliasMap) {
        this.currentEntity = Objects.requireNonNull(root);
        this.path = Objects.requireNonNull(path);
        this.iterator = path.iterator();
        this.aliasMap = Objects.requireNonNull(aliasMap);
        this.embeddedAttribute = null;
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public PathComponent<A, R> next() {
        CayennePathSegment nextSegment = iterator.next();
        String pathComp = nextSegment.value();

        JoinType relationshipJoinType = nextSegment.isOuterJoin()
                ? JoinType.LEFT_OUTER
                : JoinType.INNER;

        // see if this is an attribute
        A attr;
        if(embeddedAttribute != null) {
            // TODO: assert that this iterator is for ObjEntity
            attr = (A)embeddedAttribute.getAttribute(pathComp);
            embeddedAttribute = null;
        } else {
            attr = currentEntity.getAttribute(pathComp);
        }

        if (attr != null) {
            // do a sanity check...
            if(attr instanceof EmbeddedAttribute) {
                embeddedAttribute = (EmbeddedAttribute)attr;
            } else if (iterator.hasNext()) {
                throw new ExpressionException(
                        "Attribute must be the last component of the path: '" + pathComp + "'.", path, null);
            }

            return new AttributePathComponent<>(attr);
        }

        R rel = currentEntity.getRelationship(pathComp);
        if (rel != null) {
            currentEntity = rel.getTargetEntity();
            return new RelationshipPathComponent<>(rel, relationshipJoinType, !hasNext());
        }

        PathComponent<A, R> aliasedPathComponent = getAliasedPathComponent(pathComp);
        if (aliasedPathComponent != null) {
            return aliasedPathComponent;
        }

        throw invalidPathException("Can't resolve path component", pathComp);
    }

    private PathComponent<A, R> getAliasedPathComponent(String pathComp) {
        String aliasedPath = aliasMap.get(pathComp);
        if(aliasedPath == null) {
            return null;
        }

        // a few fairly arbitrary assumptions.... if we find that they restrict valid
        // and useful cases, we can change this behavior:
        //
        // 1. No nested aliases. Aliased path must contain only unaliased component names.
        // 2. Subpath must be relationship-only. Aliasing attributes doesn't seem
        // useful, so we don't handle this case for simplicity...

        // fully resolve subpath here... since we need to know the target entity of
        // the subpath, we have to fully traverse it, hence instead of lazy iterator
        // we might as well reuse obtained information in the AliasPathComponent

        Iterator<PathComponent<A, R>> subpathIt =
                new PathComponentIterator<>(currentEntity, CayennePath.of(aliasedPath), Collections.emptyMap());

        Collection<PathComponent<A, R>> parsedSubpath = new ArrayList<>(4);

        while (subpathIt.hasNext()) {
            PathComponent<A, R> subpathComponent = subpathIt.next();

            R subpathRelationship = subpathComponent.getRelationship();
            if (subpathRelationship == null) {
                throw invalidPathException(
                        "Expected a relationship in the aliased subpath. Alias [" + pathComp + "]",
                        subpathComponent.getName());
            }

            currentEntity = subpathRelationship.getTargetEntity();
            parsedSubpath.add(subpathComponent);
        }

        return new AliasPathComponent<>(pathComp, parsedSubpath, !hasNext());
    }

    private ExpressionException invalidPathException(String message, String pathComponent) {
        String buffer = message + ": [" + currentEntity.getName() + '.' + pathComponent + "].";
        return new ExpressionException(buffer, path, null);
    }

    public void remove() {
        throw new UnsupportedOperationException("'remove' operation is not supported.");
    }
}
