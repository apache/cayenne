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

package org.apache.cayenne.dba.ingres;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.oracle.OraclePkGenerator;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ingres-specific sequence based PK generator.
 * 
 * @since 1.2
 */
public class IngresPkGenerator extends OraclePkGenerator {

	protected IngresPkGenerator(JdbcAdapter adapter) {
		super(adapter);
	}

	@Override
	protected long longPkFromDatabase(DataNode node, DbEntity entity) throws Exception {

		DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
		String pkGeneratingSequenceName;
		if (pkGenerator != null && DbKeyGenerator.ORACLE_TYPE.equals(pkGenerator.getGeneratorType())
				&& pkGenerator.getGeneratorName() != null) {
			pkGeneratingSequenceName = pkGenerator.getGeneratorName();
		} else {
			pkGeneratingSequenceName = sequenceName(entity);
		}

		try (Connection con = node.getDataSource().getConnection();) {

			try (Statement st = con.createStatement();) {
				String sql = "SELECT " + pkGeneratingSequenceName + ".nextval";
				adapter.getJdbcEventLogger().logQuery(sql, Collections.EMPTY_LIST);

				try (ResultSet rs = st.executeQuery(sql);) {
					// Object pk = null;
					if (!rs.next()) {
						throw new CayenneRuntimeException("Error generating pk for DbEntity " + entity.getName());
					}
					return rs.getLong(1);
				}
			}
		}
	}

	@Override
	protected List<String> getExistingSequences(DataNode node) throws SQLException {

		// check existing sequences

		try (Connection connection = node.getDataSource().getConnection();) {

			try (Statement select = connection.createStatement();) {
				String sql = "select seq_name from iisequences where seq_owner != 'DBA'";
				adapter.getJdbcEventLogger().logQuery(sql, Collections.EMPTY_LIST);

				try (ResultSet rs = select.executeQuery(sql);) {
					List<String> sequenceList = new ArrayList<>();
					while (rs.next()) {
						String name = rs.getString(1);
						if (name != null) {
							sequenceList.add(name.trim());
						}
					}
					return sequenceList;
				}
			}
		}
	}
}
