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
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

class DbEntityValidator extends ConfigurationNodeValidator {

    void validate(DbEntity entity, ValidationResult validationResult) {
        validateName(entity, validationResult);
        validateAttributes(entity, validationResult);
        validatePK(entity, validationResult);
    }

    /**
     * Validates the presence of the primary key. A warning is given only if the parent
     * map also contains an ObjEntity mapped to this entity, since unmapped primary key is
     * ok if working with data rows.
     */
    void validatePK(DbEntity entity, ValidationResult validationResult) {
        if (entity.getAttributes().size() > 0 && entity.getPrimaryKeys().size() == 0) {
            DataMap map = entity.getDataMap();
            if (map != null && map.getMappedEntities(entity).size() > 0) {

                addFailure(
                        validationResult,
                        entity,
                        "DbEntity '%s' has no primary key attributes defined",
                        entity.getName());
            }
        }
    }

    /**
     * Tables must have columns.
     */
    void validateAttributes(DbEntity entity, ValidationResult validationResult) {
        if (entity.getAttributes().size() == 0) {
            addFailure(
                    validationResult,
                    entity,
                    "DbEntity '%s' has no attributes defined",
                    entity.getName());
        }
    }

    void validateName(DbEntity entity, ValidationResult validationResult) {
        String name = entity.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, entity, "Unnamed DbEntity");
            return;
        }

        DataMap map = entity.getDataMap();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (DbEntity otherEntity : map.getDbEntities()) {
            if (otherEntity == entity) {
                continue;
            }

            if (name.equals(otherEntity.getName())) {
                addFailure(validationResult, entity, "Duplicate DbEntity name: %s", name);
                break;
            }
        }
    }
}
