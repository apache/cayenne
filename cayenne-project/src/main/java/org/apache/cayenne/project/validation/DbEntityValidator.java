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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationResult;

import java.util.function.Supplier;

class DbEntityValidator extends ConfigurationNodeValidator<DbEntity> {

    /**
     * @param configSupplier the config defining the behavior of this validator.
     * @since 5.0
     */
    public DbEntityValidator(Supplier<ValidationConfig> configSupplier) {
        super(configSupplier);
    }

    @Override
    public void validate(DbEntity node, ValidationResult validationResult) {
        on(node, validationResult)
                .performIfEnabled(Inspection.DB_ENTITY_NO_NAME, this::checkForName)
                .performIfEnabled(Inspection.DB_ENTITY_NAME_DUPLICATE, this::checkForNameDuplicates)
                .performIfEnabled(Inspection.DB_ENTITY_NO_ATTRIBUTES, this::checkForAttributes)
                .performIfEnabled(Inspection.DB_ENTITY_NO_PK, this::checkForPK);
    }

    private void checkForName(DbEntity entity, ValidationResult validationResult) {

        // Must have name
        String name = entity.getName();
        if (Util.isEmptyString(name)) {
            addFailure(validationResult, entity, "Unnamed DbEntity");
        }
    }

    private void checkForNameDuplicates(DbEntity entity, ValidationResult validationResult) {
        String name = entity.getName();
        DataMap map = entity.getDataMap();
        if (map == null || Util.isEmptyString(name)) {
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

    /**
     * Tables must have columns.
     */
    private void checkForAttributes(DbEntity entity, ValidationResult validationResult) {
        if (entity.getAttributes().isEmpty()) {
            addFailure(validationResult, entity, "DbEntity '%s' has no attributes defined", entity.getName());
        }
    }

    /**
     * Validates the presence of the primary key. A warning is given only if the parent
     * map also contains an ObjEntity mapped to this entity, since unmapped primary key is
     * ok if working with data rows.
     */
    private void checkForPK(DbEntity entity, ValidationResult validationResult) {
        if (entity.getAttributes().isEmpty() || !entity.getPrimaryKeys().isEmpty()) {
            return;
        }
        DataMap map = entity.getDataMap();
        if (map == null || map.getMappedEntities(entity).isEmpty()) {
            return;
        }
        addFailure(validationResult, entity, "DbEntity '%s' has no primary key attributes defined", entity.getName());
    }
}
