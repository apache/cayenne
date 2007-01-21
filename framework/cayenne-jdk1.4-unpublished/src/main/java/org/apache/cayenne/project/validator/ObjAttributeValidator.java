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

import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

/**
 * @author Andrus Adamchik
 */
public class ObjAttributeValidator extends TreeNodeValidator {

    /**
     * Constructor for ObjAttributeValidator.
     */
    public ObjAttributeValidator() {
        super();
    }

    public void validateObject(ProjectPath path, Validator validator) {
        ObjAttribute attribute = (ObjAttribute) path.getObject();

        // skip validation of inherited attributes
        if (path.getObjectParent() != null
                && path.getObjectParent() != attribute.getEntity()) {
            return;
        }

        // Must have name
        if (Util.isEmptyString(attribute.getName())) {
            validator.registerError("Unnamed ObjAttribute.", path);
        }
        else {
            MappingNamesHelper helper = MappingNamesHelper.getInstance();
            String invalidChars = helper.invalidCharsInObjPathComponent(attribute
                    .getName());

            if (invalidChars != null) {
                validator.registerWarning(
                        "ObjAttribute name contains invalid characters: " + invalidChars,
                        path);
            }
            else if (helper.invalidDataObjectProperty(attribute.getName())) {
                validator.registerWarning("ObjAttribute name is invalid: "
                        + attribute.getName(), path);
            }
        }

        // all attributes must have type
        if (Util.isEmptyString(attribute.getType())) {
            validator.registerWarning("ObjAttribute has no type.", path);
        }

        if (attribute.getDbAttribute() == null) {
            validator.registerWarning("ObjAttribute has no DbAttribute mapping.", path);
        }
        // can't support generated meaningful attributes for now; besides they don't make
        // sense.
        else if (attribute.getDbAttribute().isPrimaryKey()
                && attribute.getDbAttribute().isGenerated()) {
            validator.registerWarning("ObjAttribute is mapped to a generated PK: "
                    + attribute.getDbAttributeName(), path);
        }
    }
}
