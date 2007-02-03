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

package org.apache.cayenne.jpa;

import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.ConnectionProperties;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.enhancer.Enhancer;
import org.apache.cayenne.jpa.bridge.DataMapConverter;
import org.apache.cayenne.jpa.conf.EntityMapLoader;
import org.apache.cayenne.jpa.conf.EntityMapLoaderContext;
import org.apache.cayenne.jpa.enhancer.JpaEnhancerVisitorFactory;
import org.apache.cayenne.jpa.instrument.UnitClassTranformer;
import org.apache.cayenne.jpa.map.JpaClassDescriptor;
import org.apache.cayenne.jpa.reflect.CjpaClassDescriptorFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.util.ResourceLocator;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.SimpleValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A PersistenceProvider implementation based on Cayenne stack. Wraps a Cayenne
 * Configuration instance.
 * 
 * @author Andrus Adamchik
 */
public class Provider extends JpaPersistenceProvider {

    public static final String CREATE_SCHEMA_PROPERTY = "cayenne.schema.create";

    protected Configuration configuration;
    protected Log logger;

    /**
     * Creates a new PersistenceProvider with properties configured to run in a standalone
     * mode with Cayenne stack.
     */
    public Provider() {
        this(false);
    }

    public Provider(boolean validateDescriptors) {
        super(validateDescriptors);

        this.logger = LogFactory.getLog(getClass());
        this.configuration = new LazyConfiguration();

        // set a singleton that may be used by Cayenne
        Configuration.initializeSharedConfiguration(configuration);
    }

    protected void configureDefaultProperties() {
        if (!defaultProperties.containsKey(DATA_SOURCE_FACTORY_PROPERTY)) {
            defaultProperties.put(
                    DATA_SOURCE_FACTORY_PROPERTY,
                    DefaultDataSourceFactory.class.getName());
        }

        if (!defaultProperties.containsKey(TRANSACTION_TYPE_PROPERTY)) {
            defaultProperties.put(
                    TRANSACTION_TYPE_PROPERTY,
                    PersistenceUnitTransactionType.RESOURCE_LOCAL.name());
        }
    }

