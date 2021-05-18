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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class CgenSaverDelegateTest {

    @Test
    public void testExistingRootOverride() throws Exception {
        CgenConfiguration config = new CgenConfiguration(false);

        config.setRootPath(Paths.get("/tmp/src/main/java").toAbsolutePath());
        URL baseURL = Paths.get("/tmp/src/main/resources").toUri().toURL();

        CgenSaverDelegate.resolveOutputDir(baseURL, config);

        assertEquals(Paths.get("/tmp/src/main/resources").toAbsolutePath(), config.getRootPath());
        assertEquals(Paths.get("../java"), config.getRelPath());
    }

    @Test
    public void testExistingRootAndRelPath() throws Exception {
        CgenConfiguration config = new CgenConfiguration(false);

        config.setRootPath(Paths.get("/tmp/src/main/java").toAbsolutePath());
        config.setRelPath(Paths.get(""));

        URL baseURL = Paths.get("/tmp/src/main/resources").toUri().toURL();

        CgenSaverDelegate.resolveOutputDir(baseURL, config);

        assertEquals(Paths.get("/tmp/src/main/resources").toAbsolutePath(), config.getRootPath());
        assertEquals(Paths.get("../java"), config.getRelPath());
    }

    @Test
    public void testEmptyRoot() throws Exception {
        CgenConfiguration config = new CgenConfiguration(false);

        URL baseURL = Paths.get("/tmp/src/main/resources").toUri().toURL();

        CgenSaverDelegate.resolveOutputDir(baseURL, config);

        assertEquals(Paths.get("/tmp/src/main/resources").toAbsolutePath(), config.getRootPath());
        assertEquals(Paths.get(""), config.getRelPath());
    }






}