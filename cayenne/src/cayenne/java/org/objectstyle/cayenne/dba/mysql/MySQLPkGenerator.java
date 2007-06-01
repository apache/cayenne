/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.dba.mysql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.dba.JdbcPkGenerator;
import org.objectstyle.cayenne.map.DbEntity;

/**
 * @author Andrei Adamchik
 */
public class MySQLPkGenerator extends JdbcPkGenerator {
	/**
	 * Overrides superclass's implementation to perform locking of the 
	 * primary key lookup table.
	 */
	protected int pkFromDatabase(DataNode node, DbEntity ent)
		throws Exception {

		// must work directly with JDBC connection, since we 
		// must unlock the AUTO_PK_SUPPORT table in case of
		// failures.... ah..JDBC is fun...

		// chained SQL exception
		SQLException exception = null;
		int pk = -1;

		Connection con = node.getDataSource().getConnection();
		try {
			Statement st = con.createStatement();

			try {
				pk = getPrimaryKey(st, ent.getName());
			} catch (SQLException pkEx) {
				exception = processSQLException(pkEx, exception);
			} finally {
				// UNLOCK! 
				// THIS MUST BE EXECUTED NO MATTER WHAT, OR WE WILL LOCK THE PRIMARY KEY TABLE!!
				try {
					String unlockString = "UNLOCK TABLES";
					QueryLogger.logQuery(QueryLogger.DEFAULT_LOG_LEVEL, unlockString, Collections.EMPTY_LIST);
					st.execute(unlockString);
				} catch (SQLException unlockEx) {
					exception = processSQLException(unlockEx, exception);
				} finally {
					// close statement
					try {
						st.close();
					} catch (SQLException stClosingEx) {
						// ignoring...
					}
				}
			}
		} catch (SQLException otherEx) {
			exception = processSQLException(otherEx, exception);
		} finally {
			try {
				con.close();
			} catch (SQLException closingEx) {
				// ignoring
			}
		}

		// check errors
		if (exception != null) {
			throw exception;
		}

		return pk;
	}
	
	/**
	 * Appends a new SQLException to the chain. If parent is null, uses the exception as the
	 * chain root.
	 */
	protected SQLException processSQLException(SQLException exception, SQLException parent) {
		if(parent == null) {
			return exception;
		}
		
		parent.setNextException(exception);
		return parent;
	}
    
    protected String pkTableCreateString() {
        StringBuffer buf = new StringBuffer();
        buf
            .append("CREATE TABLE AUTO_PK_SUPPORT (")
            .append("  TABLE_NAME CHAR(100) NOT NULL,")
            .append("  NEXT_ID INTEGER NOT NULL, UNIQUE (TABLE_NAME)")
            .append(")");

        return buf.toString();
    }

	protected int getPrimaryKey(Statement statement, String entityName)
		throws SQLException {
		// lock
		String lockString = "LOCK TABLES AUTO_PK_SUPPORT WRITE";
		QueryLogger.logQuery(QueryLogger.DEFAULT_LOG_LEVEL, lockString, Collections.EMPTY_LIST);
		statement.execute(lockString);
		
		// select
		int pk = -1;
		
		String selectString = super.pkSelectString(entityName);
		QueryLogger.logQuery(QueryLogger.DEFAULT_LOG_LEVEL, selectString, Collections.EMPTY_LIST);
		ResultSet rs = statement.executeQuery(selectString);
		try {
			if(!rs.next()) {
				throw new SQLException("No rows for '" + entityName + "'");
			}
			
			pk = rs.getInt(1);
			
			if(rs.next()) {
				throw new SQLException("More than one row for '" + entityName + "'");
			}
		} finally {
			try {
				rs.close();
			} catch (Exception ex) {
				// ignoring...
			}
		}
		
		// update
		String updateString = super.pkUpdateString(entityName) + " AND NEXT_ID = " + pk;
		QueryLogger.logQuery(QueryLogger.DEFAULT_LOG_LEVEL, updateString, Collections.EMPTY_LIST);
		int updated = statement.executeUpdate(updateString);
		// optimistic lock failure...
        if(updated != 1) {
        	throw new SQLException("Error updating PK count '" + entityName + "': " + updated);
        }
        
		return pk;
	}
}