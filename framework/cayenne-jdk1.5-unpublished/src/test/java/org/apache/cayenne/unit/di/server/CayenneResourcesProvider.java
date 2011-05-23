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

import javax.sql.DataSource;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.InputStreamResource;
import org.xml.sax.InputSource;

public class CayenneResourcesProvider implements Provider<CayenneResources> {

    private static Log logger = LogFactory.getLog(CayenneResourcesProvider.class);

    public static final String TEST_RESOURCES_DESCRIPTOR = "spring-test-resources.xml";

    public static final String CONNECTION_NAME_KEY = "cayenneTestConnection";
    public static final String DEFAULT_CONNECTION_KEY = "internal_embedded_datasource";

    public static final String SKIP_SCHEMA_KEY = "cayenne.test.schema.skip";

    private static String[] DATA_MAPS_REQUIREING_SCHEMA_SETUP = {
            "testmap.map.xml", "people.map.xml", "locking.map.xml",
            "relationships.map.xml", "multi-tier.map.xml", "generic.map.xml",
            "map-db1.map.xml", "map-db2.map.xml", "embeddable.map.xml",
            "qualified.map.xml", "quoted-identifiers.map.xml",
            "inheritance-single-table1.map.xml", "inheritance-vertical.map.xml"
    };

    @Inject
    private DataSource dataSource;

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

        // rebuild schema after the resources instance is loaded so that after
        // possible initial failure we don't attempt rebuilding schema in subsequent
        // tests
        try {
            rebuildSchema(resources);
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
    private void rebuildSchema(CayenneResources resources) throws Exception {

        if ("true".equalsIgnoreCase(System.getProperty(SKIP_SCHEMA_KEY))) {
            logger.info("skipping schema generation... ");
            return;
        }

        // generate schema combining all DataMaps that require schema support.
        // Schema generation is done like that instead of per DataMap on demand to avoid
        // conflicts when dropping and generating PK objects.

        DataMap[] maps = new DataMap[DATA_MAPS_REQUIREING_SCHEMA_SETUP.length];

        for (int i = 0; i < maps.length; i++) {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(
                    DATA_MAPS_REQUIREING_SCHEMA_SETUP[i]);
            InputSource in = new InputSource(stream);
            in.setSystemId(DATA_MAPS_REQUIREING_SCHEMA_SETUP[i]);
            maps[i] = new MapLoader().loadDataMap(in);
        }

        SchemaHelper schemaHelper = new SchemaHelper(dataSource, dataSourceInfo
                .getAdapterClassName(), resources, maps);

        schemaHelper.dropSchema();
        schemaHelper.dropPKSupport();
        schemaHelper.createSchema();
        schemaHelper.createPKSupport();
    }
}
