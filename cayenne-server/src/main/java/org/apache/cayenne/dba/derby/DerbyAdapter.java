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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslatorFactory;
import org.apache.cayenne.access.translator.ejbql.JdbcEJBQLTranslatorFactory;
import org.apache.cayenne.access.translator.select.QualifierTranslator;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.access.types.ByteType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.ShortType;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.resource.ResourceLocator;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

/**
 * DbAdapter implementation for the <a href="http://db.apache.org/derby/"> Derby RDBMS
 * </a>. Sample connection settings to use with Derby are shown below. <h3>Embedded</h3>
 * 
 * <pre>
 *  test-derby.jdbc.url = jdbc:derby:testdb;create=true
 *  test-derby.jdbc.driver = org.apache.derby.jdbc.EmbeddedDriver
 * </pre>
 * 
 * <h3>Network Server</h3>
 * 
 * <pre>
 *  derbynet.jdbc.url = jdbc:derby://localhost/cayenne
 *  derbynet.jdbc.driver = org.apache.derby.jdbc.ClientDriver
 *  derbynet.jdbc.username = someuser
 *  derbynet.jdbc.password = secret;
 * </pre>
 */
public class DerbyAdapter extends JdbcAdapter {

    static final String FOR_BIT_DATA_SUFFIX = " FOR BIT DATA";

    public DerbyAdapter(
            @Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject(Constants.SERVER_RESOURCE_LOCATOR) ResourceLocator resourceLocator) {
        super(
                runtimeProperties,
                defaultExtendedTypes,
                userExtendedTypes,
                extendedTypeFactories,
                resourceLocator);
        setSupportsGeneratedKeys(true);
        setSupportsBatchUpdates(true);
    }

    @Override
    protected PkGenerator createPkGenerator() {
        return new DerbyPkGenerator(this);
    }

    /**
     * Installs appropriate ExtendedTypes as converters for passing values between JDBC
     * and Java layers.
     */
    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        map.registerType(new CharType(true, true));

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
    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {

        String[] types = externalTypesForJdbcType(column.getType());
        if (types == null || types.length == 0) {
            String entityName = column.getEntity() != null ? ((DbEntity) column
                    .getEntity()).getFullyQualifiedName() : "<null>";
            throw new CayenneRuntimeException("Undefined type for attribute '"
                    + entityName + "." + column.getName() + "': " + column.getType());
        }



        sqlBuffer.append(quotingStrategy.quotedName(column));
        sqlBuffer.append(' ');

        String type = types[0];
        String length = sizeAndPrecision(this, column);

        // assemble...
        // note that max length for types like XYZ FOR BIT DATA must be entered in the
        // middle of type name, e.g. VARCHAR (100) FOR BIT DATA.
        int suffixIndex = type.indexOf(FOR_BIT_DATA_SUFFIX);
        if (!length.isEmpty() && suffixIndex > 0) {
            sqlBuffer.append(type.substring(0, suffixIndex)).append(length).append(FOR_BIT_DATA_SUFFIX);
        } else {
            sqlBuffer.append(type).append(" ").append(length);
        }

        if (column.isMandatory()) {
            sqlBuffer.append(" NOT NULL");
        }

        if (column.isGenerated()) {
            sqlBuffer.append(" GENERATED BY DEFAULT AS IDENTITY");
        }
    }

    @Override
    public boolean typeSupportsLength(int type) {
        // "BLOB" and "CLOB" type support length. default length is 1M.
        switch (type) {
            case Types.BLOB:
            case Types.CLOB:
            case Types.NCLOB:
                return true;
            default:
                return super.typeSupportsLength(type);
        }
    }

    /**
     * Returns a trimming translator.
     */
    @Override
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        QualifierTranslator translator = new DerbyQualifierTranslator(
                queryAssembler,
                "RTRIM");
        translator.setCaseInsensitive(caseInsensitiveCollations);
        return translator;
    }
    
    /**
     * @since 3.1
     */
    @Override
    protected EJBQLTranslatorFactory createEJBQLTranslatorFactory() {
        JdbcEJBQLTranslatorFactory translatorFactory = new DerbyEJBQLTranslatorFactory();
        translatorFactory.setCaseInsensitive(caseInsensitiveCollations);
        return translatorFactory;
    }

    @Override
    public MergerFactory mergerFactory() {
        return new DerbyMergerFactory();
    }

    @Override
    public void bindParameter(
            PreparedStatement statement,
            Object object,
            int pos,
            int sqlType,
            int precision) throws SQLException, Exception {

        if (object == null && sqlType == 0) {
            statement.setNull(pos, Types.VARCHAR);
        } else {
            super.bindParameter(statement, object, pos, convertNTypes(sqlType), precision);
        }
    }

    private int convertNTypes(int sqlType) {
        switch (sqlType) {
            case Types.NCHAR: return Types.CHAR;
            case Types.NVARCHAR: return Types.VARCHAR;
            case Types.LONGNVARCHAR: return Types.LONGVARCHAR;
            case Types.NCLOB: return Types.CLOB;

            default:
                return sqlType;
        }
    }

}
