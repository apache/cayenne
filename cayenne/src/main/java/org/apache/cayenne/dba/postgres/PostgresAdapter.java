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

package org.apache.cayenne.dba.postgres;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.translator.procedure.ProcedureTranslator;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.JsonType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.ProcedureQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.resource.ResourceLocator;

import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * DbAdapter implementation for <a href="http://www.postgresql.org">PostgreSQL
 * RDBMS </a>. Sample connection settings to use with PostgreSQL are shown
 * below:
 *
 * <pre>
 *      postgres.jdbc.username = test
 *      postgres.jdbc.password = secret
 *      postgres.jdbc.url = jdbc:postgresql://serverhostname/cayenne
 *      postgres.jdbc.driver = org.postgresql.Driver
 * </pre>
 */
public class PostgresAdapter extends JdbcAdapter {

    public static final String BYTEA = "bytea";

    private static final List<String> SYSTEM_SCHEMAS = List.of("information_schema", "pg_catalog");

    public PostgresAdapter(@Inject RuntimeProperties runtimeProperties,
                           @Inject(Constants.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                           @Inject(Constants.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                           @Inject(Constants.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                           @Inject(Constants.RESOURCE_LOCATOR) ResourceLocator resourceLocator,
                           @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator, valueObjectTypeRegistry);
        setSupportsBatchUpdates(true);
        setSupportsGeneratedKeys(true);
    }

    /**
     * @since 4.2
     */
    @Override
    public SQLTreeProcessor getSqlTreeProcessor() {
        return new PostgreSQLTreeProcessor();
    }

    /**
     * Uses PostgresActionBuilder to create the right action.
     *
     * @since 1.2
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new PostgresActionBuilder(node));
    }

    /**
     * @since 5.0
     */
    @Override
    public ProcedureTranslator getProcedureTranslator(ProcedureQuery query, EntityResolver entityResolver) {
        return new PostgresProcedureTranslator();
    }

    /**
     * Installs appropriate ExtendedTypes as converters for passing values
     * between JDBC and Java layers.
     */
    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {

        super.configureExtendedTypes(map);

        CharType charType = new CharType(true, false);
        map.registerType(charType);
        map.registerType(new PostgresByteArrayType(true, true));
        map.registerType(new JsonType(charType, false));
    }

    @Override
    public DbAttribute buildAttribute(String name, String typeName, int type, int size, int scale, boolean allowNulls) {

        int amendedType = switch (typeName != null ? typeName.toLowerCase() : "") {
            case "json" -> Types.OTHER;
            // "bytea" maps to pretty much any binary type, so it is up to us to select the most sensible default.
            // And the winner is LONGVARBINARY
            case BYTEA -> Types.LONGVARBINARY;
            // oid is returned as INTEGER, need to make it BLOB
            case "oid" -> Types.BLOB;
            default -> type;
        };

        return super.buildAttribute(name, typeName, amendedType, size, scale, allowNulls);
    }

    @Override
    public int preferredBindingType(int jdbcType) {
        return switch (jdbcType) {
            case Types.NCHAR -> Types.CHAR;
            case Types.NCLOB -> Types.CLOB;
            case Types.NVARCHAR -> Types.VARCHAR;
            case Types.LONGNVARCHAR -> Types.LONGVARCHAR;
            default -> jdbcType;
        };
    }

    /**
     * Customizes table creating procedure for PostgreSQL. One difference with
     * generic implementation is that "bytea" type has no explicit length unlike
     * similar binary types in other databases.
     *
     * @since 1.0.2
     */
    @Override
    public String createTable(DbEntity ent) {

        QuotingStrategy context = getQuotingStrategy();
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE TABLE ").append(context.quotedFullyQualifiedName(ent)).append(" (");

        // columns
        Iterator<DbAttribute> it = ent.getAttributes().iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }

            createAttribute(ent, context, buf, it.next());
        }

        // primary key clause
        Iterator<DbAttribute> pkit = ent.getPrimaryKeys().iterator();
        if (pkit.hasNext()) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }

            buf.append("PRIMARY KEY (");
            boolean firstPk = true;
            while (pkit.hasNext()) {
                if (firstPk) {
                    firstPk = false;
                } else {
                    buf.append(", ");
                }

                DbAttribute at = pkit.next();
                buf.append(context.quotedName(at));
            }
            buf.append(')');
        }
        buf.append(')');
        return buf.toString();
    }

    private void createAttribute(DbEntity ent, QuotingStrategy context, StringBuilder buf, DbAttribute at) {
        // attribute may not be fully valid, do a simple check
        if (at.getType() == TypesMapping.NOT_DEFINED) {
            throw new CayenneRuntimeException("Undefined type for attribute '%s.%s'"
                    , ent.getFullyQualifiedName(), at.getName());
        }

        String[] types = externalTypesForJdbcType(at.getType());
        if (types == null || types.length == 0) {
            throw new CayenneRuntimeException("Undefined type for attribute '%s.%s': %s"
                    , ent.getFullyQualifiedName(), at.getName(), at.getType());
        }

        // Checking that attribute is generated and we have alternative types in types.xml.
        // If so, use those autoincremented types. For example serial, bigserial, smallserial.
        String type = (at.isGenerated() && types.length > 1) ? types[1] : types[0];

        buf.append(context.quotedName(at)).append(' ').append(type).append(sizeAndPrecision(this, at))
                .append(at.isMandatory() ? " NOT" : "").append(" NULL");
    }

    @Override
    public boolean typeSupportsLength(int type) {
        // "bytea" type does not support length
        if (Types.DOUBLE == type || Types.REAL == type) {
            return false;
        }
        String[] externalTypes = externalTypesForJdbcType(type);
        if (externalTypes != null && externalTypes.length > 0) {
            for (String externalType : externalTypes) {
                if (BYTEA.equalsIgnoreCase(externalType)) {
                    return false;
                }
            }
        }

        return super.typeSupportsLength(type);
    }

    /**
     * Adds the CASCADE option to the DROP TABLE clause.
     */
    @Override
    public Collection<String> dropTableStatements(DbEntity table) {
        QuotingStrategy context = getQuotingStrategy();
        return Collections.singleton("DROP TABLE " + context.quotedFullyQualifiedName(table) + " CASCADE");
    }

    @Override
    public boolean supportsCatalogsOnReverseEngineering() {
        return false;
    }

    @Override
    public List<String> getSystemSchemas() {
        return SYSTEM_SCHEMAS;
    }

}
