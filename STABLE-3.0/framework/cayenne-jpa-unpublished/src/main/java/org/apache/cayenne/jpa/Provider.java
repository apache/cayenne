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

import java.io.PrintWriter;
import java.lang.instrument.ClassFileTransformer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.ConnectionProperties;
import org.apache.cayenne.conf.ResourceFinder;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.enhancer.Enhancer;
import org.apache.cayenne.jpa.bridge.DataMapConverter;
import org.apache.cayenne.jpa.conf.DefaultDataSourceFactory;
import org.apache.cayenne.jpa.conf.EntityMapLoader;
import org.apache.cayenne.jpa.conf.EntityMapLoaderContext;
import org.apache.cayenne.jpa.conf.UnitLoader;
import org.apache.cayenne.jpa.enhancer.JpaEnhancerVisitorFactory;
import org.apache.cayenne.jpa.instrument.UnitClassTransformer;
import org.apache.cayenne.jpa.map.JpaClassDescriptor;
import org.apache.cayenne.jpa.reflect.JpaClassDescriptorFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.FaultFactory;
import org.apache.cayenne.reflect.SingletonFaultFactory;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.SimpleValidationFailure;
import org.apache.cayenne.validation.ValidationResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A PersistenceProvider implementation based on Cayenne stack. Wraps a Cayenne
 * Configuration instance.
 * 
 */
public class Provider implements PersistenceProvider {

    // spec-defined properties per ch. 7.1.3.1.
    public static final String PROVIDER_PROPERTY = "javax.persistence.provider";
    public static final String TRANSACTION_TYPE_PROPERTY = "javax.persistence.transactionType";
    public static final String JTA_DATA_SOURCE_PROPERTY = "javax.persistence.jtaDataSource";
    public static final String NON_JTA_DATA_SOURCE_PROPERTY = "javax.persistence.nonJtaDataSource";

    // provider-specific properties. Must use provider namespace per ch. 7.1.3.1.
    public static final String DROP_SCHEMA_PROPERTY = "org.apache.cayenne.schema.drop";
    public static final String CREATE_SCHEMA_PROPERTY = "org.apache.cayenne.schema.create";
    public static final String DATA_SOURCE_FACTORY_PROPERTY = "org.apache.cayenne.jpa.jpaDataSourceFactory";

    // ... DataSource
    public static final String ADAPTER_PROPERTY = "org.apache.cayenne."
            + ConnectionProperties.ADAPTER_KEY;
    public static final String DATA_SOURCE_DRIVER_PROPERTY = "org.apache.cayenne.datasource."
            + ConnectionProperties.DRIVER_KEY;
    public static final String DATA_SOURCE_URL_PROPERTY = "org.apache.cayenne.datasource."
            + ConnectionProperties.URL_KEY;
    public static final String DATA_SOURCE_USER_NAME_PROPERTY = "org.apache.cayenne.datasource."
            + ConnectionProperties.USER_NAME_KEY;
    public static final String DATA_SOURCE_PASSWORD_PROPERTY = "org.apache.cayenne.datasource."
            + ConnectionProperties.PASSWORD_KEY;
    public static final String DATA_SOURCE_MIN_CONNECTIONS_PROPERTY = "org.apache.cayenne.datasource.jdbc.minConnections";
    public static final String DATA_SOURCE_MAX_CONNECTIONS_PROPERTY = "org.apache.cayenne.datasource.jdbc.maxConnections";

    protected boolean validateDescriptors;
    protected UnitLoader unitLoader;
    protected Properties defaultProperties;
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
        this.validateDescriptors = validateDescriptors;
        this.defaultProperties = new Properties();

        configureEnvironmentProperties();
        configureDefaultProperties();

        this.logger = LogFactory.getLog(getClass());
        this.configuration = new LazyConfiguration();

