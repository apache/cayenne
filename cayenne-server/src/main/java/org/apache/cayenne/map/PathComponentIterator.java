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
package org.apache.cayenne.map;

import org.apache.cayenne.exp.ExpressionException;

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
class PathComponentIterator implements Iterator<PathComponent<Attribute, Relationship>> {

    private final StringTokenizer toks;
    private final String path;
    private final Map<String, String> aliasMap;

    private Entity currentEntity;

    PathComponentIterator(Entity root, String path, Map<String, String> aliasMap) {
        this.currentEntity = Objects.requireNonNull(root);
        this.path = Objects.requireNonNull(path);
        this.aliasMap = Objects.requireNonNull(aliasMap);
        this.toks = new StringTokenizer(path, Entity.PATH_SEPARATOR);
    }

    public boolean hasNext() {
        return toks.hasMoreTokens();
    }

    public PathComponent<Attribute, Relationship> next() {
        String pathComp = toks.nextToken();

        JoinType relationshipJoinType = JoinType.INNER;

        // we only support LEFT JOINS for now...
        if (pathComp.endsWith(Entity.OUTER_JOIN_INDICATOR)) {
            relationshipJoinType = JoinType.LEFT_OUTER;
            pathComp = pathComp.substring(0, pathComp.length() - Entity.OUTER_JOIN_INDICATOR.length());
        }

        // see if this is an attribute
        Attribute attr = currentEntity.getAttribute(pathComp);
        if (attr != null) {
            // do a sanity check...
            if (toks.hasMoreTokens()) {
                throw new ExpressionException(
                        "Attribute must be the last component of the path: '" + pathComp + "'.", path, null);
            }

            return new AttributePathComponent<>(attr);
        }

        Relationship rel = currentEntity.getRelationship(pathComp);
        if (rel != null) {
            currentEntity = rel.getTargetEntity();
            return new RelationshipPathComponent<>(rel, relationshipJoinType, !hasNext());
        }

        PathComponent<Attribute, Relationship> aliasedPathComponent = getAliasedPathComponent(pathComp);
        if (aliasedPathComponent != null) {
            return aliasedPathComponent;
        }

        throw invalidPathException("Can't resolve path component", pathComp);
    }

    private PathComponent<Attribute, Relationship> getAliasedPathComponent(String pathComp) {
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

        Iterator<PathComponent<Attribute, Relationship>> subpathIt =
                new PathComponentIterator(currentEntity, aliasedPath, Collections.emptyMap());

        Collection<PathComponent<Attribute, Relationship>> parsedSubpath = new ArrayList<>(4);

        while (subpathIt.hasNext()) {
            PathComponent<Attribute, Relationship> subpathComponent = subpathIt.next();

            Relationship subpathRelationship = subpathComponent.getRelationship();
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
