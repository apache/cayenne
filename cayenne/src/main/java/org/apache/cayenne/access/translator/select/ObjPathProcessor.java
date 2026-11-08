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
import org.apache.cayenne.exp.path.CayennePathSegment;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledEmbeddable;
import org.apache.cayenne.map.compiled.CompiledEmbeddedAttribute;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.compiled.CompiledAttribute;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;

import java.util.Optional;

/**
 * @since 4.2
 */
class ObjPathProcessor extends PathProcessor<CompiledEntity> {

    private CompiledAttribute attribute;
    private CompiledEmbeddedAttribute embeddedAttribute;

    ObjPathProcessor(SelectTranslatorContext context, CompiledEntity entity, CayennePath parentPath) {
        super(context, entity);
        if (parentPath != null) {
            currentDbPath = currentDbPath.withMarker(parentPath.marker()).dot(parentPath);
        }
    }

    CompiledAttribute getAttribute() {
        return attribute;
    }

    @Override
    protected void processNormalAttribute(String next) {
        attribute = fetchAttribute(next);
        if (attribute != null) {
            processAttribute(attribute);
            return;
        }

        CompiledEmbeddedAttribute embedded = entity.embeddedAttribute(next);
        if (embedded != null) {
            processEmbeddedAttribute(embedded);
            return;
        }

        CompiledRelationship relationship = entity.relationship(next);
        if (relationship != null) {
            processRelationship(relationship);
            return;
        }

        throw new IllegalStateException("Unable to resolve path: " + currentDbPath.toString()
                + " (unknown '" + next + "' component)");
    }

    @Override
    protected void processAliasedAttribute(String next, String alias) {
        CompiledRelationship relationship = entity.relationship(alias);
        if (relationship == null) {
            throw new IllegalStateException("Non-relationship aliased path part: " + alias);
        }

        pathSplitAliases.put(DB_PATH_ALIAS_INDICATOR + next, relationship.dbPath().value());
        processRelationship(relationship);
    }

    protected CompiledAttribute fetchAttribute(String name) {
        if (embeddedAttribute != null) {
            // an attribute of the embeddable follows the embedded attribute, and is named after both
            CompiledAttribute attribute = entity.attribute(embeddedAttribute.name() + "." + name);
            embeddedAttribute = null;
            return attribute;
        } else {
            return entity.attribute(name);
        }
    }

    protected void processEmbeddedAttribute(CompiledEmbeddedAttribute embedded) {
        embeddedAttribute = embedded;
        if (lastComponent) {
            embeddedAttribute.attributes().forEach(a -> {
                processAttribute(a);
                currentDbPath = currentDbPath.parent();
            });
        }
    }

    protected void processAttribute(CompiledAttribute attribute) {
        PathTranslationResult result = context.getPathTranslator().translatePath(entity.table()
                , attribute.dbPath()
                , currentDbPath
                , attribute.isFlattened());
        columns.addAll(result.getDbAttributes());
        attributePaths.addAll(result.getAttributePaths());
        join = result.getDbRelationship().orElse(join);

        currentDbPath = result.getFinalPath();
    }

    protected void processRelationship(CompiledRelationship relationship) {
        if (lastComponent) {
            // if this is a last relationship in the path, it needs special handling
            processRelTermination(relationship);
        } else {
            entity = relationship.target();
            // find and add joins ....
            int count = relationship.joins().size();
            for (int i = 0; i < count; i++) {
                CompiledJoin join = relationship.joins().get(i);
                appendCurrentPath(join.name());
                boolean leftJoin = isOuterJoin() || count > 1;
                context.getTableTree().addJoinTable(currentDbPath, join,
                        leftJoin ? JoinType.LEFT_OUTER : JoinType.INNER);
            }
        }
    }

    protected void processRelTermination(CompiledRelationship relationship) {
        CayennePath path = currentAlias != null
                ? CayennePath.EMPTY_PATH
                : relationship.dbPath().parent();
        CayennePathSegment lastSegment = currentAlias != null
                ? CayennePath.segmentOf(DB_PATH_ALIAS_INDICATOR + currentAlias)
                : relationship.dbPath().last();
        if (isOuterJoin()) {
            lastSegment = lastSegment.outer();
        }
        path = path.dot(lastSegment);

        PathTranslationResult result = context.getPathTranslator()
                .translatePath(entity.table(), path, currentDbPath, relationship.isFlattened());
        columns.addAll(result.getDbAttributes());
        attributePaths.addAll(result.getAttributePaths());
        this.join = result.getDbRelationship().orElse(this.join);

        currentDbPath = result.getFinalPath();
    }

    @Override
    public Optional<CompiledEmbeddable> getEmbeddable() {
        if (embeddedAttribute != null) {
            return Optional.of(embeddedAttribute.embeddable());
        }
        return Optional.empty();
    }
}
