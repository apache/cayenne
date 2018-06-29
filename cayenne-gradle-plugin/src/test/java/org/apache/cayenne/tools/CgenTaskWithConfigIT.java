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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @since 4.1
 */
public class CgenTaskWithConfigIT extends BaseTaskIT{

    @Test
    public void cgenWithConfig() throws Exception {
        GradleRunner runner = createRunner(
                "cgen_with_config",
                "cgen",
                "-PdataMap=" + URLDecoder.decode(getClass().getResource("cgenMap.map.xml").getFile(), "UTF-8")
        );

        BuildResult result = runner.forwardOutput().build();

        String generatedDirectoryPath = projectDir.getAbsolutePath() + "/customDirectory/";

        String generatedClassPath = generatedDirectoryPath + "ObjEntity1.txt";
        String datamap = generatedDirectoryPath + "TestCgenMap.txt";
        String notIncludedEntity = generatedDirectoryPath + "ObjEntity.txt";
        String notIncludedSuperDatamap = generatedDirectoryPath + "_TestCgenMap.txt";

        File notIncludeSuperDatamap = new File("_TestCgenMap.txt");
        assertFalse(notIncludeSuperDatamap.exists());

        File generatedClass = new File(generatedClassPath);
        File generatedDatamap = new File(datamap);
        File generatedNotIncludedEntity = new File(notIncludedEntity);
        File generatedNotIncludedSuperDatamap = new File(notIncludedSuperDatamap);

        assertTrue(generatedClass.exists());
        assertFalse(generatedDatamap.exists());
        assertFalse(generatedNotIncludedEntity.exists());
        assertFalse(generatedNotIncludedSuperDatamap.exists());
    }

}
