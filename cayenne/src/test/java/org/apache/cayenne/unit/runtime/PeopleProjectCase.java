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
package org.apache.cayenne.unit.runtime;

import java.sql.Types;

import org.apache.cayenne.test.jdbc.DbHelper;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PeopleProjectCase {

	@RegisterExtension
	protected static final CayenneTestsEnv env = CayenneTestsEnv
			.forProject(CayenneProjects.PEOPLE_PROJECT)
			.withoutAutoClean();

	protected DbHelper dbHelper;

	@BeforeEach
	public void cleanUpDB() throws Exception {
		// must null out the circular FK before DBCleaner.clean() runs, otherwise
		// PostgreSQL's strict FK enforcement aborts the cleanup
		dbHelper = env.dbHelper();
		dbHelper.update("PERSON").set("DEPARTMENT_ID", null, Types.INTEGER).execute();
		env.dbCleaner().clean();
	}
}
