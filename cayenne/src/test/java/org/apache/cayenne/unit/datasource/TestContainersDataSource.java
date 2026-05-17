/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.unit.datasource;

import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

abstract class TestContainersDataSource {

    private static final String CONNECTION_DB_VERSION = "cayenneTestDbVersion";

    protected static DataSourceDescriptor start(TestContainersDataSource dataSource) {
        String version = System.getProperty(CONNECTION_DB_VERSION);
        JdbcDatabaseContainer<?> container = dataSource.startContainer(version);

        return DataSourceDescriptorFactory.create(
                container.getUsername(),
                container.getPassword(),
                container.getJdbcUrl(),
                container.getDriverClassName());
    }

    protected JdbcDatabaseContainer<?> startContainer(String version) {
        DockerImageName baseName = DockerImageName.parse(dockerImage());
        DockerImageName fullName = version != null ? baseName.withTag(version) : baseName;

        JdbcDatabaseContainer<?> container = createContainer(fullName);
        container.start();
        return container;
    }

    protected abstract JdbcDatabaseContainer<?> createContainer(DockerImageName dockerImageName);

    protected abstract String dockerImage();
}
