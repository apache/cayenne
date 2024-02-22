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
package org.apache.cayenne.gen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.cayenne.PersistentObject;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.velocity.context.Context;

/**
 * {@link Artifact} facade for an ObjEntity.
 * 
 * @since 3.0
 */
public class EntityArtifact implements Artifact {

    public static String ENTITY_UTILS_KEY = "entityUtils";

    protected ObjEntity entity;

    public EntityArtifact(ObjEntity entity) {
        this.entity = entity;
        // make sure we have attributes and relationships in order
        sortAttributes();
        sortRelationships();
    }

    /**
     * Returns ObjEntity.
     */
    public Object getObject() {
        return entity;
    }

    public String getQualifiedBaseClassName() {
        return (entity.getSuperClassName() != null)
                ? entity.getSuperClassName()
                : PersistentObject.class.getName();
    }

    public String getQualifiedClassName() {
        return entity.getClassName();
    }

    public TemplateType getSingleClassType() {
        return TemplateType.ENTITY_SINGLE_CLASS;
    }

    public TemplateType getSubclassType() {
        return TemplateType.ENTITY_SUBCLASS;
    }

    public TemplateType getSuperClassType() {
        return TemplateType.ENTITY_SUPERCLASS;
    }

    public TemplateType[] getTemplateTypes(ArtifactGenerationMode mode) {
        switch (mode) {
            case SINGLE_CLASS:
                return new TemplateType[] {
                    TemplateType.ENTITY_SINGLE_CLASS
                };
            case GENERATION_GAP:
                return new TemplateType[] {
                        TemplateType.ENTITY_SUPERCLASS, TemplateType.ENTITY_SUBCLASS
                };
            default:
                return new TemplateType[0];
        }
    }

    public void postInitContext(Context context) {
        EntityUtils metadata = new EntityUtils(
                entity.getDataMap(),
                entity,
                (String) context.get(BASE_CLASS_KEY),
                (String) context.get(BASE_PACKAGE_KEY),
                (String) context.get(SUPER_CLASS_KEY),
                (String) context.get(SUPER_PACKAGE_KEY),
                (String) context.get(SUB_CLASS_KEY),
                (String) context.get(SUB_PACKAGE_KEY));

        context.put(ENTITY_UTILS_KEY, metadata);
    }

    private void sortAttributes() {
        List<ObjAttribute> objAttributes = new ArrayList<>(entity.getDeclaredAttributes());
        objAttributes.sort(Comparator.comparing(ObjAttribute::getName));
        for(ObjAttribute attribute : objAttributes) {
            entity.removeAttribute(attribute.getName());
            entity.addAttribute(attribute);
        }
    }

    private void sortRelationships() {
        List<ObjRelationship> relationships = new ArrayList<>(entity.getDeclaredRelationships());
        relationships.sort(Comparator.comparing(ObjRelationship::getName));
        for(ObjRelationship relationship : relationships) {
            entity.removeRelationship(relationship.getName());
            entity.addRelationship(relationship);
        }
    }
}
