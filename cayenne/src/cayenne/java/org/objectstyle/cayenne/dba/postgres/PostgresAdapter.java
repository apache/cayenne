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
package org.objectstyle.cayenne.dba.postgres;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.trans.QualifierTranslator;
import org.objectstyle.cayenne.access.trans.QueryAssembler;
import org.objectstyle.cayenne.access.types.ByteArrayType;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbEntity;

/**
 * DbAdapter implementation for <a href="http://www.postgresql.org">PostgreSQL RDBMS</a>.
 * Sample <a target="_top" href="../../../../../../../developerguide/unit-tests.html">connection 
 * settings</a> to use with PostgreSQL are shown below:
 * 
<pre>
test-postgresql.cayenne.adapter = org.objectstyle.cayenne.dba.postgres.PostgresAdapter
test-postgresql.jdbc.username = test
test-postgresql.jdbc.password = secret
test-postgresql.jdbc.url = jdbc:postgresql://serverhostname/cayenne
test-postgresql.jdbc.driver = org.postgresql.Driver
</pre>
 * 
 * @author Dirk Olmes
 * @author Holger Hoffstaette
 * @author Andrus Adamchik
 */
public class PostgresAdapter extends JdbcAdapter {
    
    /**
     * Installs appropriate ExtendedTypes as converters for passing values
     * between JDBC and Java layers.
     */
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        map.registerType(new CharType(true, false));

        // create specially configured ByteArrayType handler
        map.registerType(new PostgresByteArrayType(true, false));
    }
    
    public DbAttribute buildAttribute(
        String name,
        String typeName,
        int type,
        int size,
        int precision,
        boolean allowNulls) {

        // "bytea" maps to pretty much any binary type, so
        // it is up to us to select the most sensible default.
        // And the winner is LONGVARBINARY
        if ("bytea".equalsIgnoreCase(typeName)) {
            type = Types.LONGVARBINARY;
        }
        // somehow the driver reverse-engineers "text" as VARCHAR, must be CLOB
        else if("text".equalsIgnoreCase(typeName)) {
            type = Types.CLOB;
        }

        return super.buildAttribute(name, typeName, type, size, precision, allowNulls);
    }

    /**
     * Customizes table creating procedure for PostgreSQL. One difference
     * with generic implementation is that "bytea" type has no explicit length
     * unlike similar binary types in other databases.
     * 
     * @since 1.0.2
     */
    public String createTable(DbEntity ent) {

        // later we may support view creation
        // for derived DbEntities
        if (ent instanceof DerivedDbEntity) {
            throw new CayenneRuntimeException(
                "Can't create table for derived DbEntity '" + ent.getName() + "'.");
        }

        StringBuffer buf = new StringBuffer();
        buf.append("CREATE TABLE ").append(ent.getFullyQualifiedName()).append(" (");

        // columns
        Iterator it = ent.getAttributes().iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }

            DbAttribute at = (DbAttribute) it.next();

            // attribute may not be fully valid, do a simple check
            if (at.getType() == TypesMapping.NOT_DEFINED) {
                throw new CayenneRuntimeException(
                    "Undefined type for attribute '"
                        + ent.getFullyQualifiedName()
                        + "."
                        + at.getName()
                        + "'.");
            }

            String[] types = externalTypesForJdbcType(at.getType());
            if (types == null || types.length == 0) {
                throw new CayenneRuntimeException(
                    "Undefined type for attribute '"
                        + ent.getFullyQualifiedName()
                        + "."
                        + at.getName()
                        + "': "
                        + at.getType());
            }

            String type = types[0];
            buf.append(at.getName()).append(' ').append(type);

            // append size and precision (if applicable)
            if (typeSupportsLength(at.getType())) {
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
            } else {
                buf.append(" NULL");
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

    private boolean typeSupportsLength(int type) {
        // "bytea" type does not support length
        String[] externalTypes = externalTypesForJdbcType(type);
        if (externalTypes != null && externalTypes.length > 0) {
            for (int i = 0; i < externalTypes.length; i++) {
                if ("bytea".equalsIgnoreCase(externalTypes[i])) {
                    return false;
                }
            }
        }

        return TypesMapping.supportsLength(type);
    }

    /**
     * Adds the CASCADE option to the DROP TABLE clause.
     * @see JdbcAdapter#dropTable(DbEntity)
     */
    public String dropTable(DbEntity ent) {
        return super.dropTable(ent) + " CASCADE";
    }

    /**
     * Returns a trimming translator.
     */
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new PostgresQualifierTranslator(queryAssembler);
    }

    /**
     * @see JdbcAdapter#createPkGenerator()
     */
    protected PkGenerator createPkGenerator() {
        return new PostgresPkGenerator();
    }
    
    
    /**
     * PostgresByteArrayType is a byte[] type handler that patches the problem with PostgreSQL 
     * JDBC driver. Namely the fact that for some misterious reason PostgreSQL JDBC driver 
     * (as of 7.3.5) completely ignores the existence of LONGVARCHAR type. 
     * 
     * @since 1.0.4
     */
    class PostgresByteArrayType extends ByteArrayType {

        public PostgresByteArrayType(boolean trimmingBytes, boolean usingBlobs) {
            super(trimmingBytes, usingBlobs);
        }
        
        public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int precision)
            throws Exception {
                
            // patch PGSQL driver LONGVARBINARY ignorance 
            if (type == Types.LONGVARBINARY) {
                type = Types.VARBINARY;
            }

            super.setJdbcObject(st, val, pos, type, precision);
        }
    }
}
