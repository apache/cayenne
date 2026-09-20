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
package org.apache.cayenne.docs;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StartingTest {

    @Test
    public void startAndStop() {
        // tag::start[]
        CayenneRuntime runtime = CayenneRuntime.of()
                .addConfig("com/example/cayenne-project.xml")
                .build();
        // end::start[]

        assertNotNull(runtime.getDataDomain());

        // tag::stop[]
        runtime.shutdown();
        // end::stop[]
    }

    @Test
    public void externalTransactions() {
        // tag::externalTransactions[]
        Module extensions = binder ->
                CoreModule.extend(binder).setProperty(Constants.EXTERNAL_TX_PROPERTY, "true");

        CayenneRuntime runtime = CayenneRuntime.of()
                .addConfig("com/example/cayenne-project.xml")
                .addModule(extensions)
                .build();
        // end::externalTransactions[]

        assertNotNull(runtime.getDataDomain());
        runtime.shutdown();
    }

    @Test
    public void multipleProjects() {
        // tag::multipleProjects[]
        CayenneRuntime runtime = CayenneRuntime.of()
                .addConfig("com/example/cayenne-project.xml")
                .addConfig("org/foo/cayenne-library1.xml")
                .addConfig("org/foo/cayenne-library2.xml")
                .build();
        // end::multipleProjects[]

        assertNotNull(runtime.getDataDomain());
        runtime.shutdown();
    }
}
