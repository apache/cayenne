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

import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

class ObjRelationshipValidator extends ConfigurationNodeValidator {

    void validate(ObjRelationship relationship, ValidationResult validationResult) {

        if (Util.isEmptyString(relationship.getName())) {
            addFailure(validationResult, relationship, "Unnamed ObjRelationship");
        }

        // check if there are attributes having the same name
        else if (relationship.getSourceEntity().getAttribute(relationship.getName()) != null) {
            addFailure(
                    validationResult,
                    relationship,
                    "ObjRelationship '%s' has the same name as one of ObjAttributes",
                    toString(relationship));
        }
        else {
            NameValidationHelper helper = NameValidationHelper.getInstance();
            String invalidChars = helper.invalidCharsInObjPathComponent(relationship
                    .getName());

            if (invalidChars != null) {
                addFailure(
                        validationResult,
                        relationship,
                        "ObjRelationship name '%s' contains invalid characters: %s",
                        toString(relationship),
                        invalidChars);
            }
            else if (helper.invalidDataObjectProperty(relationship.getName())) {
                addFailure(
                        validationResult,
                        relationship,
                        "ObjRelationship name '%s' is a reserved word",
                        toString(relationship));
            }
        }

        if (relationship.getTargetEntity() == null) {
            addFailure(
                    validationResult,
                    relationship,
                    "ObjRelationship '%s' has no target entity",
                    toString(relationship));
        }
        else {

            // check for missing DbRelationship mappings
            List<DbRelationship> dbRels = relationship.getDbRelationships();
            if (dbRels.isEmpty()) {
                addFailure(
                        validationResult,
                        relationship,
                        "ObjRelationship '%s' has no DbRelationship mapping",
                        toString(relationship));
            }
            else {
                DbEntity expectedSrc = ((ObjEntity) relationship.getSourceEntity())
                        .getDbEntity();
                DbEntity expectedTarget = ((ObjEntity) relationship.getTargetEntity())
                        .getDbEntity();

                if ((dbRels.get(0)).getSourceEntity() != expectedSrc
                        || (dbRels.get(dbRels.size() - 1)).getTargetEntity() != expectedTarget) {

                    addFailure(
                            validationResult,
                            relationship,
                            "ObjRelationship '%s' has incomplete DbRelationship mapping",
                            toString(relationship));
                }
            }
        }

        // Disallow a Nullify delete rule where the relationship is toMany and the
        // foreign key attributes are mandatory.
        if (relationship.isToMany()
                && !relationship.isFlattened()
                && (relationship.getDeleteRule() == DeleteRule.NULLIFY)) {
            ObjRelationship inverse = relationship.getReverseRelationship();
            if (inverse != null) {
                DbRelationship firstRel = inverse.getDbRelationships().get(0);
                Iterator<DbJoin> attributePairIterator = firstRel.getJoins().iterator();
                // by default, the relation will be check for mandatory.
                boolean check = true;
                while (attributePairIterator.hasNext()) {
                    DbJoin pair = attributePairIterator.next();
                    if (!pair.getSource().isMandatory()) {
                        // a field of the fk can be nullable, cancel the check.
                        check = false;
                        break;
                    }
                }

                if (check) {
                    addFailure(
                            validationResult,
                            relationship,
                            "ObjRelationship '%s' has a Nullify delete rule and a mandatory reverse relationship",
                            toString(relationship));
                }
            }
        }

        // check for relationships with same source and target entities
        ObjEntity entity = (ObjEntity) relationship.getSourceEntity();
        for (ObjRelationship rel : entity.getRelationships()) {
            if (relationship.getDbRelationshipPath() != null && relationship.getDbRelationshipPath().equals(rel.getDbRelationshipPath())) {
                if (relationship != rel &&
                        relationship.getTargetEntity() == rel.getTargetEntity() &&
                        relationship.getSourceEntity() == rel.getSourceEntity()) {
                    addFailure(
                            validationResult,
                            relationship,
                            "ObjectRelationship '%s' duplicates relationship '%s'",
                            toString(relationship),
                            toString(rel));
                }
            }
        }

        // check for invalid relationships in inherited entities
        if (relationship.getReverseRelationship() != null) {
            ObjRelationship revRel = relationship.getReverseRelationship();
            if (relationship.getSourceEntity() != revRel.getTargetEntity()
                    || relationship.getTargetEntity() != revRel.getSourceEntity()) {
                addFailure(
                        validationResult,
                        revRel,
                        "Usage of super entity's relationships '%s' as reversed relationships for sub entity is discouraged",
                        toString(revRel));
            }
        }

        checkForDuplicates(relationship, validationResult);
    }

    /**
     * Per CAY-1813, make sure two (or more) ObjRelationships do not map to the
     * same database path.
     */
    private void checkForDuplicates(ObjRelationship  relationship,
                                    ValidationResult validationResult) {
        if (relationship                       != null &&
            relationship.getName()             != null &&
            relationship.getTargetEntityName() != null) {

            String dbRelationshipPath =
                       relationship.getTargetEntityName() +
                       "." +
                       relationship.getDbRelationshipPath();

            if (dbRelationshipPath != null) {
                ObjEntity entity = (ObjEntity) relationship.getSourceEntity();

                for (ObjRelationship comparisonRelationship : entity.getRelationships()) {
                    if (relationship != comparisonRelationship) {
                        String comparisonDbRelationshipPath =
                                   comparisonRelationship.getTargetEntityName() +
                                   "." +
                                   comparisonRelationship.getDbRelationshipPath();

                        if (dbRelationshipPath.equals(comparisonDbRelationshipPath)) {
                            addFailure(validationResult,
                                       relationship,
                                       "ObjEntity '%s' contains a duplicate ObjRelationship mapping ('%s' -> '%s')",
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

    private String toString(ObjRelationship relationship) {
        if (relationship.getSourceEntity() == null) {
            return "[null source entity]." + relationship.getName();
        }

        return relationship.getSourceEntity().getName() + "." + relationship.getName();
    }

}
