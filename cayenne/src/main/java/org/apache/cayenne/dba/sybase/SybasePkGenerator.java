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

package org.apache.cayenne.dba.sybase;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.Transaction;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Primary key generator implementation for Sybase. Uses a lookup table named
 * "AUTO_PK_SUPPORT" and a stored procedure "auto_pk_for_table" to search and
 * increment primary keys for tables.
 */
public class SybasePkGenerator extends JdbcPkGenerator {

	/**
	 * Used by DI
	 * @since 4.1
	 */
	public SybasePkGenerator(){
		super();
	}

	protected SybasePkGenerator(JdbcAdapter adapter) {
		super(adapter);
	}

	@Override
	protected String pkTableCreateString() {
		return "CREATE TABLE AUTO_PK_SUPPORT (TABLE_NAME CHAR(100) NOT NULL, NEXT_ID DECIMAL(19,0) NOT NULL, PRIMARY KEY(TABLE_NAME))";
	}

	/**
	 * Generates database objects to provide automatic primary key support.
	 * Method will execute the following SQL statements:
	 * <p>
	 * 1. Executed only if a corresponding table does not exist in the database.
	 * </p>
	 * 
	 * <pre>
	 *    CREATE TABLE AUTO_PK_SUPPORT (
	 *       TABLE_NAME VARCHAR(32) NOT NULL,
	 *       NEXT_ID DECIMAL(19,0) NOT NULL
	 *    )
	 * </pre>
	 * <p>
	 * 2. Executed under any circumstances.
	 * </p>
	 * 
	 * <pre>
	 * if exists (SELECT * FROM sysobjects WHERE name = 'auto_pk_for_table')
	 * BEGIN
	 *    DROP PROCEDURE auto_pk_for_table 
	 * END
	 * </pre>
	 * <p>
	 * 3. Executed under any circumstances.
	 * </p>
	 * CREATE PROCEDURE auto_pk_for_table
	 * 
	 * <pre>
	 * &#064;tname VARCHAR(32),
	 * &#064;pkbatchsize INT AS BEGIN BEGIN TRANSACTION UPDATE AUTO_PK_SUPPORT set NEXT_ID =
	 *              NEXT_ID +
	 * &#064;pkbatchsize WHERE TABLE_NAME =
	 * &#064;tname SELECT NEXT_ID from AUTO_PK_SUPPORT where NEXT_ID =
	 * &#064;tname COMMIT END
	 * </pre>
	 * 
	 * @param node
	 *            node that provides access to a DataSource.
	 */
	@Override
	public void createAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
		super.createAutoPk(node, dbEntities);
		runUpdate(node, safePkProcDrop());
		runUpdate(node, unsafePkProcCreate());
	}

	@Override
	public List<String> createAutoPkStatements(List<DbEntity> dbEntities) {
		List<String> list = super.createAutoPkStatements(dbEntities);

		// add stored procedure drop code
		list.add(safePkProcDrop());

		// add stored procedure creation code
		list.add(unsafePkProcCreate());

		return list;
	}

	/**
	 * Drops database objects related to automatic primary key support. Method
	 * will execute the following SQL statements:
	 * 
	 * <pre>
	 * if exists (SELECT * FROM sysobjects WHERE name = 'AUTO_PK_SUPPORT')
	 * BEGIN
	 *    DROP TABLE AUTO_PK_SUPPORT
	 * END
	 * 
	 * 
	 * if exists (SELECT * FROM sysobjects WHERE name = 'auto_pk_for_table')
	 * BEGIN
	 *    DROP PROCEDURE auto_pk_for_table 
	 * END
	 * </pre>
	 * 
	 * @param node
	 *            node that provides access to a DataSource.
	 */
	@Override
	public void dropAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
		runUpdate(node, safePkProcDrop());
		runUpdate(node, safePkTableDrop());
	}

	@Override
	public List<String> dropAutoPkStatements(List<DbEntity> dbEntities) {
		List<String> list = new ArrayList<>();
		list.add(safePkProcDrop());
		list.add(safePkTableDrop());
		return list;
	}

	/**
	 * @since 3.0
	 */
	@Override
	protected long longPkFromDatabase(DataNode node, DbEntity entity) throws Exception {
		// handle CAY-588 - get connection that is separate from the connection
		// in the current transaction.

		// TODO (andrus, 7/6/2006) Note that this will still work in a pool with
		// a single connection, as PK generator is invoked early in the transaction,
		// before the connection is grabbed for commit...
		// So maybe promote this to other adapters in 3.0?

		Transaction transaction = BaseTransaction.getThreadTransaction();
		BaseTransaction.bindThreadTransaction(null);

		try (Connection connection = node.getDataSource().getConnection()) {
			try (CallableStatement statement = connection.prepareCall("{call auto_pk_for_table(?, ?)}")) {
				statement.setString(1, entity.getName());
				statement.setInt(2, getPkCacheSize());

				// can't use "executeQuery" per http://jtds.sourceforge.net/faq.html#expectingResultSet
				statement.execute();
				if (statement.getMoreResults()) {
					try (ResultSet rs = statement.getResultSet()) {
						if (rs.next()) {
							return rs.getLong(1);
						} else {
							throw new CayenneRuntimeException("Error generating pk for DbEntity %s", entity.getName());
						}
					}
				} else {
					throw new CayenneRuntimeException("Error generating pk for DbEntity %s"
							+ ", no result set from stored procedure.", entity.getName());
				}
			}
		} finally {
			BaseTransaction.bindThreadTransaction(transaction);
		}
	}

	private String safePkTableDrop() {
		return "if exists (SELECT * FROM sysobjects WHERE name = 'AUTO_PK_SUPPORT') BEGIN " +
				" DROP TABLE AUTO_PK_SUPPORT END";
	}

	private String unsafePkProcCreate() {
		return " CREATE PROCEDURE auto_pk_for_table @tname VARCHAR(32), @pkbatchsize INT AS BEGIN BEGIN TRANSACTION"
				+ " UPDATE AUTO_PK_SUPPORT set NEXT_ID = NEXT_ID + @pkbatchsize WHERE TABLE_NAME = @tname"
				+ " SELECT NEXT_ID FROM AUTO_PK_SUPPORT WHERE TABLE_NAME = @tname COMMIT END";
	}

	private String safePkProcDrop() {
		return "if exists (SELECT * FROM sysobjects WHERE name = 'auto_pk_for_table') BEGIN DROP PROCEDURE auto_pk_for_table END";
	}

}
