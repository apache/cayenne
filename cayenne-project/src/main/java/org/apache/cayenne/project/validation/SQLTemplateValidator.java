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
import org.apache.cayenne.query.QueryDescriptor;
import org.apache.cayenne.query.SQLTemplateDescriptor;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

import java.util.Map;

class SQLTemplateValidator extends ConfigurationNodeValidator {

    void validate(SQLTemplateDescriptor query, ValidationResult validationResult) {
        validateName(query, validationResult);
        validateRoot(query, validationResult);
        validateDefaultSQL(query, validationResult);
    }

    void validateDefaultSQL(SQLTemplateDescriptor query, ValidationResult validationResult) {

        if (Util.isEmptyString(query.getSql())) {
            // see if there is at least one adapter-specific template...

            for (Map.Entry<String, String> entry : query.getAdapterSql().entrySet()) {
                if (!Util.isEmptyString(entry.getValue())) {
                    return;
                }
            }

            addFailure(
                    validationResult,
                    query,
                    "SQLTemplate query '%s' has no default SQL template",
                    query.getName());
        }
    }

    void validateRoot(QueryDescriptor query, ValidationResult validationResult) {
        DataMap map = query.getDataMap();
        if (query.getRoot() == null && map != null) {
            addFailure(
                    validationResult,
                    query,
                    "SQLTemplate query '%s' has no root",
                    query.getName());
        }
    }

    void validateName(QueryDescriptor query, ValidationResult validationResult) {
        String name = query.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, query, "Unnamed SQLTemplate");
            return;
        }

        DataMap map = query.getDataMap();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (final QueryDescriptor otherQuery : map.getQueryDescriptors()) {
            if (otherQuery == query) {
                continue;
            }

            if (name.equals(otherQuery.getName())) {
                addFailure(validationResult, query, "Duplicate query name: %s", name);
                break;
            }
        }
    }
}
