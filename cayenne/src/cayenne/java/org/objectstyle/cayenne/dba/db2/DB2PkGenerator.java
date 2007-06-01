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

package org.objectstyle.cayenne.dba.db2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.dba.JdbcPkGenerator;
import org.objectstyle.cayenne.map.DbEntity;

/**
 * PK Generator for IBM DB2 using sequences.
 * 
 * @author Mario Linke, Holger Hoffstaette
 */
public class DB2PkGenerator extends JdbcPkGenerator {

	public static final String SEQUENCE_PREFIX = "S_";

	public void createAutoPk(DataNode node, List dbEntities) throws Exception {
		List sequences = this.getExistingSequences(node);
		Iterator it = dbEntities.iterator();

		while (it.hasNext()) {
			DbEntity ent = (DbEntity) it.next();
			if (!sequences.contains(sequenceName(ent))) {
				this.runUpdate(node, this.createSequenceString(ent));
			}
		}
	}

	public List createAutoPkStatements(List dbEntities) {
		List list = new ArrayList();
		Iterator it = dbEntities.iterator();

		while (it.hasNext()) {
			DbEntity ent = (DbEntity) it.next();
			list.add(this.createSequenceString(ent));
		}

		return list;
	}
	
	public void dropAutoPk(DataNode node, List dbEntities) throws Exception {
		List sequences = this.getExistingSequences(node);
		
		Iterator it = dbEntities.iterator();
		while (it.hasNext()) {
			DbEntity ent = (DbEntity) it.next();
			if (sequences.contains(this.sequenceName(ent))) {
				this.runUpdate(node, this.dropSequenceString(ent));
			}
		}
	}

	public List dropAutoPkStatements(List dbEntities) {
		 List list = new ArrayList();
		 Iterator it = dbEntities.iterator();

		 while (it.hasNext()) {
			 DbEntity ent = (DbEntity) it.next();
			 list.add(this.dropSequenceString(ent));
		 }

		 return list;
	 }	
	
	/**
	 * Returns the sequence name for a given table name.
	 */
	protected String sequenceName(DbEntity ent) {
		String seqName = SEQUENCE_PREFIX + ent.getName();

		if (ent.getSchema() != null && ent.getSchema().length() > 0) {
			seqName = ent.getSchema() + "." + seqName;
		}

		return seqName;
	}	
	
	
	/**
	 * Creates SQL needed for creating a sequence.
	 */
	protected String createSequenceString(DbEntity ent) {
		StringBuffer buf = new StringBuffer();
		buf.append("CREATE SEQUENCE ")
			.append(this.sequenceName(ent))
			.append(" START WITH 200")
			.append(" INCREMENT BY ").append(getPkCacheSize())
			.append(" NO MAXVALUE ")
			.append(" NO CYCLE ")
			.append(" CACHE ").append(getPkCacheSize());
		return buf.toString();
	}	
	
	/**
	 * Creates SQL needed for dropping a sequence.
	 */
	protected String dropSequenceString(DbEntity ent) {
		return "DROP SEQUENCE " + this.sequenceName(ent) + " RESTRICT ";
	}
	
	/**
	 * Creates a new PK from a sequence returned by
	 * <code>
	 * SELECT NEXTVAL FOR sequence_name FROM SYSIBM.SYSDUMMY1 
	 * </code>
	 * SYSIBM.SYSDUMMY1 corresponds to DUAL in Oracle.
	 */
	protected int pkFromDatabase(DataNode node, DbEntity ent) throws Exception {

		String seq_name = sequenceName (ent);
		Connection con = node.getDataSource().getConnection();
		try {
		  Statement st = con.createStatement();
		  try {
		  	String pkQueryString = "SELECT NEXTVAL FOR "
		  							+ seq_name
		  							+ " FROM SYSIBM.SYSDUMMY1";
			QueryLogger.logQuery(QueryLogger.DEFAULT_LOG_LEVEL, pkQueryString, Collections.EMPTY_LIST);
			ResultSet rs = st.executeQuery(pkQueryString);
			try {
			  if (!rs.next()) {
				throw new CayenneRuntimeException(
					"Error in pkFromDatabase() for table "
					+ ent.getName()
					+ " / sequence "
					+ seq_name);
			  }
			  return rs.getInt(1);
			} finally {
			  rs.close();
			}
		  } finally {
			st.close();
		  }
		} finally {
		  con.close();
		}
	}	
	
	
	/**
	 * Returns a List of all existing, accessible sequences.
	 */
	protected List getExistingSequences(DataNode node) throws SQLException {
		Connection con = node.getDataSource().getConnection();
		try {
			Statement sel = con.createStatement();
			try {
				StringBuffer q = new StringBuffer();
				q.append("SELECT SEQNAME FROM SYSCAT.SEQUENCES WHERE SEQNAME")
					.append(" LIKE '")
					.append(SEQUENCE_PREFIX)
					.append("%'");

				ResultSet rs = sel.executeQuery(q.toString());
				try {
					List sequenceList = new ArrayList(32);
					while (rs.next()) {
						sequenceList.add(rs.getString(1));
					}
					return sequenceList;
				} finally {
					rs.close();
				}
			} finally {
				sel.close();
			}
		} finally {
			con.close();
		}
	}	
}
