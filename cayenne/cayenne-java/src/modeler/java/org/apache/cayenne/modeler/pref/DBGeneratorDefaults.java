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

package org.apache.cayenne.modeler.pref;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.dba.DbAdapter;

public class DBGeneratorDefaults extends _DBGeneratorDefaults {

	/**
	 * Updates this object state to provide reasonable defaults for a given
	 * adapter.
	 */
	public void adjustForAdapter(DbAdapter adapter) {
		if (!adapter.supportsFkConstraints()
				&& booleanForBooleanProperty(CREATE_FK_PROPERTY)) {
			setCreateFK(Boolean.FALSE);
		}
	}

	/**
	 * Updates DbGenerator settings, consulting its own state.
	 */
	public void configureGenerator(DbGenerator generator) {
		generator
				.setShouldCreateFKConstraints(booleanForBooleanProperty(CREATE_FK_PROPERTY));
		generator
				.setShouldCreatePKSupport(booleanForBooleanProperty(CREATE_PK_PROPERTY));
		generator
				.setShouldCreateTables(booleanForBooleanProperty(CREATE_TABLES_PROPERTY));
		generator
				.setShouldDropPKSupport(booleanForBooleanProperty(DROP_PK_PROPERTY));
		generator
				.setShouldDropTables(booleanForBooleanProperty(DROP_TABLES_PROPERTY));
	}

	public void setPersistenceState(int persistenceState) {

		// init defaults on insert...
		if (this.persistenceState == PersistenceState.TRANSIENT
				&& persistenceState == PersistenceState.NEW) {
			setCreateFK(Boolean.TRUE);
			setCreatePK(Boolean.TRUE);
			setCreateTables(Boolean.TRUE);
			setDropPK(Boolean.FALSE);
			setDropTables(Boolean.FALSE);
		}
		super.setPersistenceState(persistenceState);
	}

	protected boolean booleanForBooleanProperty(String property) {
		Boolean b = (Boolean) readProperty(property);
		return (b != null) ? b.booleanValue() : false;
	}
}
