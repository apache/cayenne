/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/


package org.apache.cayenne.dba.derby;

import java.sql.Types;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.trans.TrimmingQualifierTranslator;
import org.apache.cayenne.access.types.ByteType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.ShortType;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;

/**
 * DbAdapter implementation for the <a href="http://incubator.apache.org/derby/"> Derby
 * RDBMS </a>. Sample <a target="_top"
 * href="../../../../../../../developerguide/unit-tests.html">connection settings </a> to
 * use with Derby are shown below.
 * <h3>Embedded</h3>
 * 
 * <pre>
 *  test-derby.cayenne.adapter = org.apache.cayenne.dba.derby.DerbyAdapter
 *  test-derby.jdbc.url = jdbc:derby:testdb;create=true
 *  test-derby.jdbc.driver = org.apache.derby.jdbc.EmbeddedDriver
 * </pre>
 * 
 * <h3>Network Server</h3>
 * 
 * <pre>
 *  derbynet.cayenne.adapter = org.apache.cayenne.dba.derby.DerbyAdapter
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
        setSupportsBatchUpdates(true);
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
            int scale = TypesMapping.isDecimal(column.getType())
                    ? column.getScale()
                    : -1;

            // sanity check
            if (scale > len) {
                scale = -1;
            }

            if (len > 0) {
                length = " (" + len;

                if (scale >= 0) {
                    length += ", " + scale;
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
