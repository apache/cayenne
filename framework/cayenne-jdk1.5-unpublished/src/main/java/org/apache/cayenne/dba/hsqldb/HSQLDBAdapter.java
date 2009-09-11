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

package org.apache.cayenne.dba.hsqldb;

import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

/**
 * DbAdapter implementation for the <a href="http://hsqldb.sourceforge.net/"> HSQLDB RDBMS
 * </a>. Sample connection settings to use with HSQLDB are shown below:
 * 
 * <pre>
 *        test-hsqldb.cayenne.adapter = org.apache.cayenne.dba.hsqldb.HSQLDBAdapter
 *        test-hsqldb.jdbc.username = test
 *        test-hsqldb.jdbc.password = secret
 *        test-hsqldb.jdbc.url = jdbc:hsqldb:hsql://serverhostname
 *        test-hsqldb.jdbc.driver = org.hsqldb.jdbcDriver
 * </pre>
 */
public class HSQLDBAdapter extends JdbcAdapter {

    /**
     * Generate fully-qualified name for 1.8 and on. Subclass generates unqualified name.
     * 
     * @since 1.2
     */
    protected String getTableName(DbEntity entity) {
        QuotingStrategy context = getQuotingStrategy(entity
                .getDataMap()
                .isQuotingSQLIdentifiers());
        return context.quoteFullyQualifiedName(entity);
    }

    /**
     * Generate fully-qualified name for 1.8 and on. Subclass generates unqualified name.
     * 
     * @since 1.2
     */
    protected String getSchemaName(DbEntity entity) {
        if (entity.getSchema() != null && entity.getSchema().length() > 0) {
            QuotingStrategy context = getQuotingStrategy(entity
                    .getDataMap()
                    .isQuotingSQLIdentifiers());
            return context.quoteString(entity.getSchema()) + ".";
        }

        return "";
    }

    /**
     * Uses special action builder to create the right action.
     * 
     * @since 1.2
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query
                .createSQLAction(new HSQLActionBuilder(this, node.getEntityResolver()));
    }

    /**
     * Returns a DDL string to create a unique constraint over a set of columns.
     * 
     * @since 1.1
     */
    @Override
    public String createUniqueConstraint(DbEntity source, Collection<DbAttribute> columns) {
        boolean status;
        if (source.getDataMap() != null && source.getDataMap().isQuotingSQLIdentifiers()) {
            status = true;
        }
        else {
            status = false;
        }
        QuotingStrategy context = getQuotingStrategy(status);
        if (columns == null || columns.isEmpty()) {
            throw new CayenneRuntimeException(
                    "Can't create UNIQUE constraint - no columns specified.");
        }

        String srcName = getTableName(source);

        StringBuilder buf = new StringBuilder();

        buf.append("ALTER TABLE ").append(context.quoteString(srcName));
        buf.append(" ADD CONSTRAINT ");

        buf.append(context.quoteString(getSchemaName(source)));
        String name = "U_"
                + source.getName()
                + "_"
                + (long) (System.currentTimeMillis() / (Math.random() * 100000));

        buf.append(context.quoteString(name));
        buf.append(" UNIQUE (");

        Iterator<DbAttribute> it = columns.iterator();
        DbAttribute first = it.next();
        buf.append(context.quoteString(first.getName()));

        while (it.hasNext()) {
            DbAttribute next = it.next();
            buf.append(", ");
            buf.append(context.quoteString(next.getName()));
        }

        buf.append(")");

        return buf.toString();
    }

    /**
     * Adds an ADD CONSTRAINT clause to a relationship constraint.
     * 
     * @see JdbcAdapter#createFkConstraint(DbRelationship)
     */
    @Override
    public String createFkConstraint(DbRelationship rel) {
        boolean status;
        if ((rel.getSourceEntity().getDataMap() != null)
                && rel.getSourceEntity().getDataMap().isQuotingSQLIdentifiers()) {
            status = true;
        }
        else {
            status = false;
        }
        QuotingStrategy context = getQuotingStrategy(status);
        StringBuilder buf = new StringBuilder();
        StringBuilder refBuf = new StringBuilder();

        String srcName = getTableName((DbEntity) rel.getSourceEntity());
        String dstName = getTableName((DbEntity) rel.getTargetEntity());

        buf.append("ALTER TABLE ");
        buf.append(srcName);

        // hsqldb requires the ADD CONSTRAINT statement
        buf.append(" ADD CONSTRAINT ");
        buf.append(getSchemaName((DbEntity) rel.getSourceEntity()));
        String name = "U_"
                + rel.getSourceEntity().getName()
                + "_"
                + (long) (System.currentTimeMillis() / (Math.random() * 100000));

        buf.append(context.quoteString(name));
        buf.append(" FOREIGN KEY (");

        boolean first = true;
        for (DbJoin join : rel.getJoins()) {
            if (!first) {
                buf.append(", ");
                refBuf.append(", ");
            }
            else
                first = false;

            buf.append(context.quoteString(join.getSourceName()));
            refBuf.append(context.quoteString(join.getTargetName()));
        }

        buf.append(") REFERENCES ");
        buf.append(dstName);
        buf.append(" (");
        buf.append(refBuf.toString());
        buf.append(')');

        // also make sure we delete dependent FKs
        buf.append(" ON DELETE CASCADE");

        return buf.toString();
    }

    /**
     * Uses "CREATE CACHED TABLE" instead of "CREATE TABLE".
     * 
     * @since 1.2
     */
    @Override
    public String createTable(DbEntity ent) {
        // SET SCHEMA <schemaname>
        String sql = super.createTable(ent);
        if (sql != null && sql.toUpperCase().startsWith("CREATE TABLE ")) {
            sql = "CREATE CACHED TABLE " + sql.substring("CREATE TABLE ".length());
        }

        return sql;
    }

    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        // CAY-1095: if the column is type double, temporarily set the max length to 0 to
        // avoid adding precision information.
        if (column.getType() == Types.DOUBLE && column.getMaxLength() > 0) {
            int len = column.getMaxLength();
            column.setMaxLength(0);
            super.createTableAppendColumn(sqlBuffer, column);
            column.setMaxLength(len);
        }
        else {
            super.createTableAppendColumn(sqlBuffer, column);
        }
    }

    @Override
    public MergerFactory mergerFactory() {
        return new HSQLMergerFactory();
    }

}
