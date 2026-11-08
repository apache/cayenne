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

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledColumnPair;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.JoinType;

/**
 * @since 4.2
 */
class DbPathProcessor extends PathProcessor<CompiledTable> {

    private final boolean flattenedPath;

    DbPathProcessor(SelectTranslatorContext context, CompiledTable table, CayennePath parentPath, boolean flattenedPath) {
        super(context, table);
        this.flattenedPath = flattenedPath;
        if (parentPath != null) {
            currentDbPath = currentDbPath.withMarker(parentPath.marker()).dot(parentPath);
        }
    }

    @Override
    public boolean isOuterJoin() {
        return super.isOuterJoin() || flattenedPath;
    }

    @Override
    protected void processNormalAttribute(String next) {
        CompiledColumn column = entity.column(next);
        if (column != null) {
            processAttribute(column);
            return;
        }

        CompiledJoin join = entity.join(next);
        if (join != null) {
            entity = join.target();
            processRelationship(join);
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

        CompiledJoin join = entity.join(trueAlias);
        if (join == null) {
            throw new IllegalStateException("Non-relationship aliased path part: " + trueAlias);
        }

        entity = join.target();
        processRelationship(join);
        if (ending.isEmpty()) {
            return;
        }
        processNormalAttribute(ending);
    }

    private void processAttribute(CompiledColumn column) {
        addAttribute(currentDbPath, column);
        appendCurrentPath(column.name());
    }

    private void processRelationship(CompiledJoin join) {
        if (lastComponent) {
            // if this is a last relationship in the path, it needs special handling
            processRelTermination(join);
        } else {
            appendCurrentPath(join.name());
            context.getTableTree().addJoinTable(currentDbPath, join, isOuterJoin()
                                                                                        ? JoinType.LEFT_OUTER
                                                                                        : JoinType.INNER);
            if (!join.isToMany()) {
                for (CompiledColumn column : join.target().primaryKey()) {
                    addAttribute(currentDbPath, column);
                }
            }
        }
    }

    protected void processRelTermination(CompiledJoin join) {
        this.join = join;

        // Back to obj alias to keep joins optimised.
        if (currentAlias != null && currentAlias.startsWith(DB_PATH_ALIAS_INDICATOR)) {
            currentAlias = currentAlias.substring(DB_PATH_ALIAS_INDICATOR.length());
        }

        // check if we can match on the source table columns instead of joining the target: the source join
        // columns carry the target PK value both for an FK on the source and for a master-to-dependent
        // relationship, whose source PK is also the dependent PK (if the dependent row exists)
        if (!join.isToPrimaryKey()) {
            // must use target table
            // add relationship first
            appendCurrentPath(join.name());
            // match on target PK
            context.getTableTree().addJoinTable(currentDbPath, join, isOuterJoin()
                                                                                        ? JoinType.LEFT_OUTER
                                                                                        : JoinType.INNER);
            for (CompiledColumn column : join.target().primaryKey()) {
                addAttribute(currentDbPath, column);
            }
        } else {
            // can use own values
            for (CompiledColumnPair pair : join.columnPairs()) {
                addAttribute(currentDbPath, pair.source());
            }
            // add relationship last
            appendCurrentPath(join.name());
        }
    }
}
