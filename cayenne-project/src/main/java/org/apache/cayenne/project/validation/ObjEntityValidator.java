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
package org.apache.cayenne.project.validation;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

class ObjEntityValidator extends ConfigurationNodeValidator {

    void validate(ObjEntity entity, ValidationResult validationResult) {

        validateName(entity, validationResult);
        validateClassName(entity, validationResult);
        validateSuperClassName(entity, validationResult);

        // validate DbEntity presence
        if (entity.getDbEntity() == null && !entity.isAbstract()) {
            addFailure(
                    validationResult,
                    entity,
                    "ObjEntity '%s' has no DbEntity mapping",
                    entity.getName());
        }
    }

    void validateClassName(ObjEntity entity, ValidationResult validationResult) {
        String className = entity.getClassName();

        // if mapped to default class, ignore...
        if (Util.isEmptyString(className)) {
            return;
        }

        NameValidationHelper helper = NameValidationHelper.getInstance();
        String invalidChars = helper.invalidCharsInJavaClassName(className);

        if (invalidChars != null) {
            addFailure(
                    validationResult,
                    entity,
                    "ObjEntity '%s' Java class '%s' contains invalid characters: %s",
                    entity.getName(),
                    className,
                    invalidChars);
        }
        else if (helper.invalidDataObjectClass(className)) {
            addFailure(
                    validationResult,
                    entity,
                    "Java class '%s' of ObjEntity '%s' is a reserved word",
                    className,
                    entity.getName());
        }
        else if (className.indexOf('.') < 0) {
            addFailure(
                    validationResult,
                    entity,
                    "Java class '%s' of ObjEntity '%s' is in a default package",
                    className,
                    entity.getName());
        }
    }

    void validateSuperClassName(ObjEntity entity, ValidationResult validationResult) {
        String superClassName = entity.getSuperClassName();

        if (Util.isEmptyString(superClassName)) {
            return; // null is Ok
        }

        NameValidationHelper helper = NameValidationHelper.getInstance();
        String invalidChars = helper.invalidCharsInJavaClassName(superClassName);

        if (invalidChars != null) {
            addFailure(
                    validationResult,
                    entity,
                    "ObjEntity '%s' Java superclass '%s' contains invalid characters: %s",
                    entity.getName(),
                    superClassName,
                    invalidChars);
        }
        else if (helper.invalidDataObjectClass(superClassName)) {
            addFailure(
                    validationResult,
                    entity,
                    "ObjEntity '%s' Java superclass '%s' is a reserved word",
                    entity.getName(),
                    superClassName);
        }

        if (entity.getDbEntityName() != null && entity.getSuperEntityName() != null) {
            addFailure(
                    validationResult,
                    entity,
                    "Sub ObjEntity '%s' has database table declaration different from super ObjEntity '%s'",
                    entity.getName(),
                    entity.getSuperEntityName());
        }

        DataMap map = entity.getDataMap();
        if (map == null) {
            return;
        }
    }

    void validateName(ObjEntity entity, ValidationResult validationResult) {
        String name = entity.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, entity, "Unnamed ObjEntity");
            return;
        }

        DataMap map = entity.getDataMap();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (ObjEntity otherEnt : map.getObjEntities()) {
            if (otherEnt == entity) {
                continue;
            }

            if (name.equals(otherEnt.getName())) {
                addFailure(
                        validationResult,
                        entity,
                        "Duplicate ObjEntity name: '%s'",
                        name);
                break;
            }
        }

        // check for duplicates in other DataMaps
        DataChannelDescriptor domain = entity.getDataMap().getDataChannelDescriptor();
        if (domain != null) {
            for (DataMap nextMap : domain.getDataMaps()) {
                if (nextMap == map) {
                    continue;
                }

                ObjEntity conflictingEntity = nextMap.getObjEntity(name);
                if (conflictingEntity != null) {

                    if (!Util.nullSafeEquals(conflictingEntity.getClassName(), entity
                            .getClassName())) {
                        addFailure(
                                validationResult,
                                entity,
                                "Duplicate ObjEntity name in another DataMap: '%s'",
                                name);
                        break;
                    }
                }
            }
        }
    }
}
