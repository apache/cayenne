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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureQueryDescriptor;
import org.apache.cayenne.validation.ValidationResult;

class ProcedureQueryValidator extends BaseQueryValidator {

    void validate(ProcedureQueryDescriptor query, ValidationResult validationResult) {
        validateName(query, validationResult);
        validateRoot(query, validationResult);
        validateCacheGroup(query, validationResult);
    }

    void validateRoot(ProcedureQueryDescriptor query, ValidationResult validationResult) {

        DataMap map = query.getDataMap();
        Object root = query.getRoot();

        if (root == null && map != null) {
            addFailure(validationResult, query, "ProcedureQuery '%s' has no root", query
                    .getName());
        }

        // procedure query only supports procedure root
        if (root instanceof Procedure) {
            Procedure procedure = (Procedure) root;

            // procedure may have been deleted...
            if (map != null && map.getProcedure(procedure.getName()) != procedure) {
                addFailure(
                        validationResult,
                        query,
                        "ProcedureQuery '%s' has invalid Procedure root: %s",
                        query.getName(),
                        procedure.getName());
            }

            return;
        }

        if (root instanceof String) {
            if (map != null && map.getProcedure(root.toString()) == null) {
                addFailure(
                        validationResult,
                        query,
                        "ProcedureQuery '%s' has invalid Procedure root: %s",
                        query.getName(),
                        root);
            }
        }
    }
}
