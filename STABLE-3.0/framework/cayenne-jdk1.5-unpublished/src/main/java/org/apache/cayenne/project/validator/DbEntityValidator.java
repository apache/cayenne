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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.util.Util;

/**
 */
public class DbEntityValidator extends TreeNodeValidator {

	/**
	 * Constructor for DbEntityValidator.
	 */
	public DbEntityValidator() {
		super();
	}

	@Override
    public void validateObject(ProjectPath path, Validator validator) {
		DbEntity ent = (DbEntity) path.getObject();
		validateName(ent, path, validator);
		validateAttributes(ent, path, validator);
		validatePK(ent, path, validator);
	}

	/**
	 * Validates the presence of the primary key. A warning is given only if the parent
	 * map also conatins an ObjEntity mapped to this entity, since unmapped primary key
	 * is ok if working with data rows.
	 */
	protected void validatePK(
		DbEntity ent,
		ProjectPath path,
		Validator validator) {
		if (ent.getAttributes().size() > 0
			&& ent.getPrimaryKeys().size() == 0) {
			DataMap map = ent.getDataMap();
			if (map != null && map.getMappedEntities(ent).size() > 0) {
				// there is an objentity, so complain about no pk
				validator.registerWarning(
					"DbEntity \""
						+ ent.getName()
						+ "\" has no primary key attributes defined.",
					path);
			}
		}
	}

	/**
	 * Tables must have columns.
	 */
	protected void validateAttributes(
		DbEntity ent,
		ProjectPath path,
		Validator validator) {
		if (ent.getAttributes().size() == 0) {
			// complain about missing attributes
			validator.registerWarning(
				"DbEntity \"" + ent.getName() + "\" has no attributes defined.",
				path);
		}
	}

	protected void validateName(
		DbEntity ent,
		ProjectPath path,
		Validator validator) {
		String name = ent.getName();

		// Must have name
		if (Util.isEmptyString(name)) {
			validator.registerError("Unnamed DbEntity.", path);
			return;
		}

		DataMap map = (DataMap) path.getObjectParent();
		if (map == null) {
			return;
		}

		// check for duplicate names in the parent context
        for (final DbEntity otherEnt : map.getDbEntities()) {
            if (otherEnt == ent) {
                continue;
            }

            if (name.equals(otherEnt.getName())) {
                validator.registerError(
                        "Duplicate DbEntity name: " + name + ".",
                        path);
                break;
            }
        }
	}
}
