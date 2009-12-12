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
package org.apache.cayenne.configuration;

import java.net.URL;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.resource.URLResource;

import com.mockrunner.mock.jdbc.MockDataSource;

public class XMLPoolingDataSourceFactoryTest extends TestCase {

    public void testGetDataSource() throws Exception {

        String baseUrl = getClass().getPackage().getName().replace('.', '/');
        URL url = getClass().getClassLoader().getResource(
                baseUrl + "/testNode1.driver.xml");
        assertNotNull(url);

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setConfigurationSource(new URLResource(url));

        final DataSource dataSource = new MockDataSource();

        XMLPoolingDataSourceFactory factory = new XMLPoolingDataSourceFactory() {

            // override super to make test assertions and to prevent DB connection
            @Override
            protected DataSource createDataSource(DataSourceInfo dataSourceDescriptor)
                    throws Exception {

                assertEquals("jdbcDriver", dataSourceDescriptor.getJdbcDriver());
                assertEquals("jdbcUrl", dataSourceDescriptor.getDataSourceUrl());
                assertEquals(2, dataSourceDescriptor.getMinConnections());
                assertEquals(3, dataSourceDescriptor.getMaxConnections());
                assertEquals("jdbcUserName", dataSourceDescriptor.getUserName());
                assertEquals("jdbcPassword", dataSourceDescriptor.getPassword());
                return dataSource;
            }
        };

        DataSource newDataSource = factory.getDataSource(nodeDescriptor);
        assertSame(dataSource, newDataSource);
    }
}
