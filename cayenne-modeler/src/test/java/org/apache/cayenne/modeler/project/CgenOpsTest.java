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

package org.apache.cayenne.modeler.project;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.CgenConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CgenOpsTest {

    @Test
    public void uniqueConfigNameForEmptyList() {
        assertEquals("Default", CgenOps.createUniqueConfigName(new CgenConfigList()));
    }

    @Test
    public void uniqueConfigNameSkipsExisting() {
        CgenConfigList configurations = new CgenConfigList();
        configurations.add(configuration("Default"));
        assertEquals("Default1", CgenOps.createUniqueConfigName(configurations));

        configurations.add(configuration("Default1"));
        assertEquals("Default2", CgenOps.createUniqueConfigName(configurations));
    }

    @Test
    public void uniqueConfigNameIgnoresUnrelatedNames() {
        CgenConfigList configurations = new CgenConfigList();
        configurations.add(configuration("client"));
        assertEquals("Default", CgenOps.createUniqueConfigName(configurations));
    }

    @Test
    public void uniqueConfigNameGivesUp() {
        CgenConfigList configurations = new CgenConfigList();
        configurations.add(configuration(CgenConfigList.DEFAULT_CONFIG_NAME));
        for (int i = 1; i <= CgenOps.MAX_NAME_ATTEMPTS; i++) {
            configurations.add(configuration(CgenConfigList.DEFAULT_CONFIG_NAME + i));
        }

        assertThrows(CayenneRuntimeException.class, () -> CgenOps.createUniqueConfigName(configurations));
    }

    private CgenConfiguration configuration(String name) {
        CgenConfiguration configuration = new CgenConfiguration();
        configuration.setName(name);
        return configuration;
    }
}
