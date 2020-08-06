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

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

import java.io.File;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @since 4.0
 */
public class CgenTaskIT extends BaseTaskIT {

    @Test
    public void classGeneratingWithDefaultConfigSuccess() throws Exception {

        GradleRunner runner = createRunner(
                "cgen_default_config",
                "cgen",
                "-PdataMap=test_datamap.map.xml"
        );

        BuildResult result = runner.forwardOutput().build();

        String generatedDirectoryPath = projectDir.getAbsolutePath() + "/src/main/java/org/example/cayenne/persistent/";

        String generatedClassPath = generatedDirectoryPath + "City.java";
        String generatedParentClassPath = generatedDirectoryPath + "auto/_City.java";
        File generatedClass = new File(generatedClassPath);
        File generatedParentClass = new File(generatedParentClassPath);

        assertTrue(generatedClass.exists());
        assertTrue(generatedParentClass.exists());
        assertEquals(TaskOutcome.SUCCESS, result.task(":cgen").getOutcome());
    }

    @Test
    public void classGeneratingWithCustomConfigSuccess() throws Exception {

        GradleRunner runner = createRunner(
                "cgen_custom_config",
                "cgen",
                "-PdataMap=test_datamap.map.xml"
        );
        BuildResult result = runner.build();

        String generatedDirectoryPath = projectDir.getAbsolutePath() + "/customDirectory/org/example/cayenne/persistent/";

        String generatedClassPath = generatedDirectoryPath + "City.groovy";
        String excludedClassPath = generatedDirectoryPath + "Artist.groovy";
        String generatedParentClassPath = generatedDirectoryPath + "auto/_City.groovy";
        String excludedParentClassPath = generatedDirectoryPath + "auto/_Artist.groovy";

        File generatedClass = new File(generatedClassPath);
        File excludedClass = new File(excludedClassPath);
        File generatedParentClass = new File(generatedParentClassPath);
        File excludedParentClass = new File(excludedParentClassPath);

        assertTrue(generatedClass.exists());
        assertTrue(!excludedClass.exists());
        assertTrue(!excludedParentClass.exists());
        assertTrue(!generatedParentClass.exists());
        assertEquals(TaskOutcome.SUCCESS, result.task(":cgen").getOutcome());
    }

    @Test
    public void cgenWithConfigInDm() throws Exception {
        GradleRunner runner = createRunner(
                "cgen_with_config",
                "cgen",
                "-PdataMap=cgen_with_config.map.xml"
        );

        BuildResult result = runner.forwardOutput().build();

        String generatedDirectoryPath = projectDir.getAbsolutePath() + "/customDirectory1/";

        String generatedClassPath = generatedDirectoryPath + "ObjEntity1.txt";
        String datamap = generatedDirectoryPath + "CgenMap.txt";
        String notIncludedEntity = generatedDirectoryPath + "ObjEntity.txt";
        String notIncludedEmbeddable = generatedDirectoryPath + "Embeddable.txt";

        Path generatedClass = Paths.get(generatedClassPath);
        Path generatedDataMap = Paths.get(datamap);
        Path generatedNotIncludedEntity = Paths.get(notIncludedEntity);
        Path generatedNotIncludedEmbeddable = Paths.get(notIncludedEmbeddable);

        assertTrue(Files.exists(generatedClass));
        assertFalse(Files.exists(generatedDataMap));
        assertFalse(Files.exists(generatedNotIncludedEmbeddable));
        assertFalse(Files.exists(generatedNotIncludedEntity));
        assertEquals(TaskOutcome.SUCCESS, result.task(":cgen").getOutcome());
    }

    @Test
    public void testWithConfigsInDmAndPom() throws Exception {
        GradleRunner runner = createRunner(
                "cgen_with_configs",
                "cgen",
                "-PdataMap=cgenMap.map.xml"
        );

        BuildResult result = runner.forwardOutput().build();

        String generatedDirectoryPath = projectDir.getAbsolutePath() + "/customDirectory/";

        String generatedClassPath = generatedDirectoryPath + "ObjEntity.groovy";
        Path generatedClass = Paths.get(generatedClassPath);
        assertTrue(Files.exists(generatedClass));

        String notIncludedEntity = generatedDirectoryPath + "ObjEntity1.groovy";
        Path generatedNotIncludedEntity = Paths.get(notIncludedEntity);
        assertFalse(Files.exists(generatedNotIncludedEntity));

        String includedDataMap = generatedDirectoryPath + "CgenMap.groovy";
        Path generatedIncludedDataMap = Paths.get(includedDataMap);
        assertTrue(Files.exists(generatedIncludedDataMap));

        assertEquals(TaskOutcome.SUCCESS, result.task(":cgen").getOutcome());
    }

    @Test
    public void testReplaceDatamapMode() throws Exception {
        GradleRunner runner = createRunner(
                "cgen_replaceDatamapMode",
                "cgen",
                "-PdataMap=cgenMap.map.xml"
        );

        BuildResult result = runner.forwardOutput().build();

        String generatedDirectoryPath = projectDir.getAbsolutePath() + "/customDirectory/";

        String notIncludedEntity = generatedDirectoryPath + "ObjEntity.txt";
        Path generatedNotIncludedEntity = Paths.get(notIncludedEntity);
        assertFalse(Files.exists(generatedNotIncludedEntity));

        String notIncludedEntity1 = generatedDirectoryPath + "ObjEntity1.txt";
        Path generatedNotIncludedEntity1 = Paths.get(notIncludedEntity1);
        assertFalse(Files.exists(generatedNotIncludedEntity1));

        String notIncludedEmbeddable = generatedDirectoryPath + "Embeddable.txt";
        Path generatedNotIncludedEmbeddable = Paths.get(notIncludedEmbeddable);
        assertFalse(Files.exists(generatedNotIncludedEmbeddable));

        String includedDataMap = generatedDirectoryPath + "CgenMap.txt";
        Path generatedIncludedDataMap = Paths.get(includedDataMap);
        assertTrue(Files.exists(generatedIncludedDataMap));

        String includedSuperDataMap = generatedDirectoryPath + "auto/_CgenMap.txt";
        Path generatedIncludedSuperDataMap = Paths.get(includedSuperDataMap);
        assertTrue(Files.exists(generatedIncludedSuperDataMap));

        assertEquals(TaskOutcome.SUCCESS, result.task(":cgen").getOutcome());
    }
}