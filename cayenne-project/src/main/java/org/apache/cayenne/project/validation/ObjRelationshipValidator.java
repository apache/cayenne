/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.project.validation;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

public class ObjRelationshipValidator extends ConfigurationNodeValidator<ObjRelationship> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public ObjRelationshipValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(ObjRelationship node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.OBJ_RELATIONSHIP_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.OBJ_RELATIONSHIP_NAME_DUPLICATE, this::checkForNameDuplicates)
                .performIfEnabled(Inspection.OBJ_RELATIONSHIP_INVALID_NAME, this::validateName)
                .performIfEnabled(Inspection.OBJ_RELATIONSHIP_NO_TARGET, this::checkForTarget)
                .performIfEnabled(Inspection.OBJ_RELATIONSHIP_TARGET_NOT_PK, this::checkForPK)
                .performIfEnabled(Inspection.OBJ_RELATIONSHIP_INVALID_REVERSED, this::validateReverse)
                .performIfEnabled(Inspection.OBJ_RELATIONSHIP_SEMANTIC_DUPLICATE, this::checkForSemanticDuplicates)
                .performIfEnabled(Inspection.OBJ_RELATIONSHIP_INVALID_MAPPING, this::validateMapping)
                .performIfEnabled(Inspection.OBJ_RELATIONSHIP_NULLIFY_NOT_NULL, this::validateDeleteRule)
                .performIfEnabled(Inspection.OBJ_RELATIONSHIP_DUPLICATE_IN_ENTITY, this::checkForDuplicatesInEntity);
    }

    private void checkForName(ObjRelationship relationship, ValidationResult validationResult) {
        if (Util.isEmptyString(relationship.getName())) {
            addFailure(validationResult, relationship, "Unnamed ObjRelationship");
        }
    }

    private void checkForNameDuplicates(ObjRelationship relationship, ValidationResult validationResult) {
        if (relationship.getSourceEntity().getAttribute(relationship.getName()) != null) {
            // check if there are attributes having the same name
            addFailure(validationResult, relationship,
                    "ObjRelationship '%s' has the same name as one of ObjAttributes", toString(relationship));
        }
    }

    private void validateName(ObjRelationship relationship, ValidationResult validationResult) {
        NameValidationHelper helper = NameValidationHelper.getInstance();
        String invalidChars = helper.invalidCharsInObjPathComponent(relationship.getName());

        if (invalidChars != null) {
            addFailure(validationResult, relationship, "ObjRelationship name '%s' contains invalid characters: %s",
                    toString(relationship), invalidChars);
        } else if (helper.invalidPersistentObjectProperty(relationship.getName())) {
            addFailure(validationResult, relationship, "ObjRelationship name '%s' is a reserved word",
                    toString(relationship));
        }
    }

    private void checkForTarget(ObjRelationship relationship, ValidationResult validationResult) {
        if (relationship.getTargetEntity() == null) {
            addFailure(validationResult, relationship, "ObjRelationship '%s' has no target entity",
                    toString(relationship));
        }
    }

    private void checkForPK(ObjRelationship relationship, ValidationResult validationResult) {
        if (relationship.getDbRelationships().isEmpty() || relationship.isToPK()) {
            return;
        }
        ObjRelationship reverseRelationship = relationship.getReverseRelationship();
        if (reverseRelationship != null
                && !relationship.getDbRelationships().isEmpty()
                && !reverseRelationship.isToPK()) {
            addFailure(validationResult, relationship,
                    "ObjRelationship '%s' has join not to PK. This is not fully supported by Cayenne.",
                    toString(relationship));
        }
    }

    private void validateReverse(ObjRelationship relationship, ValidationResult validationResult) {

        // check for invalid relationships in inherited entities
        if (relationship.getReverseRelationship() == null) {
            return;
        }
        ObjRelationship revRel = relationship.getReverseRelationship();
        if (relationship.getSourceEntity() != revRel.getTargetEntity()
                || relationship.getTargetEntity() != revRel.getSourceEntity()) {
            addFailure(validationResult, revRel,
                    "Usage of super entity's relationships '%s' as reversed relationships for sub entity is discouraged",
                    toString(revRel));
        }
    }

    private void checkForSemanticDuplicates(ObjRelationship relationship, ValidationResult validationResult) {

        // check for relationships with same source and target entities
        ObjEntity entity = relationship.getSourceEntity();
        for (ObjRelationship otherRelationship : entity.getRelationships()) {
            if (relationship.getDbRelationshipPath() != null
                    && relationship != otherRelationship
                    && relationship.getTargetEntity() == otherRelationship.getTargetEntity()
                    && relationship.getSourceEntity() == otherRelationship.getSourceEntity()
                    && relationship.getDbRelationshipPath().equals(otherRelationship.getDbRelationshipPath())) {
                addFailure(validationResult, relationship, "ObjectRelationship '%s' duplicates relationship '%s'",
                        toString(relationship), toString(otherRelationship));
            }
        }
    }

    private void validateMapping(ObjRelationship relationship, ValidationResult validationResult) {

        // check for missing DbRelationship mappings
        List<DbRelationship> dbRels = relationship.getDbRelationships();
        if (dbRels.isEmpty()) {
            addFailure(validationResult, relationship, "ObjRelationship '%s' has no DbRelationship mapping",
                    toString(relationship));
        } else {
            DbEntity expectedSrc = relationship.getSourceEntity().getDbEntity();
            DbEntity expectedTarget = relationship.getTargetEntity().getDbEntity();
            if ((dbRels.get(0)).getSourceEntity() != expectedSrc
                    || (dbRels.get(dbRels.size() - 1)).getTargetEntity() != expectedTarget) {
                addFailure(validationResult, relationship,
                        "ObjRelationship '%s' has incomplete DbRelationship mapping",
                        toString(relationship));
            }
        }
    }

    private void validateDeleteRule(ObjRelationship relationship, ValidationResult validationResult) {
        // Disallow a Nullify delete rule where the relationship is toMany and the
        // foreign key attributes are mandatory.
        if (!relationship.isToMany()
                || relationship.isFlattened()
                || relationship.getDeleteRule() != DeleteRule.NULLIFY) {
            return;
        }
        ObjRelationship inverse = relationship.getReverseRelationship();
        if (inverse == null) {
            return;
        }
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
            addFailure(validationResult, relationship,
                    "ObjRelationship '%s' has a Nullify delete rule and a mandatory reverse relationship",
                    toString(relationship));
        }
    }

    /**
     * Per CAY-1813, make sure two (or more) ObjRelationships do not map to the
     * same database path.
     */
    private void checkForDuplicatesInEntity(ObjRelationship relationship, ValidationResult validationResult) {
        if (relationship == null || relationship.getName() == null || relationship.getTargetEntityName() == null) {
            return;
        }

        String dbRelationshipPath = relationship.getTargetEntityName() + "." + relationship.getDbRelationshipPath();
        ObjEntity entity = relationship.getSourceEntity();
        for (ObjRelationship otherRelationship : entity.getRelationships()) {
            if (relationship == otherRelationship) {
                continue;
            }
            String otherDbRelationshipPath = otherRelationship.getTargetEntityName()
                    + "." + otherRelationship.getDbRelationshipPath();

            if (dbRelationshipPath.equals(otherDbRelationshipPath)) {
                addFailure(validationResult, relationship,
                        "ObjEntity '%s' contains a duplicate ObjRelationship mapping ('%s' -> '%s')",
                        entity.getName(), relationship.getName(), dbRelationshipPath);
                return; // Duplicate found, stop.
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
