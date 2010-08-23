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

import java.util.List;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

class ProcedureValidator extends ConfigurationNodeValidator {

    void validate(Procedure procedure, ValidationResult validationResult) {

        validateName(procedure, validationResult);

        // check that return value is present
        if (procedure.isReturningValue()) {
            List<ProcedureParameter> parameters = procedure.getCallParameters();
            if (parameters.isEmpty()) {
                addFailure(
                        validationResult,
                        procedure,
                        "Procedure '%s' returns a value, but has no parameters",
                        procedure.getName());
            }
        }
    }

    void validateName(Procedure procedure, ValidationResult validationResult) {
        String name = procedure.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, procedure, "Unnamed Procedure");
            return;
        }

        DataMap map = procedure.getDataMap();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (final Procedure otherProcedure : map.getProcedures()) {
            if (otherProcedure == procedure) {
                continue;
            }

            if (name.equals(otherProcedure.getName())) {
                addFailure(
                        validationResult,
                        procedure,
                        "Duplicate Procedure name: %s",
                        procedure.getName());
                break;
            }
        }
    }
}
