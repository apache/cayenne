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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.Transaction;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DbEntity;

/** 
 * Primary key generator implementation for Sybase. Uses a lookup table named
 * "AUTO_PK_SUPPORT" and a stored procedure "auto_pk_for_table" 
 * to search and increment primary keys for tables.   
 * 
 * @author Andrus Adamchik
 */
public class SybasePkGenerator extends JdbcPkGenerator {

    /** Generates database objects to provide
     *  automatic primary key support. Method will execute the following
     *  SQL statements:
     * 
     * <p>1. Executed only if a corresponding table does not exist in the
     * database.</p>
     * 
     * <pre>
     *    CREATE TABLE AUTO_PK_SUPPORT (
     *       TABLE_NAME VARCHAR(32) NOT NULL,
     *       NEXT_ID INTEGER NOT NULL
     *    )
     * </pre>
     * 
     * <p>2. Executed under any circumstances. </p>
     * 
     * <pre>
     * if exists (SELECT * FROM sysobjects WHERE name = 'auto_pk_for_table')
     * BEGIN
     *    DROP PROCEDURE auto_pk_for_table 
     * END
     * </pre>
     * 
     * <p>3. Executed under any circumstances. </p>
     * CREATE PROCEDURE auto_pk_for_table @tname VARCHAR(32), @pkbatchsize INT AS
     * BEGIN
     *      BEGIN TRANSACTION
     *         UPDATE AUTO_PK_SUPPORT set NEXT_ID = NEXT_ID + @pkbatchsize 
     *         WHERE TABLE_NAME = @tname
     * 
     *         SELECT NEXT_ID from AUTO_PK_SUPPORT where NEXT_ID = @tname
     *      COMMIT
     * END
     * </pre>
     *
     *  @param node node that provides access to a DataSource.
     */
    @Override
    public void createAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
    	super.createAutoPk(node, dbEntities);
    	super.runUpdate(node, safePkProcDrop());
        super.runUpdate(node, unsafePkProcCreate());
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
     * Drops database objects related to automatic primary
     * key support. Method will execute the following SQL
     * statements:
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
     *  @param node node that provides access to a DataSource.
     */
    @Override
    public void dropAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
        super.runUpdate(node, safePkProcDrop());
        super.runUpdate(node, safePkTableDrop());
    }
    
    @Override
    public List<String> dropAutoPkStatements(List<DbEntity> dbEntities) {
		List<String> list = new ArrayList<String>();
		list.add(safePkProcDrop());
		list.add(safePkTableDrop());
		return list;
	}

    @Override
    protected int pkFromDatabase(DataNode node, DbEntity ent) throws Exception {
        // handle CAY-588 - get connection that is separate from the connection in the
        // current transaction. 
        
        // TODO (andrus, 7/6/2006) Note that this will still work in a pool with a single
        // connection, as PK generator is invoked early in the transaction, before the
        // connection is grabbed for commit... So maybe promote this to other adapters in
        // 3.0?

        Transaction transaction = Transaction.getThreadTransaction();
        Transaction.bindThreadTransaction(null);

        try {

            Connection connection = node.getDataSource().getConnection();
            try {
                CallableStatement statement = connection
                        .prepareCall("{call auto_pk_for_table(?, ?)}");
                try {
                    statement.setString(1, ent.getName());
                    statement.setInt(2, super.getPkCacheSize());

                    // can't use "executeQuery"
                    // per http://jtds.sourceforge.net/faq.html#expectingResultSet
                    statement.execute();
                    if (statement.getMoreResults()) {
                        ResultSet rs = statement.getResultSet();

                        try {
                            if (rs.next()) {
                                return rs.getInt(1);
                            }
                            else {
                                throw new CayenneRuntimeException(
                                        "Error generating pk for DbEntity "
                                                + ent.getName());
                            }
                        }
                        finally {
                            rs.close();
                        }
                    }
                    else {
                        throw new CayenneRuntimeException(
                                "Error generating pk for DbEntity "
                                        + ent.getName()
                                        + ", no result set from stored procedure.");
                    }
                }
                finally {
                    statement.close();
                }
            }
            finally {
                connection.close();
            }
        }
        finally {
            Transaction.bindThreadTransaction(transaction);
        }
    }


    private String safePkTableDrop() {
        StringBuffer buf = new StringBuffer();
        buf
            .append("if exists (SELECT * FROM sysobjects WHERE name = 'AUTO_PK_SUPPORT')")
            .append(" BEGIN ")
            .append(" DROP TABLE AUTO_PK_SUPPORT")
            .append(" END");

        return buf.toString();
    }

    private String unsafePkProcCreate() {
        StringBuffer buf = new StringBuffer();
        buf
            .append(" CREATE PROCEDURE auto_pk_for_table @tname VARCHAR(32), @pkbatchsize INT AS")
            .append(" BEGIN")
            .append(" BEGIN TRANSACTION")
            .append(" UPDATE AUTO_PK_SUPPORT set NEXT_ID = NEXT_ID + @pkbatchsize")
            .append(" WHERE TABLE_NAME = @tname")
            .append(" SELECT NEXT_ID FROM AUTO_PK_SUPPORT WHERE TABLE_NAME = @tname")
            .append(" COMMIT")
            .append(" END");
        return buf.toString();
    }

    private String safePkProcDrop() {
        StringBuffer buf = new StringBuffer();
        buf
            .append("if exists (SELECT * FROM sysobjects WHERE name = 'auto_pk_for_table')")
            .append(" BEGIN")
            .append(" DROP PROCEDURE auto_pk_for_table")
            .append(" END");
        return buf.toString();
    }

}
