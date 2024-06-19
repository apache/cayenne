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

import java.sql.Types;
import java.util.function.Supplier;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

public class ProcedureParameterValidator extends ConfigurationNodeValidator<ProcedureParameter> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public ProcedureParameterValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(ProcedureParameter node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.PROCEDURE_PARAMETER_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.PROCEDURE_PARAMETER_NO_TYPE, this::checkForType)
                .performIfEnabled(Inspection.PROCEDURE_PARAMETER_NO_LENGTH, this::checkForLength)
                .performIfEnabled(Inspection.PROCEDURE_PARAMETER_NO_DIRECTION, this::checkForDirection);
    }

    private void checkForName(ProcedureParameter parameter, ValidationResult validationResult) {
        // Must have name
        if (Util.isEmptyString(parameter.getName())) {
            addFailure(validationResult, parameter, "Unnamed ProcedureParameter");
        }
    }

    private void checkForType(ProcedureParameter parameter, ValidationResult validationResult) {
        // all attributes must have type
        if (parameter.getType() == TypesMapping.NOT_DEFINED) {
            addFailure(validationResult, parameter, "ProcedureParameter '%s' has no type", parameter.getName());
        }
    }

    private void checkForLength(ProcedureParameter parameter, ValidationResult validationResult) {
        // VARCHAR and CHAR attributes must have max length
        if (parameter.getMaxLength() < 0
                && (parameter.getType() == Types.VARCHAR
                    || parameter.getType() == Types.NVARCHAR
                    || parameter.getType() == Types.CHAR
                    || parameter.getType() == Types.NCHAR)) {

            addFailure(validationResult, parameter, "Character ProcedureParameter '%s' doesn't have max length",
                    parameter.getName());
        }
    }

    private void checkForDirection(ProcedureParameter parameter, ValidationResult validationResult) {
        // all attributes must have type
        if (parameter.getDirection() <= 0) {
            addFailure(validationResult, parameter, "ProcedureParameter '%s' has no direction", parameter.getName());
        }
    }
}
