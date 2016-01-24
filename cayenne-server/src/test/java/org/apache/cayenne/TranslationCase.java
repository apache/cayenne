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

package org.apache.cayenne;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TranslationCase {

	protected Object tstObject;
	protected String sqlExp;
	protected String rootEntity;

	public TranslationCase(String rootEntity, Object tstObject, String sqlExp) {
		this.tstObject = tstObject;
		this.rootEntity = rootEntity;
		this.sqlExp = sqlExp;
	}

	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(this.getClass().getName()).append(tstObject);
		return buf.toString();
	}

	public void assertTranslatedWell(String translated) {
		if (sqlExp == null) {
			assertNull(translated);
			return;
		}

		assertNotNull(translated);
		assertEquals("Unexpected translation: " + translated + "....", sqlExp, translated);
	}

	public String getRootEntity() {
		return rootEntity;
	}

	public String getSqlExp() {
		return sqlExp;
	}
}
