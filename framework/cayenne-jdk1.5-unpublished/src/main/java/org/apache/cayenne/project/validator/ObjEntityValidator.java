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

package org.apache.cayenne.project.validator;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

/**
 */
public class ObjEntityValidator extends TreeNodeValidator {

    /**
     * Constructor for ObjEntityValidator.
     */
    public ObjEntityValidator() {
        super();
    }

    @Override
    public void validateObject(ProjectPath path, Validator validator) {
        ObjEntity ent = (ObjEntity) path.getObject();
        validateName(ent, path, validator);
        validateClassName(ent, path, validator);
        validateSuperClassName(ent, path, validator);
        validateAttributes(ent, path, validator);
        validateRelationships(ent, path, validator);

        // validate DbEntity presence
        if (ent.getDbEntity() == null && !ent.isAbstract()) {
            validator.registerWarning("ObjEntity has no DbEntity mapping.", path);
        }
    }

    private void validateClassName(ObjEntity ent, ProjectPath path, Validator validator) {
        String className = ent.getClassName();

        // if mapped to default class, ignore...
        if (Util.isEmptyString(className)) {
            return;
        }

        MappingNamesHelper helper = MappingNamesHelper.getInstance();
        String invalidChars = helper.invalidCharsInJavaClassName(className);

        if (invalidChars != null) {
            validator.registerWarning(
                    "ObjEntity Java class contains invalid characters: " + invalidChars,
                    path);
        }
        else if (helper.invalidDataObjectClass(className)) {
            validator.registerWarning(
                    "ObjEntity Java class is invalid: " + className,
                    path);
        }
        else if (className.indexOf('.') < 0) {
            validator.registerWarning(
                    "Placing Java class in default package is discouraged: " + className,
                    path);
        }
    }

    private void validateSuperClassName(
            ObjEntity ent,
            ProjectPath path,
            Validator validator) {
        String superClassName = ent.getSuperClassName();

        if (Util.isEmptyString(superClassName)) {
            return; // null is Ok
        }

        MappingNamesHelper helper = MappingNamesHelper.getInstance();
        String invalidChars = helper.invalidCharsInJavaClassName(superClassName);

        if (invalidChars != null) {
            validator.registerWarning(
                    "ObjEntity Java superclass contains invalid characters: "
                            + invalidChars,
                    path);
        }
        else if (helper.invalidDataObjectClass(superClassName)) {
            validator.registerWarning("ObjEntity Java superclass is invalid: "
                    + superClassName, path);
        }

        DataMap map = (DataMap) path.getObjectParent();
        if (map == null) {
            return;
        }
    }

    private void validateAttributes(ObjEntity entity, ProjectPath path, Validator validator) {
        // Map of dbAttributeName:objAttributeName.
        Map<String,String> dbAttributes = new HashMap<String,String>();

        for (ObjAttribute attribute : entity.getAttributes()) {
            DbAttribute dbAttribute = attribute.getDbAttribute();

            /*
             * When a dbAttributeName has not been encountered, the
             * dbAttributeName is added to the map along with the
             * objAttributeName. This is the original occurrence.
             *
             * When a duplicate dbAttributeName is encountered, check to see if
             * the objAttributeName is NOT null (indicating the first time the
             * duplicate is encountered) and warn about the original
             * dbAttributeName (which is a duplication of the current one) and
             * set the original objAttributeName to null (indicating it has been
             * processed), then warn about the duplicate, too.
             */

            // Embleddables do not have DB Attributes.
            if (attribute instanceof EmbeddedAttribute) {
                if (dbAttribute != null)
                    validator.registerWarning("Embeddable (" + attribute.getName() + ") cannot have a DbAttribute mapping", path);
            }
            else if (dbAttribute == null) {
                validator.registerWarning("Attribute (" + attribute.getName() + ") must have a DbAttribute mapping", path);
            }
            else {
                String dbAttributeName = dbAttribute.getName();

                if (Util.isEmptyString(dbAttributeName) == false) {
                    // Be sure to add the original duplicate if not already processed:
                    if (dbAttributes.containsKey(dbAttributeName)) {
                        if (dbAttributes.get(dbAttributeName) != null) {
                            addAttributeWarning(validator, entity.getName(), dbAttributes.get(dbAttributeName), dbAttributeName, path);
                            dbAttributes.put(dbAttributeName, null);
                        }

                        // Add the current duplicate:
                        addAttributeWarning(validator, entity.getName(), attribute.getName(), dbAttributeName, path);
                    }
                    else {
                        // Add the original (not duplicated):
                        dbAttributes.put(dbAttributeName, attribute.getName());
                    }
                }
            }
        }
    }

