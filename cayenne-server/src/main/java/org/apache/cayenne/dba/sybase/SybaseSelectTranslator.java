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
package org.apache.cayenne.dba.sybase;

import org.apache.cayenne.access.translator.select.DefaultSelectTranslator;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

public class SybaseSelectTranslator extends DefaultSelectTranslator {
	/**
	 * @since 4.0
	 */
	public SybaseSelectTranslator(Query query, DbAdapter adapter, EntityResolver entityResolver) {
		super(query, adapter, entityResolver);
	}

	@Override
	protected void appendLimitAndOffsetClauses(StringBuilder buffer) {

		int limit = queryMetadata.getFetchLimit();
		int offset = queryMetadata.getFetchOffset();

		if (limit > 0) {
			String sql = buffer.toString();

			// If contains distinct insert top limit after
			if (sql.startsWith("SELECT DISTINCT ")) {
				buffer.replace(0, 15, "SELECT DISTINCT TOP " + (offset + limit));

			} else {
				buffer.replace(0, 6, "SELECT TOP " + (offset + limit));
			}
		}
	}
}
