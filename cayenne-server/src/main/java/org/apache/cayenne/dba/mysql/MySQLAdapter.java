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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.EJBQLTranslatorFactory;
import org.apache.cayenne.access.jdbc.JdbcEJBQLTranslatorFactory;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.types.ByteArrayType;
import org.apache.cayenne.access.types.CharType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.DefaultQuotingStrategy;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.resource.ResourceLocator;

/**
 * DbAdapter implementation for <a href="http://www.mysql.com">MySQL RDBMS</a>.
 * <h3>
 * Foreign Key Constraint Handling</h3>
 * <p>
 * Foreign key constraints are supported by InnoDB engine and NOT supported by
 * MyISAM engine. This adapter by default assumes MyISAM, so
 * {@link org.apache.cayenne.dba.JdbcAdapter#supportsFkConstraints()} will
 * return false. Users can manually change this by calling
 * <em>setSupportsFkConstraints(true)</em> or better by using an
 * {@link org.apache.cayenne.dba.AutoAdapter}, i.e. not entering the adapter
 * name at all for the DataNode, letting Cayenne guess it in runtime. In the
 * later case Cayenne will check the <em>table_type</em> MySQL variable to
 * detect whether InnoDB is the default, and configure the adapter accordingly.
 * <h3>Sample Connection Settings</h3>
 * <ul>
 * <li>Adapter name: org.apache.cayenne.dba.mysql.MySQLAdapter</li>
 * <li>DB URL: jdbc:mysql://serverhostname/dbname</li>
 * <li>Driver Class: com.mysql.jdbc.Driver</li>
 * </ul>
 */
public class MySQLAdapter extends JdbcAdapter {

    final static String DEFAULT_STORAGE_ENGINE = "InnoDB";
    final static String MYSQL_QUOTE_SQL_IDENTIFIERS_CHAR_START = "`";
    final static String MYSQL_QUOTE_SQL_IDENTIFIERS_CHAR_END = "`";

    protected String storageEngine;
    protected boolean supportsFkConstraints;

    public MySQLAdapter(@Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
            @Inject ResourceLocator resourceLocator) {
        super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator);

        // init defaults
        this.storageEngine = DEFAULT_STORAGE_ENGINE;

