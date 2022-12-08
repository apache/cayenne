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

    public static class CgenWindowsConfigurationTest {

        CgenConfiguration configuration;

        @Before
        public void setUp() {
            configuration = new CgenConfiguration();
        }

        @Before
        public void checkPlatform() {
            Assume.assumeTrue(System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win"));
        }

        @Test
        public void equalRootsEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path relPath = Paths.get("C:\\test1\\test2\\test3");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get(""), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path relPath = Paths.get("C:\\test1\\test2\\testAnother");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("..\\testAnother"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void equalRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("C:\\"));
            Path relPath = Paths.get("C:\\");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get(""), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void notEqualRootsEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path relPath = Paths.get("E:\\test1\\test2\\test3");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("E:\\test1\\test2\\test3"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void notEqualRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("C:\\test1\\test2\\test3"));
            Path relPath = Paths.get("E:\\test1\\test2\\testAnother");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("E:\\test1\\test2\\testAnother"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void notEqualRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("C:\\"));
            Path relPath = Paths.get("E:\\");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("E:\\"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test(expected = ValidationException.class)
        public void emptyRootNotEmptyRelPath() {
            configuration.setRootPath(Paths.get(""));
            Path relPath = Paths.get("E:\\");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("E:\\"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void notEmptyRootEmptyRelPath() {
            configuration.setRootPath(Paths.get("E:\\"));
            Path relPath = Paths.get("");

            configuration.updateOutputPath(relPath);

            assertEquals(relPath, configuration.getRawOutputPath());
            assertEquals(Paths.get("E:\\"), configuration.buildOutputPath());
        }

        @Test(expected = InvalidPathException.class)
        public void invalidRootPath() {
            configuration.setRootPath(Paths.get("invalidRoot:\\test"));
        }

        @Test
        public void nullRootPath() {
            configuration.updateOutputPath(Path.of("C:\\test1\\test2\\test3"));
            assertEquals(Paths.get("C:\\test1\\test2\\test3"), configuration.getRawOutputPath());
            assertEquals(Paths.get("C:\\test1\\test2\\test3"), configuration.buildOutputPath());
        }
    }

    public static class CgenUnixConfigurationTest {

        CgenConfiguration configuration;

        @Before
        public void setUp() {
            configuration = new CgenConfiguration();
        }

        @Before
        public void checkPlatform() {
            Assume.assumeFalse(System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("win"));
        }

        @Test
        public void equalRootsEqualDirectories() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            Path relPath = Paths.get("/test1/test2/test3");
            configuration.updateOutputPath(relPath);


            assertEquals(Paths.get(""), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void equalRootsNotEqualDirectories() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            Path relPath = Paths.get("/test1/test2/testAnother");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("../testAnother"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void equalRootsEmptyDirectories() {
            configuration.setRootPath(Paths.get("/"));
            Path relPath = Paths.get("/");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get(""), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void concatCorrectRootPathAndRelPath() {
            configuration.setRootPath(Paths.get("/test1/test2/test3"));
            Path relPath = Paths.get("test1/test2/test3");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("test1/test2/test3"), configuration.getRawOutputPath());
            assertEquals(Paths.get("/test1/test2/test3/test1/test2/test3"), configuration.buildOutputPath());
        }

        @Test(expected = ValidationException.class)
        public void emptyRootNotEmptyRelPath() {
            configuration.setRootPath(Paths.get(""));
            Path relPath = Paths.get("/");
            configuration.updateOutputPath(relPath);

            assertEquals(Paths.get("/"), configuration.getRawOutputPath());
            assertEquals(relPath, configuration.buildOutputPath());
        }

        @Test
        public void notEmptyRootEmptyRelPath() {
            configuration.setRootPath(Paths.get("/"));
            configuration.updateOutputPath(Paths.get(""));

            assertEquals(Paths.get(""), configuration.getRawOutputPath());
            assertEquals(Paths.get("/"), configuration.buildOutputPath());
        }

        @Test(expected = ValidationException.class)
        public void invalidRootPath() {
            configuration.setRootPath(Paths.get("invalidRoot:/test"));
            configuration.updateOutputPath(Paths.get("/test1/test2/test3"));
        }

        @Test(expected = ValidationException.class)
        public void concatInvalidRootPathAndRelPath() {
            configuration.setRootPath(Paths.get("invalidRoot:/test"));
            configuration.updateOutputPath(Paths.get("test1/test2/test3"));
        }

        @Test
        public void nullRootPath() {
            configuration.updateOutputPath(Paths.get("/test1/test2/test3"));
            assertEquals(Paths.get("/test1/test2/test3"), configuration.getRawOutputPath());
            assertEquals(Paths.get("/test1/test2/test3"), configuration.buildOutputPath());
        }
    }

}