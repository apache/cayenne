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

package org.apache.cayenne.dba.oracle;

import java.lang.reflect.Field;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.EJBQLTranslatorFactory;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.access.types.ByteType;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.access.types.ShortType;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.merge.MergerFactory;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.InsertBatchQuery;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.UpdateBatchQuery;

/**
 * DbAdapter implementation for <a href="http://www.oracle.com">Oracle RDBMS </a>. Sample
 * connection settings to use with Oracle are shown below:
 * 
 * <pre>
 *          test-oracle.jdbc.username = test
 *          test-oracle.jdbc.password = secret
 *          test-oracle.jdbc.url = jdbc:oracle:thin:@//192.168.0.20:1521/ora1 
 *          test-oracle.jdbc.driver = oracle.jdbc.driver.OracleDriver
 * </pre>
 */
public class OracleAdapter extends JdbcAdapter {

    public static final String ORACLE_FLOAT = "FLOAT";
    public static final String ORACLE_BLOB = "BLOB";
    public static final String ORACLE_CLOB = "CLOB";

    public static final String TRIM_FUNCTION = "RTRIM";
    public static final String NEW_CLOB_FUNCTION = "EMPTY_CLOB()";
    public static final String NEW_BLOB_FUNCTION = "EMPTY_BLOB()";

    protected static boolean initDone;
    protected static int oracleCursorType = Integer.MAX_VALUE;

    protected static boolean supportsOracleLOB;

    static {
        // TODO: as CAY-234 shows, having such initialization done in a static fashion
        // makes it untestable and any potential problems hard to reproduce. Make this
        // an instance method (with all the affected vars) and write unit tests.
        initDriverInformation();
    }

    protected static void initDriverInformation() {
        initDone = true;

        // configure static information
        try {
            Class<?> oraTypes = Class.forName("oracle.jdbc.driver.OracleTypes");
            Field cursorField = oraTypes.getField("CURSOR");
            oracleCursorType = cursorField.getInt(null);

            supportsOracleLOB = true;
        }
        catch (Throwable th) {
            // ignoring...
        }
    }

    // TODO: rename to something that looks like English ...
    public static boolean isSupportsOracleLOB() {
        return supportsOracleLOB;
    }

