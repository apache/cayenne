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

package org.objectstyle.cayenne.dba.derby;

import java.sql.Types;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.trans.QualifierTranslator;
import org.objectstyle.cayenne.access.trans.QueryAssembler;
import org.objectstyle.cayenne.access.trans.TrimmingQualifierTranslator;
import org.objectstyle.cayenne.access.types.ByteType;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.access.types.ShortType;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;

/**
 * DbAdapter implementation for the <a href="http://incubator.apache.org/derby/"> Derby
 * RDBMS </a>. Sample <a target="_top"
 * href="../../../../../../../developerguide/unit-tests.html">connection settings </a> to
 * use with Derby are shown below.
 * <h3>Embedded</h3>
 * 
 * <pre>
 *  test-derby.cayenne.adapter = org.objectstyle.cayenne.dba.derby.DerbyAdapter
 *  test-derby.jdbc.url = jdbc:derby:testdb;create=true
 *  test-derby.jdbc.driver = org.apache.derby.jdbc.EmbeddedDriver
 * </pre>
 * 
 * <h3>Network Server</h3>
 * 
 * <pre>
 *  derbynet.cayenne.adapter = org.objectstyle.cayenne.dba.derby.DerbyAdapter
 *  derbynet.jdbc.url = jdbc:derby://localhost/cayenne
 *  derbynet.jdbc.driver = org.apache.derby.jdbc.ClientDriver
 *  derbynet.jdbc.username = someuser
 *  derbynet.jdbc.password = secret;
 * </pre>
 */
public class DerbyAdapter extends JdbcAdapter {

    static final String FOR_BIT_DATA_SUFFIX = " FOR BIT DATA";

    public DerbyAdapter() {
        setSupportsGeneratedKeys(true);
    }

    protected PkGenerator createPkGenerator() {
        return new DerbyPkGenerator();
    }

    /**
     * Installs appropriate ExtendedTypes as converters for passing values between JDBC
     * and Java layers.
     */
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        map.registerType(new CharType(true, false));

        // address Derby driver inability to handle java.lang.Short and java.lang.Byte
        map.registerType(new ShortType(true));
        map.registerType(new ByteType(true));
    }

    /**
     * Appends SQL for column creation to CREATE TABLE buffer. Only change for Derby is
     * that " NULL" is not supported.
     * 
     * @since 1.2
     */
    protected void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {

        String[] types = externalTypesForJdbcType(column.getType());
        if (types == null || types.length == 0) {
            String entityName = column.getEntity() != null ? ((DbEntity) column
                    .getEntity()).getFullyQualifiedName() : "<null>";
            throw new CayenneRuntimeException("Undefined type for attribute '"
                    + entityName
                    + "."
                    + column.getName()
                    + "': "
                    + column.getType());
        }

        String type = types[0];

        String length = "";
        if (typeSupportsLength(column.getType())) {
            int len = column.getMaxLength();
            int prec = TypesMapping.isDecimal(column.getType())
                    ? column.getPrecision()
                    : -1;

            // sanity check
            if (prec > len) {
                prec = -1;
            }

            if (len > 0) {
                length = " (" + len;

                if (prec >= 0) {
                    length += ", " + prec;
                }

                length += ")";
            }
        }

        // assemble...
        // note that max length for types like XYZ FOR BIT DATA must be entered in the
        // middle of type name, e.g. VARCHAR (100) FOR BIT DATA.

        sqlBuffer.append(column.getName()).append(' ');
        if (length.length() > 0 && type.endsWith(FOR_BIT_DATA_SUFFIX)) {
            sqlBuffer.append(type.substring(0, type.length()
                    - FOR_BIT_DATA_SUFFIX.length()));
            sqlBuffer.append(length);
            sqlBuffer.append(FOR_BIT_DATA_SUFFIX);
        }
        else {
            sqlBuffer.append(type).append(length);
        }

        if (column.isMandatory()) {
            sqlBuffer.append(" NOT NULL");
        }

        if (column.isGenerated()) {
            sqlBuffer.append(" GENERATED BY DEFAULT AS IDENTITY");
        }
    }
    
    private boolean typeSupportsLength(int type) {
        // "BLOB" and "CLOB" type support length. default length is 1M.
        switch (type) {
            case Types.BLOB:
            case Types.CLOB:
                return true;
            default:
                return TypesMapping.supportsLength(type);
        }
    }

    /**
     * Returns a trimming translator.
     */
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new TrimmingQualifierTranslator(queryAssembler, "RTRIM");
    }

}