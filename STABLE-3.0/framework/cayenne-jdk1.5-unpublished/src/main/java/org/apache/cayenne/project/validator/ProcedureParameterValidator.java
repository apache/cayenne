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

package org.apache.cayenne.project.validator;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.ProcedureParameter;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

/**
 * Validator for stored procedure parameters.
 * 
 */
public class ProcedureParameterValidator extends TreeNodeValidator {

    @Override
    public void validateObject(ProjectPath treeNodePath, Validator validator) {
        ProcedureParameter parameter = (ProcedureParameter) treeNodePath.getObject();

        // Must have name
        if (Util.isEmptyString(parameter.getName())) {
            validator.registerError("Unnamed ProcedureParameter.", treeNodePath);
        }

        // all attributes must have type
        if (parameter.getType() == TypesMapping.NOT_DEFINED) {
            validator.registerWarning("ProcedureParameter has no type.", treeNodePath);
        }

        // VARCHAR and CHAR attributes must have max length
        if (parameter.getMaxLength() < 0
            && (parameter.getType() == java.sql.Types.VARCHAR
                || parameter.getType() == java.sql.Types.CHAR)) {

            validator.registerWarning(
                "Character procedure parameter doesn't have max length.",
                treeNodePath);
        }

        // all attributes must have type
        if (parameter.getDirection() <= 0) {
            validator.registerWarning(
                "ProcedureParameter has no direction.",
                treeNodePath);
        }

    }
}
