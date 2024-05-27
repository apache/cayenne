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

import java.util.List;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

public class ProcedureValidator extends ConfigurationNodeValidator<Procedure> {

    /**
     * @param validationConfig the config defining the behavior of this validator.
     * @since 5.0
     */
    public ProcedureValidator(ValidationConfig validationConfig) {
        super(validationConfig);
    }

    @Override
    public void validate(Procedure node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.PROCEDURE_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.PROCEDURE_NAME_DUPLICATE, this::checkForDuplicateName)
                .performIfEnabled(Inspection.PROCEDURE_NO_PARAMS, this::checkForParams);
    }

    private void checkForName(Procedure procedure, ValidationResult validationResult) {

        // Must have name
        String name = procedure.getName();
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, procedure, "Unnamed Procedure");
        }
    }

    private void checkForDuplicateName(Procedure procedure, ValidationResult validationResult) {
        String name = procedure.getName();
        DataMap map = procedure.getDataMap();
        if (map == null || Util.isEmptyString(name)) {
            return;
        }

        // check for duplicate names in the parent context
        for (final Procedure otherProcedure : map.getProcedures()) {
            if (otherProcedure == procedure) {
                continue;
            }

            if (name.equals(otherProcedure.getName())) {
                addFailure(validationResult, procedure, "Duplicate Procedure name: %s", procedure.getName());
                return;
            }
        }
    }

    private void checkForParams(Procedure procedure, ValidationResult validationResult) {
        // check that return value is present
        if (!procedure.isReturningValue()) {
            return;
        }
        List<ProcedureParameter> parameters = procedure.getCallParameters();
        if (parameters.isEmpty()) {
            addFailure(validationResult, procedure, "Procedure '%s' returns a value, but has no parameters",
                    procedure.getName());
        }
    }
}
