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

import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.*;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

import java.util.Map;
import java.util.function.Supplier;

class ObjAttributeValidator extends ConfigurationNodeValidator<ObjAttribute> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public ObjAttributeValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(ObjAttribute node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.OBJ_ATTRIBUTE_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.OBJ_ATTRIBUTE_INVALID_NAME, this::validateName)
                .performIfEnabled(Inspection.OBJ_ATTRIBUTE_NO_TYPE, this::checkForType)
                .performIfEnabled(Inspection.OBJ_ATTRIBUTE_NO_EMBEDDABLE, this::checkForEmbeddable)
                .performIfEnabled(Inspection.OBJ_ATTRIBUTE_INVALID_MAPPING, this::validateDbAttributeMapping)
                .performIfEnabled(Inspection.OBJ_ATTRIBUTE_PATH_DUPLICATE, this::checkForPathDuplicates)
                .performIfEnabled(Inspection.OBJ_ATTRIBUTE_SUPER_NAME_DUPLICATE,
                        this::checkOnNameDuplicatesInSuperEntity);
    }

    private void checkForName(ObjAttribute attribute, ValidationResult validationResult) {

        // Must have name
        if (Util.isEmptyString(attribute.getName())) {
            addFailure(validationResult, attribute, "Unnamed ObjAttribute");
        }
    }

    private void validateName(ObjAttribute attribute, ValidationResult validationResult) {
        NameValidationHelper helper = NameValidationHelper.getInstance();
        String invalidChars = helper.invalidCharsInObjPathComponent(attribute.getName());

        if (invalidChars != null) {
            addFailure(validationResult, attribute,
                    "ObjAttribute name '%s' contains invalid characters: %s",
                    attribute.getName(),
                    invalidChars);
        } else if (helper.invalidPersistentObjectProperty(attribute.getName())) {
            addFailure(validationResult, attribute,
                    "ObjAttribute name '%s' is invalid",
                    attribute.getName());
        }
    }

    private void checkForType(ObjAttribute attribute, ValidationResult validationResult) {

        // all attributes must have type
        if (Util.isEmptyString(attribute.getType())) {
            addFailure(validationResult, attribute, "ObjAttribute '%s' has no Java type", attribute.getName());
        }
    }

    private void checkForEmbeddable(ObjAttribute attribute, ValidationResult validationResult) {
        if (!(attribute instanceof EmbeddedAttribute)) {
            return;
        }
        Embeddable embeddable = ((EmbeddedAttribute) attribute).getEmbeddable();
        if (embeddable == null) {
            String msg = attribute.getType() == null
                    ? "EmbeddedAttribute '%s' has no Embeddable"
                    : "EmbeddedAttribute '%s' has incorrect Embeddable";
            addFailure(validationResult, attribute, msg, attribute.getName());
        }
    }

    private void validateDbAttributeMapping(ObjAttribute attribute, ValidationResult validationResult) {
        if (attribute instanceof EmbeddedAttribute) {
            validateDbAttributeMappingEmbedded(((EmbeddedAttribute) attribute), validationResult);
        } else {
            validateDbAttributeMappingGeneral(attribute, validationResult);
        }
    }

    private void validateDbAttributeMappingEmbedded(EmbeddedAttribute attribute, ValidationResult validationResult) {
        Embeddable embeddable = attribute.getEmbeddable();
        if (embeddable == null) {
            return;
        }
        Map<String, String> attrOverrides = attribute.getAttributeOverrides();
        for (EmbeddableAttribute embeddableAttribute : embeddable.getAttributes()) {
            String dbAttributeName = attrOverrides.containsKey(embeddableAttribute.getName())
                    ? attrOverrides.get(embeddableAttribute.getName())
                    : embeddableAttribute.getDbAttributeName();
            if (Util.isEmptyString(dbAttributeName)) {
                addFailure(validationResult, attribute, "EmbeddedAttribute '%s' has no DbAttribute mapping",
                        attribute.getName());
            } else if (attribute.getEntity().getDbEntity().getAttribute(dbAttributeName) == null) {
                addFailure(validationResult, attribute, "EmbeddedAttribute '%s' has invalid DbAttribute mapping",
                        attribute.getName());
            }
        }
    }

    private void validateDbAttributeMappingGeneral(ObjAttribute attribute, ValidationResult validationResult) {
        if (attribute.getEntity().isAbstract()) {
            // nothing to validate
            // abstract entity does not have to define a dbAttribute
            return;
        }

        DbAttribute dbAttribute;
        try {
            dbAttribute = attribute.getDbAttribute();
        } catch (ExpressionException e) {
            // see CAY-2153
            // getDbAttribute() can fail if db path for this attribute is invalid
            // so we catch it here and show nice validation failure instead of crash
            addFailure(validationResult, attribute, "ObjAttribute '%s' has invalid DB path: %s",
                    attribute.getName(), e.getExpressionString());
            return;
        }

        if (dbAttribute == null) {
            addFailure(validationResult, attribute, "ObjAttribute '%s' has no DbAttribute mapping",
                    attribute.getName());
            return;
        }

        if (dbAttribute.isPrimaryKey() && dbAttribute.isGenerated()) {
            // can't support generated meaningful attributes for now;
            // besides they don't make sense.
            // TODO: andrus 03/10/2010 - is that really so? I think those are supported...
            addFailure(validationResult, attribute, "ObjAttribute '%s' is mapped to a generated PK: %s",
                    attribute.getName(), attribute.getDbAttributeName());
        }
    }

    /**
     * Per CAY-1813, make sure two (or more) ObjAttributes do not map to the
     * same database path.
     */
    private void checkForPathDuplicates(ObjAttribute attribute, ValidationResult validationResult) {
        if (attribute == null || attribute.getName() == null || attribute.isInherited()) {
            return;
        }

        ObjEntity entity = attribute.getEntity();
        CayennePath dbAttributePath = attribute.getDbAttributePath();

        for (ObjAttribute comparisonAttribute : entity.getAttributes()) {
            if (attribute != comparisonAttribute
                    && dbAttributePath != null
                    && dbAttributePath.equals(comparisonAttribute.getDbAttributePath())) {
                addFailure(validationResult, attribute,
                        "ObjEntity '%s' contains a duplicate DbAttribute mapping ('%s' -> '%s')",
                        entity.getName(), attribute.getName(), dbAttributePath);
                return; // Duplicate found, stop.
            }
        }
    }

    private void checkOnNameDuplicatesInSuperEntity(ObjAttribute attribute, ValidationResult validationResult) {

        // Check there is an attribute in entity and super entity at the same time
        boolean selfAttribute = attribute.getEntity().getDeclaredAttribute(attribute.getName()) != null;
        ObjEntity superEntity = attribute.getEntity().getSuperEntity();
        if (selfAttribute && superEntity != null && superEntity.getAttribute(attribute.getName()) != null) {
            addFailure(validationResult, attribute, "'%s' and super '%s' can't both have attribute '%s'",
                    attribute.getEntity().getName(), superEntity.getName(), attribute.getName());
        }
    }
}