        setSupportsBatchUpdates(true);
        setSupportsFkConstraints(true);
        setSupportsUniqueConstraints(true);
        setSupportsGeneratedKeys(true);
    }

    void setSupportsFkConstraints(boolean flag) {
        this.supportsFkConstraints = flag;
    }

    @Override
    protected QuotingStrategy createQuotingStrategy() {
        return new DefaultQuotingStrategy("`", "`");
    }

    @Override
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        QualifierTranslator translator = new MySQLQualifierTranslator(queryAssembler);
        translator.setCaseInsensitive(caseInsensitiveCollations);
        return translator;
    }

    /**
     * Uses special action builder to create the right action.
     * 
     * @since 1.2
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new MySQLActionBuilder(node));
    }

    /**
     * @since 3.0
     */
    @Override
    public Collection<String> dropTableStatements(DbEntity table) {
        // note that CASCADE is a noop as of MySQL 5.0, so we have to use FK
        // checks
        // statement
        StringBuffer buf = new StringBuffer();
        QuotingStrategy context = getQuotingStrategy();
        buf.append(context.quotedFullyQualifiedName(table));

        return Arrays.asList("SET FOREIGN_KEY_CHECKS=0", "DROP TABLE IF EXISTS " + buf.toString() + " CASCADE",
                "SET FOREIGN_KEY_CHECKS=1");
    }

    /**
     * Installs appropriate ExtendedTypes used as converters for passing values
     * between JDBC and Java layers.
     */
    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // must handle CLOBs as strings, otherwise there
        // are problems with NULL clobs that are treated
        // as empty strings... somehow this doesn't happen
        // for BLOBs (ConnectorJ v. 3.0.9)
        map.registerType(new CharType(false, false));
        map.registerType(new ByteArrayType(false, false));
    }

    @Override
    public DbAttribute buildAttribute(String name, String typeName, int type, int size, int precision,
            boolean allowNulls) {

        if (typeName != null) {
            typeName = typeName.toLowerCase();
        }

        // all LOB types are returned by the driver as OTHER... must remap them
        // manually
        // (at least on MySQL 3.23)
        if (type == Types.OTHER) {
            if ("longblob".equals(typeName)) {
                type = Types.BLOB;
            } else if ("mediumblob".equals(typeName)) {
                type = Types.BLOB;
            } else if ("blob".equals(typeName)) {
                type = Types.BLOB;
            } else if ("tinyblob".equals(typeName)) {
                type = Types.VARBINARY;
            } else if ("longtext".equals(typeName)) {
                type = Types.CLOB;
            } else if ("mediumtext".equals(typeName)) {
                type = Types.CLOB;
            } else if ("text".equals(typeName)) {
                type = Types.CLOB;
            } else if ("tinytext".equals(typeName)) {
                type = Types.VARCHAR;
            }
        }
        // types like "int unsigned" map to Long
        else if (typeName != null && typeName.endsWith(" unsigned")) {
            // per
            // http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-type-conversions.html
            if (typeName.equals("int unsigned") || typeName.equals("integer unsigned")
                    || typeName.equals("mediumint unsigned")) {
                type = Types.BIGINT;
            }
            // BIGINT UNSIGNED maps to BigInteger according to MySQL docs, but
            // there is no
            // JDBC mapping for BigInteger
        }

        return super.buildAttribute(name, typeName, type, size, precision, allowNulls);
    }

    /**
     * Creates and returns a primary key generator. Overrides superclass
     * implementation to return an instance of MySQLPkGenerator that does the
     * correct table locking.
     */
    @Override
    protected PkGenerator createPkGenerator() {
        return new MySQLPkGenerator(this);
    }

    /**
     * @since 3.0
     */
    @Override
    protected EJBQLTranslatorFactory createEJBQLTranslatorFactory() {
        JdbcEJBQLTranslatorFactory translatorFactory = new MySQLEJBQLTranslatorFactory();
        translatorFactory.setCaseInsensitive(caseInsensitiveCollations);
        return translatorFactory;
    }

    /**
     * Overrides super implementation to explicitly set table engine to InnoDB
     * if FK constraints are supported by this adapter.
     */
    @Override
    public String createTable(DbEntity entity) {
        String ddlSQL = super.createTable(entity);

        if (storageEngine != null) {
            ddlSQL += " ENGINE=" + storageEngine;
        }

        return ddlSQL;
    }

    /**
     * Customizes PK clause semantics to ensure that generated columns are in
     * the beginning of the PK definition, as this seems to be a requirement for
     * InnoDB tables.
     * 
     * @since 1.2
     */
    // See CAY-358 for details of the InnoDB problem
    @Override
    protected void createTableAppendPKClause(StringBuffer sqlBuffer, DbEntity entity) {

        // must move generated to the front...
        List<DbAttribute> pkList = new ArrayList<DbAttribute>(entity.getPrimaryKeys());
        Collections.sort(pkList, new PKComparator());

        Iterator<DbAttribute> pkit = pkList.iterator();
        if (pkit.hasNext()) {

            sqlBuffer.append(", PRIMARY KEY (");
            boolean firstPk = true;
            while (pkit.hasNext()) {
                if (firstPk)
                    firstPk = false;
                else
                    sqlBuffer.append(", ");

                DbAttribute at = pkit.next();
                sqlBuffer.append(quotingStrategy.quotedName(at));
            }
            sqlBuffer.append(')');
        }

        // if FK constraints are supported, we must add indices to all FKs
        // Note that according to MySQL docs, FK indexes are created
        // automatically when
        // constraint is defined, starting at MySQL 4.1.2
        if (supportsFkConstraints) {
            for (DbRelationship r : entity.getRelationships()) {
                if (r.getJoins().size() > 0 && r.isToPK() && !r.isToDependentPK()) {

                    sqlBuffer.append(", KEY (");

                    Iterator<DbAttribute> columns = r.getSourceAttributes().iterator();
                    DbAttribute column = columns.next();
                    sqlBuffer.append(quotingStrategy.quotedName(column));

                    while (columns.hasNext()) {
                        column = columns.next();
                        sqlBuffer.append(", ").append(quotingStrategy.quotedName(column));
                    }

                    sqlBuffer.append(")");
                }
            }
        }
    }

    /**
     * Appends AUTO_INCREMENT clause to the column definition for generated
     * columns.
     */
    @Override
    public void createTableAppendColumn(StringBuffer sqlBuffer, DbAttribute column) {

        String[] types = externalTypesForJdbcType(column.getType());
        if (types == null || types.length == 0) {
            String entityName = column.getEntity() != null ? ((DbEntity) column.getEntity()).getFullyQualifiedName()
                    : "<null>";
            throw new CayenneRuntimeException("Undefined type for attribute '" + entityName + "." + column.getName()
                    + "': " + column.getType());
        }

        String type = types[0];
        sqlBuffer.append(quotingStrategy.quotedName(column));
        sqlBuffer.append(' ').append(type);

        // append size and precision (if applicable)s
        if (typeSupportsLength(column.getType())) {
            int len = column.getMaxLength();

            int scale = TypesMapping.isDecimal(column.getType()) ? column.getScale() : -1;

            // sanity check
            if (scale > len) {
                scale = -1;
            }

            if (len > 0) {
                sqlBuffer.append('(').append(len);

                if (scale >= 0) {
                    sqlBuffer.append(", ").append(scale);
                }

                sqlBuffer.append(')');
            }
        }

        sqlBuffer.append(column.isMandatory() ? " NOT NULL" : " NULL");

        if (column.isGenerated()) {
            sqlBuffer.append(" AUTO_INCREMENT");
        }
    }

    private boolean typeSupportsLength(int type) {
    	// As of MySQL 5.6.4 the "TIMESTAMP" and "TIME" types support length, which is the number of decimal places for fractional seconds
    	// http://dev.mysql.com/doc/refman/5.6/en/fractional-seconds.html
    	switch (type) {
	    	case Types.TIMESTAMP:
	    	case Types.TIME:
	    		return true;
	    	default:
	    		return TypesMapping.supportsLength(type);
    	}
    }
    
    @Override
    public MergerFactory mergerFactory() {
        return new MySQLMergerFactory();
    }

    final class PKComparator implements Comparator<DbAttribute> {

        public int compare(DbAttribute a1, DbAttribute a2) {
            if (a1.isGenerated() != a2.isGenerated()) {
                return a1.isGenerated() ? -1 : 1;
            } else {
                return a1.getName().compareTo(a2.getName());
            }
        }
    }

    /**
     * @since 3.0
     */
    public String getStorageEngine() {
        return storageEngine;
    }

    /**
     * @since 3.0
     */
    public void setStorageEngine(String engine) {
        this.storageEngine = engine;
    }
}
