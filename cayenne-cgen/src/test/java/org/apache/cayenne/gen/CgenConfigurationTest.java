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

package org.apache.cayenne.gen;

import org.apache.cayenne.validation.ValidationException;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class CgenConfigurationTest {


    public static class СgenWindowsConfigurationTest {

        CgenConfiguration configuration;

        @Before
        public void setUp() {
            configuration = new CgenConfiguration(false);
        }

        @Before
        public void checkPlatform() {
            Assume.assumeTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win"));
        }

        @Test
        public void equalRootsEqualDirectories() {
            configuration.setRootPath(Paths.get("\\test1\\test2\\test3"));
            Path relPath = Paths.get("\\test1\\test2\\test3");
            configuration.setRelPath(relPath.toString());

            assertEquals(Paths.get(""), configuration.getRelPath());
            assertEquals(relPath, configuration.buildPath());
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("\\test1\\test2\\test3"));
            Path relPath = Paths.get("\\test1\\test2\\testAnother");
            configuration.setRelPath(relPath.toString());

            assertEquals(Paths.get("..\\testAnother"), configuration.getRelPath());
            assertEquals(relPath, configuration.buildPath());
        }

        @Test
        public void equalRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("\\"));
            Path relPath = Paths.get("\\");
            configuration.setRelPath(relPath.toString());

            assertEquals(Paths.get(""), configuration.getRelPath());
            assertEquals(relPath, configuration.buildPath());
        }

        @Test
        public void notEqualRootsEqualDirectories() {
            configuration.setRootPath(Paths.get("\\test1\\test2\\test3"));
            Path relPath = Paths.get("E:\\test1\\test2\\test3");
            configuration.setRelPath(relPath.toString());

            assertEquals(Paths.get("E:\\test1\\test2\\test3"), configuration.getRelPath());
            assertEquals(relPath, configuration.buildPath());
        }

        @Test
        public void notEqualRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("\\test1\\test2\\test3"));
            Path relPath = Paths.get("E:\\test1\\test2\\testAnother");
            configuration.setRelPath(relPath.toString());

            assertEquals(Paths.get("E:\\test1\\test2\\testAnother"), configuration.getRelPath());
            assertEquals(relPath, configuration.buildPath());
        }

        @Test
        public void notEqualRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("\\"));
            Path relPath = Paths.get("E:\\");
            configuration.setRelPath(relPath.toString());

            assertEquals(Paths.get("E:\\"), configuration.getRelPath());
            assertEquals(relPath, configuration.buildPath());
        }

        @Test
        public void emptyRootNotEmptyRelPath() {
            configuration.setRootPath(Paths.get(""));
            Path relPath = Paths.get("E:\\");
            configuration.setRelPath(relPath.toString());

            assertEquals(Paths.get("E:\\"), configuration.getRelPath());
            assertEquals(relPath, configuration.buildPath());
        }

        @Test(expected = ValidationException.class)
        public void notEmptyRootEmptyRelPath() {
            configuration.setRootPath(Paths.get("E:\\"));
            Path relPath = Paths.get("");
            configuration.setRelPath(relPath.toString());
        }

        @Test(expected = InvalidPathException.class)
        public void invalidRelPath() {
            configuration.setRootPath(Paths.get("\\test1\\test2\\test3"));
            configuration.setRelPath("invalidRoot:\\test");
        }
    }

    public static class СgenUnixConfigurationTest {

        CgenConfiguration configuration;

        @Before
        public void setUp() {
            configuration = new CgenConfiguration(false);
        }

        @Before
        public void checkPlatform() {
            Assume.assumeFalse(System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win"));
        }

        @Test
        public void equalRootsEqualDirectories() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            Path relPath = Paths.get("/test1/test2/test3");
            configuration.setRelPath(relPath.toString());

            assertEquals(Paths.get(""), configuration.getRelPath());
            assertEquals(relPath, configuration.buildPath());
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            Path relPath = Paths.get("/test1/test2/testAnother");
            configuration.setRelPath(relPath.toString());

            assertEquals(Paths.get("../testAnother"), configuration.getRelPath());
            assertEquals(relPath, configuration.buildPath());
        }

        @Test
        public void equalRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("/"));
            Path relPath = Paths.get("/");
            configuration.setRelPath(relPath.toString());

            assertEquals(Paths.get(""), configuration.getRelPath());
            assertEquals(relPath, configuration.buildPath());
        }

        @Test
        public void emptyRootNotEmptyRelPath() {
            configuration.setRootPath(Paths.get(""));
            Path relPath = Paths.get("/");
            configuration.setRelPath(relPath.toString());

            assertEquals(Paths.get("/"), configuration.getRelPath());
            assertEquals(relPath, configuration.buildPath());
        }

        @Test(expected = ValidationException.class)
        public void notEmptyRootEmptyRelPath() {
            configuration.setRootPath(Paths.get("/"));
            Path relPath = Paths.get("");
            configuration.setRelPath(relPath.toString());
        }

        @Test(expected = ValidationException.class)
        public void invalidRelPath() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            configuration.setRelPath("invalidRoot:/test");
        }
    }

}