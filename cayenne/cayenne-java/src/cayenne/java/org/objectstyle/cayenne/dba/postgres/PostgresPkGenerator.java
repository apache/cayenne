/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.dba.postgres;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.dba.oracle.OraclePkGenerator;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbKeyGenerator;

/**
 * Default PK generator for PostgreSQL that uses sequences for PK generation.
 */
public class PostgresPkGenerator extends OraclePkGenerator {

    protected String createSequenceString(DbEntity ent) {
        // note that PostgreSQL 7.4 and newer supports INCREMENT BY and START WITH
        // however 7.3 doesn't like BY and WITH, so using older more neutral syntax
        // that works with all tested versions.
        StringBuffer buf = new StringBuffer();
        buf
                .append("CREATE SEQUENCE ")
                .append(sequenceName(ent))
                .append(" INCREMENT ")
                .append(pkCacheSize(ent))
                .append(" START 200");
        return buf.toString();
    }

    /**
     * Generates primary key by calling Oracle sequence corresponding to the
     * <code>dbEntity</code>. Executed SQL looks like this:
     * 
     * <pre>
     *     SELECT nextval(pk_table_name)
     * </pre>
     */
    protected int pkFromDatabase(DataNode node, DbEntity ent) throws Exception {

        DbKeyGenerator pkGenerator = ent.getPrimaryKeyGenerator();
        String pkGeneratingSequenceName;
        if (pkGenerator != null
                && DbKeyGenerator.ORACLE_TYPE.equals(pkGenerator.getGeneratorType())
                && pkGenerator.getGeneratorName() != null)
            pkGeneratingSequenceName = pkGenerator.getGeneratorName();
        else
            pkGeneratingSequenceName = sequenceName(ent);

        Connection con = node.getDataSource().getConnection();
        try {
            Statement st = con.createStatement();
            try {
                String sql = "SELECT nextval('" + pkGeneratingSequenceName + "')";
                QueryLogger.logQuery(sql, Collections.EMPTY_LIST);
                ResultSet rs = st.executeQuery(sql);
                try {
                    // Object pk = null;
                    if (!rs.next()) {
                        throw new CayenneRuntimeException(
                                "Error generating pk for DbEntity " + ent.getName());
                    }
                    return rs.getInt(1);
                }
                finally {
                    rs.close();
                }
            }
            finally {
                st.close();
            }
        }
        finally {
            con.close();
        }
    }

    /**
     * Fetches a list of existing sequences that might match Cayenne generated ones.
     */
    protected List getExistingSequences(DataNode node) throws SQLException {

        // check existing sequences
        Connection con = node.getDataSource().getConnection();

        try {
            Statement sel = con.createStatement();
            try {
                String sql = "SELECT relname FROM pg_class WHERE relkind='S'";
                QueryLogger.logQuery(sql, Collections.EMPTY_LIST);
                ResultSet rs = sel.executeQuery(sql);
                try {
                    List sequenceList = new ArrayList();
                    while (rs.next()) {
                        sequenceList.add(rs.getString(1));
                    }
                    return sequenceList;
                }
                finally {
                    rs.close();
                }
            }
            finally {
                sel.close();
            }
        }
        finally {
            con.close();
        }
    }
}