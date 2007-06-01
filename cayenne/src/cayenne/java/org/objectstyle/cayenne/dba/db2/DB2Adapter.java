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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.trans.QualifierTranslator;
import org.objectstyle.cayenne.access.trans.QueryAssembler;
import org.objectstyle.cayenne.access.types.AbstractType;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.validation.ValidationResult;

/**
 * DbAdapter implementation for the <a href="http://www.ibm.com/db2/"> DB2 RDBMS </a>.
 * Sample <a target="_top"
 * href="../../../../../../../developerguide/unit-tests.html">connection settings </a> to
 * use with DB2 are shown below:
 * 
 * <pre>
 * 
 *  test-db2.cayenne.adapter = org.objectstyle.cayenne.dba.db2.DB2Adapter
 *  test-db2.jdbc.username = test
 *  test-db2.jdbc.password = secret
 *  test-db2.jdbc.url = jdbc:db2://servername:50000/databasename
 *  test-db2.jdbc.driver = com.ibm.db2.jcc.DB2Driver
 *  
 * </pre>
 * 
 * @author Holger Hoffstaette
 */
public class DB2Adapter extends JdbcAdapter {

    public DB2Adapter() {
    }

    /**
     * Creates a DB2 specific PK Generator.
     */
    protected PkGenerator createPkGenerator() {
        return new DB2PkGenerator();
    }

    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        map.registerType(new CharType(true, true));

        // configure boolean type to work with numeric columns
        map.registerType(new DB2BooleanType());
    }

    /**
     * Returns a SQL string that can be used to create database table corresponding to
     * <code>ent</code> parameter.
     */
    public String createTable(DbEntity ent) {
        // later we may support view creation
        // for derived DbEntities
        if (ent instanceof DerivedDbEntity) {
            throw new CayenneRuntimeException("Can't create table for derived DbEntity '"
                    + ent.getName()
                    + "'.");
        }

        StringBuffer buf = new StringBuffer();
        buf.append("CREATE TABLE ").append(ent.getFullyQualifiedName()).append(" (");

        // columns
        Iterator it = ent.getAttributes().iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first) {
                first = false;
            }
            else {
                buf.append(", ");
            }

            DbAttribute at = (DbAttribute) it.next();

            // attribute may not be fully valid, do a simple check
            if (at.getType() == TypesMapping.NOT_DEFINED) {
                throw new CayenneRuntimeException("Undefined type for attribute '"
                        + ent.getFullyQualifiedName()
                        + "."
                        + at.getName()
                        + "'.");
            }

            String[] types = externalTypesForJdbcType(at.getType());
            if (types == null || types.length == 0) {
                throw new CayenneRuntimeException("Undefined type for attribute '"
                        + ent.getFullyQualifiedName()
                        + "."
                        + at.getName()
                        + "': "
                        + at.getType());
            }

            String type = types[0];
            buf.append(at.getName()).append(' ').append(type);

            // append size and precision (if applicable)
            if (TypesMapping.supportsLength(at.getType())) {
                int len = at.getMaxLength();
                int prec = TypesMapping.isDecimal(at.getType()) ? at.getPrecision() : -1;

                // sanity check
                if (prec > len) {
                    prec = -1;
                }

                if (len > 0) {
                    buf.append('(').append(len);

                    if (prec >= 0) {
                        buf.append(", ").append(prec);
                    }

                    buf.append(')');
                }
            }

            if (at.isMandatory()) {
                buf.append(" NOT NULL");
            }
        }

        // primary key clause
        Iterator pkit = ent.getPrimaryKey().iterator();
        if (pkit.hasNext()) {
            if (first)
                first = false;
            else
                buf.append(", ");

            buf.append("PRIMARY KEY (");
            boolean firstPk = true;
            while (pkit.hasNext()) {
                if (firstPk)
                    firstPk = false;
                else
                    buf.append(", ");

                DbAttribute at = (DbAttribute) pkit.next();
                buf.append(at.getName());
            }
            buf.append(')');
        }
        buf.append(')');
        return buf.toString();
    }

    /**
     * Returns a trimming translator.
     */
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new DB2QualifierTranslator(queryAssembler, "RTRIM");
    }

    final class DB2BooleanType extends AbstractType {

        public String getClassName() {
            return Boolean.class.getName();
        }

        /**
         * @since 1.1
         */
        public boolean validateProperty(
                Object source,
                String property,
                Object value,
                DbAttribute dbAttribute,
                ValidationResult validationResult) {
            return true;
        }

        public Object materializeObject(ResultSet rs, int index, int type)
                throws Exception {
            boolean b = rs.getBoolean(index);
            return (rs.wasNull()) ? null : b ? Boolean.TRUE : Boolean.FALSE;
        }

        public Object materializeObject(CallableStatement st, int index, int type)
                throws Exception {
            boolean b = st.getBoolean(index);
            return (st.wasNull()) ? null : b ? Boolean.TRUE : Boolean.FALSE;
        }

        public void setJdbcObject(
                PreparedStatement st,
                Object val,
                int pos,
                int type,
                int precision) throws Exception {

            if (val != null) {
                st.setInt(pos, ((Boolean) val).booleanValue() ? 1 : 0);
            }
            else {
                st.setNull(pos, type);
            }
        }
    }

}