    /**
     * Utility method that returns <code>true</code> if the query will update at least one
     * BLOB or CLOB DbAttribute.
     * 
     * @since 1.2
     */
    static boolean updatesLOBColumns(BatchQuery query) {
        boolean isInsert = query instanceof InsertBatchQuery;
        boolean isUpdate = query instanceof UpdateBatchQuery;

        if (!isInsert && !isUpdate) {
            return false;
        }

        List<DbAttribute> updatedAttributes = (isInsert)
                ? query.getDbAttributes()
                : ((UpdateBatchQuery) query).getUpdatedAttributes();

        for (DbAttribute attr : updatedAttributes) {
            int type = attr.getType();
            if (type == Types.CLOB || type == Types.BLOB) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns an Oracle JDBC extension type defined in
     * oracle.jdbc.driver.OracleTypes.CURSOR. This value is determined from Oracle driver
     * classes via reflection in runtime, so that Cayenne code has no compile dependency
     * on the driver. This means that calling this method when the driver is not available
     * will result in an exception.
     */
    public static int getOracleCursorType() {

        if (oracleCursorType == Integer.MAX_VALUE) {
            throw new CayenneRuntimeException(
                    "No information exists about oracle types. "
                            + "Check that Oracle JDBC driver is available to the application.");
        }

        return oracleCursorType;
    }

    public OracleAdapter(
            @Inject RuntimeProperties runtimeProperties,
            @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
            @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
            @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories) {
        super(
                runtimeProperties,
                defaultExtendedTypes,
                userExtendedTypes,
                extendedTypeFactories);

        // enable batch updates by default
        setSupportsBatchUpdates(true);
    }

    /**
     * @since 3.0
     */
    @Override
    protected EJBQLTranslatorFactory createEJBQLTranslatorFactory() {
        return new OracleEJBQLTranslatorFactory();
    }

    /**
     * Installs appropriate ExtendedTypes as converters for passing values between JDBC
     * and Java layers.
     */
    @Override
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        map.registerType(new OracleCharType());

        // create specially configured ByteArrayType handler
        map.registerType(new OracleByteArrayType());

        // override date handler with Oracle handler
        map.registerType(new OracleUtilDateType());

        // At least on MacOS X, driver does not handle Short and Byte properly
        map.registerType(new ShortType(true));
        map.registerType(new ByteType(true));
        map.registerType(new OracleBooleanType());
    }

    /**
     * Creates and returns a primary key generator. Overrides superclass implementation to
     * return an instance of OraclePkGenerator.
     */
    @Override
    protected PkGenerator createPkGenerator() {
        return new OraclePkGenerator(this);
    }

    /**
     * Returns a query string to drop a table corresponding to <code>ent</code> DbEntity.
     * Changes superclass behavior to drop all related foreign key constraints.
     * 
     * @since 3.0
     */
    @Override
    public Collection<String> dropTableStatements(DbEntity table) {
        QuotingStrategy context = getQuotingStrategy();
        StringBuffer buf = new StringBuffer("DROP TABLE ");
        buf.append(context.quotedFullyQualifiedName(table));

        buf.append(" CASCADE CONSTRAINTS");
        return Collections.singleton(buf.toString());
    }

    @Override
    public void bindParameter(
            PreparedStatement statement,
            Object object,
            int pos,
            int sqlType,
            int scale) throws SQLException, Exception {

        // Oracle doesn't support BOOLEAN even when binding NULL, so have to intercept
        // NULL Boolean here, as super doesn't pass it through ExtendedType...
        if (object == null && sqlType == Types.BOOLEAN) {
            ExtendedType typeProcessor = getExtendedTypes().getRegisteredType(
                    Boolean.class);
            typeProcessor.setJdbcObject(statement, object, pos, sqlType, scale);
        }
        else {
            super.bindParameter(statement, object, pos, sqlType, scale);
        }
    }

    /**
     * Fixes some reverse engineering problems. Namely if a columns is created as DECIMAL
     * and has non-positive precision it is converted to INTEGER.
     */
    @Override
    public DbAttribute buildAttribute(
            String name,
            String typeName,
            int type,
            int size,
            int scale,
            boolean allowNulls) {

        DbAttribute attr = super.buildAttribute(
                name,
                typeName,
                type,
                size,
                scale,
                allowNulls);

        if (type == Types.DECIMAL && scale <= 0) {
            attr.setType(Types.INTEGER);
            attr.setScale(-1);
        }
        else if (type == Types.OTHER) {
            // in this case we need to guess the attribute type
            // based on its string value
            if (ORACLE_FLOAT.equals(typeName)) {
                attr.setType(Types.FLOAT);
            }
            else if (ORACLE_BLOB.equals(typeName)) {
                attr.setType(Types.BLOB);
            }
            else if (ORACLE_CLOB.equals(typeName)) {
                attr.setType(Types.CLOB);
            }
        }
        else if (type == Types.DATE) {
            // Oracle DATE can store JDBC TIMESTAMP
            if ("DATE".equals(typeName)) {
                attr.setType(Types.TIMESTAMP);
            }
        }

        return attr;
    }

    /**
     * Returns a trimming translator.
     */
    @Override
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        QualifierTranslator translator = new Oracle8QualifierTranslator(queryAssembler);
        translator.setCaseInsensitive(caseInsensitiveCollations);
        return translator;
    }

    /**
     * Uses OracleActionBuilder to create the right action.
     * 
     * @since 1.2
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new OracleActionBuilder(this, node
                .getEntityResolver()));
    }

    /**
     * @since 3.0
     */
    final class OracleBooleanType implements ExtendedType {

        public String getClassName() {
            return Boolean.class.getName();
        }

        public void setJdbcObject(
                PreparedStatement st,
                Object val,
                int pos,
                int type,
                int precision) throws Exception {

            // Oracle does not support Types.BOOLEAN, so we have to override user mapping
            // unconditionally
            if (val == null) {
                st.setNull(pos, Types.INTEGER);
            }
            else {
                boolean flag = Boolean.TRUE.equals(val);
                st.setInt(pos, flag ? 1 : 0);
            }
        }

        public Object materializeObject(ResultSet rs, int index, int type)
                throws Exception {

            // Oracle does not support Types.BOOLEAN, so we have to override user mapping
            // unconditionally
            int i = rs.getInt(index);
            return (rs.wasNull()) ? null : i == 0 ? Boolean.FALSE : Boolean.TRUE;
        }

        public Object materializeObject(CallableStatement st, int index, int type)
                throws Exception {

            // Oracle does not support Types.BOOLEAN, so we have to override user mapping
            // unconditionally
            int i = st.getInt(index);
            return (st.wasNull()) ? null : i == 0 ? Boolean.FALSE : Boolean.TRUE;
        }
    }

    @Override
    public MergerFactory mergerFactory() {
        return new OracleMergerFactory();
    }
}
