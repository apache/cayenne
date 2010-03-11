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
package org.apache.cayenne.project2.validation;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.Util;

class ObjEntityValidator {

    void validate(Object object, ValidationVisitor validationVisitor) {
        ObjEntity ent = (ObjEntity) object;

        validateName(ent, object, validationVisitor);
        validateClassName(ent, object, validationVisitor);
        validateSuperClassName(ent, object, validationVisitor);

        // validate DbEntity presence
        if (ent.getDbEntity() == null && !ent.isAbstract()) {
            validationVisitor.registerWarning(
                    "ObjEntity has no DbEntity mapping.",
                    object);
        }
    }

    void validateClassName(
            ObjEntity ent,
            Object object,
            ValidationVisitor validationVisitor) {
        String className = ent.getClassName();

        // if mapped to default class, ignore...
        if (Util.isEmptyString(className)) {
            return;
        }

        NameValidationHelper helper = NameValidationHelper.getInstance();
        String invalidChars = helper.invalidCharsInJavaClassName(className);

        if (invalidChars != null) {
            validationVisitor.registerWarning(
                    "ObjEntity Java class contains invalid characters: " + invalidChars,
                    object);
        }
        else if (helper.invalidDataObjectClass(className)) {
            validationVisitor.registerWarning("ObjEntity Java class is invalid: "
                    + className, object);
        }
        else if (className.indexOf('.') < 0) {
            validationVisitor.registerWarning(
                    "Placing Java class in default package is discouraged: " + className,
                    object);
        }
    }

    void validateSuperClassName(
            ObjEntity ent,
            Object object,
            ValidationVisitor validationVisitor) {
        String superClassName = ent.getSuperClassName();

        if (Util.isEmptyString(superClassName)) {
            return; // null is Ok
        }

        NameValidationHelper helper = NameValidationHelper.getInstance();
        String invalidChars = helper.invalidCharsInJavaClassName(superClassName);

        if (invalidChars != null) {
            validationVisitor.registerWarning(
                    "ObjEntity Java superclass contains invalid characters: "
                            + invalidChars,
                    object);
        }
        else if (helper.invalidDataObjectClass(superClassName)) {
            validationVisitor.registerWarning("ObjEntity Java superclass is invalid: "
                    + superClassName, object);
        }

        DataMap map = ent.getDataMap();
        if (map == null) {
            return;
        }
    }

    void validateName(ObjEntity entity, Object object, ValidationVisitor validationVisitor) {
        String name = entity.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validationVisitor.registerError("Unnamed ObjEntity.", object);
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
                validationVisitor.registerError(
                        "Duplicate ObjEntity name: " + name + ".",
                        object);
                break;
            }
        }

        // check for dupliucates in other DataMaps
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
                        validationVisitor.registerWarning(
                                "Duplicate ObjEntity name in another DataMap: "
                                        + name
                                        + ".",
                                object);
                        break;
                    }
                }
            }
        }
    }
}
