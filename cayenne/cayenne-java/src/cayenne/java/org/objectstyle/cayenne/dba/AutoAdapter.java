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
package org.objectstyle.cayenne.dba;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import javax.sql.DataSource;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.access.OperationObserver;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.access.QueryTranslator;
import org.objectstyle.cayenne.access.trans.QualifierTranslator;
import org.objectstyle.cayenne.access.trans.QueryAssembler;
import org.objectstyle.cayenne.access.types.ExtendedTypeMap;
import org.objectstyle.cayenne.dba.db2.DB2Sniffer;
import org.objectstyle.cayenne.dba.derby.DerbySniffer;
import org.objectstyle.cayenne.dba.firebird.FirebirdSniffer;
import org.objectstyle.cayenne.dba.frontbase.FrontBaseSniffer;
import org.objectstyle.cayenne.dba.hsqldb.HSQLDBSniffer;
import org.objectstyle.cayenne.dba.ingres.IngresSniffer;
import org.objectstyle.cayenne.dba.mysql.MySQLSniffer;
import org.objectstyle.cayenne.dba.openbase.OpenBaseSniffer;
import org.objectstyle.cayenne.dba.oracle.OracleSniffer;
import org.objectstyle.cayenne.dba.postgres.PostgresSniffer;
import org.objectstyle.cayenne.dba.sqlserver.SQLServerSniffer;
import org.objectstyle.cayenne.dba.sybase.SybaseSniffer;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.query.BatchQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SQLAction;

