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

import java.sql.Types;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

class ProcedureParameterValidator extends ConfigurationNodeValidator {

    void validate(ProcedureParameter parameter, ValidationResult validationResult) {

        // Must have name
        if (Util.isEmptyString(parameter.getName())) {
            addFailure(validationResult, parameter, "Unnamed ProcedureParameter");
        }

        // all attributes must have type
        if (parameter.getType() == TypesMapping.NOT_DEFINED) {
            addFailure(
                    validationResult,
                    parameter,
                    "ProcedureParameter '%s' has no type",
                    parameter.getName());
        }

        // VARCHAR and CHAR attributes must have max length
        if (parameter.getMaxLength() < 0
                && (parameter.getType() == Types.VARCHAR || parameter.getType() == Types.CHAR)) {

            addFailure(
                    validationResult,
                    parameter,
                    "Character ProcedureParameter '%s' doesn't have max length",
                    parameter.getName());
        }

        // all attributes must have type
        if (parameter.getDirection() <= 0) {
            addFailure(
                    validationResult,
                    parameter,
                    "ProcedureParameter '%s' has no direction",
                    parameter.getName());
        }
    }
}
