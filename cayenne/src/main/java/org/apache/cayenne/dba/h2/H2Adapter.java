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

package org.apache.cayenne.dba.h2;

import org.apache.cayenne.dba.NativeColumnType;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.EJBQLTranslator;
import org.apache.cayenne.access.translator.ejbql.JdbcEJBQLTranslator;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.JsonType;
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
 * DbAdapter implementation for H2 RDBMS.
 * 
 * @since 3.0
 */
public class H2Adapter extends JdbcAdapter {
    public H2Adapter(@Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, valueObjectTypeRegistry);
        setSupportsGeneratedKeys(true);
    }

    @Override
    protected NativeColumnType[] createNativeTypes() {
        return new NativeColumnType[]{
            NativeColumnType.of(Types.ARRAY, "ARRAY"),
            NativeColumnType.of(Types.BIGINT, "BIGINT"),
            NativeColumnType.of(Types.BINARY, "BINARY"),
            NativeColumnType.of(Types.BIT, "BIT"),
            NativeColumnType.of(Types.BLOB, "BLOB"),
            NativeColumnType.of(Types.BOOLEAN, "BOOLEAN"),
            NativeColumnType.of(Types.CHAR, "CHAR").asUnconstrained(),
            NativeColumnType.of(Types.CLOB, "CLOB"),
            NativeColumnType.of(Types.DATALINK, "DATALINK"),
            NativeColumnType.of(Types.DATE, "DATE"),
            NativeColumnType.of(Types.DECIMAL, "DECIMAL"),
            NativeColumnType.of(Types.DOUBLE, "DOUBLE"),
            NativeColumnType.of(Types.FLOAT, "FLOAT"),
            NativeColumnType.of(Types.INTEGER, "INTEGER"),
            NativeColumnType.of(Types.JAVA_OBJECT, "JAVA_OBJECT"),
            NativeColumnType.of(Types.LONGNVARCHAR, "NCLOB"),
            NativeColumnType.of(Types.LONGVARBINARY, "LONGVARBINARY"),
            NativeColumnType.of(Types.LONGVARCHAR, "CLOB"),
            NativeColumnType.of(Types.NCHAR, "NCHAR").asUnconstrained(),
            NativeColumnType.of(Types.NCLOB, "NCLOB"),
            NativeColumnType.of(Types.NUMERIC, "NUMERIC"),
            NativeColumnType.of(Types.NVARCHAR, "NVARCHAR").asUnconstrained(),
            NativeColumnType.of(Types.OTHER, "OTHER"),
            NativeColumnType.of(Types.REAL, "REAL"),
            NativeColumnType.of(Types.REF, "REF"),
            NativeColumnType.of(Types.SMALLINT, "SMALLINT"),
            NativeColumnType.of(Types.SQLXML, "NCLOB"),
            NativeColumnType.of(Types.STRUCT, "STRUCT"),
            NativeColumnType.of(Types.TIME, "TIME"),
            NativeColumnType.of(Types.TIMESTAMP, "TIMESTAMP"),
            NativeColumnType.of(Types.TINYINT, "TINYINT"),
            NativeColumnType.of(Types.VARBINARY, "VARBINARY"),
            NativeColumnType.of(Types.VARCHAR, "VARCHAR").asUnconstrained(),
        };
    }

    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        super.createTableAppendColumn(sqlBuffer, column);

        if (column.isGenerated()) {
            sqlBuffer.append(" AUTO_INCREMENT");
        }
    }

    /**
     * @since 4.2
     */
    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return new H2SQLTreeProcessor();
    }

    /**
     * @return translator factory for EJBQL queries
     * @since 5.0
     */
    @Override
    protected EJBQLTranslator createEJBQLTranslator() {
        JdbcEJBQLTranslator translatorFactory = new H2EJBQLTranslator();
        translatorFactory.setCaseInsensitive(caseInsensitiveCollations);
        return translatorFactory;
    }

    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new H2ActionBuilder(node));
    }

    /**
     * Installs appropriate ExtendedTypes as converters for passing values
     * between JDBC and Java layers.
     * @since 4.1.2
     */
    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        H2CharType charType = new H2CharType();
        map.registerType(charType);

        map.registerType(new JsonType(charType, false));
    }

    @Override
    public DbAttribute buildAttribute(String name, String typeName, int type, int maxLength, int scale, boolean allowNulls) {
        if ("json".equalsIgnoreCase(typeName)) {
            type = Types.OTHER;
        }
        return super.buildAttribute(name, typeName, type, maxLength, scale, allowNulls);
    }


}
