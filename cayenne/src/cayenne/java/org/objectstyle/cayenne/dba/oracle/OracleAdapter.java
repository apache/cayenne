/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

package org.objectstyle.cayenne.dba.oracle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.trans.QualifierTranslator;
import org.objectstyle.cayenne.access.trans.QueryAssembler;
import org.objectstyle.cayenne.access.trans.TrimmingQualifierTranslator;
import org.objectstyle.cayenne.access.types.ByteArrayType;
import org.objectstyle.cayenne.access.types.ByteType;
import org.objectstyle.cayenne.access.types.CharType;
import org.objectstyle.cayenne.access.types.DefaultType;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.access.types.ShortType;
import org.objectstyle.cayenne.access.util.BatchQueryUtils;
import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.Util;

/** 
 * DbAdapter implementation for <a href="http://www.oracle.com">Oracle RDBMS</a>.
 * Sample <a target="_top" href="../../../../../../../developerguide/unit-tests.html">connection 
 * settings</a> to use with Oracle are shown below:
 * 
<pre>
test-oracle.cayenne.adapter = org.objectstyle.cayenne.dba.oracle.OracleAdapter
test-oracle.jdbc.username = test
test-oracle.jdbc.password = secret
test-oracle.jdbc.url = jdbc:oracle:thin:@192.168.0.20:1521:ora1 
test-oracle.jdbc.driver = oracle.jdbc.driver.OracleDriver
</pre>
 *
 * @author Andrei Adamchik
 */
public class OracleAdapter extends JdbcAdapter {
    private static Logger logObj = Logger.getLogger(OracleAdapter.class);

    public static final String ORACLE_FLOAT = "FLOAT";
    public static final String ORACLE_BLOB = "BLOB";
    public static final String ORACLE_CLOB = "CLOB";

    public static final String TRIM_FUNCTION = "RTRIM";
    public static final String NEW_CLOB_FUNCTION = "EMPTY_CLOB()";
    public static final String NEW_BLOB_FUNCTION = "EMPTY_BLOB()";

    protected static boolean initDone;
    protected static int oracleCursorType = Integer.MAX_VALUE;
    protected static Method outputStreamFromBlobMethod;
    protected static Method writerFromClobMethod;
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
            Class oraTypes = Class.forName("oracle.jdbc.driver.OracleTypes");
            Field cursorField = oraTypes.getField("CURSOR");
            oracleCursorType = cursorField.getInt(null);

            outputStreamFromBlobMethod =
                Class.forName("oracle.sql.BLOB").getMethod(
                    "getBinaryOutputStream",
                    new Class[0]);

