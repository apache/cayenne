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
package org.objectstyle.cayenne.unit;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.ConnectionProperties;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.conn.PoolDataSource;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.unit.util.SQLTemplateCustomizer;
import org.objectstyle.cayenne.util.Util;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.InputStreamResource;

/**
 * Initializes connections for Cayenne unit tests.
 * 
 * @author Andrei Adamchik
 */
// TODO: switch to Spring
public class CayenneTestResources implements BeanFactoryAware {

    private static Logger logObj = Logger.getLogger(CayenneTestResources.class);

    public static final String TEST_RESOURCES_DESCRIPTOR = "spring-test-resources.xml";

    public static final String CONNECTION_NAME_KEY = "cayenne.test.connection";
    public static final String SKIP_SCHEMA_KEY = "cayenne.test.schema.skip";
    public static final String TEST_DIR_KEY = "cayenne.test.dir";

    public static final String SCHEMA_SETUP_STACK = "SchemaSetupStack";
    public static final String SQL_TEMPLATE_CUSTOMIZER = "SQLTemplateCustomizer";
    public static final String DEFAULT_CONNECTION_KEY = "internal_embedded_datasource";

    private static CayenneTestResources resources;

    static CayenneTestResources loadResources() {
        Configuration.configureCommonLogging();

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
        CayenneTestResources resources = (CayenneTestResources) factory.getBean(
                "TestResources",
                CayenneTestResources.class);

        String connectionKey = System.getProperty(CONNECTION_NAME_KEY);
        if (connectionKey == null) {
            logObj.info("No connection key property set '"
                    + CONNECTION_NAME_KEY
                    + "', using default: "
                    + DEFAULT_CONNECTION_KEY);
            connectionKey = DEFAULT_CONNECTION_KEY;
        }

        resources.setConnectionKey(connectionKey);

        try {
            resources.rebuildSchema();
        }
        catch (Exception ex) {
            logObj.error("Error generating schema...", ex);
            throw new RuntimeException("Error generating schema");
        }

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
    public static CayenneTestResources getResources() {
        if (resources == null) {
            resources = loadResources();
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

    public CayenneTestResources(Map adapterMap) {
        this.adapterMap = adapterMap;

        setupTestDir();
    }

    /**
     * Completely rebuilds test schema.
     */
    void rebuildSchema() throws Exception {
        
        if("true".equalsIgnoreCase(System.getProperty(SKIP_SCHEMA_KEY))) {
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
        if (connectionKey == null) {
            throw new RuntimeException("Null connection key");
        }

        connectionInfo = ConnectionProperties.getInstance().getConnectionInfo(
                connectionKey);

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
            // data source
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
            testDirName = "testrun";

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