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

package org.apache.cayenne.modeler.dialog.objentity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.Relationship;
import org.apache.cayenne.util.Util;
import org.scopemvc.core.ModelChangeEvent;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;

/**
 * A model representing an Entity with a set of Relationships, with zero or one selected
 * Relationship.
 * 
 * @since 1.1
 */
public class EntityRelationshipsModel extends BasicModel {

    static final Pattern NAME_PATTERN = Pattern.compile("\\s\\[.*\\]$");

    public static final Selector RELATIONSHIP_DISPLAY_NAME_SELECTOR = Selector
            .fromString("relationshipDisplayName");

    protected Entity sourceEntity;
    protected String relationshipDisplayName;
    protected String defaultTargetName;
    protected Object[] relationshipNames;

    static String nameFromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }

        return NAME_PATTERN.matcher(displayName).replaceAll("");
    }

    static String displayName(Relationship relationship) {
        if (relationship == null) {
            return null;
        }

        String src = relationship.getSourceEntity() != null ? relationship
                .getSourceEntity()
                .getName() : "?";
        String target = relationship.getTargetEntityName() != null ? relationship
                .getTargetEntityName() : "?";

        return relationship.getName() + " [" + src + " -> " + target + "]";
    }

    /**
     * Creates EntityRelationshipsModel with two unconnected Entities.
     */
    public EntityRelationshipsModel(Entity sourceEntity, Entity targetEntity) {
        this.sourceEntity = sourceEntity;
        this.defaultTargetName = targetEntity.getName();
        this.relationshipDisplayName = "";
    }

    /**
     * Creates EntityRelationshipsModel over the relationship connecting two Entities.
     */
    public EntityRelationshipsModel(Relationship relationship) {
        this.sourceEntity = relationship.getSourceEntity();
        this.relationshipDisplayName = displayName(relationship);
    }

    public synchronized Object[] getRelationshipNames() {
        // build an ordered list of available relationship names
        // on demand
        if (relationshipNames == null) {
            Collection relationships = getSourceEntity().getRelationships();
            int size = relationships.size();
            Object[] names = new Object[size];

            Iterator it = relationships.iterator();
            for (int i = 0; i < size; i++) {
                DbRelationship next = (DbRelationship) it.next();
                names[i] = displayName(next);
            }
            Arrays.sort(names);
            this.relationshipNames = names;
        }

        return relationshipNames;
    }

    /**
     * Returns a root entity of this model.
     * 
     * @return
     */
    public Entity getSourceEntity() {
        return sourceEntity;
    }

    /**
     * Returns a String describing currently selected relationship.
     */
    public String getRelationshipDisplayName() {
        return relationshipDisplayName;
    }

    public void setRelationshipDisplayName(String relationshipDisplayName) {
        if (!Util.nullSafeEquals(relationshipDisplayName, this.relationshipDisplayName)) {
            this.relationshipDisplayName = relationshipDisplayName;
            relationshipNames = null;
            fireModelChange(
                    ModelChangeEvent.VALUE_CHANGED,
                    RELATIONSHIP_DISPLAY_NAME_SELECTOR);
        }
    }

    public void setRelationshipName(String relationshipName) {
        setRelationshipDisplayName(displayName(sourceEntity
                .getRelationship(relationshipName)));
    }

    public Relationship getSelectedRelationship() {
        return sourceEntity.getRelationship(nameFromDisplayName(relationshipDisplayName));
    }

    public String getSourceEntityName() {
        return sourceEntity.getName();
    }

    public String getTargetEntityName() {
        Relationship selected = getSelectedRelationship();
        return (selected != null) ? selected.getTargetEntityName() : defaultTargetName;
    }
}
