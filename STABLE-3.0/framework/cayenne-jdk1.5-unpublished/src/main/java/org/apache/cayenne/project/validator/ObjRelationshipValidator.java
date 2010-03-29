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

import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

/**
 */
public class ObjRelationshipValidator extends TreeNodeValidator {

    /**
     * Constructor for ObjRelationshipValidator.
     */
    public ObjRelationshipValidator() {
        super();
    }

    @Override
    public void validateObject(ProjectPath path, Validator validator) {
        ObjRelationship rel = (ObjRelationship) path.getObject();

        // skip validation of inherited relationships
        if (path.getObjectParent() != null
                && path.getObjectParent() != rel.getSourceEntity()) {
            return;
        }

        if (Util.isEmptyString(rel.getName())) {
            validator.registerError("Unnamed ObjRelationship.", path);
        }
        // check if there are attributes having the same name
        else if (rel.getSourceEntity().getAttribute(rel.getName()) != null) {
            validator.registerWarning("ObjRelationship "
                    + objRelationshipIdentifier(rel)
                    + " has the same name as one of ObjAttributes", path);
        }
        else {
            MappingNamesHelper helper = MappingNamesHelper.getInstance();
            String invalidChars = helper.invalidCharsInObjPathComponent(rel.getName());

            if (invalidChars != null) {
                validator.registerWarning("ObjRelationship "
                        + objRelationshipIdentifier(rel)
                        + " name contains invalid characters: "
                        + invalidChars, path);
            }
            else if (helper.invalidDataObjectProperty(rel.getName())) {
                validator.registerWarning("ObjRelationship "
                        + objRelationshipIdentifier(rel)
                        + " name is invalid.", path);
            }
        }

        if (rel.getTargetEntity() == null) {
            validator.registerWarning("ObjRelationship "
                    + objRelationshipIdentifier(rel)
                    + " has no target entity.", path);
        }
        else {
            // check for missing DbRelationship mappings
            List<DbRelationship> dbRels = rel.getDbRelationships();
            if (dbRels.size() == 0) {
                validator.registerWarning("ObjRelationship "
                        + objRelationshipIdentifier(rel)
                        + " has no DbRelationship mapping.", path);
            }
            else {
                DbEntity expectedSrc = ((ObjEntity) rel.getSourceEntity()).getDbEntity();
                DbEntity expectedTarget = ((ObjEntity) rel.getTargetEntity())
                        .getDbEntity();

                if ((dbRels.get(0)).getSourceEntity() != expectedSrc
                        || (dbRels.get(dbRels.size() - 1))
                                .getTargetEntity() != expectedTarget) {
                    validator.registerWarning("ObjRelationship "
                            + objRelationshipIdentifier(rel)
                            + " has incomplete DbRelationship mapping.", path);
                }
            }
        }

        // Disallow a Nullify delete rule where the relationship is toMany and the
        // foreign key attributes are mandatory.
        if (rel.isToMany()
                && !rel.isFlattened()
                && (rel.getDeleteRule() == DeleteRule.NULLIFY)) {
            ObjRelationship inverse = rel.getReverseRelationship();
            if (inverse != null) {
                DbRelationship firstRel = inverse
                        .getDbRelationships()
                        .get(0);
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
                    validator
                            .registerWarning(
                                    "ObjRelationship "
                                            + objRelationshipIdentifier(rel)
                                            + " has a Nullify delete rule and a mandatory reverse relationship ",
                                    path);
                }
            }
        }
    }

    public String objRelationshipIdentifier(ObjRelationship rel) {
        if (null == rel.getSourceEntity()) {
            return "<[null source entity]." + rel.getName() + ">";
        }
        return "<" + rel.getSourceEntity().getName() + "." + rel.getName() + ">";
    }
}
