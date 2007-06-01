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
package org.objectstyle.cayenne.dba.openbase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.trans.QualifierTranslator;
import org.objectstyle.cayenne.access.trans.QueryAssembler;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.DefaultType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbJoin;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DerivedDbEntity;

/**
 * DbAdapter implementation for <a href="http://www.openbase.com">OpenBase</a>.
 * Sample <a target="_top" href="../../../../../../../developerguide/unit-tests.html">connection 
 * settings</a> to use with OpenBase are shown below:
 * 
<pre>
test-openbase.cayenne.adapter = org.objectstyle.cayenne.dba.openbase.OpenBaseAdapter
test-openbase.jdbc.username = test
test-openbase.jdbc.password = secret
test-openbase.jdbc.url = jdbc:openbase://serverhostname/cayenne
test-openbase.jdbc.driver = com.openbase.jdbc.ObDriver
</pre>
 * 
 * @author <a href="mailto:mkienenb@alaska.net">Mike Kienenberger</a>
 * @author Andrei Adamchik
 * 
 * @since 1.1
 */
public class OpenBaseAdapter extends JdbcAdapter {
    private static Logger logObj = Logger.getLogger(OpenBaseAdapter.class);

    public OpenBaseAdapter() {
        // init defaults
        this.setSupportsUniqueConstraints(false);
    }

    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // Byte handling doesn't work on read... 
        // need special converter
        map.registerType(new OpenBaseByteType());

        map.registerType(new OpenBaseCharType());
    }

    public DbAttribute buildAttribute(
        String name,
        String typeName,
        int type,
        int size,
        int precision,
        boolean allowNulls) {

        // OpenBase makes no distinction between CHAR and VARCHAR
        // so lets use VARCHAR, since it seems more generic
        if (type == Types.CHAR) {
            type = Types.VARCHAR;
        }

        return super.buildAttribute(name, typeName, type, size, precision, allowNulls);
    }

    /**
     * Returns word "go".
     */
    public String getBatchTerminator() {
        return "go";
    }

    /** 
     * Returns null, since views are not yet supported in openbase.
     */
    public String tableTypeForView() {
        // TODO: according to OpenBase docs views *ARE* supported.
        return null;
    }

    /** 
     * Returns OpenBase-specific translator for queries.
     */
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new OpenBaseQualifierTranslator(queryAssembler);
    }

    /**
      * Creates and returns a primary key generator. Overrides superclass 
      * implementation to return an
      * instance of OpenBasePkGenerator that uses built-in multi-server primary key generation.
      */
    protected PkGenerator createPkGenerator() {
        return new OpenBasePkGenerator();
    }

    /**
      * Returns a SQL string that can be used to create database table
      * corresponding to <code>ent</code> parameter.
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
            }
            else {
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
            else {
                buf.append(" NULL");
            }
        }

        buf.append(')');
        return buf.toString();
    }

    /**
     * Returns a SQL string that can be used to create
     * a foreign key constraint for the relationship.
     */
    public String createFkConstraint(DbRelationship rel) {
        StringBuffer buf = new StringBuffer();

        // OpendBase Specifics is that we need to create a constraint going
        // from destination to source for this to work...

        DbEntity sourceEntity = (DbEntity) rel.getSourceEntity();
        DbEntity targetEntity = (DbEntity) rel.getTargetEntity();
        String toMany = (!rel.isToMany()) ? "'1'" : "'0'";

        // TODO: doesn't seem like OpenBase supports compound joins... 
        // need to doublecheck that

        int joinsLen = rel.getJoins().size();
        if (joinsLen == 0) {
            throw new CayenneRuntimeException(
                "Relationship has no joins: " + rel.getName());
        }
        else if (joinsLen > 1) {
            logObj.warn(
                "Only a single join relationships are supported by OpenBase. Ignoring extra joins.");
        }

        DbJoin join = (DbJoin) rel.getJoins().get(0);

        buf
            .append("INSERT INTO _SYS_RELATIONSHIP (")
            .append("dest_table, dest_column, source_table, source_column, ")
            .append("block_delete, cascade_delete, one_to_many, operator, relationshipName")
            .append(") VALUES ('")
            .append(sourceEntity.getFullyQualifiedName())
            .append("', '")
            .append(join.getSourceName())
            .append("', '")
            .append(targetEntity.getFullyQualifiedName())
            .append("', '")
            .append(join.getTargetName())
            .append("', 0, 0, ")
            .append(toMany)
            .append(", '=', '")
            .append(rel.getName())
            .append("')");

        return buf.toString();
    }

    // OpenBase JDBC driver has trouble reading "integer" as byte
    // this converter addresses such problem
    static class OpenBaseByteType extends DefaultType {
        OpenBaseByteType() {
            super(Byte.class.getName());
        }

        public Object materializeObject(ResultSet rs, int index, int type)
            throws Exception {

            // read value as int, and then narrow it down
            int val = rs.getInt(index);
            return (rs.wasNull()) ? null : new Byte((byte) val);
        }
    }

    static class OpenBaseCharType extends CharType {
        OpenBaseCharType() {
            super(false, true);
        }

        public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int precision)
            throws Exception {

            // These to types map to "text"; and when setting "text" as object
            // OB assumes that the object is the actual CLOB... weird
            if (type == Types.CLOB || type == Types.LONGVARCHAR) {
                st.setString(pos, (String) val);
            }
            else {
                super.setJdbcObject(st, val, pos, type, precision);
            }
        }
    }
}
