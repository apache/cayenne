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
package org.objectstyle.cayenne.unit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.ConnectionProperties;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.conn.PoolDataSource;
import org.objectstyle.cayenne.conn.PoolManager;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.unit.util.SQLTemplateCustomizer;
import org.objectstyle.cayenne.util.Util;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.xml.XmlBeanFactory;

/**
 * Initializes connections for Cayenne unit tests.
 * 
 * @author Andrei Adamchik
 */
// TODO: switch to Spring
public class CayenneTestResources implements BeanFactoryAware {

    private static Logger logObj = Logger.getLogger(CayenneTestResources.class);

    public static final String CONFIG_KEY = "cayenne.test.config";

    public static final String CONNECTION_NAME_KEY = "cayenne.test.connection";
    public static final String TEST_DIR_KEY = "cayenne.test.dir";

    public static final String SCHEMA_SETUP_STACK = "SchemaSetupStack";
    public static final String SQL_TEMPLATE_CUSTOMIZER = "SQLTemplateCustomizer";

    private static CayenneTestResources resources;

    static {
        Configuration.configureCommonLogging();

        // bootstrap shared CayenneTestResources

        String testResourcesPath = System.getProperty(CONFIG_KEY);
        File testResources = null;
        
        if(testResourcesPath != null) {
            File altResources = new File(testResourcesPath);
            if(altResources.isFile()) {
                testResources = altResources;
            }
        }

        if (testResources == null) {
            File testDir = new File(
                    new File(new File(new File("build"), "tests"), "deps"),
                    "test-resources");
            testResources = new File(testDir, "spring-test-resources.xml");
        }
        
        // configure shared resources instance with Spring
        logObj.info("== Loading test configuration from " + testResources);
        logObj.info("== To override set property '" + CONFIG_KEY + "'");
        InputStream in = null;
        try {
            in = new FileInputStream(testResources);
        }
        catch (IOException ioex) {
            logObj.error("Can't open test resources..." + testResources, ioex);
            throw new RuntimeException("Error loading", ioex);
        }
        BeanFactory factory = new XmlBeanFactory(in);
        resources = (CayenneTestResources) factory.getBean(
                "TestResources",
                CayenneTestResources.class);

        // must finish config manually
        resources.setConnectionKey(System.getProperty(CONNECTION_NAME_KEY));

        try {
            resources.rebuildSchema();
        }
        catch (Exception ex) {
            logObj.error("Error generating schema...", ex);
            throw new RuntimeException("Error generating schema");
        }
    }

    protected File testDir;
    protected File testResourcesDir;
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
        return resources;
    }

    public CayenneTestResources(Map adapterMap) {
        this.adapterMap = adapterMap;

        setupTestDir();
    }

    /**
     * Completely rebuilds test schema.
     */
    public void rebuildSchema() throws Exception {
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
            throw new RuntimeException("Null connection key.");
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
    public AccessStackAdapter getAccessStackAdapter(DbAdapter adapter) {
        AccessStackAdapter stackAdapter = null;

        if (adapter != null) {
            stackAdapter = (AccessStackAdapter) adapterMap.get(adapter
                    .getClass()
                    .getName());
        }
        return stackAdapter != null ? stackAdapter : new AccessStackAdapter(adapter);
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

    public File getTestResourcesDir() {
        return testResourcesDir;
    }

    public void setTestResourcesDir(File dir) {
        this.testResourcesDir = dir;
    }

    /**
     * Creates new DataNode.
     */
    public DataNode newDataNode(String name) throws Exception {
        DbAdapter adapter = (DbAdapter) Class.forName(
                connectionInfo.getAdapterClassName()).newInstance();
        DataNode node = adapter.createDataNode(name);
        node.setDataSource(dataSource);
        node.setAdapter(adapter);
        return node;
    }

    /**
     * Returns connection information.
     */
    public DataSourceInfo getConnectionInfo() throws Exception {
        return connectionInfo;
    }

    protected DataSource createDataSource() {
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