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

package org.objectstyle.cayenne.dba.sybase;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.dba.JdbcPkGenerator;
import org.objectstyle.cayenne.map.DbEntity;

/** 
 * Primary key generator implementation for Sybase. Uses a lookup table named
 * "AUTO_PK_SUPPORT" and a stored procedure "auto_pk_for_table" 
 * to search and increment primary keys for tables.   
 * 
 * @author Andrei Adamchik
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
    public void createAutoPk(DataNode node, List dbEntities) throws Exception {
    	super.createAutoPk(node, dbEntities);
    	super.runUpdate(node, safePkProcDrop());
        super.runUpdate(node, unsafePkProcCreate());
    }
    
    
    public List createAutoPkStatements(List dbEntities) {
		List list = super.createAutoPkStatements(dbEntities);
		
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
    public void dropAutoPk(DataNode node, List dbEntities) throws Exception {
        super.runUpdate(node, safePkProcDrop());
        super.runUpdate(node, safePkTableDrop());
    }
    
    public List dropAutoPkStatements(List dbEntities) {
		List list = new ArrayList();
		list.add(safePkProcDrop());
		list.add(safePkTableDrop());
		return list;
	}

    protected int pkFromDatabase(DataNode node, DbEntity ent) throws Exception {
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
                                    "Error generating pk for DbEntity " + ent.getName());
                        }
                    }
                    finally {
                        rs.close();
                    }
                }
                else {
                    throw new CayenneRuntimeException("Error generating pk for DbEntity "
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