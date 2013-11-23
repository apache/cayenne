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

package org.apache.cayenne.dba.frontbase;

import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * DbAdapter implementation for <a href="http://www.frontbase.com/">FrontBase RDBMS</a>.
 * Sample connection settings to use with FrontBase are shown below:
 * 
 * <pre>
 *          fb.jdbc.username = _system
 *          fb.jdbc.password = secret
 *          fb.jdbc.url = jdbc:FrontBase://localhost/cayenne/
 *          fb.jdbc.driver = jdbc.FrontBase.FBJDriver
 * </pre>
 * 
 * @since 1.2
 */
// TODO, Andrus 11/8/2005:
// Limitations (also see FrontBaseStackAdapter in unit tests):
//
// 1. Case insensitive ordering (i.e. UPPER in the ORDER BY clause) is supported by
// FrontBase, however aliases don't work ( ORDER BY UPPER(t0.ARTIST_NAME)) ... not sure
// what to do about it.
public class FrontBaseAdapter extends JdbcAdapter {

    public FrontBaseAdapter(
            @Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ResourceLocator resourceLocator) {
        super(
                runtimeProperties,
                defaultExtendedTypes,
                userExtendedTypes,
                extendedTypeFactories, 
                resourceLocator);
        setSupportsBatchUpdates(true);
    }

    /**
     * Uses special action builder to create the right action.
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new FrontBaseActionBuilder(this, node
                .getEntityResolver()));
    }

    @Override
    public String tableTypeForTable() {
        return "BASE TABLE";
    }

    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        map.registerType(new FrontBaseByteArrayType());
        map.registerType(new FrontBaseBooleanType());
        map.registerType(new FrontBaseCharType());
    }

    /**
     * Customizes table creating procedure for FrontBase.
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
            buf.append(context.quotedName(at)).append(' ').append(type);

            // Mapping LONGVARCHAR without length creates a column with length "1" which
            // is definitely not what we want...so just use something very large (1Gb seems
            // to be the limit for FB)
            if (at.getType() == Types.LONGVARCHAR) {

                int len = at.getMaxLength() > 0 ? at.getMaxLength() : 1073741824;
                buf.append("(").append(len).append(")");
            }
            else if (at.getType() == Types.VARBINARY || at.getType() == Types.BINARY) {

                // use a BIT column with size * 8
                int len = at.getMaxLength() > 0 ? at.getMaxLength() : 1073741824;
                len *= 8;
                buf.append("(").append(len).append(")");
            }
            else if (TypesMapping.supportsLength(at.getType())) {
                int len = at.getMaxLength();
                int scale = TypesMapping.isDecimal(at.getType()) ? at.getScale() : -1;

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
            }
            // else: don't appen NULL for FrontBase:
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
                buf.append(quotingStrategy.quotedName(at));
            }
            buf.append(')');
        }
        buf.append(')');
        return buf.toString();
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

    @Override
    protected PkGenerator createPkGenerator() {
        return new FrontBasePkGenerator(this);
    }
}
