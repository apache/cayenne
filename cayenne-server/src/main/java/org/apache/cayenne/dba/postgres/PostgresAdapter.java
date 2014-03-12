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

package org.apache.cayenne.dba.postgres;

import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.resource.ResourceLocator;

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

    public PostgresAdapter(@Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ResourceLocator resourceLocator) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator);
        setSupportsBatchUpdates(true);
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
     * Installs appropriate ExtendedTypes as converters for passing values
     * between JDBC and Java layers.
     */
    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {

        super.configureExtendedTypes(map);

        map.registerType(new CharType(true, false));
        map.registerType(new PostgresByteArrayType(true, true));
    }

    @Override
    public DbAttribute buildAttribute(String name, String typeName, int type, int size, int scale, boolean allowNulls) {

        // "bytea" maps to pretty much any binary type, so
        // it is up to us to select the most sensible default.
        // And the winner is LONGVARBINARY
        if ("bytea".equalsIgnoreCase(typeName)) {
            type = Types.LONGVARBINARY;
        }
        // oid is returned as INTEGER, need to make it BLOB
        else if ("oid".equals(typeName)) {
            type = Types.BLOB;
        }
        // somehow the driver reverse-engineers "text" as VARCHAR, must be CLOB
        else if ("text".equalsIgnoreCase(typeName)) {
            type = Types.CLOB;
        }

        return super.buildAttribute(name, typeName, type, size, scale, allowNulls);
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
        buf.append("CREATE TABLE ");

        buf.append(context.quotedFullyQualifiedName(ent));

        buf.append(" (");

        // columns
        Iterator<DbAttribute> it = ent.getAttributes().iterator();
        boolean first = true;
        while (it.hasNext()) {
            if (first)
                first = false;
            else
                buf.append(", ");

            DbAttribute at = it.next();

            // attribute may not be fully valid, do a simple check
            if (at.getType() == TypesMapping.NOT_DEFINED) {
                throw new CayenneRuntimeException("Undefined type for attribute '" + ent.getFullyQualifiedName() + "."
                        + at.getName() + "'.");
            }

            String[] types = externalTypesForJdbcType(at.getType());
            if (types == null || types.length == 0) {
                throw new CayenneRuntimeException("Undefined type for attribute '" + ent.getFullyQualifiedName() + "."
                        + at.getName() + "': " + at.getType());
            }

            String type = types[0];
            buf.append(context.quotedName(at)).append(' ').append(type);

            // append size and precision (if applicable)
            if (typeSupportsLength(at.getType())) {

                int len = at.getMaxLength();
                // Postgres does not support notation float(a, b)
                int scale = (TypesMapping.isDecimal(at.getType()) && at.getType() != Types.FLOAT) ? at.getScale() : -1;

                // sanity check
                if (scale > len) {
                    scale = -1;
                }

                if (len > 0) {
                    buf.append('(').append(len);

                    if (scale >= 0) {
                        buf.append(", ").append(scale);
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
        Iterator<DbAttribute> pkit = ent.getPrimaryKeys().iterator();
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

                DbAttribute at = pkit.next();
                buf.append(context.quotedName(at));
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
            for (String externalType : externalTypes) {
                if ("bytea".equalsIgnoreCase(externalType)) {
                    return false;
                }
            }
        }

        return TypesMapping.supportsLength(type);
    }

    /**
     * Adds the CASCADE option to the DROP TABLE clause.
     */
    @Override
    public Collection<String> dropTableStatements(DbEntity table) {
        QuotingStrategy context = getQuotingStrategy();
        StringBuffer buf = new StringBuffer("DROP TABLE ");
        buf.append(context.quotedFullyQualifiedName(table));
        buf.append(" CASCADE");
        return Collections.singleton(buf.toString());
    }

    /**
     * Returns a trimming translator.
     */
    @Override
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        QualifierTranslator translator = new PostgresQualifierTranslator(queryAssembler);
        translator.setCaseInsensitive(caseInsensitiveCollations);
        return translator;
    }

    /**
     * @see JdbcAdapter#createPkGenerator()
     */
    @Override
    protected PkGenerator createPkGenerator() {
        return new PostgresPkGenerator(this);
    }

    @Override
    public MergerFactory mergerFactory() {
        return new PostgresMergerFactory();
    }
}
