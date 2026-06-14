/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dba.firebird;

import org.apache.cayenne.dba.NativeColumnType;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.ejbql.EJBQLTranslator;
import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

import java.sql.Types;
import java.util.List;

/**
 * DbAdapter implementation for the FirebirdSQL RDBMS
 */
public class FirebirdAdapter extends JdbcAdapter {

    private static final String NCHAR_SUFFIX = " CHARACTER SET UNICODE_FSS";

    public FirebirdAdapter(@Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(
                runtimeProperties,
                defaultExtendedTypes,
                userExtendedTypes,
                extendedTypeFactories,
                valueObjectTypeRegistry);
	    setSupportsBatchUpdates(true);
    }

    @Override
    protected NativeColumnType[] createNativeTypes() {
        return new NativeColumnType[]{
            NativeColumnType.of(Types.ARRAY, "BLOB"),
            NativeColumnType.of(Types.BIGINT, "BIGINT"),
            NativeColumnType.of(Types.BINARY, "BLOB"),
            NativeColumnType.of(Types.BIT, "SMALLINT"),
            NativeColumnType.of(Types.BLOB, "BLOB"),
            NativeColumnType.of(Types.BOOLEAN, "SMALLINT"),
            NativeColumnType.of(Types.CHAR, "VARCHAR"),
            NativeColumnType.of(Types.CLOB, "BLOB SUB_TYPE TEXT"),
            NativeColumnType.of(Types.DATALINK, "BLOB"),
            NativeColumnType.of(Types.DATE, "DATE"),
            NativeColumnType.of(Types.DECIMAL, "DECIMAL"),
            NativeColumnType.of(Types.DOUBLE, "DOUBLE PRECISION"),
            NativeColumnType.of(Types.FLOAT, "DOUBLE PRECISION"),
            NativeColumnType.of(Types.INTEGER, "INTEGER"),
            NativeColumnType.of(Types.JAVA_OBJECT, "BLOB"),
            NativeColumnType.of(Types.LONGNVARCHAR, "BLOB SUB_TYPE TEXT"),
            NativeColumnType.of(Types.LONGVARBINARY, "BLOB"),
            NativeColumnType.of(Types.LONGVARCHAR, "BLOB SUB_TYPE TEXT"),
            NativeColumnType.of(Types.NCHAR, "CHAR CHARACTER SET UNICODE_FSS"),
            NativeColumnType.of(Types.NCLOB, "BLOB SUB_TYPE TEXT"),
            NativeColumnType.of(Types.NUMERIC, "DECIMAL"),
            NativeColumnType.of(Types.NVARCHAR, "VARCHAR CHARACTER SET UNICODE_FSS"),
            NativeColumnType.of(Types.OTHER, "BLOB"),
            NativeColumnType.of(Types.REAL, "REAL"),
            NativeColumnType.of(Types.REF, "BLOB"),
            NativeColumnType.of(Types.SMALLINT, "SMALLINT"),
            NativeColumnType.of(Types.STRUCT, "BLOB"),
            NativeColumnType.of(Types.TIME, "TIME"),
            NativeColumnType.of(Types.TIMESTAMP, "TIMESTAMP"),
            NativeColumnType.of(Types.TINYINT, "SMALLINT"),
            NativeColumnType.of(Types.VARBINARY, "BLOB"),
            NativeColumnType.of(Types.VARCHAR, "VARCHAR"),
        };
    }
    
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);
        // handling internaly binary types as blobs or clobs generates exceptions
        // Blob.length() and Clob.length() methods are optional (http://docs.oracle.com/javase/7/docs/api/java/sql/Clob.html#length())
        // and firebird driver doesn't support them.
        map.registerType(new ByteArrayType(true, false));
        map.registerType(new CharType(true, false));
        
    }

    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        String type = preferredNativeColumnType(column).nativeType();
        String length = sizeAndScale(this, column);

        sqlBuffer.append(quotingStrategy.quotedName(column));
        sqlBuffer.append(' ');

        int suffixIndex = type.indexOf(NCHAR_SUFFIX);
        if (!length.isEmpty() && suffixIndex > 0) {
            sqlBuffer.append(type.substring(0, suffixIndex)).append(length).append(NCHAR_SUFFIX);
        } else {
            sqlBuffer.append(type).append(" ").append(length);
        }

        sqlBuffer.append(column.isMandatory() ? " NOT NULL" : "");
    }

    /**
     * @since 4.2
     */
    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return new FirebirdSQLTreeProcessor();
    }

    /**
     * @since 5.0
     */
    @Override
    public EJBQLTranslator getEjbqlTranslator() {
        return new FirebirdEJBQLTranslator();
    }

    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new FirebirdActionBuilder(node));
    }
}
