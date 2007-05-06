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

package org.apache.cayenne.dba.mysql;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

/**
 * DbAdapter implementation for <a href="http://www.mysql.com">MySQL RDBMS</a>.
 * <h3>Foreign Key Constraint Handling</h3>
 * <p>
 * Foreign key constraints are supported by InnoDB engine and NOT supported by MyISAM
 * engine. This adapter by default assumes MyISAM, so
 * {@link org.apache.cayenne.dba.JdbcAdapter#supportsFkConstraints()} will return
 * false. Users can manually change this by calling
 * <em>setSupportsFkConstraints(true)</em> or better by using an
 * {@link org.apache.cayenne.dba.AutoAdapter}, i.e. not entering the adapter name at
 * all for the DataNode, letting Cayenne guess it in runtime. In the later case Cayenne
 * will check the <em>table_type</em> MySQL variable to detect whether InnoDB is the
 * default, and configure the adapter accordingly.
 * <h3>Sample Connection Settings</h3>
 * <ul>
 * <li>Adapter name: org.apache.cayenne.dba.mysql.MySQLAdapter</li>
 * <li>DB URL: jdbc: mysql://serverhostname/dbname</li>
 * <li>Driver Class: com.mysql.jdbc.Driver</li>
 * </ul>
 * 
 * @author Andrus Adamchik
 */
public class MySQLAdapter extends JdbcAdapter {

    public MySQLAdapter() {
        // init defaults
        this.setSupportsFkConstraints(false);
        this.setSupportsUniqueConstraints(true);
        this.setSupportsGeneratedKeys(true);
    }

    /**
     * Uses special action builder to create the right action.
     * 
     * @since 1.2
     */
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new MySQLActionBuilder(this, node
                .getEntityResolver()));
    }

    public String dropTable(DbEntity entity) {
        return "DROP TABLE IF EXISTS " + entity.getFullyQualifiedName() + " CASCADE";
    }

    /**
     * Installs appropriate ExtendedTypes used as converters for passing values between
     * JDBC and Java layers.
     */
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // must handle CLOBs as strings, otherwise there
        // are problems with NULL clobs that are treated
        // as empty strings... somehow this doesn't happen
        // for BLOBs (ConnectorJ v. 3.0.9)
        map.registerType(new CharType(false, false));
        map.registerType(new ByteArrayType(false, false));
    }

    public DbAttribute buildAttribute(
            String name,
            String typeName,
            int type,
            int size,
            int precision,
            boolean allowNulls) {

        if (typeName != null) {
            typeName = typeName.toLowerCase();
        }

        // all LOB types are returned by the driver as OTHER... must remap them manually
        // (at least on MySQL 3.23)
        if (type == Types.OTHER) {
            if ("longblob".equals(typeName)) {
                type = Types.BLOB;
            }
            else if ("mediumblob".equals(typeName)) {
                type = Types.BLOB;
            }
            else if ("blob".equals(typeName)) {
                type = Types.BLOB;
            }
            else if ("tinyblob".equals(typeName)) {
                type = Types.VARBINARY;
            }
            else if ("longtext".equals(typeName)) {
                type = Types.CLOB;
            }
            else if ("mediumtext".equals(typeName)) {
                type = Types.CLOB;
            }
            else if ("text".equals(typeName)) {
                type = Types.CLOB;
            }
            else if ("tinytext".equals(typeName)) {
                type = Types.VARCHAR;
            }
        }
        // types like "int unsigned" map to Long
        else if (typeName != null && typeName.endsWith(" unsigned")) {
            // per
            // http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-type-conversions.html
            if (typeName.equals("int unsigned")
                    || typeName.equals("integer unsigned")
                    || typeName.equals("mediumint unsigned")) {
                type = Types.BIGINT;
            }
            // BIGINT UNSIGNED maps to BigInteger according to MySQL docs, but there is no
            // JDBC mapping for BigInteger
        }

        return super.buildAttribute(name, typeName, type, size, precision, allowNulls);
    }

    /**
     * Returns null, since views are not yet supported in MySQL. Views are available on
     * newer versions of MySQL.
     */
    public String tableTypeForView() {
        return null;
    }

    /**
     * Creates and returns a primary key generator. Overrides superclass implementation to
     * return an instance of MySQLPkGenerator that does the correct table locking.
     */
    protected PkGenerator createPkGenerator() {
        return new MySQLPkGenerator();
    }

    /**
     * Overrides super implementation to explicitly set table engine to InnoDB if FK
     * constraints are supported by this adapter.
     */
    public String createTable(DbEntity entity) {
        String ddlSQL = super.createTable(entity);

        // force InnoDB tables if constraints are enabled
        if (supportsFkConstraints()) {
            ddlSQL += " ENGINE=InnoDB";
        }

        return ddlSQL;
    }

    /**
     * Customizes PK clause semantics to ensure that generated columns are in the
     * beginning of the PK definition, as this seems to be a requirement for InnoDB
     * tables.
     * 
     * @since 1.2
     */
    // See CAY-358 for details of the InnoDB problem
    protected void createTableAppendPKClause(StringBuffer sqlBuffer, DbEntity entity) {

        // must move generated to the front...
        List pkList = new ArrayList(entity.getPrimaryKey());
        Collections.sort(pkList, new PKComparator());

        Iterator pkit = pkList.iterator();
        if (pkit.hasNext()) {

            sqlBuffer.append(", PRIMARY KEY (");
            boolean firstPk = true;
            while (pkit.hasNext()) {
                if (firstPk)
                    firstPk = false;
                else
                    sqlBuffer.append(", ");

                DbAttribute at = (DbAttribute) pkit.next();
                sqlBuffer.append(at.getName());
            }
            sqlBuffer.append(')');
        }

        // if FK constraints are supported, we must add indices to all FKs
        // Note that according to MySQL docs, FK indexes are created automatically when
        // constraint is defined, starting at MySQL 4.1.2
        if (supportsFkConstraints()) {
            Iterator relationships = entity.getRelationships().iterator();
            while (relationships.hasNext()) {
                DbRelationship relationship = (DbRelationship) relationships.next();
                if (relationship.getJoins().size() > 0
                        && relationship.isToPK()
                        && !relationship.isToDependentPK()) {

                    sqlBuffer.append(", KEY (");

                    Iterator columns = relationship.getSourceAttributes().iterator();
                    DbAttribute column = (DbAttribute) columns.next();
                    sqlBuffer.append(column.getName());

                    while (columns.hasNext()) {
                        column = (DbAttribute) columns.next();
                        sqlBuffer.append(", ").append(column.getName());
                    }

                    sqlBuffer.append(")");
                }
            }
        }
    }

    /**
     * Appends AUTO_INCREMENT clause to the column definition for generated columns.
     */
    protected void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {
        super.createTableAppendColumn(sqlBuffer, column);

        if (column.isGenerated()) {
            sqlBuffer.append(" AUTO_INCREMENT");
        }
    }

    final class PKComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            DbAttribute a1 = (DbAttribute) o1;
            DbAttribute a2 = (DbAttribute) o2;
            if (a1.isGenerated() != a2.isGenerated()) {
                return a1.isGenerated() ? -1 : 1;
            }
            else {
                return a1.getName().compareTo(a2.getName());
            }
        }
    }
}
