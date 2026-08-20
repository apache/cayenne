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
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.cayenne.gen.CgenConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CgenSaverDelegateTest {

    private static URL baseURL(String path) throws Exception {
        return Paths.get(path).toAbsolutePath().toUri().toURL();
    }

    private static Path absolute(String path) {
        return Paths.get(path).toAbsolutePath();
    }

    /**
     * A configuration anchored somewhere else but with no output directory of its own: rebasing keeps it
     * generating into the directory it already resolved to, {@code /tmp/src/main/java}. It used to silently
     * jump to the new base instead, which is what the "do we care about this case?" note on this test was
     * about; it now behaves the same way as {@link #existingRootAndRelPath()}.
     */
    @Test
    public void existingRootOverride() throws Exception {
        CgenConfiguration config = new CgenConfiguration();
        config.setRootPath(absolute("/tmp/src/main/java"));

        CgenSaverDelegate.resolveOutputDir(baseURL("/tmp/src/main/resources"), config);

        assertEquals(absolute("/tmp/src/main/resources"), config.getRootPath());
        assertEquals(absolute("/tmp/src/main/java"), config.requireOutputDirectory());
    }

    @Test
    public void existingRootAndRelPath() throws Exception {
        CgenConfiguration config = new CgenConfiguration();
        config.setRootPath(absolute("/tmp/src/main/java"));
        config.setOutputDir(Paths.get(""));

        CgenSaverDelegate.resolveOutputDir(baseURL("/tmp/src/main/resources"), config);

        assertEquals(absolute("/tmp/src/main/resources"), config.getRootPath());
        assertEquals(absolute("/tmp/src/main/java"), config.requireOutputDirectory());
    }

    @Test
    public void emptyRootInMavenTree() throws Exception {
        CgenConfiguration config = new CgenConfiguration();

        CgenSaverDelegate.resolveOutputDir(baseURL("/tmp/src/main/resources"), config);

        assertEquals(absolute("/tmp/src/main/resources"), config.getRootPath());
        assertEquals(absolute("/tmp/src/main/java"), config.requireOutputDirectory());
    }

    @Test
    public void emptyRoot() throws Exception {
        CgenConfiguration config = new CgenConfiguration();

        CgenSaverDelegate.resolveOutputDir(baseURL("/tmp/somefolder"), config);

        assertEquals(absolute("/tmp/somefolder"), config.getRootPath());
        assertEquals(absolute("/tmp/somefolder"), config.requireOutputDirectory());
    }

    /**
     * An output directory the user picked explicitly stays pointing at the same physical directory when the
     * project is saved somewhere else.
     */
    @Test
    public void absoluteOutputDirSurvivesRebase() throws Exception {
        CgenConfiguration config = new CgenConfiguration();
        config.setRootPath(absolute("/tmp/project"));
        config.setOutputDir(absolute("/tmp/generated"));

        CgenSaverDelegate.resolveOutputDir(baseURL("/tmp/other/project"), config);

        assertEquals(absolute("/tmp/other/project"), config.getRootPath());
        assertEquals(absolute("/tmp/generated"), config.requireOutputDirectory());
    }
}
