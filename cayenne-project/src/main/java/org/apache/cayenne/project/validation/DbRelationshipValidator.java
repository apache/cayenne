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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

class DbRelationshipValidator extends ConfigurationNodeValidator {

    void validate(DbRelationship relationship, ValidationResult validationResult) {

        if (relationship.getTargetEntity() == null) {
            addFailure(
                    validationResult,
                    relationship,
                    "DbRelationship '%s' has no target entity",
                    toString(relationship));
        }
        else if (relationship.getJoins().isEmpty()) {
            addFailure(
                    validationResult,
                    relationship,
                    "DbRelationship '%s' has no joins",
                    toString(relationship));
        }
        else {
            // validate joins
            for (DbJoin join : relationship.getJoins()) {
                if (join.getSource() == null && join.getTarget() == null) {
                    addFailure(
                            validationResult,
                            relationship,
                            "DbRelationship '%s' has a join with no source and target attributes selected",
                            toString(relationship));
                }
                else if (join.getSource() == null) {
                    addFailure(
                            validationResult,
                            relationship,
                            "DbRelationship '%s' has a join with no source attribute selected",
                            toString(relationship));
                }
                else if (join.getTarget() == null) {
                    addFailure(
                            validationResult,
                            relationship,
                            "DbRelationship '%s' has a join with no target attribute selected",
                            toString(relationship));
                }
            }
        }

        if (Util.isEmptyString(relationship.getName())) {
            addFailure(validationResult, relationship, "Unnamed DbRelationship");
        }
        // check if there are attributes having the same name
        else if (relationship.getSourceEntity().getAttribute(relationship.getName()) != null) {
            addFailure(
                    validationResult,
                    relationship,
                    "Name of DbRelationship '%s' conflicts with the name of one of DbAttributes in the same entity",
                    toString(relationship));
        }
        else {
            NameValidationHelper helper = NameValidationHelper.getInstance();
            String invalidChars = helper.invalidCharsInDbPathComponent(relationship
                    .getName());

            if (invalidChars != null) {
                addFailure(
                        validationResult,
                        relationship,
                        "Name of DbRelationship '%s' contains invalid characters: %s",
                        toString(relationship),
                        invalidChars);
            }
        }

        checkForDuplicates(relationship, validationResult);
    }

    /**
     * Per CAY-1813, make sure two (or more) DbRelationships do not map to the
     * same database path.
     */
    private void checkForDuplicates(DbRelationship relationship, ValidationResult validationResult) {
        if (relationship                       != null &&
            relationship.getName()             != null &&
            relationship.getTargetEntityName() != null) {

            String dbRelationshipPath =
                       relationship.getTargetEntityName() +
                       "." +
                       getJoins(relationship);

            if (dbRelationshipPath != null) {
                DbEntity entity = (DbEntity) relationship.getSourceEntity();

                for (DbRelationship comparisonRelationship : entity.getRelationships()) {
                    if (relationship != comparisonRelationship) {
                        String comparisonDbRelationshipPath =
                                   comparisonRelationship.getTargetEntityName() +
                                   "." +
                                   getJoins(comparisonRelationship);

                        if (dbRelationshipPath.equals(comparisonDbRelationshipPath)) {
                            addFailure(validationResult,
                                       relationship,
                                       "DbEntity '%s' contains a duplicate DbRelationship mapping ('%s' -> '%s')",
                                       entity.getName(),
                                       relationship.getName(),
                                       dbRelationshipPath);
                            return; // Duplicate found, stop.
                        }
                    }
                }
            }
        }
    }

    private String getJoins(DbRelationship relationship) {
        List<String> joins = new ArrayList<String>();

        for (DbJoin join : relationship.getJoins()) {
            StringBuilder builder = new StringBuilder();

            builder.append("[");
            builder.append("source=").append(join.getSourceName());
            builder.append(",");
            builder.append("target=").append(join.getTargetName());
            builder.append("]");

            joins.add(builder.toString());
        }

        Collections.sort(joins);

        return Util.join(joins, ",");
    }

    private String toString(DbRelationship relationship) {
        if (relationship.getSourceEntity() == null) {
            return "[null source entity]." + relationship.getName();
        }

        return relationship.getSourceEntity().getName() + "." + relationship.getName();
    }

}