            writerFromClobMethod =
                Class.forName("oracle.sql.CLOB").getMethod(
                    "getCharacterOutputStream",
                    new Class[0]);
            supportsOracleLOB = true;

        } catch (Throwable th) {
            logObj.info(
                "Error getting Oracle driver information, ignoring. "
                    + "Note that certain adapter features will be disabled.",
                Util.unwindException(th));
        }
    }
    
	public static Method getOutputStreamFromBlobMethod() {
		return outputStreamFromBlobMethod;
	}

	// TODO: rename to something that looks like English ...
	public static boolean isSupportsOracleLOB() {
		return supportsOracleLOB;
	}

	public static Method getWriterFromClobMethod() {
		return writerFromClobMethod;
	}

    /**
     * Returns an Oracle JDBC extension type defined in
     * oracle.jdbc.driver.OracleTypes.CURSOR. This value is determined
     * from Oracle driver classes via reflection in runtime, so that
     * Cayenne code has no compile dependency on the driver. This means
     * that calling this method when the driver is not available will
     * result in an exception.
     */
    public static int getOracleCursorType() {

        if (oracleCursorType == Integer.MAX_VALUE) {
            throw new CayenneRuntimeException(
                "No information exists about oracle types. "
                    + "Check that Oracle JDBC driver is available to the application.");
        }

        return oracleCursorType;
    }

    public OracleAdapter() {
        // enable batch updates by default
        setSupportsBatchUpdates(true);
    }

    /**
     * Installs appropriate ExtendedTypes as converters for passing values
     * between JDBC and Java layers.
     */
    protected void configureExtendedTypes(ExtendedTypeMap map) {
        super.configureExtendedTypes(map);

        // create specially configured CharType handler
        map.registerType(new CharType(true, true));

        // create specially configured ByteArrayType handler
        map.registerType(new ByteArrayType(true, true));

        // override date handler with Oracle handler
        map.registerType(new OracleUtilDateType());
        
        // At least on MacOS X, driver does not handle Short and Byte properly
        map.registerType(new ShortType(true));
        map.registerType(new ByteType(true));
        
        // these two types are needed to replace PreparedStatement binding
        // via "setObject()" to a call to setInt or setDouble to make
        // Oracle happy, esp. with the AST* expression classes
        // that do not evaluate constants to BigDecimals, but rather 
        // Integer and Double
        map.registerType(new OracleIntegerType());
        map.registerType(new OracleDoubleType());
    }

    /**
     * Creates and returns a primary key generator.
     * Overrides superclass implementation to return an
     * instance of OraclePkGenerator.
     */
    protected PkGenerator createPkGenerator() {
        return new OraclePkGenerator();
    }

    /**
     * Returns a query string to drop a table corresponding
     * to <code>ent</code> DbEntity. Changes superclass behavior
     * to drop all related foreign key constraints.
     */
    public String dropTable(DbEntity ent) {
        return "DROP TABLE "
            + ent.getFullyQualifiedName()
            + " CASCADE CONSTRAINTS";
    }

    /**
     * Fixes some reverse engineering problems. Namely if a columns
     * is created as DECIMAL and has non-positive precision it is
     * converted to INTEGER.
     */
    public DbAttribute buildAttribute(
        String name,
        String typeName,
        int type,
        int size,
        int precision,
        boolean allowNulls) {

        DbAttribute attr =
            super.buildAttribute(
                name,
                typeName,
                type,
                size,
                precision,
                allowNulls);

        if (type == Types.DECIMAL && precision <= 0) {
            attr.setType(Types.INTEGER);
            attr.setPrecision(-1);
        } else if (type == Types.OTHER) {
            // in this case we need to guess the attribute type 
            // based on its string value
            if (ORACLE_FLOAT.equals(typeName)) {
                attr.setType(Types.FLOAT);
            } else if (ORACLE_BLOB.equals(typeName)) {
                attr.setType(Types.BLOB);
            } else if (ORACLE_CLOB.equals(typeName)) {
                attr.setType(Types.CLOB);
            }
        }

        return attr;
    }

    /** 
     * Returns Oracle-specific translator for object SELECT queries. 
     */
    protected Class queryTranslatorClass(Query q) {
        if (q instanceof SelectQuery) {
            return OracleSelectTranslator.class;
        } else {
            return super.queryTranslatorClass(q);
        }
    }

    /**
     * Returns a trimming translator.
     */
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new TrimmingQualifierTranslator(
            queryAssembler,
            OracleAdapter.TRIM_FUNCTION);
    }

    /**
     * Creates an instance of OracleDataNode.
     */
    public DataNode createDataNode(String name) {
        DataNode node = new OracleDataNode(name);
        node.setAdapter(this);
        return node;
    }

    /**
     * Implements special LOB handling in batches.
     */
    public boolean shouldRunBatchQuery(
        DataNode node,
        Connection con,
        BatchQuery query,
        OperationObserver delegate)
        throws SQLException, Exception {

        // special handling for LOB updates
        if (isSupportsOracleLOB() && BatchQueryUtils.updatesLOBColumns(query)
            && (node instanceof OracleDataNode)) {

            OracleDataNode oracleNode = (OracleDataNode) node;
            oracleNode.runBatchUpdateWithLOBColumns(con, query, delegate);

            return false;
        } else {
            return super.shouldRunBatchQuery(node, con, query, delegate);
        }
    }
    
    /**
     * @since 1.1
     */
    final class OracleIntegerType extends DefaultType {

        public OracleIntegerType() {
            super(Integer.class.getName());
        }

        public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int precision)
            throws Exception {

            if (val != null) {
                st.setInt(pos, ((Number) val).intValue());
            }
            else {
                super.setJdbcObject(st, val, pos, type, precision);
            }
        }
    }
    
    /**
     * @since 1.1
     */
    final class OracleDoubleType extends DefaultType {

        public OracleDoubleType() {
            super(Double.class.getName());
        }

        public void setJdbcObject(
            PreparedStatement st,
            Object val,
            int pos,
            int type,
            int precision)
            throws Exception {

            if (val != null) {
                st.setDouble(pos, ((Number) val).doubleValue());
            }
            else {
                super.setJdbcObject(st, val, pos, type, precision);
            }
        }
    }
}