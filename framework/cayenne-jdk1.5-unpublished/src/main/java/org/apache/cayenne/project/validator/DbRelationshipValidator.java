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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

/**
 */
public class DbRelationshipValidator extends TreeNodeValidator {

    /**
     * Constructor for DbRelationshipValidator.
     */
    public DbRelationshipValidator() {
        super();
    }

    @Override
    public void validateObject(ProjectPath path, Validator validator) {
        DbRelationship rel = (DbRelationship) path.getObject();
        if (rel.getTargetEntity() == null) {
            validator.registerWarning("DbRelationship " + dbRelationshipIdentifier(rel) + " has no target entity.", path);
        }
        else if (rel.getJoins().size() == 0) {
            validator.registerWarning("DbRelationship " + dbRelationshipIdentifier(rel) + " has no joins.", path);
        }
        else {
            // validate joins
            for (final DbJoin join : rel.getJoins()) {
                if (join.getSource() == null && join.getTarget() == null) {
                    validator
                            .registerWarning(
                                    "DbRelationship " + dbRelationshipIdentifier(rel) + " join has no source and target attributes selected.",
                                    path);
                }
                else if (join.getSource() == null) {
                    validator.registerWarning(
                            "DbRelationship " + dbRelationshipIdentifier(rel) + " join has no source attribute selected.",
                            path);
                }
                else if (join.getTarget() == null) {
                    validator.registerWarning(
                            "DbRelationship " + dbRelationshipIdentifier(rel) + " join has no target attribute selected.",
                            path);
                }
            }
        }

        if (Util.isEmptyString(rel.getName())) {
            validator.registerError("Unnamed DbRelationship.", path);
        }
        // check if there are attributes having the same name
        else if (rel.getSourceEntity().getAttribute(rel.getName()) != null) {
            validator.registerError(
                    "DbRelationship " + dbRelationshipIdentifier(rel) + " has the same name as one of DbAttributes",
                    path);
        }
        else {
            MappingNamesHelper helper = MappingNamesHelper.getInstance();
            String invalidChars = helper.invalidCharsInDbPathComponent(rel.getName());

            if (invalidChars != null) {
                validator
                        .registerWarning(
                                "DbRelationship " + dbRelationshipIdentifier(rel) + " name contains invalid characters: "
                                        + invalidChars,
                                path);
            }
        }

        checkForDuplicates(path, validator, rel);
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

    /**
     * Per CAY-1813, make sure two (or more) DbRelationships do not map to the
     * same database path.
     */
    private void checkForDuplicates(ProjectPath    path,
                                    Validator      validator,
                                    DbRelationship relationship) {
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
                            validator.registerWarning
                                ("DbEntity " +
                                 entity.getName() +
                                 " contains a duplicate DbRelationship mapping (" +
                                 relationship.getName() +
                                 " -> " +
                                 dbRelationshipPath +
                                 ")", path);
                            return; // Duplicate found, stop.
                        }
                    }
                }
            }
        }
    }

    public String dbRelationshipIdentifier(DbRelationship rel)
    {
        if (null == rel.getSourceEntity())
        {
            return "<[null source entity]." + rel.getName() + ">";
        }
        return "<" + rel.getSourceEntity().getName() + "." + rel.getName() + ">";
    }
}
