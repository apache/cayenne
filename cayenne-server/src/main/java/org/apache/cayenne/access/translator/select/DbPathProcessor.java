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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;

/**
 * @since 4.2
 */
class DbPathProcessor extends PathProcessor<DbEntity> {

    private final boolean flattenedPath;

    DbPathProcessor(TranslatorContext context, DbEntity entity, String parentPath, boolean flattenedPath) {
        super(context, entity);
        this.flattenedPath = flattenedPath;
        if (parentPath != null) {
            currentDbPath.append(parentPath);
        }
    }

    @Override
    public boolean isOuterJoin() {
        return super.isOuterJoin() || flattenedPath;
    }

    @Override
    protected void processNormalAttribute(String next) {
        if(next.startsWith("p:")) {
            next = next.substring(2);
        }
        DbAttribute dbAttribute = entity.getAttribute(next);
        if (dbAttribute != null) {
            processAttribute(dbAttribute);
            return;
        }

        DbRelationship relationship = entity.getRelationship(next);
        if (relationship != null) {
            entity = relationship.getTargetEntity();
            processRelationship(relationship);
            return;
        }

        // special case when the path should be processed in the context of the current join clause
        if(TableTree.CURRENT_ALIAS.equals(next)) {
            entity = context.getTableTree().nonNullActiveNode().getEntity();
            appendCurrentPath(next);
            return;
        }

        throw new IllegalStateException("Unable to resolve path: " + currentDbPath.toString() + "." + next);
    }

    @Override
    protected void processAliasedAttribute(String next, String alias) {
        int dotPosition = alias.indexOf(".");
        boolean isCompositeAlias = dotPosition >= 0;
        String trueAlias = isCompositeAlias ? alias.substring(0, dotPosition) : alias;
        String ending = isCompositeAlias ? alias.substring(dotPosition + 1) : "";

        DbRelationship relationship = entity.getRelationship(trueAlias);
        if (relationship == null) {
            throw new IllegalStateException("Non-relationship aliased path part: " + trueAlias);
        }

        entity = relationship.getTargetEntity();
        processRelationship(relationship);
        if (ending.isEmpty()) {
            return;
        }
        processNormalAttribute(ending);
    }

    private void processAttribute(DbAttribute attribute) {
        addAttribute(currentDbPath.toString(), attribute);
        appendCurrentPath(attribute.getName());
    }

    private void processRelationship(DbRelationship relationship) {
        if (lastComponent) {
            // if this is a last relationship in the path, it needs special handling
            processRelTermination(relationship);
        } else {
            appendCurrentPath(relationship.getName());
            context.getTableTree().addJoinTable(currentDbPath.toString(), relationship, isOuterJoin()
                                                                                        ? JoinType.LEFT_OUTER
                                                                                        : JoinType.INNER);
            if (!relationship.isToMany()) {
                String path = currentDbPath.toString();
                for (DbAttribute attribute : relationship.getTargetEntity().getPrimaryKeys()) {
                    addAttribute(path, attribute);
                }
            }
        }
    }

    protected void processRelTermination(DbRelationship relationship) {
        this.relationship = relationship;
        String path = currentDbPath.toString();

        // Back to obj alias to keep joins optimised.
        if (currentAlias != null && currentAlias.startsWith(DB_PATH_ALIAS_INDICATOR)) {
            currentAlias = currentAlias.substring(DB_PATH_ALIAS_INDICATOR.length());
        }
        appendCurrentPath(relationship.getName());

        if (relationship.isToMany() || !relationship.isToPK()) {
            // match on target PK
            context.getTableTree().addJoinTable(currentDbPath.toString(), relationship, isOuterJoin()
                                                                                        ? JoinType.LEFT_OUTER
                                                                                        : JoinType.INNER);
            path = currentDbPath.toString();
            for (DbAttribute attribute : relationship.getTargetEntity().getPrimaryKeys()) {
                addAttribute(path, attribute);
            }
        } else {
            for (DbJoin join : relationship.getJoins()) {
                addAttribute(path, join.getSource());
            }
        }
    }
}
