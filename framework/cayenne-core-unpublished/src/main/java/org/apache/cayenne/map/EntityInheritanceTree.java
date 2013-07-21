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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.Expression;

/**
 * A tree structure representing inheritance hierarchy of an ObjEntity and its
 * subentities.
 * 
 * @since 1.1
 */
public class EntityInheritanceTree {

    protected ObjEntity entity;
    protected Collection<EntityInheritanceTree> subentities;
    protected Expression normalizedQualifier;

    public EntityInheritanceTree(ObjEntity entity) {
        this.entity = entity;
    }

    /**
     * Returns a qualifier Expression that matches root entity of this tree and all its
     * subentities.
     */
    public Expression qualifierForEntityAndSubclasses() {
        Expression qualifier = entity.getDeclaredQualifier();

        if (qualifier == null) {
            // match all
            return null;
        }

        if (subentities != null) {
            for (EntityInheritanceTree child : subentities) {
                Expression childQualifier = child.qualifierForEntityAndSubclasses();

                // if any child qualifier is null, just return null, since no filtering is
                // possible
                if (childQualifier == null) {
                    return null;
                }

                qualifier = qualifier.orExp(childQualifier);
            }
        }

        return qualifier;
    }

    /**
     * Returns the deepest possible entity in the inheritance hierarchy that can be used
     * to create objects from a given DataRow.
     */
    public ObjEntity entityMatchingRow(DataRow row) {
        // match depth first
        if (subentities != null) {
            for (EntityInheritanceTree child : subentities) {
                ObjEntity matched = child.entityMatchingRow(row);

                if (matched != null) {
                    return matched;
                }
            }
        }

        Expression qualifier = getDbQualifier();
        if (qualifier != null) {
            return qualifier.match(row) ? entity : null;
        }

        // no qualifier ... matches all rows
        return entity;
    }

    /**
     * Returns entity qualifier expressed as DB path qualifier or null if entity has no
     * qualifier.
     * 
     * @since 3.0
     */
    public Expression getDbQualifier() {
        if (entity.getDeclaredQualifier() == null) {
            return null;
        }

        if (normalizedQualifier == null) {
            normalizedQualifier = entity.translateToDbPath(entity.getDeclaredQualifier());
        }

        return normalizedQualifier;
    }

    public void addChildNode(EntityInheritanceTree node) {
        if (subentities == null) {
            subentities = new ArrayList<EntityInheritanceTree>(2);
        }

        subentities.add(node);
    }

    public int getChildrenCount() {
        return (subentities != null) ? subentities.size() : 0;
    }

    public Collection<EntityInheritanceTree> getChildren() {
        return (subentities != null) ? subentities : Collections.EMPTY_LIST;
    }

    public ObjEntity getEntity() {
        return entity;
    }

    /**
     * Returns a collection containing this inheritance tree node entity and all its
     * subentities.
     * 
     * @since 3.0
     */
    public Collection<ObjEntity> allSubEntities() {
        if (subentities == null) {
            return Collections.singletonList(entity);
        }

        Collection<ObjEntity> c = new ArrayList<ObjEntity>();
        appendSubentities(c);
        return c;
    }

    private void appendSubentities(Collection<ObjEntity> c) {
        c.add(entity);
        if (subentities == null) {
            return;
        }

        for (EntityInheritanceTree subentity : subentities) {
            subentity.appendSubentities(c);
        }
    }

    public Collection<ObjAttribute> allAttributes() {
        if (subentities == null) {
            return entity.getAttributes();
        }

        Collection<ObjAttribute> c = new ArrayList<ObjAttribute>();
        appendDeclaredAttributes(c);

        // add base attributes if any
        ObjEntity superEntity = entity.getSuperEntity();
        if (superEntity != null) {
            c.addAll(superEntity.getAttributes());
        }

        return c;
    }

    public Collection<ObjRelationship> allRelationships() {
        if (subentities == null) {
            return entity.getRelationships();
        }

        Collection<ObjRelationship> c = new ArrayList<ObjRelationship>();
        appendDeclaredRelationships(c);

        // add base relationships if any
        ObjEntity superEntity = entity.getSuperEntity();
        if (superEntity != null) {
            c.addAll(superEntity.getRelationships());
        }

        return c;
    }

    protected void appendDeclaredAttributes(Collection<ObjAttribute> c) {
        c.addAll(entity.getDeclaredAttributes());

        if (subentities != null) {
            for (EntityInheritanceTree child : subentities) {
                child.appendDeclaredAttributes(c);
            }
        }
    }

    protected void appendDeclaredRelationships(Collection<ObjRelationship> c) {
        c.addAll(entity.getDeclaredRelationships());

        if (subentities != null) {
            for (EntityInheritanceTree child : subentities) {
                child.appendDeclaredRelationships(c);
            }
        }
    }
}
