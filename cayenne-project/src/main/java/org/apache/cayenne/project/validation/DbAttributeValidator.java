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

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

public class DbAttributeValidator extends ConfigurationNodeValidator<DbAttribute> {

    /**
     * @param validationConfig the config defining the behavior of this validator.
     * @since 5.0
     */
    public DbAttributeValidator(ValidationConfig validationConfig) {
        super(validationConfig);
    }

    @Override
    public void validate(DbAttribute node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.DB_ATTRIBUTE_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.DB_ATTRIBUTE_INVALID_NAME, this::validateName)
                .performIfEnabled(Inspection.DB_ATTRIBUTE_NO_TYPE, this::checkForType)
                .performIfEnabled(Inspection.DB_ATTRIBUTE_NO_LENGTH, this::checkForLength);
    }

    private void checkForName(DbAttribute attribute, ValidationResult validationResult) {

        // Must have name
        if (Util.isEmptyString(attribute.getName())) {
            addFailure(validationResult, attribute, "Unnamed DbAttribute");
        }
    }

    private void validateName(DbAttribute attribute, ValidationResult validationResult) {
        NameValidationHelper helper = NameValidationHelper.getInstance();
        String name = attribute.getName();
        String invalidChars = helper.invalidCharsInDbPathComponent(name);
        if (Util.isEmptyString(name)) {
            return;
        }
        if (invalidChars != null) {
            addFailure(validationResult, attribute, "DbAttribute name '%s' contains invalid characters: %s",
                    name, invalidChars);
        }
    }

    private void checkForType(DbAttribute attribute, ValidationResult validationResult) {

        // all attributes must have type
        if (attribute.getType() == TypesMapping.NOT_DEFINED) {
            addFailure(validationResult, attribute, "DbAttribute has no type");
        }
    }

    private void checkForLength(DbAttribute attribute, ValidationResult validationResult) {
        if (attribute.getMaxLength() < 0
                && (attribute.getType() == java.sql.Types.VARCHAR
                || attribute.getType() == java.sql.Types.NVARCHAR
                || attribute.getType() == java.sql.Types.CHAR
                || attribute.getType() == java.sql.Types.NCHAR)) {
            // VARCHAR and CHAR attributes must have max length

            addFailure(validationResult, attribute, "Character DbAttribute '%s' doesn't have max length",
                    attribute.getName());
        }
    }
}
