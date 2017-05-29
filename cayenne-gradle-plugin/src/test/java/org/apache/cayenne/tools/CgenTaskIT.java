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

package org.apache.cayenne.tools;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;


import java.io.File;

import static org.junit.Assert.assertEquals;
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
                "-PdataMap=" + getClass().getResource("test_datamap.map.xml").getFile()
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
                "-PdataMap=" + getClass().getResource("test_datamap.map.xml").getFile()
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

}