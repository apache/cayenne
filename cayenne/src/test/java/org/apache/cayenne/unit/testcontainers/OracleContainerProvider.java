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
package org.apache.cayenne.unit.testcontainers;

import java.time.Duration;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

public class OracleContainerProvider extends TestContainerProvider {

    @Override
    public JdbcDatabaseContainer<?> startContainer(String version) {
        JdbcDatabaseContainer<?> container = super.startContainer(version);
        return container;
    }

    @Override
    JdbcDatabaseContainer<?> createContainer(DockerImageName dockerImageName) {
        return new OracleContainer(dockerImageName)
                .withStartupTimeout(Duration.ofMinutes(5));
    }

    @Override
    String getDockerImage() {
        return "gvenzl/oracle-xe:18-slim-faststart";
    }

    @Override
    public Class<? extends JdbcAdapter> getAdapterClass() {
        return OracleAdapter.class;
    }
}
