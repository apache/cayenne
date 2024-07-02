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
package org.apache.cayenne.modeler.editor.validation;

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.modeler.editor.ObjAttributeTableModel;
import org.apache.cayenne.modeler.editor.wrapper.ObjAttributeWrapper;
import org.apache.cayenne.project.validation.ConfigurationNodeValidator;
import org.apache.cayenne.project.validation.ValidationConfig;
import org.apache.cayenne.validation.ValidationResult;

import java.util.function.Supplier;

public class ObjAttributeWrapperValidator extends ConfigurationNodeValidator<ObjAttributeWrapper> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public ObjAttributeWrapperValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(ObjAttributeWrapper node, ValidationResult validationResult) {
        validateName(node, validationResult);
    }

    private void validateName(ObjAttributeWrapper wrapper, ValidationResult validationResult) {
        if (isAttributeNameOverlapped(wrapper)) {
            addFailure(validationResult, new AttributeValidationFailure(
                    ObjAttributeTableModel.OBJ_ATTRIBUTE,
                    "Duplicate attribute name."));
        }
    }

    /**
     * @return true if entity has attribute with the same name.
     */
    private boolean isAttributeNameOverlapped(ObjAttributeWrapper wrapper) {
    	ObjAttribute otherAttribute = wrapper.getEntity().getAttributeMap().get(wrapper.getName());
        return otherAttribute != null && wrapper.getValue() != otherAttribute;
    }
}
