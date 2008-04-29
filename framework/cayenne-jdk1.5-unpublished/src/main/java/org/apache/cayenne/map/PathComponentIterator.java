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

import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.cayenne.exp.ExpressionException;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
class PathComponentIterator implements Iterator<PathComponent<Attribute, Relationship>> {

    private StringTokenizer toks;
    private Entity currentEntity;
    private String path;

    PathComponentIterator(Entity root, String path) {
        currentEntity = root;
        toks = new StringTokenizer(path, Entity.PATH_SEPARATOR);
        this.path = path;
    }

    public boolean hasNext() {
        return toks.hasMoreTokens();
    }

    public PathComponent<Attribute, Relationship> next() {
        String pathComp = toks.nextToken();
        
        JoinType relationshipJoinType = JoinType.INNER;
        
        // we only support LEFT JOINS for now...
        if(pathComp.endsWith(Entity.OUTER_JOIN_INDICATOR)) {
            relationshipJoinType = JoinType.LEFT_OUTER;
            pathComp = pathComp.substring(0, pathComp.length() - Entity.OUTER_JOIN_INDICATOR.length());
        }

        // see if this is an attribute
        Attribute attr = currentEntity.getAttribute(pathComp);
        if (attr != null) {
            // do a sanity check...
            if (toks.hasMoreTokens())
                throw new ExpressionException(
                        "Attribute must be the last component of the path: '"
                                + pathComp
                                + "'.",
                        path,
                        null);

            return new AttributePathComponent<Attribute, Relationship>(attr);
        }

        Relationship rel = currentEntity.getRelationship(pathComp);
        if (rel != null) {
            currentEntity = rel.getTargetEntity();
            return new RelationshipPathComponent<Attribute, Relationship>(
                    rel,
                    relationshipJoinType,
                    !hasNext());
        }

        // build error message
        StringBuilder buf = new StringBuilder();
        buf
                .append("Can't resolve path component: [")
                .append(currentEntity.getName())
                .append('.')
                .append(pathComp)
                .append("].");
        throw new ExpressionException(buf.toString(), path, null);
    }

    public void remove() {
        throw new UnsupportedOperationException("'remove' operation is not supported.");
    }
}