        // set a singleton that may be used by Cayenne
        Configuration.initializeSharedConfiguration(configuration);
    }

    /**
     * Loads default properties from the Java environment.
     */
    protected void configureEnvironmentProperties() {
        String dsFactory = System.getProperty(DATA_SOURCE_FACTORY_PROPERTY);
        if (dsFactory != null) {
            defaultProperties.put(DATA_SOURCE_FACTORY_PROPERTY, dsFactory);
        }

        String transactionType = System.getProperty(TRANSACTION_TYPE_PROPERTY);
        if (transactionType != null) {
            defaultProperties.put(TRANSACTION_TYPE_PROPERTY, transactionType);
        }
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
     * Called by Persistence class when an EntityManagerFactory is to be created. Creates
     * a {@link JpaUnit} and calls
     * {@link #createContainerEntityManagerFactory(PersistenceUnitInfo, Map)}.
     */
    @SuppressWarnings("unchecked")
    public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {

        JpaUnit ui = loadUnit(emName);

        if (ui == null) {
            return null;
        }

        if (logger.isInfoEnabled()) {
            logger.info("Extra PersistenceUnitInfo properties: " + map);
        }

        // override properties
        if (map != null) {
            ui.addProperties(map);
        }

        // set default properties if they are not set explicitly
        Properties properties = ui.getProperties();
        for (Map.Entry property : defaultProperties.entrySet()) {
            if (!properties.containsKey(property.getKey())) {
                properties.put(property.getKey(), property.getValue());
            }
        }

        // check if we are allowed to handle this unit (JPA Spec, 7.2)
        String provider = ui.getPersistenceProviderClassName();
        if (provider != null && !provider.equals(this.getClass().getName())) {
            return null;
        }

        // do not pass properties further down, they are already acounted for in the
        // PersistenceUnitInfo.
        return createContainerEntityManagerFactory(ui, null);
    }

    /**
     * Called by the container when an EntityManagerFactory is to be created. Returns a
     * {@link EntityManagerFactory} which is a DataDomain wrapper. Note that Cayenne
     * provider will ignore all but 'javax.persistence.transactionType' property in the
     * passed property map.
     */
    @SuppressWarnings("unchecked")
    public synchronized EntityManagerFactory createContainerEntityManagerFactory(
            PersistenceUnitInfo unit,
            Map map) {

        if (logger.isInfoEnabled() && map != null) {
            logger.info("Extra container PersistenceUnitInfo properties: " + map);
        }

        String name = unit.getPersistenceUnitName();
        DataDomain domain = configuration.getDomain(name);

        // TODO: andrus, 2/3/2007 - considering property overrides, it may be a bad idea
        // to cache domains. Essentially we are caching a PersistenceUnitInfo with a given
        // name, without a possibility to refresh it. But maybe this is ok...?
        if (domain == null) {

            long t0 = System.currentTimeMillis();

            boolean isJTA = isJta(unit, map);

            EntityMapLoader loader = new EntityMapLoader(unit);

            // add transformer before DataMapConverter starts loading the classes via app
            // class loader
            ClassFileTransformer enhancer = new Enhancer(new JpaEnhancerVisitorFactory(
                    loader.getEntityMap()));
            Map<String, JpaClassDescriptor> managedClasses = loader
                    .getEntityMap()
                    .getManagedClasses();
            unit.addTransformer(new UnitClassTransformer(managedClasses, loader
                    .getContext()
                    .getTempClassLoader(), enhancer));

            DataMapConverter converter = new DataMapConverter();
            DataMap cayenneMap = converter.toDataMap(name, loader.getContext());

            // configure Cayenne domain
            domain = new DataDomain(name);
            ClassDescriptorMap descriptors = domain
                    .getEntityResolver()
                    .getClassDescriptorMap();
            FaultFactory faultFactory = new SingletonFaultFactory();
            descriptors.addFactory(new JpaClassDescriptorFactory(
                    loader.getEntityMap(),
                    descriptors,
                    faultFactory));
            configuration.addDomain(domain);

            // TODO: andrus, 2/3/2007 - clarify this logic.... JTA EM may not always mean
            // JTA DS?
            DataSource dataSource = isJTA ? unit.getJtaDataSource() : unit
                    .getNonJtaDataSource();

            if (dataSource == null) {
                String jta = isJTA ? "JTA" : "non-JTA";
                logger.warn("NULL "
                        + jta
                        + " DataSource returned from PersistenceUnitInfo");
            }

            DbAdapter adapter = createCustomAdapter(loader.getContext(), unit);
            DataNode node = new DataNode(name);
            if (adapter == null) {
                adapter = new AutoAdapter(new NodeDataSource(node));
            }
            
            // TODO: add a property to generate schema per bundled or custom strategies
            node.setSchemaUpdateStrategy(new SkipSchemaUpdateStrategy());

            node.setAdapter(adapter);
            node.setDataSource(dataSource);
            node.addDataMap(cayenneMap);

            domain.addNode(node);

            // note that for now we do not apply object layer defaults, as that would
            // require extra enhancement for runtime relationships...
            domain.getEntityResolver().applyDBLayerDefaults();
            domain.setUsingExternalTransactions(isJTA);

            if ("true".equalsIgnoreCase(unit.getProperties().getProperty(
                    DROP_SCHEMA_PROPERTY))) {
                dropSchema(dataSource, adapter, cayenneMap);
            }

            if ("true".equalsIgnoreCase(unit.getProperties().getProperty(
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

        // see TODO above - JTA vs RESOURCE_LOCAL is cached per domain... maybe need to
        // change that
        return domain.isUsingExternalTransactions() ? new JtaEntityManagerFactory(
                this,
                domain,
                unit) : new ResourceLocalEntityManagerFactory(this, domain, unit);
    }

    /**
     * Returns whether provided configuration specifies a JTA or RESOURCE_LOCAL
     * EntityManager.
     */
    private boolean isJta(PersistenceUnitInfo unit, Map<?, ?> overrides) {
        PersistenceUnitTransactionType txType;
        String txTypeOverride = (overrides != null) ? (String) overrides
                .get(TRANSACTION_TYPE_PROPERTY) : null;
        if (txTypeOverride != null) {
            txType = PersistenceUnitTransactionType.valueOf(txTypeOverride);
        }
        else {
            txType = unit.getTransactionType();
        }

        return txType == PersistenceUnitTransactionType.JTA;
    }

    /**
     * Drops database schema if it exists.
     */
    protected void dropSchema(DataSource dataSource, DbAdapter adapter, DataMap map) {

        Collection<DbEntity> tables = map.getDbEntities();
        if (tables.isEmpty()) {
            return;
        }

        if (!existingTablesDetected(dataSource, tables)) {
            logger.debug("did not find at least one table; will skip dropping schema.");
            return;
        }

        logger.debug("detected existings schema;; will continue with schema generation.");

        // run generator
        DbGenerator generator = new DbGenerator(adapter, map);
        generator.setShouldCreateFKConstraints(false);
        generator.setShouldCreatePKSupport(false);
        generator.setShouldCreateTables(false);
        generator.setShouldDropPKSupport(true);
        generator.setShouldDropTables(true);

        try {
            generator.runGenerator(dataSource);
        }
        catch (Exception e) {

        }
    }

    /**
     * Loads database schema if it doesn't yet exist.
     */
    protected void loadSchema(DataSource dataSource, DbAdapter adapter, DataMap map) {

        Collection<DbEntity> tables = map.getDbEntities();
        if (tables.isEmpty()) {
            return;
        }

        if (existingTablesDetected(dataSource, tables)) {
            logger.debug("detected existings schema; will skip schema generation.");
            return;
        }

        logger.debug("did not find at least one table; "
                + "will continue with schema generation.");

        // run generator
        DbGenerator generator = new DbGenerator(adapter, map);
        generator.setShouldCreateFKConstraints(true);
        generator.setShouldCreatePKSupport(true);
        generator.setShouldCreateTables(true);
        generator.setShouldDropPKSupport(false);
        generator.setShouldDropTables(false);

        try {
            generator.runGenerator(dataSource);
        }
        catch (Exception e) {

        }
    }

    /**
     * Checks that at least one table in the collection of DbEntities is present in the
     * DB. Negative or unknown outcomes result in false being returned.
     */
    protected boolean existingTablesDetected(
            DataSource dataSource,
            Collection<DbEntity> tables) {

        if (tables.isEmpty()) {
            return false;
        }

        // sniff a first table presence
        DbEntity table = tables.iterator().next();

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
                            return true;
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
        catch (SQLException e) {
            // db exists
            logger.debug("error checking schema", e);
        }

        return false;
    }

    protected DbAdapter createCustomAdapter(
            EntityMapLoaderContext context,
            PersistenceUnitInfo info) {

        String adapterClass = info.getProperties().getProperty(ADAPTER_PROPERTY);

        if (Util.isEmptyString(adapterClass)) {
            return null;
        }

        try {
            // adapter class is not enhanced, so use a normal class loader
            Class<?> dbAdapterClass = Class.forName(adapterClass, true, Thread
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

    /**
     * Loads a named JpaUnit using internal UnitLoader.
     */
    protected JpaUnit loadUnit(String emName) {
        // TODO: Andrus, 2/11/2006 - cache loaded units (or factories)...?
        return getUnitLoader().loadUnit(emName);
    }

    /**
     * Returns unit loader, lazily creating it on first invocation.
     */
    protected UnitLoader getUnitLoader() {
        if (unitLoader == null) {
            this.unitLoader = new UnitLoader(validateDescriptors);
        }

        return unitLoader;
    }

    protected String getDefaultProperty(String key) {
        return defaultProperties.getProperty(key);
    }

    // TODO: andrus, 4/29/2006 - this is copied from non-public conf.NodeDataSource. In
    // Cayenne > 1.2 make it public.
    class NodeDataSource implements DataSource {

        DataNode node;

        NodeDataSource(DataNode node) {
            this.node = node;
        }

        private DataSource getDataSource() {
            DataSource ds = node.getDataSource();

            if (ds == null) {
                throw new IllegalStateException("DataNode has null DataSource");
            }

            return ds;
        }

        public Connection getConnection() throws SQLException {
            return getDataSource().getConnection();
        }

        public Connection getConnection(String username, String password)
                throws SQLException {
            return getDataSource().getConnection(username, password);
        }

        public PrintWriter getLogWriter() throws SQLException {
            return getDataSource().getLogWriter();
        }

        public void setLogWriter(PrintWriter out) throws SQLException {
            getDataSource().setLogWriter(out);
        }

        public void setLoginTimeout(int seconds) throws SQLException {
            getDataSource().setLoginTimeout(seconds);
        }

        public int getLoginTimeout() throws SQLException {
            return getDataSource().getLoginTimeout();
        }
        
        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }

        /**
         * @since 3.0
         */
        // JDBC 4 compatibility under Java 1.5
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }

    class LazyConfiguration extends Configuration {

        @Override
        public void initialize() throws Exception {
        }

        @Override
        protected ResourceFinder getResourceFinder() {
            throw new UnsupportedOperationException();
        }
    }
}
