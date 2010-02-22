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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.util.Util;

class DbEntityValidator {

    void validate(Object object, ValidationVisitor validationVisitor) {
        DbEntity ent = (DbEntity) object;
        validateName(ent, object, validationVisitor);
        validateAttributes(ent, object, validationVisitor);
        validatePK(ent, object, validationVisitor);
    }

    /**
     * Validates the presence of the primary key. A warning is given only if the parent
     * map also conatins an ObjEntity mapped to this entity, since unmapped primary key is
     * ok if working with data rows.
     */
    void validatePK(DbEntity ent, Object object, ValidationVisitor validationVisitor) {
        if (ent.getAttributes().size() > 0 && ent.getPrimaryKeys().size() == 0) {
            DataMap map = ent.getDataMap();
            if (map != null && map.getMappedEntities(ent).size() > 0) {
                // there is an objentity, so complain about no pk
                validationVisitor.registerWarning("DbEntity \""
                        + ent.getName()
                        + "\" has no primary key attributes defined.", object);
            }
        }
    }

    /**
     * Tables must have columns.
     */
    void validateAttributes(
            DbEntity ent,
            Object object,
            ValidationVisitor validationVisitor) {
        if (ent.getAttributes().size() == 0) {
            // complain about missing attributes
            validationVisitor.registerWarning("DbEntity \""
                    + ent.getName()
                    + "\" has no attributes defined.", object);
        }
    }

    void validateName(DbEntity ent, Object object, ValidationVisitor validationVisitor) {
        String name = ent.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validationVisitor.registerError("Unnamed DbEntity.", object);
            return;
        }

        if (object instanceof Entity) {
            DataMap map = ((Entity) object).getDataMap();
            if (map == null) {
                return;
            }

            // check for duplicate names in the parent context
            for (final DbEntity otherEnt : map.getDbEntities()) {
                if (otherEnt == ent) {
                    continue;
                }

                if (name.equals(otherEnt.getName())) {
                    validationVisitor.registerError("Duplicate DbEntity name: "
                            + name
                            + ".", object);
                    break;
                }
            }
        }
    }
}
