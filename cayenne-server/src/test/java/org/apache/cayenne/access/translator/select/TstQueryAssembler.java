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

package org.apache.cayenne.access.translator.select;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.query.Query;

public class TstQueryAssembler extends QueryAssembler {

	protected List<DbRelationship> dbRels;

	public TstQueryAssembler(Query q, DbAdapter adapter, EntityResolver resolver) {
		super(q, adapter, resolver);
		dbRels = new ArrayList<DbRelationship>();
	}

	@Override
	public void dbRelationshipAdded(DbRelationship relationship, JoinType joinType, String joinSplitAlias) {
		dbRels.add(relationship);
	}

	@Override
	public String getCurrentAlias() {
		return "ta";
	}

	@Override
	public void resetJoinStack() {
		// noop
	}

	@Override
	public boolean supportsTableAliases() {
		return true;
	}

	@Override
	public String getAliasForExpression(Expression exp) {
		return null;
	}

	@Override
	protected void doTranslate() {
		this.sql = "SELECT * FROM ARTIST";
	}
}
