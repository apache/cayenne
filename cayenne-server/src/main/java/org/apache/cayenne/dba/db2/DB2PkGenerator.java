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
package org.apache.cayenne.dba.db2;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A sequence-based PK generator used by {@link DB2Adapter}.
 */
public class DB2PkGenerator extends JdbcPkGenerator {

	DB2PkGenerator(JdbcAdapter adapter) {
		super(adapter);
	}

	private static final String _SEQUENCE_PREFIX = "S_";

	/**
	 * @since 3.0
	 */
	@Override
	protected long longPkFromDatabase(DataNode node, DbEntity entity) throws Exception {

		String pkGeneratingSequenceName = sequenceName(entity);
		try (Connection con = node.getDataSource().getConnection()) {
			try (Statement st = con.createStatement()) {
				String sql = "SELECT NEXTVAL FOR " + pkGeneratingSequenceName + " FROM SYSIBM.SYSDUMMY1";
				adapter.getJdbcEventLogger().logQuery(sql, Collections.EMPTY_LIST);
				try (ResultSet rs = st.executeQuery(sql)) {
					if (!rs.next()) {
						throw new CayenneRuntimeException("Error generating pk for DbEntity " + entity.getName());
					}
					return rs.getLong(1);
				}
			}
		}
	}

	@Override
	public void createAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
		Collection<String> sequences = getExistingSequences(node);
		for (DbEntity entity : dbEntities) {
			if (!sequences.contains(sequenceName(entity))) {
				this.runUpdate(node, createSequenceString(entity));
			}
		}
	}

	/**
	 * Creates a list of CREATE SEQUENCE statements for the list of DbEntities.
	 */
	@Override
	public List<String> createAutoPkStatements(List<DbEntity> dbEntities) {
		List<String> list = new ArrayList<>(dbEntities.size());
		for (DbEntity entity : dbEntities) {
			list.add(createSequenceString(entity));
		}
		return list;
	}

	/**
	 * Drops PK sequences for all specified DbEntities.
	 */
	@Override
	public void dropAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
		Collection<String> sequences = getExistingSequences(node);

		for (DbEntity ent : dbEntities) {
			String name;
			if (ent.getDataMap().isQuotingSQLIdentifiers()) {
				DbEntity tempEnt = new DbEntity();
				DataMap dm = new DataMap();
				dm.setQuotingSQLIdentifiers(false);
				tempEnt.setDataMap(dm);
				tempEnt.setName(ent.getName());
				name = sequenceName(tempEnt);
			} else {
				name = sequenceName(ent);
			}
			if (sequences.contains(name)) {
				runUpdate(node, dropSequenceSql(ent));
			}
		}
	}

	/**
	 * Creates a list of DROP SEQUENCE statements for the list of DbEntities.
	 */
	@Override
	public List<String> dropAutoPkStatements(List<DbEntity> dbEntities) {
		List<String> list = new ArrayList<>(dbEntities.size());
		for (DbEntity entity : dbEntities) {
			list.add(dropSequenceSql(entity));
		}
		return list;
	}

	/**
	 * Fetches a list of existing sequences that might match Cayenne generated
	 * ones.
	 */
	protected List<String> getExistingSequences(DataNode node) throws SQLException {

		// check existing sequences

		try (Connection con = node.getDataSource().getConnection()) {
			try (Statement sel = con.createStatement()) {
				String sql = "SELECT SEQNAME FROM SYSCAT.SEQUENCES WHERE SEQNAME LIKE '" + _SEQUENCE_PREFIX + "%'";
				adapter.getJdbcEventLogger().logQuery(sql, Collections.EMPTY_LIST);

				try (ResultSet rs = sel.executeQuery(sql)) {
					List<String> sequenceList = new ArrayList<>();
					while (rs.next()) {
						sequenceList.add(rs.getString(1).toUpperCase());
					}
					return sequenceList;
				}
			}
		}
	}

	/**
	 * Returns default sequence name for DbEntity.
	 */
	protected String sequenceName(DbEntity entity) {
		String entName = entity.getName().toUpperCase();
		String seqName = _SEQUENCE_PREFIX + entName;

		return adapter.getQuotingStrategy().quotedIdentifier(entity, entity.getCatalog(), entity.getSchema(), seqName);
	}

	/**
	 * Returns DROP SEQUENCE statement.
	 */
	protected String dropSequenceSql(DbEntity entity) {
		return "DROP SEQUENCE " + sequenceName(entity) + " RESTRICT ";
	}

	/**
	 * Returns CREATE SEQUENCE statement for entity.
	 */
	protected String createSequenceString(DbEntity entity) {
		StringBuilder buf = new StringBuilder();
		buf.append("CREATE SEQUENCE ").append(sequenceName(entity)).append(" AS BIGINT START WITH ").append(pkStartValue)
				.append(" INCREMENT BY ").append(getPkCacheSize()).append(" NO MAXVALUE ").append(" NO CYCLE ")
				.append(" CACHE ").append(getPkCacheSize());
		return buf.toString();
	}
}
