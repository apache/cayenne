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
package org.apache.cayenne.tools;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the layout and the content of the files produced by the "cgen" goal with the default templates.
 */
@MojoTest
public class CayenneGeneratorMojoOutputTest {

    private static final String POM_DIR = "src/test/resources/cgen/output/";

    private Path testDir;

    private void run(CayenneGeneratorMojo mojo, String testName) throws Exception {
        assertNotNull(mojo);
        mojo.execute();

        testDir = Path.of("target/generated-tests", testName);
        assertTrue(Files.isDirectory(testDir), "Not a directory: " + testDir.toAbsolutePath());
    }

    /**
     * Test single classes with a non-standard template.
     */
    @Test
    public void singleClassesCustTemplate(
            @InjectMojo(goal = "cgen", pom = POM_DIR + "single-classes-cust-template-pom.xml")
            CayenneGeneratorMojo mojo) throws Exception {
        run(mojo, "single-classes-cust-template");

        assertContents("org/apache/cayenne/testdo/testmap/Artist.java", "Artist", "org.apache.cayenne.testdo.testmap",
                "PersistentObject");
        assertNotExists("org/apache/cayenne/testdo/testmap/_Artist.java");
    }

    /** Test single classes generation including full package path. */
    @Test
    public void singleClasses1(@InjectMojo(goal = "cgen", pom = POM_DIR + "single-classes1-pom.xml")
                               CayenneGeneratorMojo mojo) throws Exception {
        run(mojo, "single-classes1");

        assertContents("org/apache/cayenne/testdo/testmap/Artist.java", "Artist", "org.apache.cayenne.testdo.testmap",
                "PersistentObject");
        assertNotExists("org/apache/cayenne/testdo/testmap/_Artist.java");
    }

    /** Test single classes generation ignoring package path. */
    @Test
    public void singleClasses2(@InjectMojo(goal = "cgen", pom = POM_DIR + "single-classes2-pom.xml")
                               CayenneGeneratorMojo mojo) throws Exception {
        run(mojo, "single-classes2");

        assertContents("Artist.java", "Artist", "org.apache.cayenne.testdo.testmap", "PersistentObject");
        assertNotExists("_Artist.java");
        assertNotExists("org/apache/cayenne/testdo/testmap/Artist.java");
    }

    /** Test pairs generation including full package path. */
    @Test
    public void pairs1(@InjectMojo(goal = "cgen", pom = POM_DIR + "pairs1-pom.xml")
                       CayenneGeneratorMojo mojo) throws Exception {
        run(mojo, "pairs1");

        assertContents("org/apache/cayenne/testdo/testmap/Artist.java", "Artist", "org.apache.cayenne.testdo.testmap",
                "_Artist");
        assertContents("org/apache/cayenne/testdo/testmap/auto/_Artist.java", "_Artist",
                "org.apache.cayenne.testdo.testmap.auto", "PersistentObject");
    }

    /** Test pairs generation in the same directory. */
    @Test
    public void pairs2(@InjectMojo(goal = "cgen", pom = POM_DIR + "pairs2-pom.xml")
                       CayenneGeneratorMojo mojo) throws Exception {
        run(mojo, "pairs2");

        assertContents("Artist.java", "Artist", "org.apache.cayenne.testdo.testmap", "_Artist");
        assertContents("_Artist.java", "_Artist", "org.apache.cayenne.testdo.testmap", "PersistentObject");
        assertNotExists("org/apache/cayenne/testdo/testmap/Artist.java");
    }

    /**
     * Test pairs generation including full package path with superclass and subclass in different packages.
     */
    @Test
    public void pairs3(@InjectMojo(goal = "cgen", pom = POM_DIR + "pairs3-pom.xml")
                       CayenneGeneratorMojo mojo) throws Exception {
        run(mojo, "pairs3");

        assertContents("org/apache/cayenne/testdo/testmap/Artist.java", "Artist", "org.apache.cayenne.testdo.testmap",
                "_Artist");
        assertContents("org/apache/cayenne/testdo/testmap/superart/_Artist.java", "_Artist",
                "org.apache.cayenne.testdo.testmap.superart", "PersistentObject");
    }

    @Test
    public void pairsEmbeddable3(@InjectMojo(goal = "cgen", pom = POM_DIR + "pairs-embeddables3-pom.xml")
                                 CayenneGeneratorMojo mojo) throws Exception {
        run(mojo, "pairs-embeddables3");

        assertContents("org/apache/cayenne/testdo/embeddable/EmbedEntity1.java", "EmbedEntity1",
                "org.apache.cayenne.testdo.embeddable", "_EmbedEntity1");
        assertContents("org/apache/cayenne/testdo/embeddable/auto/_EmbedEntity1.java", "_EmbedEntity1",
                "org.apache.cayenne.testdo.embeddable.auto", "PersistentObject");
        assertContents("org/apache/cayenne/testdo/embeddable/Embeddable1.java", "Embeddable1",
                "org.apache.cayenne.testdo.embeddable", "_Embeddable1");
        assertContents("org/apache/cayenne/testdo/embeddable/auto/_Embeddable1.java", "_Embeddable1",
                "org.apache.cayenne.testdo.embeddable.auto", null);
    }

    private void assertContents(String filePath, String className, String packageName, String extendsName)
            throws Exception {

        Path file = testDir.resolve(filePath);
        assertTrue(Files.isRegularFile(file), "Not a file: " + file.toAbsolutePath());

        try (BufferedReader in = Files.newBufferedReader(file)) {
            assertPackage(in, packageName);
            assertClass(in, className, extendsName);
        }
    }

    private void assertNotExists(String filePath) {
        Path file = testDir.resolve(filePath);
        assertFalse(Files.exists(file), "Unexpected file: " + file.toAbsolutePath());
    }

    private void assertPackage(BufferedReader in, String packageName) throws Exception {

        String s;
        while ((s = in.readLine()) != null) {
            if (Pattern.matches("^package\\s+([^\\s;]+);", s)) {
                assertTrue(s.indexOf(packageName) > 0);
                return;
            }
        }

        fail("No package declaration found.");
    }

    private void assertClass(BufferedReader in, String className, String extendsName) throws Exception {

        Pattern classPattern = Pattern.compile("^public\\s+");

        String s;
        while ((s = in.readLine()) != null) {
            if (classPattern.matcher(s).find()) {
                assertTrue(s.indexOf(className) > 0);
                if (extendsName != null) {
                    assertTrue(s.indexOf(extendsName) > 0);
                    assertTrue(s.indexOf(className) < s.indexOf(extendsName));
                }
                return;
            }
        }

        fail("No class declaration found.");
    }
}