    private void validateRelationships(ObjEntity entity, ProjectPath path, Validator validator) {
        // Map of relationshipPath:relationshipName.
        Map<String,String> dbRelationshipPaths = new HashMap<String,String>();

        for (ObjRelationship relationship : entity.getRelationships()) {
            String dbRelationshipPath = relationship.getTargetEntityName() + "." + relationship.getDbRelationshipPath();

            /*
             * When a relationshipPath has not been encountered, the
             * relationshipPath is added to the map along with the
             * relationshipName. This is the original occurrence.
             *
             * When a duplicate relationshipPath is encountered, check to see if
             * the relationshipName is NOT null (indicating the first time the
             * duplicate is encountered) and warn about the original
             * relationshipPath (which is a duplication of the current one) and
             * set the original relationshipName to null (indicating it has been
             * processed), then warn about the duplicate, too.
             */

            if (Util.isEmptyString(dbRelationshipPath) == false) {
                if (dbRelationshipPaths.containsKey(dbRelationshipPath)) {
                    // Be sure to add the original duplicate if not already processed:
                    if (dbRelationshipPaths.get(dbRelationshipPath) != null) {
                        addRelationshipWarning(validator, entity.getName(), dbRelationshipPaths.get(dbRelationshipPath), dbRelationshipPath, path);
                        dbRelationshipPaths.put(dbRelationshipPath, null);
                    }

                    // Add the current duplicate:
                    addRelationshipWarning(validator, entity.getName(), relationship.getName(), dbRelationshipPath, path);
                }
                else {
                    // Add the original (not duplicated):
                    dbRelationshipPaths.put(dbRelationshipPath, relationship.getName());
                }
            }
        }
    }

    private void addAttributeWarning(Validator validator, String entityName, String objAttributeName, String dbAttributeName, ProjectPath path) {
        validator.registerWarning("ObjEntity " + entityName + " contains duplicate DbRelationship mappings (" + objAttributeName + " -> " + dbAttributeName + ")", path);
    }

    private void addRelationshipWarning(Validator validator, String entityName, String relationshipName, String relationshipPath, ProjectPath path) {
        validator.registerWarning("ObjEntity " + entityName + " contains duplicate DbRelationship mappings (" + relationshipName + " -> " + relationshipPath + ")", path);
    }

    protected void validateName(ObjEntity entity, ProjectPath path, Validator validator) {
        String name = entity.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed ObjEntity.", path);
            return;
        }

        DataMap map = (DataMap) path.getObjectParent();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (ObjEntity otherEnt : map.getObjEntities()) {
            if (otherEnt == entity) {
                continue;
            }

            if (name.equals(otherEnt.getName())) {
                validator.registerError("Duplicate ObjEntity name: " + name + ".", path);
                break;
            }
        }

        // check for dupliucates in other DataMaps
        DataDomain domain = path.firstInstanceOf(DataDomain.class);
        if (domain != null) {
            for (DataMap nextMap : domain.getDataMaps()) {
                if (nextMap == map) {
                    continue;
                }

                ObjEntity conflictingEntity = nextMap.getObjEntity(name);
                if (conflictingEntity != null) {

                    if (!Util.nullSafeEquals(conflictingEntity.getClassName(), entity
                            .getClassName())) {
                        validator.registerWarning(
                                "Duplicate ObjEntity name in another DataMap: "
                                        + name
                                        + ".",
                                path);
                        break;
                    }
                }
            }
        }
    }
}
