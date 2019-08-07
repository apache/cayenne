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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * @since 4.2
 */
class ObjPathProcessor extends PathProcessor<ObjEntity> {

    private ObjAttribute attribute;
    private EmbeddedAttribute embeddedAttribute;

    ObjPathProcessor(TranslatorContext context, ObjEntity entity, String parentPath) {
        super(context, entity);
        if(parentPath != null) {
            currentDbPath.append(parentPath);
        }
    }

    ObjAttribute getAttribute() {
        return attribute;
    }

    @Override
    protected void processNormalAttribute(String next) {
        attribute = fetchAttribute(next);
        if(attribute != null) {
            processAttribute(attribute);
            return;
        }

        ObjRelationship relationship = entity.getRelationship(next);
        if(relationship != null) {
            processRelationship(relationship);
            return;
        }

        throw new IllegalStateException("Unable to resolve path: " + currentDbPath.toString()
                + " (unknown '" + next + "' component)");
    }

    @Override
    protected void processId(String id) {
        if(id.equals(Cayenne.PROPERTY_ID)) {
            for (DbAttribute attribute : entity.getDbEntity().getPrimaryKeys()) {
                attributes.add(attribute);
                attributePaths.add(currentDbPath.toString());
            }
        } else {
            DbAttribute attribute = entity.getDbEntity().getAttribute(id.substring(Cayenne.PROPERTY_ID.length() + 1));
            attributes.add(attribute);
            attributePaths.add(currentDbPath.toString());
        }
    }

    @Override
    protected void processAliasedAttribute(String next, String alias) {
        ObjRelationship relationship = entity.getRelationship(alias);
        if(relationship == null) {
            throw new IllegalStateException("Non-relationship aliased path part: " + alias);
        }

        processRelationship(relationship);
    }

    protected ObjAttribute fetchAttribute(String name) {
        if(embeddedAttribute != null) {
            ObjAttribute attribute = embeddedAttribute.getAttribute(name);
            embeddedAttribute = null;
            return attribute;
        } else {
            return entity.getAttribute(name);
        }
    }

    protected void processAttribute(ObjAttribute attribute) {
        if(attribute instanceof EmbeddedAttribute) {
            embeddedAttribute = (EmbeddedAttribute)attribute;
            return;
        }

        PathTranslationResult result = context.getPathTranslator().translatePath(entity.getDbEntity()
                , attribute.getDbAttributePath()
                , currentDbPath.toString()
                , attribute.isFlattened());
        attributes.addAll(result.getDbAttributes());
        attributePaths.addAll(result.getAttributePaths());
        relationship = result.getDbRelationship().orElse(relationship);
        currentDbPath.delete(0, currentDbPath.length());
        currentDbPath.append(result.getFinalPath());
    }

    protected void processRelationship(ObjRelationship relationship) {
        if (lastComponent) {
            // if this is a last relationship in the path, it needs special handling
            processRelTermination(relationship);
        } else {
            entity = relationship.getTargetEntity();
            // find and add joins ....
            int count = relationship.getDbRelationships().size();
            for (int i=0; i<count; i++) {
                DbRelationship dbRel = relationship.getDbRelationships().get(i);
                appendCurrentPath(dbRel.getName());
                boolean leftJoin = isOuterJoin() || count > 1;
                context.getTableTree().addJoinTable(currentDbPath.toString(), dbRel,
                        leftJoin ? JoinType.LEFT_OUTER : JoinType.INNER);
            }
        }
    }

    protected void processRelTermination(ObjRelationship relationship) {
        String path = currentAlias != null ? currentAlias : relationship.getDbRelationshipPath();
        if(isOuterJoin()) {
            path += OUTER_JOIN_INDICATOR;
        }
        PathTranslationResult result = context.getPathTranslator()
                .translatePath(entity.getDbEntity(), path, currentDbPath.toString(), relationship.isFlattened());
        attributes.addAll(result.getDbAttributes());
        attributePaths.addAll(result.getAttributePaths());
        this.relationship = result.getDbRelationship().orElse(this.relationship);
        currentDbPath.delete(0, currentDbPath.length());
        currentDbPath.append(result.getFinalPath());
    }

}
