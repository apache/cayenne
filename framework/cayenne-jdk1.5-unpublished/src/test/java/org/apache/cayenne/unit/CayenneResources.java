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

package org.apache.cayenne.unit;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.BatchQueryBuilderFactory;
import org.apache.cayenne.access.jdbc.DefaultBatchQueryBuilderFactory;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.conn.PoolDataSource;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.InputStreamResource;

/**
 * Initializes connections for Cayenne unit tests.
 */
public class CayenneResources implements BeanFactoryAware {

    private static Log logger = LogFactory.getLog(CayenneResources.class);

    public static final String TEST_RESOURCES_DESCRIPTOR = "spring-test-resources.xml";

    public static final String CONNECTION_NAME_KEY = "cayenneTestConnection";
    public static final String SKIP_SCHEMA_KEY = "cayenne.test.schema.skip";

    public static final String SCHEMA_SETUP_STACK = "SchemaSetupStack";
    public static final String SQL_TEMPLATE_CUSTOMIZER = "SQLTemplateCustomizer";
    public static final String DEFAULT_CONNECTION_KEY = "internal_embedded_datasource";

    private static CayenneResources resources;

    private static CayenneResources loadResources() {

        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream(TEST_RESOURCES_DESCRIPTOR);

        if (in == null) {
            logger.error("Can't locate resource: " + TEST_RESOURCES_DESCRIPTOR);
            throw new RuntimeException(
                    "Can't locate resource descriptor in the ClassLoader: "
                            + TEST_RESOURCES_DESCRIPTOR);
        }

        BeanFactory factory = new XmlBeanFactory(new InputStreamResource(in));
        CayenneResources resources = (CayenneResources) factory.getBean(
                "TestResources",
                CayenneResources.class);

        resources.setConnectionKey(System.getProperty(CONNECTION_NAME_KEY));

        return resources;
    }

    protected DataSourceInfo connectionInfo;
    protected DataSource dataSource;
    protected BeanFactory beanFactory;
    protected Map<String, AccessStackAdapter> adapterMap;

    /**
     * Returns shared test resource instance.
     */
    public static CayenneResources getResources() {
        if (resources == null) {
            resources = loadResources();

            // rebuild schema after the resources static var is initialized so that after
            // possible initial failure we don't attempt rebuilding schema in subsequent
            // tests
            try {
                resources.rebuildSchema();
            }
            catch (Exception ex) {
                logger.error("Error generating schema...", ex);
                throw new RuntimeException("Error generating schema");
            }
        }
        return resources;
    }

    public CayenneResources(Map<String, AccessStackAdapter> adapterMap) {
        this.adapterMap = adapterMap;

        // kludge until we stop using Spring for unit tests and use Cayenne DI
        BatchQueryBuilderFactory factory = new DefaultBatchQueryBuilderFactory();
        for (AccessStackAdapter adapter : adapterMap.values()) {
            ((JdbcAdapter) adapter.getAdapter()).setBatchQueryBuilderFactory(factory);
        }
    }

    /**
     * Completely rebuilds test schema.
     */
    void rebuildSchema() throws Exception {

        if ("true".equalsIgnoreCase(System.getProperty(SKIP_SCHEMA_KEY))) {
            logger.info("skipping schema generation... ");
            return;
        }

        // generate schema using a special AccessStack that
        // combines all DataMaps that require schema support
        // schema generation is done like that instead of
        // per stack on demand, to avoid conflicts when
        // dropping and generating PK objects.
        AccessStack stack = getAccessStack(SCHEMA_SETUP_STACK);

        stack.dropSchema();
        stack.dropPKSupport();
        stack.createSchema();
        stack.createPKSupport();
    }

    public void setConnectionKey(String connectionKey) {

        connectionInfo = ConnectionProperties.getInstance().getConnectionInfo(
                connectionKey);

        // attempt default if invalid key is specified
        if (connectionInfo == null) {

            logger.info("Invalid connection key '"
                    + connectionKey
                    + "', trying default: "
                    + DEFAULT_CONNECTION_KEY);

            connectionInfo = ConnectionProperties.getInstance().getConnectionInfo(
                    DEFAULT_CONNECTION_KEY);
        }

        if (connectionInfo == null) {
            throw new RuntimeException("Null connection info for key: " + connectionKey);
        }

        logger.info("test connection info: " + connectionInfo);
        this.dataSource = createDataSource();
    }

    /**
     * BeanFactoryAware implementation to store BeanFactory.
     */
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public AccessStack getAccessStack(String name) {
        return (AccessStack) beanFactory.getBean(name, AccessStack.class);
    }

    public SQLTemplateCustomizer getSQLTemplateCustomizer() {
        BeanFactory child = (BeanFactory) beanFactory.getBean(
                SQL_TEMPLATE_CUSTOMIZER,
                BeanFactory.class);
        return (SQLTemplateCustomizer) child.getBean(
                SQL_TEMPLATE_CUSTOMIZER,
                SQLTemplateCustomizer.class);
    }

    /**
     * Returns DB-specific testing adapter.
     */
    public AccessStackAdapter getAccessStackAdapter(String adapterClassName) {
        AccessStackAdapter stackAdapter = adapterMap.get(adapterClassName);

        if (stackAdapter == null) {
            throw new RuntimeException("No AccessStackAdapter for DbAdapter class: "
                    + adapterClassName);
        }

        // post init
        stackAdapter.unchecked(this);

        return stackAdapter;
    }

    /**
     * Returns shared DataSource.
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Creates new DataNode.
     */
    public DataNode newDataNode(String name) throws Exception {
        AccessStackAdapter adapter = getAccessStackAdapter(connectionInfo
                .getAdapterClassName());

        DataNode node = new DataNode(name);
        node.setDataSource(dataSource);
        node.setAdapter(adapter.getAdapter());
        return node;
    }

    /**
     * Returns connection information.
     */
    public DataSourceInfo getConnectionInfo() {
        return connectionInfo;
    }

    public DataSource createDataSource() {

        try {
            PoolDataSource poolDS = new PoolDataSource(
                    connectionInfo.getJdbcDriver(),
                    connectionInfo.getDataSourceUrl());
            return new PoolManager(
                    poolDS,
                    1,
                    1,
                    connectionInfo.getUserName(),
                    connectionInfo.getPassword()) {

                @Override
                public void shutdown() throws SQLException {
                    // noop - make sure we are not shutdown by the test scope, but at the
                    // same time PoolManager methods are exposed (so we can't wrap
                    // PoolManager)
                }
            };
        }
        catch (Exception ex) {
            logger.error("Can not create shared data source.", ex);
            throw new RuntimeException("Can not create shared data source.", ex);
        }
    }

}
