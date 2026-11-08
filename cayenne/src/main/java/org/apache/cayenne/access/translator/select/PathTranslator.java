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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.compiled.CompiledEntity;

/**
 * @since 4.2
 */
class PathTranslator {

    private final Map<CayennePath, PathTranslationResult> objResultCache = new ConcurrentHashMap<>();
    private final Map<CayennePath, PathTranslationResult> dbResultCache = new ConcurrentHashMap<>();

    private final SelectTranslatorContext context;

    PathTranslator(SelectTranslatorContext context) {
        this.context = context;
    }

    PathTranslationResult translatePath(CompiledEntity entity, CayennePath path, CayennePath parentPath) {
        CayennePath key = parentPath == null
                ? path.dot(entity.name())
                : parentPath.dot(path).dot(entity.name());
        return objResultCache.computeIfAbsent(key,
                (k) -> new ObjPathProcessor(context, entity, parentPath).process(path));
    }

    PathTranslationResult translatePath(CompiledEntity entity, CayennePath path) {
        return translatePath(entity, path, null);
    }

    PathTranslationResult translatePath(CompiledTable table, CayennePath path, CayennePath parentPath, boolean flattenedPath) {
        CayennePath key = parentPath == null
                ? path.dot(table.name())
                : parentPath.dot(path).dot(table.name());
        return dbResultCache.computeIfAbsent(key,
                (k) -> new DbPathProcessor(context, table, parentPath, flattenedPath).process(path));
    }

    PathTranslationResult translatePath(CompiledTable table, CayennePath path, CayennePath parentPath) {
        return translatePath(table, path, parentPath, false);
    }

    PathTranslationResult translatePath(CompiledTable table, CayennePath path) {
        return translatePath(table, path, null);
    }

    PathTranslationResult translateIdPath(CompiledEntity entity, CayennePath path) {
        CayennePath objPathPart = path.parent();
        String pkName = path.last().value();
        if(objPathPart.isEmpty()) {
            // get PK directly from the query root
            if(pkName.isEmpty()) {
                throw new CayenneRuntimeException("Can't translate empty dbid path");
            }
            CompiledColumn pk = entity.table().column(pkName);
            if(pk == null) {
                throw new CayenneRuntimeException("Can't translate dbid path '%s', no such pk", path);
            }
            return new DbIdPathTranslationResult(CayennePath.EMPTY_PATH, pk);
        } else {
            // resolve object path part and get PK from the target entity
            PathTranslationResult dbIdResult = translatePath(entity, objPathPart);
            CompiledJoin join = dbIdResult.getDbRelationship()
                    .orElseThrow(() -> new CayenneRuntimeException("Can't translate dbid path '%s', can't resolve relationship %s", path, objPathPart));

            // manually join last segment as obj path translation would skip it
            JoinType joinType = objPathPart.last().isOuterJoin() ? JoinType.LEFT_OUTER : JoinType.INNER;
            context.getTableTree().addJoinTable(dbIdResult.getFinalPath(), join, joinType);

            CompiledColumn pk = join.target().column(pkName);
            if(pk == null) {
                throw new CayenneRuntimeException("Can't translate dbid path '%s', no such pk", path);
            }
            return new DbIdPathTranslationResult(dbIdResult.getFinalPath(), pk);
        }
    }

}
