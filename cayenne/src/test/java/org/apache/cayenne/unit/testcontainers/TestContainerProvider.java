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

import org.apache.cayenne.dba.JdbcAdapter;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class TestContainerProvider {

    abstract JdbcDatabaseContainer<?> createContainer(DockerImageName dockerImageName);

    abstract String getDockerImage();

    public abstract Class<? extends JdbcAdapter> getAdapterClass();

    public JdbcDatabaseContainer<?> startContainer(String version) {
        DockerImageName dockerImageName = DockerImageName.parse(getDockerImage());
        if(version != null) {
            dockerImageName = dockerImageName.withTag(version);
        }
        JdbcDatabaseContainer<?> container = createContainer(dockerImageName);
        container.start();
        return container;
    }

}
