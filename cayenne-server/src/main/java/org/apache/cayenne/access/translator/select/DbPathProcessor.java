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

    DbPathProcessor(TranslatorContext context, DbEntity entity, String parentPath) {
        super(context, entity);
        if(parentPath != null) {
            currentDbPath.append(parentPath);
        }
    }

    @Override
    protected void processNormalAttribute(String next) {
        DbAttribute dbAttribute = entity.getAttribute(next);
        if(dbAttribute != null) {
            processAttribute(dbAttribute);
            return;
        }

        DbRelationship relationship = entity.getRelationship(next);
        if(relationship != null) {
            entity = relationship.getTargetEntity();
            processRelationship(relationship);
            return;
        }

        throw new IllegalStateException("Unable to resolve path: " + currentDbPath.toString() + "." + next);
    }

    @Override
    protected void processAliasedAttribute(String next, String alias) {
        DbRelationship relationship = entity.getRelationship(alias);
        if(relationship == null) {
            throw new IllegalStateException("Non-relationship aliased path part: " + alias);
        }

        processRelationship(relationship);
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
            context.getTableTree().addJoinTable(currentDbPath.toString(), relationship, JoinType.LEFT_OUTER);
            if(!relationship.isToMany()) {
                String path = currentDbPath.toString();
                for (DbAttribute attribute : relationship.getTargetEntity().getPrimaryKeys()) {
                    addAttribute(path, attribute);
                }
            }
        }
    }

    protected void processRelTermination(DbRelationship rel) {
        this.relationship = rel;
        String path = currentDbPath.toString();
        appendCurrentPath(rel.getName());

        if (rel.isToMany() || !rel.isToPK()) {
            // match on target PK
            context.getTableTree().addJoinTable(currentDbPath.toString(), rel, JoinType.LEFT_OUTER);
            path = currentDbPath.toString();
            for(DbAttribute attribute : rel.getTargetEntity().getPrimaryKeys()) {
                addAttribute(path, attribute);
            }
        } else {
            for(DbJoin join : rel.getJoins()) {
                addAttribute(path, join.getSource());
            }
        }
    }

}
