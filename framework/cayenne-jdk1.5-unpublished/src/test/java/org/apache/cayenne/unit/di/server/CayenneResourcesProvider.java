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
package org.apache.cayenne.unit.di.server;

import java.io.InputStream;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.InputStreamResource;

public class CayenneResourcesProvider implements Provider<CayenneResources> {

    private static Log logger = LogFactory.getLog(CayenneResourcesProvider.class);

    public static final String TEST_RESOURCES_DESCRIPTOR = "spring-test-resources.xml";

    public static final String CONNECTION_NAME_KEY = "cayenneTestConnection";
    public static final String DEFAULT_CONNECTION_KEY = "internal_embedded_datasource";

    public static final String SKIP_SCHEMA_KEY = "cayenne.test.schema.skip";
    public static final String SCHEMA_SETUP_STACK = "SchemaSetupStack";

    @Inject
    private DataSourceInfo dataSourceInfo;

    public CayenneResources get() throws ConfigurationException {

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

        resources.setConnectionInfo(dataSourceInfo);

        // rebuild schema after the resources instance is loaded so that after
        // possible initial failure we don't attempt rebuilding schema in subsequent
        // tests
        try {
            rebuildSchema(factory);
        }
        catch (Exception ex) {
            logger.error("Error generating schema...", ex);
            throw new RuntimeException("Error generating schema");
        }

        return resources;
    }

    /**
     * Completely rebuilds test schema.
     */
    private void rebuildSchema(BeanFactory beanFactory) throws Exception {

        if ("true".equalsIgnoreCase(System.getProperty(SKIP_SCHEMA_KEY))) {
            logger.info("skipping schema generation... ");
            return;
        }

        // generate schema using a special AccessStack that
        // combines all DataMaps that require schema support
        // schema generation is done like that instead of
        // per stack on demand, to avoid conflicts when
        // dropping and generating PK objects.
        AccessStack stack = (AccessStack) beanFactory.getBean(
                SCHEMA_SETUP_STACK,
                AccessStack.class);

        stack.dropSchema();
        stack.dropPKSupport();
        stack.createSchema();
        stack.createPKSupport();
    }

}
