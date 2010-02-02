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
package org.apache.cayenne.project2.validate;

import java.util.List;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

public class ProcedureValidator implements Validator {

    public void validate(Object object, ConfigurationValidationVisitor validator) {
        Procedure procedure = (Procedure) object;

        ProjectPath path = new ProjectPath(new Object[] {
                (DataChannelValidator) validator.getProject().getRootNode(),
                procedure.getDataMap(), procedure
        });

        validateName(procedure, path, validator);

        // check that return value is present
        if (procedure.isReturningValue()) {
            List<ProcedureParameter> parameters = procedure.getCallParameters();
            if (parameters.size() == 0) {
                validator.registerWarning(
                        "Procedure returns a value, but has no parameters.",
                        path);
            }
        }
    }

    protected void validateName(
            Procedure procedure,
            ProjectPath path,
            ConfigurationValidationVisitor validator) {
        String name = procedure.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed Procedure.", path);
            return;
        }

        DataMap map = (DataMap) path.getObjectParent();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (final Procedure otherProcedure : map.getProcedures()) {
            if (otherProcedure == procedure) {
                continue;
            }

            if (name.equals(otherProcedure.getName())) {
                validator.registerError("Duplicate Procedure name: " + name + ".", path);
                break;
            }
        }
    }
}
