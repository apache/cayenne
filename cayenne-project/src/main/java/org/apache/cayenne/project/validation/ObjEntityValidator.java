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
package org.apache.cayenne.project.validation;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

public class ObjEntityValidator extends ConfigurationNodeValidator<ObjEntity> {

    /**
     * @param validationConfig the config defining the behavior of this validator.
     * @since 5.0
     */
    public ObjEntityValidator(ValidationConfig validationConfig) {
        super(validationConfig);
    }

    @Override
    public void validate(ObjEntity node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.OBJ_ENTITY_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.OBJ_ENTITY_NAME_DUPLICATE, this::checkForNameDuplicates)
                .performIfEnabled(Inspection.OBJ_ENTITY_NO_DB_ENTITY, this::checkForDbEntity)
                .performIfEnabled(Inspection.OBJ_ENTITY_INVALID_CLASS, this::validateClassName)
                .performIfEnabled(Inspection.OBJ_ENTITY_INVALID_SUPER_CLASS, this::validateSuperClassName);
    }

    private void checkForName(ObjEntity entity, ValidationResult validationResult) {

        // Must have name
        String name = entity.getName();
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, entity, "Unnamed ObjEntity");
        }
    }

    private void checkForNameDuplicates(ObjEntity entity, ValidationResult validationResult) {
        String name = entity.getName();
        DataMap map = entity.getDataMap();
        if (map == null || Util.isEmptyString(name)) {
            return;
        }

        // check for duplicate names in the parent context
        for (ObjEntity otherEnt : map.getObjEntities()) {
            if (otherEnt == entity) {
                continue;
            }
            if (name.equals(otherEnt.getName())) {
                addFailure(validationResult, entity, "Duplicate ObjEntity name: '%s'", name);
                break;
            }
        }

        // check for duplicates in other DataMaps
        DataChannelDescriptor domain = entity.getDataMap().getDataChannelDescriptor();
        if (domain == null) {
            return;
        }
        for (DataMap nextMap : domain.getDataMaps()) {
            if (nextMap == map) {
                continue;
            }

            ObjEntity conflictingEntity = nextMap.getObjEntity(name);
            if (conflictingEntity != null
                    && !Util.nullSafeEquals(conflictingEntity.getClassName(), entity.getClassName())) {
                addFailure(validationResult, entity, "Duplicate ObjEntity name in another DataMap: '%s'", name);
                break;
            }
        }
    }

    private void checkForDbEntity(ObjEntity entity, ValidationResult validationResult) {

        // validate DbEntity presence
        if (entity.getDbEntity() == null && !entity.isAbstract()) {
            addFailure(validationResult, entity, "ObjEntity '%s' has no DbEntity mapping", entity.getName());
        }
    }

    private void validateClassName(ObjEntity entity, ValidationResult validationResult) {
        String className = entity.getClassName();

        // if mapped to default class, ignore...
        if (Util.isEmptyString(className)) {
            return;
        }

        NameValidationHelper helper = NameValidationHelper.getInstance();
        String invalidChars = helper.invalidCharsInJavaClassName(className);

        if (invalidChars != null) {
            addFailure(validationResult, entity, "ObjEntity '%s' Java class '%s' contains invalid characters: %s",
                    entity.getName(), className, invalidChars);
        } else if (helper.invalidPersistentObjectClass(className)) {
            addFailure(validationResult, entity, "Java class '%s' of ObjEntity '%s' is a reserved word",
                    className, entity.getName());
        } else if (className.indexOf('.') < 0) {
            addFailure(validationResult, entity, "Java class '%s' of ObjEntity '%s' is in a default package",
                    className, entity.getName());
        }
    }

    private void validateSuperClassName(ObjEntity entity, ValidationResult validationResult) {
        String superClassName = entity.getSuperClassName();

        if (Util.isEmptyString(superClassName)) {
            return; // null is Ok
        }

        NameValidationHelper helper = NameValidationHelper.getInstance();
        String invalidChars = helper.invalidCharsInJavaClassName(superClassName);

        if (invalidChars != null) {
            addFailure(validationResult, entity, "ObjEntity '%s' Java superclass '%s' contains invalid characters: %s",
                    entity.getName(), superClassName, invalidChars);
        } else if (helper.invalidPersistentObjectClass(superClassName)) {
            addFailure(validationResult, entity, "ObjEntity '%s' Java superclass '%s' is a reserved word",
                    entity.getName(), superClassName);
        }

        if (entity.getDbEntityName() != null && entity.getSuperEntityName() != null) {
            addFailure(validationResult, entity,
                    "Sub ObjEntity '%s' has database table declaration different from super ObjEntity '%s'",
                    entity.getName(), entity.getSuperEntityName());
        }
    }
}
