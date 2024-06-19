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

import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

import java.util.function.Supplier;

public class EmbeddableAttributeValidator extends ConfigurationNodeValidator<EmbeddableAttribute> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public EmbeddableAttributeValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(EmbeddableAttribute node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.EMBEDDABLE_ATTRIBUTE_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.EMBEDDABLE_ATTRIBUTE_NO_TYPE, this::checkForType);
    }

    private void checkForName(EmbeddableAttribute attribute, ValidationResult validationResult) {

        // Must have name
        if (Util.isEmptyString(attribute.getName())) {
            addFailure(validationResult, attribute, "Unnamed EmbeddableAttribute");
        }
    }

    private void checkForType(EmbeddableAttribute attribute, ValidationResult validationResult) {

        // all attributes must have type
        if (Util.isEmptyString(attribute.getType())) {
            addFailure(validationResult, attribute, "EmbeddableAttribute '%s' has no type", attribute.getName());
        }
    }
}
