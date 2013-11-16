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

import java.util.Map;

import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

class ObjAttributeValidator extends ConfigurationNodeValidator {

    void validate(ObjAttribute attribute, ValidationResult validationResult) {

        // Must have name
        if (Util.isEmptyString(attribute.getName())) {
            addFailure(validationResult, attribute, "Unnamed ObjAttribute");
        }
        else {
            NameValidationHelper helper = NameValidationHelper.getInstance();
            String invalidChars = helper.invalidCharsInObjPathComponent(attribute
                    .getName());

            if (invalidChars != null) {
                addFailure(
                        validationResult,
                        attribute,
                        "ObjAttribute name '%s' contains invalid characters: %s",
                        attribute.getName(),
                        invalidChars);
            }
            else if (helper.invalidDataObjectProperty(attribute.getName())) {
                addFailure(
                        validationResult,
                        attribute,
                        "ObjAttribute name '%s' is invalid",
                        attribute.getName());
            }
        }

        // all attributes must have type
        if (Util.isEmptyString(attribute.getType())) {
            addFailure(
                    validationResult,
                    attribute,
                    "ObjAttribute '%s' has no Java type",
                    attribute.getName());
        }

        if (attribute.getEntity() instanceof ObjEntity
                && ((ObjEntity) attribute.getEntity()).isAbstract()) {
            // nothing, abstract entity does not have to define a dbAttribute
        }
        else if (attribute instanceof EmbeddedAttribute) {
            Map<String, String> attrOverrides = ((EmbeddedAttribute) attribute)
                    .getAttributeOverrides();

            Embeddable embeddable = ((EmbeddedAttribute) attribute).getEmbeddable();
            if (embeddable == null && ((EmbeddedAttribute) attribute).getType() != null) {

                addFailure(
                        validationResult,
                        attribute,
                        "EmbeddedAttribute '%s' has incorrect Embeddable",
                        attribute.getName());
            }
            else if (embeddable == null
                    && ((EmbeddedAttribute) attribute).getType() == null) {
                addFailure(
                        validationResult,
                        attribute,
                        "EmbeddedAttribute '%s' has no Embeddable",
                        attribute.getName());
            }

            if (embeddable != null) {

                for (EmbeddableAttribute embeddableAttribute : embeddable.getAttributes()) {
                    String dbAttributeName;
                    if (attrOverrides.size() > 0
                            && attrOverrides.containsKey(embeddableAttribute.getName())) {
                        dbAttributeName = attrOverrides
                                .get(embeddableAttribute.getName());
                    }
                    else {
                        dbAttributeName = embeddableAttribute.getDbAttributeName();
                    }

                    if (dbAttributeName == "" || dbAttributeName == null) {

                        addFailure(
                                validationResult,
                                attribute,
                                "EmbeddedAttribute '%s' has no DbAttribute mapping",
                                attribute.getName());
                    }
                    else if (((ObjEntity) attribute.getEntity())
                            .getDbEntity()
                            .getAttribute(dbAttributeName) == null) {

                        addFailure(
                                validationResult,
                                attribute,
                                "EmbeddedAttribute '%s' has incorrect DbAttribute mapping",
                                attribute.getName());
                    }
                }
            }

        }
        else if (attribute.getDbAttribute() == null) {
            addFailure(
                    validationResult,
                    attribute,
                    "ObjAttribute '%s' has no DbAttribute mapping",
                    attribute.getName());
        }
        // can't support generated meaningful attributes for now; besides they don't make
        // sense.
        // TODO: andrus 03/10/2010 - is that really so? I think those are supported...
        else if (attribute.getDbAttribute().isPrimaryKey()
                && attribute.getDbAttribute().isGenerated()) {

            addFailure(
                    validationResult,
                    attribute,
                    "ObjAttribute '%s' is mapped to a generated PK: %s",
                    attribute.getName(),
                    attribute.getDbAttributeName());
        }

        checkForDuplicates(attribute, validationResult);
    }

    /**
     * Per CAY-1813, make sure two (or more) ObjAttributes do not map to the
     * same database path.
     */
    private void checkForDuplicates(ObjAttribute     attribute,
                                    ValidationResult validationResult) {
        if (attribute               != null &&
            attribute.getName()     != null &&
            attribute.isInherited() == false) {

            ObjEntity entity = (ObjEntity) attribute.getEntity();

            for (ObjAttribute comparisonAttribute : entity.getAttributes()) {
                if (attribute != comparisonAttribute) {
                    String dbAttributePath = attribute.getDbAttributePath();

                    if (dbAttributePath != null) {
                        if (dbAttributePath.equals(comparisonAttribute.getDbAttributePath())) {
                            addFailure
                                (validationResult,
                                 attribute,
                                 "ObjEntity '%s' contains a duplicate DbAttribute mapping ('%s' -> '%s')",
                                 entity.getName(),
                                 attribute.getName(),
                                 dbAttributePath);
                            return; // Duplicate found, stop.
                        }
                    }
                }
            }
        }
    }
}
