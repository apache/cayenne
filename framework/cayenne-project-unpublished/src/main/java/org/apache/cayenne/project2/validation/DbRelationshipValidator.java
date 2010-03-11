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

import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.util.Util;

class DbRelationshipValidator {

    void validate(Object object, ValidationVisitor validationVisitor) {
        DbRelationship rel = (DbRelationship) object;

        if (rel.getTargetEntity() == null) {
            validationVisitor.registerWarning("DbRelationship "
                    + dbRelationshipIdentifier(rel)
                    + " has no target entity.", object);
        }
        else if (rel.getJoins().size() == 0) {
            validationVisitor.registerWarning("DbRelationship "
                    + dbRelationshipIdentifier(rel)
                    + " has no joins.", object);
        }
        else {
            // validate joins
            for (final DbJoin join : rel.getJoins()) {
                if (join.getSource() == null && join.getTarget() == null) {
                    validationVisitor
                            .registerWarning(
                                    "DbRelationship "
                                            + dbRelationshipIdentifier(rel)
                                            + " join has no source and target attributes selected.",
                                    object);
                }
                else if (join.getSource() == null) {
                    validationVisitor.registerWarning("DbRelationship "
                            + dbRelationshipIdentifier(rel)
                            + " join has no source attribute selected.", object);
                }
                else if (join.getTarget() == null) {
                    validationVisitor.registerWarning("DbRelationship "
                            + dbRelationshipIdentifier(rel)
                            + " join has no target attribute selected.", object);
                }
            }
        }

        if (Util.isEmptyString(rel.getName())) {
            validationVisitor.registerError("Unnamed DbRelationship.", object);
        }
        // check if there are attributes having the same name
        else if (rel.getSourceEntity().getAttribute(rel.getName()) != null) {
            validationVisitor.registerError("DbRelationship "
                    + dbRelationshipIdentifier(rel)
                    + " has the same name as one of DbAttributes", object);
        }
        else {
            NameValidationHelper helper = NameValidationHelper.getInstance();
            String invalidChars = helper.invalidCharsInDbPathComponent(rel.getName());

            if (invalidChars != null) {
                validationVisitor.registerWarning("DbRelationship "
                        + dbRelationshipIdentifier(rel)
                        + " name contains invalid characters: "
                        + invalidChars, object);
            }
        }
    }

    String dbRelationshipIdentifier(DbRelationship rel) {
        if (null == rel.getSourceEntity()) {
            return "<[null source entity]." + rel.getName() + ">";
        }
        return "<" + rel.getSourceEntity().getName() + "." + rel.getName() + ">";
    }

}
