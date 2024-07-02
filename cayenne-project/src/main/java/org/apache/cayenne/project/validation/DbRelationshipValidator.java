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

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

class DbRelationshipValidator extends ConfigurationNodeValidator<DbRelationship> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public DbRelationshipValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(DbRelationship node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.DB_RELATIONSHIP_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.DB_RELATIONSHIP_NAME_DUPLICATE, this::checkForNameDuplicates)
                .performIfEnabled(Inspection.DB_RELATIONSHIP_INVALID_NAME, this::validateName)
                .performIfEnabled(Inspection.DB_RELATIONSHIP_PATH_DUPLICATE, this::checkForPathDuplicates)
                .performIfEnabled(Inspection.DB_RELATIONSHIP_NO_TARGET, this::checkForTarget)
                .performIfEnabled(Inspection.DB_RELATIONSHIP_TARGET_NOT_PK, this::checkForTargetPK)
                .performIfEnabled(Inspection.DB_RELATIONSHIP_NO_JOINS, this::checkForJoins)
                .performIfEnabled(Inspection.DB_RELATIONSHIP_INVALID_JOIN, this::validateJoins)
                .performIfEnabled(Inspection.DB_RELATIONSHIP_BOTH_TO_MANY, this::validateReverse)
                .performIfEnabled(Inspection.DB_RELATIONSHIP_DIFFERENT_TYPES, this::checkForSameTypes)
                .performIfEnabled(Inspection.DB_RELATIONSHIP_GENERATED_WITH_DEPENDENT_PK,
                        this::checkOnGeneratedStrategyConflict);
    }

    private void checkForName(DbRelationship relationship, ValidationResult validationResult) {
        if (Util.isEmptyString(relationship.getName())) {
            addFailure(validationResult, relationship, "Unnamed DbRelationship");
        }
    }

    private void checkForNameDuplicates(DbRelationship relationship, ValidationResult validationResult) {
        if (relationship.getSourceEntity().getAttribute(relationship.getName()) != null) {
            // check if there are attributes having the same name
            addFailure(validationResult, relationship,
                    "Name of DbRelationship '%s' conflicts with the name of one of DbAttributes in the same entity",
                    toString(relationship));
        }
    }

    private void validateName(DbRelationship relationship, ValidationResult validationResult) {
        NameValidationHelper helper = NameValidationHelper.getInstance();
        String invalidChars = helper.invalidCharsInDbPathComponent(relationship.getName());
        if (invalidChars != null) {
            addFailure(validationResult, relationship, "Name of DbRelationship '%s' contains invalid characters: %s",
                    toString(relationship), invalidChars);
        }
    }

    /**
     * Per CAY-1813, make sure two (or more) DbRelationships do not map to the
     * same database path.
     */
    private void checkForPathDuplicates(DbRelationship relationship, ValidationResult validationResult) {
        if (relationship == null || relationship.getName() == null || relationship.getTargetEntityName() == null) {
            return;
        }
        String dbRelationshipPath = relationship.getTargetEntityName() + "." + getJoins(relationship);
        DbEntity entity = relationship.getSourceEntity();

        for (DbRelationship otherRelationship : entity.getRelationships()) {
            if (relationship == otherRelationship) {
                continue;
            }
            String otherDbRelationshipPath = otherRelationship.getTargetEntityName() + "." + getJoins(otherRelationship);
            if (dbRelationshipPath.equals(otherDbRelationshipPath)) {
                addFailure(validationResult, relationship,
                        "DbEntity '%s' contains a duplicate DbRelationship mapping ('%s' -> '%s')",
                        entity.getName(), relationship.getName(), dbRelationshipPath);
                return;
            }
        }
    }

    private void checkForTarget(DbRelationship relationship, ValidationResult validationResult) {
        if (relationship.getTargetEntity() == null) {
            addFailure(validationResult, relationship, "DbRelationship '%s' has no target entity",
                    toString(relationship));
        }
    }

    private void checkForTargetPK(DbRelationship relationship, ValidationResult validationResult) {
        if (!relationship.isToPK()) {
            DbRelationship reverseRelationship = relationship.getReverseRelationship();
            if (reverseRelationship != null && !reverseRelationship.isToPK()) {
                addFailure(validationResult, relationship,
                        "DbRelationship '%s' has join not to PK. Cayenne doesn't allow this type of relationship",
                        toString(relationship));
            }
        }
    }

    private void checkForJoins(DbRelationship relationship, ValidationResult validationResult) {
        if (relationship.getJoins().isEmpty()) {
            addFailure(validationResult, relationship, "DbRelationship '%s' has no joins", toString(relationship));
        }
    }

    private void validateJoins(DbRelationship relationship, ValidationResult validationResult) {
        for (DbJoin join : relationship.getJoins()) {
            if (join.getSource() == null && join.getTarget() == null) {
                addFailure(validationResult, relationship,
                        "DbRelationship '%s' has a join with no source and target attributes selected",
                        toString(relationship));
            } else if (join.getSource() == null) {
                addFailure(validationResult, relationship,
                        "DbRelationship '%s' has a join with no source attribute selected",
                        toString(relationship));
            } else if (join.getTarget() == null) {
                addFailure(validationResult, relationship,
                        "DbRelationship '%s' has a join with no target attribute selected",
                        toString(relationship));
            }
        }
    }

    private void validateReverse(DbRelationship relationship, ValidationResult validationResult) {
        if (relationship.getReverseRelationship() != null
                && relationship.isToMany()
                && relationship.getReverseRelationship().isToMany()) {
            addFailure(validationResult, relationship, "Relationship '%s' and reverse '%s' are both toMany",
                    relationship.getName(), relationship.getReverseRelationship().getName());
        }
    }

    private void checkForSameTypes(DbRelationship relationship, ValidationResult validationResult) {
        for (DbJoin join : relationship.getJoins()) {
            if (join.getSource() != null && join.getTarget() != null
                    && join.getSource().getType() != join.getTarget().getType()) {
                addFailure(validationResult, relationship,
                        "Attributes '%s' and '%s' have different types in a relationship '%s'",
                        join.getSourceName(), join.getTargetName(), relationship.getName());
            }
        }
    }

    private void checkOnGeneratedStrategyConflict(DbRelationship relationship, ValidationResult validationResult) {
        if (!relationship.isToDependentPK()) {
            return;
        }
        Collection<DbAttribute> attributes = relationship.getTargetEntity().getGeneratedAttributes();
        for (DbAttribute attribute : attributes) {
            if (attribute.isGenerated()) {
                addFailure(validationResult, relationship,
                        "'To Dep Pk' incompatible with Database-Generated on '%s' relationship",
                        toString(relationship));
            }
        }
    }

    private String getJoins(DbRelationship relationship) {
        List<String> joins = new ArrayList<>();
        for (DbJoin join : relationship.getJoins()) {
            joins.add("[source=" + join.getSourceName() + ",target=" + join.getTargetName() + "]");
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
