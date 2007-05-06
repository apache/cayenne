/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.dba.mysql;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.types.ByteArrayType;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLAction;

/**
 * DbAdapter implementation for <a href="http://www.mysql.com">MySQL RDBMS</a>.
 * <h3>Foreign Key Constraint Handling</h3>
 * <p>
 * Foreign key constraints are supported by InnoDB engine and NOT supported by MyISAM
 * engine. This adapter by default assumes MyISAM, so
 * {@link org.objectstyle.cayenne.dba.JdbcAdapter#supportsFkConstraints()} will return
 * false. Users can manually change this by calling
 * <em>setSupportsFkConstraints(true)</em> or better by using an
 * {@link org.objectstyle.cayenne.dba.AutoAdapter}, i.e. not entering the adapter name at
 * all for the DataNode, letting Cayenne guess it in runtime. In the later case Cayenne
 * will check the <em>table_type</em> MySQL variable to detect whether InnoDB is the
 * default, and configure the adapter accordingly.
 * <h3>Sample Connection Settings</h3>
 * <ul>
 * <li>Adapter name: org.objectstyle.cayenne.dba.mysql.MySQLAdapter</li>
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
