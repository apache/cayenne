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

package org.apache.cayenne.dba.openbase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.IDUtil;

/**
 * @since 1.1
 * @deprecated since 4.2
 */
@Deprecated
public class OpenBasePkGenerator extends JdbcPkGenerator {

    /**
     * Used by DI
     * @since 4.1
     */
    public OpenBasePkGenerator() {
        super();
    }

    protected OpenBasePkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    /**
     * Returns a non-repeating primary key for a given PK attribute. Since
     * OpenBase-specific mechanism is used, key caching is disabled. Instead a
     * database operation is performed on every call.
     *
     * @since 3.0
     */
    @Override
    public Object generatePk(DataNode node, DbAttribute pk) throws Exception {

        DbEntity entity = pk.getEntity();

        switch (pk.getType()) {
            case Types.BINARY:
            case Types.VARBINARY:
                return IDUtil.pseudoUniqueSecureByteSequence(pk.getMaxLength());
        }

        long value = longPkFromDatabase(node, entity);

        if (pk.getType() == Types.BIGINT) {
            return value;
        } else {
            // leaving it up to the user to ensure that PK does not exceed max
            // int...
            return (int) value;
        }
    }

    /**
     * Generates new (unique and non-repeating) primary key for specified
     * DbEntity. Executed SQL looks like this:
     *
     * <pre>
     *  NEWID FOR Table Column
     * </pre>
     * <p>
     * COLUMN must be marked as UNIQUE in order for this to work properly.
     *
     * @since 3.0
     */
    @Override
    protected long longPkFromDatabase(DataNode node, DbEntity entity) throws Exception {

        String sql = newIDString(entity);
        adapter.getJdbcEventLogger().log(sql);

        try (Connection con = node.getDataSource().getConnection()) {
            try (Statement st = con.createStatement()) {
                try (ResultSet rs = st.executeQuery(sql)) {
                    // Object pk = null;
                    if (!rs.next()) {
                        throw new CayenneRuntimeException("Error generating pk for DbEntity %s", entity.getName());
                    }
                    return rs.getLong(1);
                }
            }
        }
    }

    /**
     * Returns SQL string that can generate new (unique and non-repeating)
     * primary key for specified DbEntity. No actual database operations are
     * performed.
     *
     * @since 1.2
     */
    protected String newIDString(DbEntity ent) {
        if (ent.getPrimaryKeys() == null || ent.getPrimaryKeys().size() != 1) {
            throw new CayenneRuntimeException("Error generating pk for DbEntity %s"
                    + ": pk must be single attribute", ent.getName());
        }
        DbAttribute primaryKeyAttribute = ent.getPrimaryKeys().iterator().next();

        return "NEWID FOR " + ent.getName() + ' ' + primaryKeyAttribute.getName();
    }

    @Override
    public void createAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
        // looks like generating a PK on top of an existing one does not
        // result in errors...

        // create needed sequences
        for (DbEntity dbEntity : dbEntities) {
            // the caller must take care of giving us the right entities
            // but lets check anyway
            if (!canCreatePK(dbEntity)) {
                continue;
            }

            runUpdate(node, createPKString(dbEntity));
            runUpdate(node, createUniquePKIndexString(dbEntity));
        }
    }

    /**
     *
     */
    @Override
    public List<String> createAutoPkStatements(List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<>(2 * dbEntities.size());
        for (DbEntity dbEntity : dbEntities) {
            // the caller must take care of giving us the right entities
            // but lets check anyway
            if (!canCreatePK(dbEntity)) {
                continue;
            }

            list.add(createPKString(dbEntity));
            list.add(createUniquePKIndexString(dbEntity));
        }

        return list;
    }

    protected boolean canCreatePK(DbEntity entity) {
        return entity.getPrimaryKeys().size() > 0;
    }

    /**
     *
     */
    @Override
    public void dropAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
        // there is no simple way to do that... probably requires
        // editing metadata tables...
        // Good thing is that it doesn't matter, since PK support
        // is attached to the table itself, so if a table is dropped,
        // it will be dropped as well
    }

    /**
     * Returns an empty list, since OpenBase doesn't support this operation.
     */
    @Override
    public List<String> dropAutoPkStatements(List<DbEntity> dbEntities) {
        return Collections.emptyList();
    }

    /**
     * Returns a String to create PK support for an entity.
     */
    protected String createPKString(DbEntity entity) {
        Collection<DbAttribute> pk = entity.getPrimaryKeys();

        if (pk == null || pk.size() == 0) {
            throw new CayenneRuntimeException("Entity '%s' has no PK defined.", entity.getName());
        }

        StringBuilder buffer = new StringBuilder();
        buffer.append("CREATE PRIMARY KEY ");

        QuotingStrategy context = getAdapter().getQuotingStrategy();

        buffer.append(context.quotedIdentifier(entity, entity.getName()));

        buffer.append(" (");

        Iterator<DbAttribute> it = pk.iterator();

        // at this point we know that there is at least on PK column
        DbAttribute firstColumn = it.next();
        buffer.append(context.quotedName(firstColumn));

        while (it.hasNext()) {
            DbAttribute column = it.next();
            buffer.append(", ");
            buffer.append(context.quotedName(column));
        }

        buffer.append(")");
        return buffer.toString();
    }

    /**
     * Returns a String to create a unique index on table primary key columns
     * per OpenBase recommendations.
     */
    protected String createUniquePKIndexString(DbEntity entity) {
        Collection<DbAttribute> pk = entity.getPrimaryKeys();

        QuotingStrategy context = getAdapter().getQuotingStrategy();
        if (pk == null || pk.size() == 0) {
            throw new CayenneRuntimeException("Entity '%s' has no PK defined.", entity.getName());
        }

        StringBuilder buffer = new StringBuilder();

        // compound PK doesn't work well with UNIQUE index...
        // create a regular one in this case
        buffer.append(pk.size() == 1 ? "CREATE UNIQUE INDEX " : "CREATE INDEX ");

        buffer.append(context.quotedIdentifier(entity, entity.getName()));
        buffer.append(" (");

        Iterator<DbAttribute> it = pk.iterator();

        // at this point we know that there is at least on PK column
        DbAttribute firstColumn = it.next();
        buffer.append(context.quotedName(firstColumn));

        while (it.hasNext()) {
            DbAttribute column = it.next();
            buffer.append(", ");
            buffer.append(context.quotedName(column));
        }
        buffer.append(")");
        return buffer.toString();
    }

    @Override
    public void reset() {
        // noop
    }

    /**
     * Returns zero, since PK caching is not feasible with OpenBase PK
     * generation mechanism.
     */
    @Override
    public int getPkCacheSize() {
        return 0;
    }

    @Override
    public void setPkCacheSize(int pkCacheSize) {
        // noop, no PK caching
    }

}
