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

package org.objectstyle.cayenne.dba.oracle;

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
import org.objectstyle.cayenne.map.DbKeyGenerator;

/**
 * Sequence-based primary key generator implementation for Oracle.
 * Uses Oracle sequences to generate primary key values. This approach is
 * at least 50% faster when tested with Oracle compared to the lookup table
 * approach.
 *
 * <p>When using Cayenne key caching mechanism, make sure that sequences in
 * the database have "INCREMENT BY" greater or equal to OraclePkGenerator
 * "pkCacheSize" property value. If this is not the case, you will need to
 * adjust PkGenerator value accordingly. For example when sequence is
 * incremented by 1 each time, use the following code:</p>
 *
 * <pre>
 * dataNode.getAdapter().getPkGenerator().setPkCacheSize(1);
 * </pre>
 *
 * @author Andrei Adamchik
 */
public class OraclePkGenerator extends JdbcPkGenerator {
    private static final String _SEQUENCE_PREFIX = "pk_";

    public void createAutoPk(DataNode node, List dbEntities) throws Exception {
        List sequences = getExistingSequences(node);

        // create needed sequences
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            if (!sequences.contains(sequenceName(ent))) {
                runUpdate(node, createSequenceString(ent));
            }
        }
    }

    public List createAutoPkStatements(List dbEntities) {
        List list = new ArrayList();
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            list.add(createSequenceString(ent));
        }

        return list;
    }

    public void dropAutoPk(DataNode node, List dbEntities) throws Exception {
        List sequences = getExistingSequences(node);

        // drop obsolete sequences
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            if (sequences.contains(stripSchemaName(sequenceName(ent)))) {
                runUpdate(node, dropSequenceString(ent));
            }
        }
    }

    public List dropAutoPkStatements(List dbEntities) {
        List list = new ArrayList();
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            list.add(dropSequenceString(ent));
        }

        return list;
    }

    protected String createSequenceString(DbEntity ent) {
        StringBuffer buf = new StringBuffer();
        buf
            .append("CREATE SEQUENCE ")
            .append(sequenceName(ent))
            .append(" START WITH 200")
            .append(" INCREMENT BY ")
            .append(pkCacheSize(ent));
        return buf.toString();
    }

    /**
     * Returns a SQL string needed to drop any database objects associated
     * with automatic primary key generation process for a specific DbEntity.
     */
    protected String dropSequenceString(DbEntity ent) {
        StringBuffer buf = new StringBuffer();
        buf.append("DROP SEQUENCE ").append(sequenceName(ent));
        return buf.toString();
    }

    /**
     * Generates primary key by calling Oracle sequence corresponding to the
     * <code>dbEntity</code>. Executed SQL looks like this:
     *
     * <pre>
     * SELECT pk_table_name.nextval FROM DUAL
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
                String sql = "SELECT " + pkGeneratingSequenceName + ".nextval FROM DUAL";
                QueryLogger.logQuery(
                    QueryLogger.DEFAULT_LOG_LEVEL,
                    sql,
                    Collections.EMPTY_LIST);
                ResultSet rs = st.executeQuery(sql);
                try {
                    //Object pk = null;
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

    protected int pkCacheSize(DbEntity entity) {
        // use custom generator if possible
        DbKeyGenerator keyGenerator = entity.getPrimaryKeyGenerator();
        if (keyGenerator != null
            && DbKeyGenerator.ORACLE_TYPE.equals(keyGenerator.getGeneratorType())
            && keyGenerator.getGeneratorName() != null) {

            Integer size = keyGenerator.getKeyCacheSize();
            return (size != null && size.intValue() >= 1)
                ? size.intValue()
                : super.getPkCacheSize();
        }
        else {
            return super.getPkCacheSize();
        }
    }

    /** Returns expected primary key sequence name for a DbEntity. */
    protected String sequenceName(DbEntity entity) {

        // use custom generator if possible
        DbKeyGenerator keyGenerator = entity.getPrimaryKeyGenerator();
        if (keyGenerator != null
            && DbKeyGenerator.ORACLE_TYPE.equals(keyGenerator.getGeneratorType())
            && keyGenerator.getGeneratorName() != null) {

            return keyGenerator.getGeneratorName().toLowerCase();
        }
        else {
            String entName = entity.getName();
            String seqName = _SEQUENCE_PREFIX + entName.toLowerCase();

            if (entity.getSchema() != null && entity.getSchema().length() > 0) {
                seqName = entity.getSchema() + "." + seqName;
            }
            return seqName;
        }
    }

    protected String stripSchemaName(String sequenceName) {
        int ind = sequenceName.indexOf('.');
        return (ind >= 0) ? sequenceName.substring(ind + 1) : sequenceName;
    }

    /**
     * Fetches a list of existing sequences that might match Cayenne
     * generated ones.
     */
    protected List getExistingSequences(DataNode node) throws SQLException {

        // check existing sequences
        Connection con = node.getDataSource().getConnection();

        try {
            Statement sel = con.createStatement();
            try {
                String sql = "SELECT LOWER(SEQUENCE_NAME) FROM ALL_SEQUENCES";
                QueryLogger.logQuery(
                    QueryLogger.DEFAULT_LOG_LEVEL,
                    sql,
                    Collections.EMPTY_LIST);
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
