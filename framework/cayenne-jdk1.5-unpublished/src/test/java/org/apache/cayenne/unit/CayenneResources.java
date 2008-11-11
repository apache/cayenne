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

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.ConnectionProperties;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.conn.PoolDataSource;
import org.apache.cayenne.conn.PoolManager;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.InputStreamResource;

/**
 * Initializes connections for Cayenne unit tests.
 * 
 */
// TODO: switch to Spring
public class CayenneResources implements BeanFactoryAware {

    private static Log logObj = LogFactory.getLog(CayenneResources.class);

    public static final String TEST_RESOURCES_DESCRIPTOR = "spring-test-resources.xml";

    public static final String CONNECTION_NAME_KEY = "cayenne.test.connection";
    public static final String SKIP_SCHEMA_KEY = "cayenne.test.schema.skip";
    public static final String TEST_DIR_KEY = "cayenne.test.dir";
    public static final String DEFAULT_TEST_DIR = "target/testrun";

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
            logObj.error("Can't locate resource: " + TEST_RESOURCES_DESCRIPTOR);
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

    protected File testDir;
    protected DataSourceInfo connectionInfo;
    protected DataSource dataSource;
    protected BeanFactory beanFactory;
    protected Map adapterMap;

    /**
     * Returns shared test resource instance.
     * 
     * @return CayenneTestResources
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
                logObj.error("Error generating schema...", ex);
                throw new RuntimeException("Error generating schema");
            }
        }
        return resources;
    }

    /**
     * Copies resources to a file, thus making it available to the caller as File.
     */
    public static void copyResourceToFile(String resourceName, File file) {
        URL in = getResourceURL(resourceName);

        if (!Util.copy(in, file)) {
            throw new CayenneRuntimeException("Error copying resource to file : " + file);
        }
    }

    /**
     * Returns a guaranteed non-null resource for a given name.
     */
    public static URL getResourceURL(String name) {
        URL in = Thread.currentThread().getContextClassLoader().getResource(name);

        // Fix for the issue described at https://issues.apache.org/struts/browse/SB-35
        // Basically, spaces in filenames make maven cry.
        try {
            in = new URL(in.toExternalForm().replaceAll(" ", "%20"));
        }
        catch (MalformedURLException e) {
            throw new CayenneRuntimeException("Error constructing URL.", e);
        }

        if (in == null) {
            throw new CayenneRuntimeException("Resource not found: " + name);
        }

        return in;
    }

    /**
     * Returns a guaranteed non-null resource for a given name.
     */
    public static InputStream getResource(String name) {
        InputStream in = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream(name);

        if (in == null) {
            throw new CayenneRuntimeException("Resource not found: " + name);
        }

        return in;
    }

    public CayenneResources(Map adapterMap) {
        this.adapterMap = adapterMap;

        setupTestDir();
    }

    /**
     * Completely rebuilds test schema.
     */
    void rebuildSchema() throws Exception {

        if ("true".equalsIgnoreCase(System.getProperty(SKIP_SCHEMA_KEY))) {
            logObj.info("skipping schema generation... ");
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

            logObj.info("Invalid connection key '"
                    + connectionKey
                    + "', trying default: "
                    + DEFAULT_CONNECTION_KEY);

            connectionInfo = ConnectionProperties.getInstance().getConnectionInfo(
                    DEFAULT_CONNECTION_KEY);
        }

        if (connectionInfo == null) {
            throw new RuntimeException("Null connection info for key: " + connectionKey);
        }

        logObj.info("test connection info: " + connectionInfo);
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
    public AccessStackAdapter getAccessStackAdapter(Class adapterClass) {
        AccessStackAdapter stackAdapter = (AccessStackAdapter) adapterMap
                .get(adapterClass.getName());

        if (stackAdapter == null) {
            throw new RuntimeException("No AccessStackAdapter for DbAdapter class: "
                    + adapterClass);
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
     * Returns a test directory that is used as a scratch area.
     */
    public File getTestDir() {
        return testDir;
    }

    /**
     * Creates new DataNode.
     */
    public DataNode newDataNode(String name) throws Exception {
        AccessStackAdapter adapter = getAccessStackAdapter(Class.forName(connectionInfo
                .getAdapterClassName()));

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
                    connectionInfo.getPassword());
        }
        catch (Exception ex) {
            logObj.error("Can not create shared data source.", ex);
            throw new RuntimeException("Can not create shared data source.", ex);
        }
    }

    protected void setupTestDir() {
        String testDirName = System.getProperty(TEST_DIR_KEY);

        if (testDirName == null) {
            testDirName = DEFAULT_TEST_DIR;

            logObj.info("No property '"
                    + TEST_DIR_KEY
                    + "' set. Using default directory: '"
                    + testDirName
                    + "'");
        }

        testDir = new File(testDirName);

        // delete old tests
        if (testDir.exists()) {
            if (!Util.delete(testDirName, true)) {
                throw new RuntimeException("Error deleting test directory: "
                        + testDirName);
            }
        }

        if (!testDir.mkdirs()) {
            throw new RuntimeException("Error creating test directory: " + testDirName);
        }
    }
}
