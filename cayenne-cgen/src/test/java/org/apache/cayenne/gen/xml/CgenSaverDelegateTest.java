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

package org.apache.cayenne.gen.xml;

import java.net.URL;
import java.nio.file.Paths;

import org.apache.cayenne.gen.CgenConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @since 4.2
 */
public class CgenSaverDelegateTest {

    @Test
    public void existingRootOverride() throws Exception {
        CgenConfiguration config = new CgenConfiguration();

        config.setRootPath(Paths.get("/tmp/src/main/java").toAbsolutePath());
        URL baseURL = Paths.get("/tmp/src/main/resources").toUri().toURL();

        CgenSaverDelegate.resolveOutputDir(baseURL, config);

        assertEquals(Paths.get("/tmp/src/main/resources").toAbsolutePath(), config.getRootPath());
        assertEquals(Paths.get(""), config.getRawOutputPath()); // TODO: do we care about this case?
    }

    @Test
    public void existingRootAndRelPath() throws Exception {
        CgenConfiguration config = new CgenConfiguration();

        config.setRootPath(Paths.get("/tmp/src/main/java").toAbsolutePath());
        config.updateOutputPath(Paths.get(""));

        URL baseURL = Paths.get("/tmp/src/main/resources").toUri().toURL();

        CgenSaverDelegate.resolveOutputDir(baseURL, config);

        assertEquals(Paths.get("/tmp/src/main/resources").toAbsolutePath(), config.getRootPath());
        assertEquals(Paths.get("../java"), config.getRawOutputPath());
    }

    @Test
    public void emptyRootInMavenTree() throws Exception {
        CgenConfiguration config = new CgenConfiguration();

        URL baseURL = Paths.get("/tmp/src/main/resources").toUri().toURL();

        CgenSaverDelegate.resolveOutputDir(baseURL, config);

        assertEquals(Paths.get("/tmp/src/main/resources").toAbsolutePath(), config.getRootPath());
        assertEquals(Paths.get("../java"), config.getRawOutputPath());
    }

    @Test
    public void emptyRoot() throws Exception {
        CgenConfiguration config = new CgenConfiguration();

        URL baseURL = Paths.get("/tmp/somefolder").toUri().toURL();

        CgenSaverDelegate.resolveOutputDir(baseURL, config);

        assertEquals(Paths.get("/tmp/somefolder").toAbsolutePath(), config.getRootPath());
        assertEquals(Paths.get(""), config.getRawOutputPath());
    }
}
