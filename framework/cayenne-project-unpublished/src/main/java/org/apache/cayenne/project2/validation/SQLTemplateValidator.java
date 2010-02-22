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
package org.apache.cayenne.project2.validation;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.Util;

class SQLTemplateValidator {

    void validate(Object object, ValidationVisitor validationVisitor) {
        SQLTemplate query = (SQLTemplate) object;

        validateName(query, validationVisitor);
        validateRoot(query, validationVisitor);
        validateDefaultSQL(query, validationVisitor);
    }

    void validateDefaultSQL(SQLTemplate query, ValidationVisitor validationVisitor) {

        if (Util.isEmptyString(query.getDefaultTemplate())) {
            // see if there is at least one adapter-specific template...

            for (final String key : query.getTemplateKeys()) {
                if (!Util.isEmptyString(query.getCustomTemplate(key))) {
                    return;
                }
            }

            validationVisitor.registerWarning("Query has no default SQL template", query);
        }
    }

    void validateRoot(SQLTemplate query, ValidationVisitor validationVisitor) {
        DataMap map = query.getDataMap();
        if (query.getRoot() == null && map != null) {
            validationVisitor.registerWarning("Query has no root", query);
        }
    }

    void validateName(SQLTemplate query, ValidationVisitor validationVisitor) {
        String name = query.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validationVisitor.registerError("Unnamed Query.", query);
            return;
        }

        DataMap map = query.getDataMap();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (final Query otherQuery : map.getQueries()) {
            if (otherQuery == query) {
                continue;
            }

            if (name.equals(otherQuery.getName())) {
                validationVisitor.registerError(
                        "Duplicate Query name: " + name + ".",
                        query);
                break;
            }
        }
    }
}