    /**
     * Maps PersistenceUnitInfo to Cayenne DataDomain and returns a
     * {@link CjpaEntityManagerFactory} which is a DataDomain wrapper.
     */
    @Override
    // TODO: andrus, 07/24/2006 - extract properties from the second map parameter as well
    // as PUI.
    public synchronized EntityManagerFactory createContainerEntityManagerFactory(
            PersistenceUnitInfo info,
            Map map) {
        String name = info.getPersistenceUnitName();
        DataDomain domain = configuration.getDomain(name);

        if (domain == null) {

            long t0 = System.currentTimeMillis();

            // configure Cayenne domain
            domain = new DataDomain(name);
            ClassDescriptorMap descriptors = domain
                    .getEntityResolver()
                    .getClassDescriptorMap();

            descriptors.addFactory(new CjpaClassDescriptorFactory(descriptors));
            configuration.addDomain(domain);

            EntityMapLoader loader = new EntityMapLoader(info);

            // we must set enhancer in this exact place, between JPA and Cayenne mapping
            // loading. By now all the JpaEntities are loaded (using separate unit class
            // loader) and Cayenne mapping will be using the App ClassLoader.
            Map<String, JpaClassDescriptor> managedClasses = loader
                    .getEntityMap()
                    .getMangedClasses();

            info.addTransformer(new UnitClassTranformer(managedClasses, new Enhancer(
                    new JpaEnhancerVisitorFactory(managedClasses))));

            DataMapConverter converter = new DataMapConverter();
            DataMap cayenneMap = converter.toDataMap(name, loader.getContext());

            DataSource dataSource = info.getTransactionType() == PersistenceUnitTransactionType.JTA
                    ? info.getJtaDataSource()
                    : info.getNonJtaDataSource();

            DbAdapter adapter = createCustomAdapter(loader.getContext(), info);

            DataNode node = new DataNode(name);

            if (adapter == null) {
                adapter = new AutoAdapter(new NodeDataSource(node));
            }

            node.setAdapter(adapter);

            node.setDataSource(dataSource);
            node.addDataMap(cayenneMap);

            domain.addNode(node);

            if ("true".equalsIgnoreCase(info.getProperties().getProperty(
                    CREATE_SCHEMA_PROPERTY))) {
                loadSchema(dataSource, adapter, cayenneMap);
            }

            long t1 = System.currentTimeMillis();

            // report conflicts...
            ValidationResult conflicts = loader.getContext().getConflicts();
            if (conflicts.hasFailures()) {
                for (Object failure : conflicts.getFailures()) {
                    logger.info("*** mapping conflict: " + failure);
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("loaded persistence unit '"
                        + name
                        + "' in "
                        + (t1 - t0)
                        + " ms.");
            }
        }

        JpaEntityManagerFactory factory = new JpaEntityManagerFactory(domain, info);
        factory.setDelegate(this);
        return factory;
    }

    /**
     * Loads database schema if it doesn't yet exist.
     */
    protected void loadSchema(DataSource dataSource, DbAdapter adapter, DataMap map) {

        Collection tables = map.getDbEntities();
        if (tables.isEmpty()) {
            return;
        }

        // sniff a first table precense

        // TODO: andrus 9/1/2006 - should we make this check a part of DbGenerator (and
        // query - a part of DbAdapter)?
        DbEntity table = (DbEntity) tables.iterator().next();

        try {
            Connection c = dataSource.getConnection();
            try {

                String tableName = table.getName().toLowerCase();

                // select all tables to avoid case sensitivity issues.
                ResultSet rs = c.getMetaData().getTables(
                        table.getCatalog(),
                        table.getSchema(),
                        null,
                        null);

                try {
                    while (rs.next()) {
                        String sqlName = rs.getString("TABLE_NAME");
                        if (tableName.equals(sqlName.toLowerCase())) {
                            logger.debug("table "
                                    + table.getFullyQualifiedName()
                                    + " is present; will skip schema generation.");
                            return;
                        }
                    }
                }
                finally {
                    rs.close();
                }
            }
            finally {
                c.close();
            }
        }
        catch (SQLException e1) {
            // db exists
            logger.debug("error generating schema, assuming schema exists.");
            return;
        }

        logger.debug("table "
                + table.getFullyQualifiedName()
                + " is absent; will continue with schema generation.");

        // run generator
        DbGenerator generator = new DbGenerator(adapter, map);
        try {
            generator.runGenerator(dataSource);
        }
        catch (Exception e) {

        }
    }

    protected DbAdapter createCustomAdapter(
            EntityMapLoaderContext context,
            PersistenceUnitInfo info) {

        String adapterKey = DefaultDataSourceFactory.getPropertyName(info
                .getPersistenceUnitName(), ConnectionProperties.ADAPTER_KEY);
        String adapterClass = info.getProperties().getProperty(adapterKey);

        if (Util.isEmptyString(adapterClass)) {
            return null;
        }

        try {
            // adapter class is not enhanced, so use a normal class loader
            Class dbAdapterClass = Class.forName(adapterClass, true, Thread
                    .currentThread()
                    .getContextClassLoader());
            return (DbAdapter) dbAdapterClass.newInstance();
        }
        catch (Exception e) {
            context.recordConflict(new SimpleValidationFailure(
                    info,
                    "Failed to load adapter '"
                            + adapterClass
                            + "', message: "
                            + e.getLocalizedMessage()));
            return null;
        }
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    // TODO: andrus, 4/29/2006 - this is copied from non-public conf.NodeDataSource. In
    // Cayenne > 1.2 make it public.
    class NodeDataSource implements DataSource {

        DataNode node;

        NodeDataSource(DataNode node) {
            this.node = node;
        }

        public Connection getConnection() throws SQLException {
            return node.getDataSource().getConnection();
        }

        public Connection getConnection(String username, String password)
                throws SQLException {
            return node.getDataSource().getConnection(username, password);
        }

        public PrintWriter getLogWriter() throws SQLException {
            return node.getDataSource().getLogWriter();
        }

        public void setLogWriter(PrintWriter out) throws SQLException {
            node.getDataSource().setLogWriter(out);
        }

        public void setLoginTimeout(int seconds) throws SQLException {
            node.getDataSource().setLoginTimeout(seconds);
        }

        public int getLoginTimeout() throws SQLException {
            return node.getDataSource().getLoginTimeout();
        }
    }

    class LazyConfiguration extends Configuration {

        @Override
        public boolean canInitialize() {
            return true;
        }

        @Override
        public void initialize() throws Exception {
        }

        @Override
        public void didInitialize() {
        }

        @Override
        protected ResourceLocator getResourceLocator() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected InputStream getDomainConfiguration() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected InputStream getMapConfiguration(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected InputStream getViewConfiguration(String location) {
            throw new UnsupportedOperationException();
        }
    }
}
