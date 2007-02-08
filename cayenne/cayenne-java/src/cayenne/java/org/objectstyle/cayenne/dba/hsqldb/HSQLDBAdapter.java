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

package org.objectstyle.cayenne.dba.hsqldb;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.types.DefaultType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLAction;

/**
 * DbAdapter implementation for the <a href="http://hsqldb.sourceforge.net/"> HSQLDB RDBMS
 * </a>. Sample <a target="_top"
 * href="../../../../../../../developerguide/unit-tests.html">connection settings </a> to
 * use with HSQLDB are shown below:
 * 
 * <pre>
 *       
 *        test-hsqldb.cayenne.adapter = org.objectstyle.cayenne.dba.hsqldb.HSQLDBAdapter
 *        test-hsqldb.jdbc.username = test
 *        test-hsqldb.jdbc.password = secret
 *        test-hsqldb.jdbc.url = jdbc:hsqldb:hsql://serverhostname
 *        test-hsqldb.jdbc.driver = org.hsqldb.jdbcDriver
 *        
 * </pre>
 * 
 * @author Holger Hoffstaette
 */
public class HSQLDBAdapter extends JdbcAdapter {

    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);
        map.registerType(new ShortType());
        map.registerType(new ByteType());
    }

    /**
     * Generate fully-qualified name for 1.8 and on. Subclass generates unqualified name.
     * 
     * @since 1.2
     */
    protected String getTableName(DbEntity entity) {
        return entity.getFullyQualifiedName();
    }

    /**
     * Generate fully-qualified name for 1.8 and on. Subclass generates unqualified name.
     * 
     * @since 1.2
     */
    protected String getSchemaName(DbEntity entity) {
        if (entity.getSchema() != null && entity.getSchema().length() > 0) {
            return entity.getSchema() + ".";
        }

        return "";
    }

    /**
     * Uses special action builder to create the right action.
     * 
     * @since 1.2
     */
    public SQLAction getAction(Query query, DataNode node) {
        return query
                .createSQLAction(new HSQLActionBuilder(this, node.getEntityResolver()));
    }

    /**
     * Returns a DDL string to create a unique constraint over a set of columns.
     * 
     * @since 1.1
     */
    public String createUniqueConstraint(DbEntity source, Collection columns) {
        if (columns == null || columns.isEmpty()) {
            throw new CayenneRuntimeException(
                    "Can't create UNIQUE constraint - no columns specified.");
        }

        String srcName = getTableName(source);

        StringBuffer buf = new StringBuffer();

        buf.append("ALTER TABLE ").append(srcName);
        buf.append(" ADD CONSTRAINT ");

        buf.append(getSchemaName(source));
        buf.append("U_");
        buf.append(source.getName());
        buf.append("_");
        buf.append((long) (System.currentTimeMillis() / (Math.random() * 100000)));
        buf.append(" UNIQUE (");

        Iterator it = columns.iterator();
        DbAttribute first = (DbAttribute) it.next();
        buf.append(first.getName());

        while (it.hasNext()) {
            DbAttribute next = (DbAttribute) it.next();
            buf.append(", ");
            buf.append(next.getName());
        }

        buf.append(")");

        return buf.toString();
    }

    /**
     * Adds an ADD CONSTRAINT clause to a relationship constraint.
     * 
     * @see JdbcAdapter#createFkConstraint(DbRelationship)
     */
    public String createFkConstraint(DbRelationship rel) {
        StringBuffer buf = new StringBuffer();
        StringBuffer refBuf = new StringBuffer();

        String srcName = getTableName((DbEntity) rel.getSourceEntity());
        String dstName = getTableName((DbEntity) rel.getTargetEntity());

        buf.append("ALTER TABLE ");
        buf.append(srcName);

        // hsqldb requires the ADD CONSTRAINT statement
        buf.append(" ADD CONSTRAINT ");
        buf.append(getSchemaName((DbEntity) rel.getSourceEntity()));
        buf.append("C_");
        buf.append(rel.getSourceEntity().getName());
        buf.append("_");
        buf.append((long) (System.currentTimeMillis() / (Math.random() * 100000)));

        buf.append(" FOREIGN KEY (");

        Iterator jit = rel.getJoins().iterator();
        boolean first = true;
        while (jit.hasNext()) {
            DbJoin join = (DbJoin) jit.next();
            if (!first) {
                buf.append(", ");
                refBuf.append(", ");
            }
            else
                first = false;

            buf.append(join.getSourceName());
            refBuf.append(join.getTargetName());
        }

        buf.append(") REFERENCES ");
        buf.append(dstName);
        buf.append(" (");
        buf.append(refBuf.toString());
        buf.append(')');

        // also make sure we delete dependent FKs
        buf.append(" ON DELETE CASCADE");

        return buf.toString();
    }

    /**
     * Uses "CREATE CACHED TABLE" instead of "CREATE TABLE".
     * 
     * @since 1.2
     */
    public String createTable(DbEntity ent) {
        // SET SCHEMA <schemaname>

        String sql = super.createTable(ent);

        if (sql != null && sql.toUpperCase().startsWith("CREATE TABLE ")) {
            sql = "CREATE CACHED TABLE " + sql.substring("CREATE TABLE ".length());
        }

        return sql;
    }

    final class ShortType extends DefaultType {

        ShortType() {
            super(Short.class.getName());
        }

        public void setJdbcObject(
                PreparedStatement st,
                Object val,
                int pos,
                int type,
                int precision) throws Exception {

            if (val == null) {
                super.setJdbcObject(st, val, pos, type, precision);
            }
            else {

                short s = ((Number) val).shortValue();
                st.setShort(pos, s);
            }
        }
    }
    
    final class ByteType extends DefaultType {

        ByteType() {
            super(Byte.class.getName());
        }

        public void setJdbcObject(
                PreparedStatement st,
                Object val,
                int pos,
                int type,
                int precision) throws Exception {

            if (val == null) {
                super.setJdbcObject(st, val, pos, type, precision);
            }
            else {

                byte b = ((Number) val).byteValue();
                st.setByte(pos, b);
            }
        }
    }
}