/**
 * A DbAdapter that automatically detects the kind of database it is running on and
 * instantiates an appropriate DB-specific adapter, delegating all subsequent method calls
 * to this adapter.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class AutoAdapter implements DbAdapter {

    // hardcoded factories for adapters that we know how to auto-detect
    static final DbAdapterFactory[] DEFAULT_FACTORIES = new DbAdapterFactory[] {
            new MySQLSniffer(), new PostgresSniffer(), new OracleSniffer(),
            new SQLServerSniffer(), new HSQLDBSniffer(), new DB2Sniffer(),
            new SybaseSniffer(), new DerbySniffer(), new OpenBaseSniffer(),
            new FirebirdSniffer(), new FrontBaseSniffer(), new IngresSniffer()
    };

    /**
     * Returns a DbAdapterFactory configured to detect all databases officially supported
     * by Cayenne.
     */
    public static DbAdapterFactory getDefaultFactory() {
        return new DbAdapterFactoryChain(Arrays.asList(DEFAULT_FACTORIES));
    }

    protected DbAdapterFactory adapterFactory;
    protected DataSource dataSource;
    protected PkGenerator pkGenerator;

    /**
     * The actual adapter that is delegated method execution.
     */
    DbAdapter adapter;

    /**
     * Creates an AutoAdapter that can detect adapters known to Cayenne.
     */
    public AutoAdapter(DataSource dataSource) {
        this(null, dataSource);
    }

    /**
     * Creates an AutoAdapter with specified adapter factory and DataSource. If
     * adapterFactory is null, default factory is used.
     */
    public AutoAdapter(DbAdapterFactory adapterFactory, DataSource dataSource) {
        // sanity check
        if (dataSource == null) {
            throw new CayenneRuntimeException("Null dataSource");
        }

        this.adapterFactory = adapterFactory != null
                ? adapterFactory
                : createDefaultFactory();
        this.dataSource = dataSource;
    }

    /**
     * Called from constructor to initialize factory in case no factory was specified by
     * the object creator.
     */
    protected DbAdapterFactory createDefaultFactory() {
        return getDefaultFactory();
    }

    /**
     * Returns a proxied DbAdapter, lazily creating it on first invocation.
     */
    protected DbAdapter getAdapter() {
        if (adapter == null) {
            synchronized (this) {
                if (adapter == null) {
                    this.adapter = loadAdapter();
                }
            }
        }

        return adapter;
    }

    /**
     * Opens a connection, retrieves JDBC metadata and attempts to guess adapter form it.
     */
    protected DbAdapter loadAdapter() {
        DbAdapter adapter = null;

        try {
            Connection c = dataSource.getConnection();

            try {
                adapter = adapterFactory.createAdapter(c.getMetaData());
            }
            finally {
                try {
                    c.close();
                }
                catch (SQLException e) {
                    // ignore...
                }
            }
        }
        catch (SQLException e) {
            throw new CayenneRuntimeException("Error detecting database type", e);
        }

        if (adapter == null) {
            QueryLogger.log("Failed to detect database type, using default adapter");
            adapter = new JdbcAdapter();
        }
        else {
            QueryLogger.log("Detected and installed adapter: "
                    + adapter.getClass().getName());
        }

        return adapter;
    }

    // ---- DbAdapter methods ----

    public String getBatchTerminator() {
        return getAdapter().getBatchTerminator();
    }

    /**
     * @deprecated since 1.2 this method is deprecated in DbAdapter interface.
     */
    public DataNode createDataNode(String name) {
        return getAdapter().createDataNode(name);
    }

    /**
     * @deprecated since 1.2 this method is deprecated in DbAdapter interface.
     */
    public QueryTranslator getQueryTranslator(Query query) throws Exception {
        return getAdapter().getQueryTranslator(query);
    }

    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return getAdapter().getQualifierTranslator(queryAssembler);
    }

    public SQLAction getAction(Query query, DataNode node) {
        return getAdapter().getAction(query, node);
    }

    public boolean supportsFkConstraints() {
        return getAdapter().supportsFkConstraints();
    }

    public boolean supportsUniqueConstraints() {
        return getAdapter().supportsUniqueConstraints();
    }

    public boolean supportsGeneratedKeys() {
        return getAdapter().supportsGeneratedKeys();
    }

    public boolean supportsBatchUpdates() {
        return getAdapter().supportsBatchUpdates();
    }

    public String dropTable(DbEntity entity) {
        return getAdapter().dropTable(entity);
    }

    public String createTable(DbEntity entity) {
        return getAdapter().createTable(entity);
    }

    public String createUniqueConstraint(DbEntity source, Collection columns) {
        return getAdapter().createUniqueConstraint(source, columns);
    }

    public String createFkConstraint(DbRelationship rel) {
        return getAdapter().createFkConstraint(rel);
    }

    public String[] externalTypesForJdbcType(int type) {
        return getAdapter().externalTypesForJdbcType(type);
    }

    public ExtendedTypeMap getExtendedTypes() {
        return getAdapter().getExtendedTypes();
    }

    /**
     * Returns a primary key generator.
     */
    public PkGenerator getPkGenerator() {
        return (pkGenerator != null) ? pkGenerator : getAdapter().getPkGenerator();
    }

    /**
     * Sets a PK generator override. If set to non-null value, such PK generator will be
     * used instead of the one provided by wrapped adapter.
     */
    public void setPkGenerator(PkGenerator pkGenerator) {
        this.pkGenerator = pkGenerator;
    }

    public DbAttribute buildAttribute(
            String name,
            String typeName,
            int type,
            int size,
            int precision,
            boolean allowNulls) {

        return getAdapter().buildAttribute(
                name,
                typeName,
                type,
                size,
                precision,
                allowNulls);
    }

    public void bindParameter(
            PreparedStatement statement,
            Object object,
            int pos,
            int sqlType,
            int precision) throws SQLException, Exception {
        getAdapter().bindParameter(statement, object, pos, sqlType, precision);
    }

    public String tableTypeForTable() {
        return getAdapter().tableTypeForTable();
    }

    public String tableTypeForView() {
        return getAdapter().tableTypeForView();
    }

    /**
     * @deprecated since 1.2 this method is deprecated in DbAdapter interface.
     */
    public boolean shouldRunBatchQuery(
            DataNode node,
            Connection con,
            BatchQuery query,
            OperationObserver delegate) throws SQLException, Exception {
        return getAdapter().shouldRunBatchQuery(node, con, query, delegate);
    }
}
