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

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.Util;

public class ProcedureQueryValidator implements Validator {

    public void validate(Object object, ConfigurationValidationVisitor validator) {
        ProcedureQuery query = (ProcedureQuery) object;

        ProjectPath path = new ProjectPath(new Object[] {
                (DataChannelDescriptor) validator.getProject().getRootNode(),
                query.getDataMap(), query
        });

        validateName(query, path, validator);
        validateRoot(query, path, validator);
    }

    private void validateRoot(
            ProcedureQuery query,
            ProjectPath path,
            ConfigurationValidationVisitor validator) {
        DataMap map = path.firstInstanceOf(DataMap.class);
        Object root = query.getRoot();

        if (root == null && map != null) {
            validator.registerWarning("Query has no root", path);
        }

        // procedure query only supports procedure root
        if (root instanceof Procedure) {
            Procedure procedure = (Procedure) root;

            // procedure may have been deleted...
            if (map != null && map.getProcedure(procedure.getName()) != procedure) {
                validator.registerWarning("Invalid Procedure Root - "
                        + procedure.getName(), path);
            }

            return;
        }

        if (root instanceof String) {
            if (map != null && map.getProcedure(root.toString()) == null) {
                validator.registerWarning("Invalid Procedure Root - " + root, path);
            }
        }
    }

    private void validateName(
            ProcedureQuery query,
            ProjectPath path,
            ConfigurationValidationVisitor validator) {
        String name = query.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed Query.", path);
            return;
        }

        DataMap map = (DataMap) path.getObjectParent();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (final Query otherQuery : map.getQueries()) {
            if (otherQuery == query) {
                continue;
            }

            if (name.equals(otherQuery.getName())) {
                validator.registerError("Duplicate Query name: " + name + ".", path);
                break;
            }
        }
    }

}
